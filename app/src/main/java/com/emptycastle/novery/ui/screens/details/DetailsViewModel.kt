// com/emptycastle/novery/ui/screens/details/DetailsViewModel.kt
package com.emptycastle.novery.ui.screens.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.service.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsViewModel : ViewModel() {

    // ================================================================
    // REPOSITORIES
    // ================================================================

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    // ================================================================
    // STATE
    // ================================================================

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    val downloadState: StateFlow<DownloadState> = DownloadServiceManager.downloadState

    private var currentProvider: MainProvider? = null
    private var currentNovelUrl: String? = null

    // ================================================================
    // INITIALIZATION
    // ================================================================

    init {
        val savedSortDescending = preferencesManager.getChapterSortDescending()
        _uiState.update { it.copy(isChapterSortDescending = savedSortDescending) }
    }

    // ================================================================
    // FILTERED CHAPTERS COMPUTATION
    // ================================================================

    private fun recomputeFilteredChapters() {
        val state = _uiState.value
        var chapters = state.novelDetails?.chapters ?: emptyList()

        // Apply filter
        chapters = when (state.chapterFilter) {
            ChapterFilter.ALL -> chapters
            ChapterFilter.UNREAD -> chapters.filter { !state.readChapters.contains(it.url) }
            ChapterFilter.DOWNLOADED -> chapters.filter { state.downloadedChapters.contains(it.url) }
            ChapterFilter.NOT_DOWNLOADED -> chapters.filter { !state.downloadedChapters.contains(it.url) }
        }

        // Apply search
        if (state.chapterSearchQuery.isNotBlank()) {
            chapters = chapters.filter {
                it.name.contains(state.chapterSearchQuery, ignoreCase = true)
            }
        }

        // Apply sort
        val sortedChapters = if (state.isChapterSortDescending) {
            chapters.reversed()
        } else {
            chapters
        }

        _uiState.update {
            it.copy(
                filteredChapters = sortedChapters,
                filterVersion = it.filterVersion + 1
            )
        }
    }

    // ================================================================
    // NOVEL LOADING
    // ================================================================

    fun loadNovel(novelUrl: String, providerName: String, forceRefresh: Boolean = false) {
        currentNovelUrl = novelUrl
        currentProvider = novelRepository.getProvider(providerName)

        val provider = currentProvider ?: run {
            _uiState.update { it.copy(error = "Provider not found", isLoading = false) }
            return
        }

        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val result = novelRepository.loadNovelDetails(provider, novelUrl, forceRefresh)

            result.fold(
                onSuccess = { details ->
                    val hasReviewsSupport = novelRepository.providerHasReviews(providerName)

                    _uiState.update {
                        it.copy(
                            novelDetails = details,
                            isLoading = false,
                            isRefreshing = false,
                            relatedNovels = details.relatedNovels ?: emptyList(),
                            hasReviewsSupport = hasReviewsSupport
                        )
                    }
                    recomputeFilteredChapters()
                    loadLibraryStatus(novelUrl)
                    observeChapterStatus(novelUrl)

                    // Load reviews if supported
                    if (hasReviewsSupport) {
                        loadReviews(reset = true)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to load novel",
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        val novelUrl = currentNovelUrl ?: return
        val providerName = currentProvider?.name ?: return
        loadNovel(novelUrl, providerName, forceRefresh = true)
    }

    private fun loadLibraryStatus(novelUrl: String) {
        viewModelScope.launch {
            val isFavorite = libraryRepository.isFavorite(novelUrl)
            val entry = libraryRepository.getEntry(novelUrl)
            val readingPosition = libraryRepository.getReadingPosition(novelUrl)
            val historyEntry = historyRepository.getLastRead(novelUrl)

            val hasStarted = readingPosition != null || historyEntry != null
            val lastChapterUrl = readingPosition?.chapterUrl ?: historyEntry?.chapterUrl
            val lastChapterName = readingPosition?.chapterName ?: historyEntry?.chapterName

            val chapters = _uiState.value.novelDetails?.chapters ?: emptyList()
            val lastReadIndex = if (lastChapterUrl != null) {
                chapters.indexOfFirst { it.url == lastChapterUrl }
            } else -1

            _uiState.update {
                it.copy(
                    isFavorite = isFavorite,
                    readingStatus = entry?.getStatus() ?: ReadingStatus.READING,
                    hasStartedReading = hasStarted,
                    lastReadChapterUrl = lastChapterUrl,
                    lastReadChapterName = lastChapterName,
                    lastReadChapterIndex = lastReadIndex
                )
            }
        }
    }

    private fun observeChapterStatus(novelUrl: String) {
        viewModelScope.launch {
            offlineRepository.observeDownloadedChapters(novelUrl).collect { downloaded ->
                _uiState.update { it.copy(downloadedChapters = downloaded) }
                recomputeFilteredChapters()
            }
        }

        viewModelScope.launch {
            historyRepository.observeReadChapters(novelUrl).collect { read ->
                _uiState.update { it.copy(readChapters = read) }
                recomputeFilteredChapters()
            }
        }
    }

    // ================================================================
    // REVIEWS
    // ================================================================

    /**
     * Load reviews for the current novel
     */
    fun loadReviews(reset: Boolean = false) {
        val state = _uiState.value
        val provider = currentProvider ?: return
        val novelUrl = currentNovelUrl ?: return

        if (state.isLoadingReviews) return
        if (!reset && !state.hasMoreReviews) return

        val page = if (reset) 1 else state.reviewsPage

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingReviews = true,
                    reviews = if (reset) emptyList() else it.reviews
                )
            }

            novelRepository.loadReviews(
                provider = provider,
                novelUrl = novelUrl,
                page = page,
                showSpoilers = state.showSpoilers
            ).onSuccess { newReviews ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingReviews = false,
                        reviews = if (reset) newReviews else current.reviews + newReviews,
                        reviewsPage = page + 1,
                        hasMoreReviews = newReviews.isNotEmpty()
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoadingReviews = false,
                        hasMoreReviews = false
                    )
                }
            }
        }
    }

    /**
     * Toggle spoiler visibility and reload reviews
     */
    fun toggleSpoilers() {
        _uiState.update { it.copy(showSpoilers = !it.showSpoilers) }
        loadReviews(reset = true)
    }

    /**
     * Load more reviews (pagination)
     */
    fun loadMoreReviews() {
        loadReviews(reset = false)
    }

    // ================================================================
    // LIBRARY ACTIONS
    // ================================================================

    fun toggleFavorite() {
        val details = _uiState.value.novelDetails ?: return
        val provider = currentProvider ?: return

        viewModelScope.launch {
            val novel = Novel(
                name = details.name,
                url = details.url,
                posterUrl = details.posterUrl,
                apiName = provider.name
            )

            if (_uiState.value.isFavorite) {
                libraryRepository.removeFromLibrary(details.url)
                _uiState.update { it.copy(isFavorite = false) }
            } else {
                libraryRepository.addToLibraryWithDetails(
                    novel = novel,
                    details = details,
                    status = _uiState.value.readingStatus
                )
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }

    fun updateReadingStatus(status: ReadingStatus) {
        val novelUrl = currentNovelUrl ?: return

        viewModelScope.launch {
            libraryRepository.updateStatus(novelUrl, status)
            _uiState.update { it.copy(readingStatus = status, showStatusMenu = false) }
        }
    }

    // ================================================================
    // UI STATE TOGGLES
    // ================================================================

    fun showStatusMenu() = _uiState.update { it.copy(showStatusMenu = true) }
    fun hideStatusMenu() = _uiState.update { it.copy(showStatusMenu = false) }
    fun showCoverZoom() = _uiState.update { it.copy(showCoverZoom = true) }
    fun hideCoverZoom() = _uiState.update { it.copy(showCoverZoom = false) }
    fun showDownloadMenu() = _uiState.update { it.copy(showDownloadMenu = true) }
    fun hideDownloadMenu() = _uiState.update { it.copy(showDownloadMenu = false) }
    fun toggleSynopsis() = _uiState.update { it.copy(isSynopsisExpanded = !it.isSynopsisExpanded) }

    // ================================================================
    // READING POSITION
    // ================================================================

    fun getChapterToOpen(): String? {
        val state = _uiState.value
        val details = state.novelDetails ?: return null

        return if (state.hasStartedReading && state.lastReadChapterUrl != null) {
            state.lastReadChapterUrl
        } else {
            details.chapters.firstOrNull()?.url
        }
    }

    // ================================================================
    // CHAPTER SORTING & FILTERING
    // ================================================================

    fun toggleChapterSort() {
        val newValue = !_uiState.value.isChapterSortDescending
        _uiState.update { it.copy(isChapterSortDescending = newValue) }
        preferencesManager.setChapterSortDescending(newValue)
        recomputeFilteredChapters()
    }

    fun setChapterFilter(filter: ChapterFilter) {
        _uiState.update { it.copy(chapterFilter = filter) }
        recomputeFilteredChapters()
    }

    fun setChapterSearchQuery(query: String) {
        _uiState.update { it.copy(chapterSearchQuery = query) }
        recomputeFilteredChapters()
    }

    fun toggleSearch() {
        val wasActive = _uiState.value.isSearchActive
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                chapterSearchQuery = if (wasActive) "" else it.chapterSearchQuery
            )
        }
        if (wasActive) {
            recomputeFilteredChapters()
        }
    }

    fun getFilteredChapters(): List<Chapter> = _uiState.value.filteredChapters

    // ================================================================
    // CHAPTER READ STATUS
    // ================================================================

    fun markChapterAsRead(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        viewModelScope.launch {
            runCatching { historyRepository.markChapterRead(novelUrl, chapterUrl) }
        }
    }

    fun markChapterAsUnread(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        viewModelScope.launch {
            runCatching { historyRepository.markChapterUnread(novelUrl, chapterUrl) }
        }
    }

    fun markAllAsRead() {
        val novelUrl = currentNovelUrl ?: return
        val chapters = _uiState.value.novelDetails?.chapters ?: return

        viewModelScope.launch {
            runCatching { historyRepository.markChaptersRead(novelUrl, chapters.map { it.url }) }
        }
    }

    fun markPreviousAsRead(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        val chapters = _uiState.value.novelDetails?.chapters ?: return

        val index = chapters.indexOfFirst { it.url == chapterUrl }
        if (index <= 0) return

        val previousChapters = chapters.take(index).map { it.url }

        viewModelScope.launch {
            runCatching { historyRepository.markChaptersRead(novelUrl, previousChapters) }
        }
    }
    fun selectTab(tab: DetailsTab) {
        _uiState.update { it.copy(selectedTab = tab) }

        // Load reviews when switching to reviews tab for the first time
        if (tab == DetailsTab.REVIEWS &&
            _uiState.value.reviews.isEmpty() &&
            !_uiState.value.isLoadingReviews &&
            _uiState.value.hasReviewsSupport
        ) {
            loadReviews()
        }
    }

    // ================================================================
    // SELECTION MODE
    // ================================================================

    fun enableSelectionMode(initialChapterUrl: String? = null) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedChapters = if (initialChapterUrl != null) setOf(initialChapterUrl) else emptySet(),
                lastSelectedIndex = -1
            )
        }
    }

    fun disableSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedChapters = emptySet(),
                lastSelectedIndex = -1
            )
        }
    }

    fun toggleChapterSelection(displayIndex: Int, chapterUrl: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedChapters.contains(chapterUrl)) {
                state.selectedChapters - chapterUrl
            } else {
                state.selectedChapters + chapterUrl
            }
            state.copy(
                selectedChapters = newSelection,
                lastSelectedIndex = displayIndex
            )
        }
    }

    fun selectRange(endDisplayIndex: Int) {
        val state = _uiState.value
        val chapters = state.filteredChapters

        if (chapters.isEmpty()) return

        val startIndex = if (state.lastSelectedIndex >= 0 && state.selectedChapters.isNotEmpty()) {
            state.lastSelectedIndex
        } else {
            val chapter = chapters.getOrNull(endDisplayIndex) ?: return
            _uiState.update {
                it.copy(
                    selectedChapters = setOf(chapter.url),
                    lastSelectedIndex = endDisplayIndex
                )
            }
            return
        }

        val rangeStart = minOf(startIndex, endDisplayIndex)
        val rangeEnd = maxOf(startIndex, endDisplayIndex)

        val rangeUrls = chapters
            .subList(rangeStart, (rangeEnd + 1).coerceAtMost(chapters.size))
            .map { it.url }
            .toSet()

        _uiState.update {
            it.copy(
                selectedChapters = it.selectedChapters + rangeUrls,
                lastSelectedIndex = endDisplayIndex
            )
        }
    }

    fun selectAll() {
        val chapters = _uiState.value.filteredChapters
        _uiState.update {
            it.copy(selectedChapters = chapters.map { ch -> ch.url }.toSet())
        }
    }

    fun selectAllNotDownloaded() {
        val state = _uiState.value
        val notDownloaded = state.filteredChapters.filter { !state.downloadedChapters.contains(it.url) }
        _uiState.update {
            it.copy(selectedChapters = notDownloaded.map { ch -> ch.url }.toSet())
        }
    }

    fun selectAllUnread() {
        val state = _uiState.value
        val unread = state.filteredChapters.filter { !state.readChapters.contains(it.url) }
        _uiState.update {
            it.copy(selectedChapters = unread.map { ch -> ch.url }.toSet())
        }
    }

    fun setLastReadToSelected() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.firstOrNull() ?: return
        val chapter = _uiState.value.novelDetails?.chapters?.find { it.url == selected } ?: return

        viewModelScope.launch {
            libraryRepository.updateReadingPosition(novelUrl, selected, chapter.name, 0)
            disableSelectionMode()
        }
    }

    /**
     * Sets the selected chapter as last read and marks all previous chapters as read
     */
    fun setAsLastReadAndMarkPrevious() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.firstOrNull() ?: return
        val chapters = _uiState.value.novelDetails?.chapters ?: return

        val selectedIndex = chapters.indexOfFirst { it.url == selected }
        if (selectedIndex < 0) return

        val selectedChapter = chapters[selectedIndex]

        viewModelScope.launch {
            // Mark all previous chapters as read
            if (selectedIndex > 0) {
                val previousChapters = chapters.take(selectedIndex).map { it.url }
                historyRepository.markChaptersRead(novelUrl, previousChapters)
            }

            // Update reading position to selected chapter
            libraryRepository.updateReadingPosition(
                novelUrl = novelUrl,
                chapterUrl = selected,
                chapterName = selectedChapter.name,
                scrollIndex = 0,
                scrollOffset = 0
            )

            // Update UI state
            _uiState.update {
                it.copy(
                    lastReadChapterUrl = selected,
                    lastReadChapterName = selectedChapter.name,
                    lastReadChapterIndex = selectedIndex,
                    hasStartedReading = true
                )
            }

            disableSelectionMode()
        }
    }

    fun deselectAll() {
        _uiState.update {
            it.copy(selectedChapters = emptySet(), lastSelectedIndex = -1)
        }
    }

    fun invertSelection() {
        val allChapterUrls = _uiState.value.filteredChapters.map { it.url }.toSet()
        _uiState.update {
            it.copy(selectedChapters = allChapterUrls - it.selectedChapters)
        }
    }

    fun markSelectedAsRead() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.toList()

        if (selected.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                historyRepository.markChaptersRead(novelUrl, selected)
                disableSelectionMode()
            }
        }
    }

    fun markSelectedAsUnread() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.toList()

        if (selected.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                historyRepository.markChaptersUnread(novelUrl, selected)
                disableSelectionMode()
            }
        }
    }

    // ================================================================
    // DOWNLOAD OPERATIONS
    // ================================================================

    fun downloadSingleChapter(context: Context, chapter: Chapter) {
        val provider = currentProvider ?: return
        val details = _uiState.value.novelDetails ?: return

        val novel = createNovel(details, provider)
        ensureInLibrary(novel, details)

        DownloadServiceManager.startDownload(
            context = context,
            provider = provider,
            novel = novel,
            chapters = listOf(chapter)
        )
    }

    fun downloadAll(context: Context) {
        val chapters = _uiState.value.novelDetails?.chapters ?: return
        startBackgroundDownload(context, chapters)
    }

    fun downloadNext100(context: Context) = downloadNextN(context, 100)

    fun downloadNextN(context: Context, count: Int) {
        val details = _uiState.value.novelDetails ?: return
        val downloaded = _uiState.value.downloadedChapters

        // Find undownloaded chapters in reading order
        val undownloadedChapters = details.chapters.filter {
            !downloaded.contains(it.url)
        }

        if (undownloadedChapters.isEmpty()) return

        val chaptersToDownload = undownloadedChapters.take(count.coerceAtLeast(1))
        startBackgroundDownload(context, chaptersToDownload)
    }

    fun downloadUnread(context: Context) {
        val details = _uiState.value.novelDetails ?: return
        val readChapters = _uiState.value.readChapters

        val unreadChapters = details.chapters.filter { !readChapters.contains(it.url) }
        startBackgroundDownload(context, unreadChapters)
    }

    fun downloadSelected(context: Context) {
        val details = _uiState.value.novelDetails ?: return
        val selected = _uiState.value.selectedChapters
        val downloaded = _uiState.value.downloadedChapters

        val chaptersToDownload = details.chapters.filter {
            selected.contains(it.url) && !downloaded.contains(it.url)
        }

        if (chaptersToDownload.isNotEmpty()) {
            startBackgroundDownload(context, chaptersToDownload)
        }
        disableSelectionMode()
    }

    fun deleteSelectedDownloads() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.toList()

        if (selected.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                offlineRepository.deleteChapters(novelUrl, selected)
                disableSelectionMode()
            }
        }
    }

    fun isDownloadingThisNovel(): Boolean {
        val novelUrl = currentNovelUrl ?: return false
        return DownloadServiceManager.isDownloadingNovel(novelUrl)
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private fun createNovel(
        details: com.emptycastle.novery.domain.model.NovelDetails,
        provider: MainProvider
    ): Novel = Novel(
        name = details.name,
        url = details.url,
        posterUrl = details.posterUrl,
        apiName = provider.name
    )

    private fun ensureInLibrary(
        novel: Novel,
        details: com.emptycastle.novery.domain.model.NovelDetails
    ) {
        if (!_uiState.value.isFavorite) {
            viewModelScope.launch {
                libraryRepository.addToLibraryWithDetails(
                    novel = novel,
                    details = details,
                    status = _uiState.value.readingStatus
                )
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }

    private fun startBackgroundDownload(context: Context, chapters: List<Chapter>) {
        val provider = currentProvider ?: return
        val details = _uiState.value.novelDetails ?: return

        if (chapters.isEmpty()) return

        val novel = createNovel(details, provider)
        ensureInLibrary(novel, details)

        DownloadServiceManager.startDownload(
            context = context,
            provider = provider,
            novel = novel,
            chapters = chapters
        )

        hideDownloadMenu()
    }
}