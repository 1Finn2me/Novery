package com.emptycastle.novery.ui.screens.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.service.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Chapter filter options
 */
enum class ChapterFilter {
    ALL,
    UNREAD,
    DOWNLOADED,
    NOT_DOWNLOADED
}

/**
 * UI State for Details Screen
 */
data class DetailsUiState(
    val novelDetails: NovelDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Refresh state
    val isRefreshing: Boolean = false,

    // Library status
    val isFavorite: Boolean = false,
    val readingStatus: ReadingStatus = ReadingStatus.READING,

    // Reading position
    val hasStartedReading: Boolean = false,
    val lastReadChapterUrl: String? = null,
    val lastReadChapterName: String? = null,
    val lastReadChapterIndex: Int = -1,

    // Chapter status
    val downloadedChapters: Set<String> = emptySet(),
    val readChapters: Set<String> = emptySet(),

    // Chapter sorting & filtering
    val isChapterSortDescending: Boolean = false,
    val chapterFilter: ChapterFilter = ChapterFilter.ALL,
    val chapterSearchQuery: String = "",
    val isSearchActive: Boolean = false,

    // Synopsis expansion
    val isSynopsisExpanded: Boolean = false,

    // Selection mode for batch operations
    val isSelectionMode: Boolean = false,
    val selectedChapters: Set<String> = emptySet(),
    val lastSelectedIndex: Int = -1,

    // Cover image zoom
    val showCoverZoom: Boolean = false,

    // Download menu
    val showDownloadMenu: Boolean = false,

    // Status menu
    val showStatusMenu: Boolean = false
) {
    // Computed properties
    val readProgress: Float
        get() {
            val total = novelDetails?.chapters?.size ?: 0
            if (total == 0) return 0f
            return readChapters.size.toFloat() / total
        }

    val downloadProgress: Float
        get() {
            val total = novelDetails?.chapters?.size ?: 0
            if (total == 0) return 0f
            return downloadedChapters.size.toFloat() / total
        }

    val unreadCount: Int
        get() = (novelDetails?.chapters?.size ?: 0) - readChapters.size

    val downloadedCount: Int
        get() = downloadedChapters.size

    val notDownloadedCount: Int
        get() = (novelDetails?.chapters?.size ?: 0) - downloadedChapters.size

    // Selection mode helpers
    val selectedDownloadedCount: Int
        get() = selectedChapters.count { downloadedChapters.contains(it) }

    val selectedNotDownloadedCount: Int
        get() = selectedChapters.count { !downloadedChapters.contains(it) }

    val selectedReadCount: Int
        get() = selectedChapters.count { readChapters.contains(it) }

    val selectedUnreadCount: Int
        get() = selectedChapters.count { !readChapters.contains(it) }
}

class DetailsViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    // Expose download state from the service manager
    val downloadState: StateFlow<DownloadState> = DownloadServiceManager.downloadState

    private var currentProvider: MainProvider? = null
    private var currentNovelUrl: String? = null

    init {
        // Load persisted chapter sort preference on init
        val savedSortDescending = preferencesManager.getChapterSortDescending()
        _uiState.update { it.copy(isChapterSortDescending = savedSortDescending) }
    }

    // ================================================================
    // LOAD NOVEL
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
                    _uiState.update {
                        it.copy(
                            novelDetails = details,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }

                    loadLibraryStatus(novelUrl)
                    observeChapterStatus(novelUrl)
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

            // Find the index of the last read chapter
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
            }
        }

        viewModelScope.launch {
            historyRepository.observeReadChapters(novelUrl).collect { read ->
                _uiState.update { it.copy(readChapters = read) }
            }
        }
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

    fun showStatusMenu() {
        _uiState.update { it.copy(showStatusMenu = true) }
    }

    fun hideStatusMenu() {
        _uiState.update { it.copy(showStatusMenu = false) }
    }

    // ================================================================
    // COVER ZOOM
    // ================================================================

    fun showCoverZoom() {
        _uiState.update { it.copy(showCoverZoom = true) }
    }

    fun hideCoverZoom() {
        _uiState.update { it.copy(showCoverZoom = false) }
    }

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
    // SYNOPSIS
    // ================================================================

    fun toggleSynopsis() {
        _uiState.update { it.copy(isSynopsisExpanded = !it.isSynopsisExpanded) }
    }

    // ================================================================
    // CHAPTER SORTING & FILTERING
    // ================================================================

    fun toggleChapterSort() {
        val newValue = !_uiState.value.isChapterSortDescending
        _uiState.update { it.copy(isChapterSortDescending = newValue) }
        preferencesManager.setChapterSortDescending(newValue)
    }

    fun setChapterFilter(filter: ChapterFilter) {
        _uiState.update { it.copy(chapterFilter = filter) }
    }

    fun setChapterSearchQuery(query: String) {
        _uiState.update { it.copy(chapterSearchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                chapterSearchQuery = if (it.isSearchActive) "" else it.chapterSearchQuery
            )
        }
    }

    fun getFilteredChapters(): List<Chapter> {
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
        return if (state.isChapterSortDescending) {
            chapters.reversed()
        } else {
            chapters
        }
    }

    // ================================================================
    // CHAPTER READ STATUS
    // ================================================================

    fun markChapterAsRead(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        viewModelScope.launch {
            try {
                historyRepository.markChapterRead(novelUrl, chapterUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markChapterAsUnread(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        viewModelScope.launch {
            try {
                historyRepository.markChapterUnread(novelUrl, chapterUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAllAsRead() {
        val novelUrl = currentNovelUrl ?: return
        val chapters = _uiState.value.novelDetails?.chapters ?: return

        viewModelScope.launch {
            try {
                historyRepository.markChaptersRead(novelUrl, chapters.map { it.url })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markPreviousAsRead(chapterUrl: String) {
        val novelUrl = currentNovelUrl ?: return
        val chapters = _uiState.value.novelDetails?.chapters ?: return

        val index = chapters.indexOfFirst { it.url == chapterUrl }
        if (index <= 0) return

        val previousChapters = chapters.take(index).map { it.url }

        viewModelScope.launch {
            try {
                historyRepository.markChaptersRead(novelUrl, previousChapters)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadSingleChapter(context: Context, chapter: Chapter) {
        val provider = currentProvider ?: return
        val details = _uiState.value.novelDetails ?: return

        val novel = Novel(
            name = details.name,
            url = details.url,
            posterUrl = details.posterUrl,
            apiName = provider.name
        )

        // Ensure in library
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

        // Start background download
        DownloadServiceManager.startDownload(
            context = context,
            provider = provider,
            novel = novel,
            chapters = listOf(chapter)
        )
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
        val chapters = getFilteredChapters()

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
        val chapters = getFilteredChapters()
        _uiState.update {
            it.copy(selectedChapters = chapters.map { ch -> ch.url }.toSet())
        }
    }

    fun selectAllNotDownloaded() {
        val state = _uiState.value
        val chapters = getFilteredChapters()
        val notDownloaded = chapters.filter { !state.downloadedChapters.contains(it.url) }
        _uiState.update {
            it.copy(selectedChapters = notDownloaded.map { ch -> ch.url }.toSet())
        }
    }

    fun selectAllUnread() {
        val state = _uiState.value
        val chapters = getFilteredChapters()
        val unread = chapters.filter { !state.readChapters.contains(it.url) }
        _uiState.update {
            it.copy(selectedChapters = unread.map { ch -> ch.url }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update {
            it.copy(
                selectedChapters = emptySet(),
                lastSelectedIndex = -1
            )
        }
    }

    fun invertSelection() {
        val allChapterUrls = getFilteredChapters().map { it.url }.toSet()
        _uiState.update {
            it.copy(selectedChapters = allChapterUrls - it.selectedChapters)
        }
    }

    fun markSelectedAsRead() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.toList()

        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                historyRepository.markChaptersRead(novelUrl, selected)
                disableSelectionMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markSelectedAsUnread() {
        val novelUrl = currentNovelUrl ?: return
        val selected = _uiState.value.selectedChapters.toList()

        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                historyRepository.markChaptersUnread(novelUrl, selected)
                disableSelectionMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ================================================================
    // DOWNLOAD MENU
    // ================================================================

    fun showDownloadMenu() {
        _uiState.update { it.copy(showDownloadMenu = true) }
    }

    fun hideDownloadMenu() {
        _uiState.update { it.copy(showDownloadMenu = false) }
    }

    // ================================================================
    // DOWNLOAD (Using Background Service)
    // ================================================================

    private fun ensureInLibrary() {
        val details = _uiState.value.novelDetails ?: return
        val provider = currentProvider ?: return

        if (!_uiState.value.isFavorite) {
            viewModelScope.launch {
                val novel = Novel(
                    name = details.name,
                    url = details.url,
                    posterUrl = details.posterUrl,
                    apiName = provider.name
                )

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

        ensureInLibrary()

        val novel = Novel(
            name = details.name,
            url = details.url,
            posterUrl = details.posterUrl,
            apiName = provider.name
        )

        DownloadServiceManager.startDownload(
            context = context,
            provider = provider,
            novel = novel,
            chapters = chapters
        )

        hideDownloadMenu()
    }

    fun downloadAll(context: Context) {
        val chapters = _uiState.value.novelDetails?.chapters ?: return
        startBackgroundDownload(context, chapters)
    }

    fun downloadNext100(context: Context) {
        val details = _uiState.value.novelDetails ?: return
        val downloaded = _uiState.value.downloadedChapters

        val firstUndownloaded = details.chapters.indexOfFirst { !downloaded.contains(it.url) }
        val startIndex = if (firstUndownloaded == -1) 0 else firstUndownloaded
        val endIndex = minOf(startIndex + 100, details.chapters.size)

        val chaptersToDownload = details.chapters.subList(startIndex, endIndex)
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

        // Only download chapters that aren't already downloaded
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
            try {
                offlineRepository.deleteChapters(novelUrl, selected)
                disableSelectionMode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Check if currently downloading this novel
     */
    fun isDownloadingThisNovel(): Boolean {
        val novelUrl = currentNovelUrl ?: return false
        return DownloadServiceManager.isDownloadingNovel(novelUrl)
    }
}
