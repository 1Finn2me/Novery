// HomeViewModel.kt - Update the showActionSheet and related methods

package com.emptycastle.novery.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.ReadingPosition
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.components.NovelActionSheetData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab {
    LIBRARY, BROWSE, SEARCH, HISTORY
}

/**
 * Source of the action sheet trigger
 */
enum class ActionSheetSource {
    LIBRARY, BROWSE, SEARCH, HISTORY
}

data class HomeUiState(
    val currentTab: HomeTab = HomeTab.LIBRARY,

    // Library state
    val libraryItems: List<LibraryItem> = emptyList(),
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.LAST_READ,
    val librarySearchQuery: String = "",
    val downloadCounts: Map<String, Int> = emptyMap(),

    // Unified action sheet state
    val showActionSheet: Boolean = false,
    val actionSheetData: NovelActionSheetData? = null,
    val actionSheetSource: ActionSheetSource? = null,
    val actionSheetHistoryItem: HistoryItem? = null,

    // Browse state
    val providers: List<MainProvider> = emptyList(),
    val activeProvider: MainProvider? = null,
    val browseNovels: List<Novel> = emptyList(),
    val browsePage: Int = 1,
    val selectedTag: String? = null,
    val selectedSort: String? = null,
    val isBrowseLoading: Boolean = false,
    val browseError: String? = null,

    // Search state
    val searchQuery: String = "",
    val searchResults: Map<String, List<Novel>> = emptyMap(),
    val isSearching: Boolean = false,

    // History state
    val historyItems: List<HistoryItem> = emptyList(),

    val isInitialized: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cache for library URLs (for quick lookup)
    private var libraryUrls: Set<String> = emptySet()

    // Cache for novel details (synopsis, metadata)
    private var novelDetailsCache: MutableMap<String, NovelDetails> = mutableMapOf()

    // Cache for read chapter counts
    private var readChapterCounts: MutableMap<String, Int> = mutableMapOf()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val appSettings = preferencesManager.appSettings.value
            val providers = novelRepository.getProviders()
            val defaultProvider = providers.firstOrNull()

            _uiState.update {
                it.copy(
                    providers = providers,
                    activeProvider = defaultProvider,
                    selectedTag = defaultProvider?.tags?.firstOrNull()?.value,
                    selectedSort = defaultProvider?.orderBys?.firstOrNull()?.value,
                    libraryFilter = appSettings.defaultLibraryFilter,
                    librarySortOrder = appSettings.defaultLibrarySort,
                    isInitialized = true
                )
            }

            observeLibrary()
            observeHistory()

            if (defaultProvider != null) {
                loadBrowsePage()
            }
        }
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            libraryRepository.observeLibrary().collect { items ->
                val counts = offlineRepository.getAllDownloadCounts()
                libraryUrls = items.map { it.novel.url }.toSet()
                _uiState.update {
                    it.copy(
                        libraryItems = items,
                        downloadCounts = counts
                    )
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.observeHistory().collect { items ->
                _uiState.update { it.copy(historyItems = items) }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(currentTab = tab) }

        when (tab) {
            HomeTab.LIBRARY -> refreshLibrary()
            HomeTab.BROWSE -> if (_uiState.value.browseNovels.isEmpty()) loadBrowsePage()
            HomeTab.HISTORY -> refreshHistory()
            HomeTab.SEARCH -> {}
        }
    }

    // ================================================================
    // LIBRARY
    // ================================================================

    fun refreshLibrary() {
        viewModelScope.launch {
            val counts = offlineRepository.getAllDownloadCounts()
            _uiState.update { it.copy(downloadCounts = counts) }
        }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(libraryFilter = filter) }
    }

    fun setLibrarySortOrder(sortOrder: LibrarySortOrder) {
        _uiState.update { it.copy(librarySortOrder = sortOrder) }
    }

    fun setLibrarySearchQuery(query: String) {
        _uiState.update { it.copy(librarySearchQuery = query) }
    }

    fun getFilteredLibrary(): List<LibraryItem> {
        val state = _uiState.value
        val downloadCounts = state.downloadCounts
        val searchQuery = state.librarySearchQuery.lowercase().trim()

        val searched = if (searchQuery.isBlank()) {
            state.libraryItems
        } else {
            state.libraryItems.filter { item ->
                item.novel.name.lowercase().contains(searchQuery) ||
                        item.novel.apiName.lowercase().contains(searchQuery) ||
                        item.readingStatus.displayName().lowercase().contains(searchQuery)
            }
        }

        val filtered = when (state.libraryFilter) {
            LibraryFilter.ALL -> searched
            LibraryFilter.DOWNLOADED -> searched.filter {
                (downloadCounts[it.novel.url] ?: 0) > 0
            }
            LibraryFilter.READING -> searched.filter { it.readingStatus == ReadingStatus.READING }
            LibraryFilter.COMPLETED -> searched.filter { it.readingStatus == ReadingStatus.COMPLETED }
            LibraryFilter.ON_HOLD -> searched.filter { it.readingStatus == ReadingStatus.ON_HOLD }
            LibraryFilter.PLAN_TO_READ -> searched.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ }
            LibraryFilter.DROPPED -> searched.filter { it.readingStatus == ReadingStatus.DROPPED }
        }

        return when (state.librarySortOrder) {
            LibrarySortOrder.LAST_READ -> filtered.sortedByDescending {
                it.lastReadPosition?.timestamp ?: it.addedAt
            }
            LibrarySortOrder.TITLE_ASC -> filtered.sortedBy { it.novel.name.lowercase() }
            LibrarySortOrder.TITLE_DESC -> filtered.sortedByDescending { it.novel.name.lowercase() }
            LibrarySortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
            LibrarySortOrder.UNREAD_COUNT -> filtered.sortedByDescending { it.downloadedChaptersCount }
        }
    }

    // ================================================================
    // LIBRARY ACTIONS (Add/Remove)
    // ================================================================

    fun addToLibrary(novel: Novel) {
        viewModelScope.launch {
            libraryRepository.addToLibrary(novel)
            libraryUrls = libraryUrls + novel.url
        }
    }

    fun removeFromLibrary(novelUrl: String) {
        viewModelScope.launch {
            libraryRepository.removeFromLibrary(novelUrl)
            offlineRepository.deleteNovelDownloads(novelUrl)
            libraryUrls = libraryUrls - novelUrl
        }
    }

    // ================================================================
    // UNIFIED ACTION SHEET - ENHANCED
    // ================================================================

    /**
     * Show action sheet with full metadata support
     */
    private fun showActionSheet(
        novel: Novel,
        source: ActionSheetSource,
        lastChapterName: String? = null,
        historyItem: HistoryItem? = null,
        libraryItem: LibraryItem? = null
    ) {
        val isInLibrary = libraryUrls.contains(novel.url)
        val cachedDetails = novelDetailsCache[novel.url]
        val readCount = readChapterCounts[novel.url]

        // Build initial data with whatever we have cached
        val initialData = NovelActionSheetData(
            novel = novel,
            synopsis = cachedDetails?.synopsis,
            isInLibrary = isInLibrary,
            lastChapterName = lastChapterName,
            providerName = novel.apiName,
            readingStatus = libraryItem?.readingStatus,
            author = cachedDetails?.author,
            tags = cachedDetails?.tags,
            rating = cachedDetails?.rating,
            votes = cachedDetails?.peopleVoted,
            chapterCount = cachedDetails?.chapters?.size,
            readCount = readCount
        )

        _uiState.update {
            it.copy(
                showActionSheet = true,
                actionSheetData = initialData,
                actionSheetSource = source,
                actionSheetHistoryItem = historyItem
            )
        }

        // Fetch full details in background if not cached
        if (cachedDetails == null) {
            viewModelScope.launch {
                fetchAndCacheNovelDetails(novel.url, libraryItem)
            }
        }
    }

    /**
     * Fetch and cache novel details, update the action sheet if still showing
     */
    private suspend fun fetchAndCacheNovelDetails(novelUrl: String, libraryItem: LibraryItem? = null) {
        // Try to get from offline cache first
        val cachedDetails = offlineRepository.getNovelDetails(novelUrl)

        if (cachedDetails != null) {
            novelDetailsCache[novelUrl] = cachedDetails

            // Get read chapter count
            val readCount = try {
                historyRepository.getReadChapterCount(novelUrl)
            } catch (e: Exception) {
                0
            }
            readChapterCounts[novelUrl] = readCount

            // Update the action sheet if it's still showing this novel
            val currentData = _uiState.value.actionSheetData
            if (currentData != null && currentData.novel.url == novelUrl && _uiState.value.showActionSheet) {
                _uiState.update {
                    it.copy(
                        actionSheetData = currentData.copy(
                            synopsis = cachedDetails.synopsis,
                            author = cachedDetails.author,
                            tags = cachedDetails.tags,
                            rating = cachedDetails.rating,
                            votes = cachedDetails.peopleVoted,
                            chapterCount = cachedDetails.chapters.size,
                            readCount = readCount,
                            readingStatus = libraryItem?.readingStatus ?: currentData.readingStatus
                        )
                    )
                }
            }
        }
    }

    fun showLibraryActionSheet(item: LibraryItem) {
        // Pre-cache read count for library items
        viewModelScope.launch {
            val readCount = try {
                historyRepository.getReadChapterCount(item.novel.url)
            } catch (e: Exception) {
                0
            }
            readChapterCounts[item.novel.url] = readCount
        }

        showActionSheet(
            novel = item.novel,
            source = ActionSheetSource.LIBRARY,
            lastChapterName = item.lastReadPosition?.chapterName,
            libraryItem = item
        )
    }

    fun showHistoryActionSheet(item: HistoryItem) {
        // Check if this novel is in library to get reading status
        val libraryItem = _uiState.value.libraryItems.find { it.novel.url == item.novel.url }

        showActionSheet(
            novel = item.novel,
            source = ActionSheetSource.HISTORY,
            lastChapterName = item.chapterName,
            historyItem = item,
            libraryItem = libraryItem
        )
    }

    fun showBrowseActionSheet(novel: Novel) {
        val libraryItem = _uiState.value.libraryItems.find { it.novel.url == novel.url }

        showActionSheet(
            novel = novel,
            source = ActionSheetSource.BROWSE,
            libraryItem = libraryItem
        )
    }

    fun showSearchActionSheet(novel: Novel) {
        val libraryItem = _uiState.value.libraryItems.find { it.novel.url == novel.url }

        showActionSheet(
            novel = novel,
            source = ActionSheetSource.SEARCH,
            libraryItem = libraryItem
        )
    }

    fun hideActionSheet() {
        _uiState.update {
            it.copy(
                showActionSheet = false,
                actionSheetData = null,
                actionSheetSource = null,
                actionSheetHistoryItem = null
            )
        }
    }

    // ================================================================
    // CONTINUE READING HELPERS
    // ================================================================

    fun getReadingPosition(novelUrl: String): ReadingPosition? {
        return _uiState.value.libraryItems
            .find { it.novel.url == novelUrl }
            ?.lastReadPosition
    }

    fun getHistoryChapter(novelUrl: String): Pair<String, String>? {
        val item = _uiState.value.historyItems.find { it.novel.url == novelUrl }
        return item?.let { Pair(it.chapterUrl, it.chapterName) }
    }

    // ================================================================
    // BROWSE
    // ================================================================

    fun setActiveProvider(provider: MainProvider) {
        _uiState.update {
            it.copy(
                activeProvider = provider,
                browsePage = 1,
                browseNovels = emptyList(),
                selectedTag = provider.tags.firstOrNull()?.value,
                selectedSort = provider.orderBys.firstOrNull()?.value
            )
        }
        loadBrowsePage()
    }

    fun setSelectedTag(tag: String?) {
        _uiState.update {
            it.copy(selectedTag = tag, browsePage = 1, browseNovels = emptyList())
        }
        loadBrowsePage()
    }

    fun setSelectedSort(sort: String?) {
        _uiState.update {
            it.copy(selectedSort = sort, browsePage = 1, browseNovels = emptyList())
        }
        loadBrowsePage()
    }

    fun loadBrowsePage() {
        val provider = _uiState.value.activeProvider ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isBrowseLoading = true, browseError = null) }

            val result = novelRepository.loadMainPage(
                provider = provider,
                page = _uiState.value.browsePage,
                orderBy = _uiState.value.selectedSort,
                tag = _uiState.value.selectedTag
            )

            result.fold(
                onSuccess = { pageResult ->
                    _uiState.update {
                        it.copy(
                            browseNovels = pageResult.novels,
                            isBrowseLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            browseError = error.message ?: "Failed to load",
                            isBrowseLoading = false
                        )
                    }
                }
            )
        }
    }

    fun nextPage() {
        _uiState.update { it.copy(browsePage = it.browsePage + 1) }
        loadBrowsePage()
    }

    fun previousPage() {
        if (_uiState.value.browsePage > 1) {
            _uiState.update { it.copy(browsePage = it.browsePage - 1) }
            loadBrowsePage()
        }
    }

    // ================================================================
    // SEARCH
    // ================================================================

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyMap()) }

            val results = novelRepository.searchAll(query)

            val successfulResults = results.mapValues { (_, result) ->
                result.getOrNull() ?: emptyList()
            }.filterValues { it.isNotEmpty() }

            _uiState.update {
                it.copy(
                    searchResults = successfulResults,
                    isSearching = false
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(searchQuery = "", searchResults = emptyMap())
        }
    }

    // ================================================================
    // HISTORY
    // ================================================================

    fun refreshHistory() {
        viewModelScope.launch {
            val history = historyRepository.getHistory()
            _uiState.update { it.copy(historyItems = history) }
        }
    }

    fun removeFromHistory(novelUrl: String) {
        viewModelScope.launch {
            historyRepository.removeFromHistory(novelUrl)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}