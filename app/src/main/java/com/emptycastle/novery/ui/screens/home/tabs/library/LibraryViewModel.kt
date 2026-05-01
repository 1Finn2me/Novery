package com.emptycastle.novery.ui.screens.home.tabs.library

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.service.DownloadPriority
import com.emptycastle.novery.service.DownloadRequest
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetManager
import com.emptycastle.novery.ui.screens.home.shared.ActionSheetSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LibraryViewModel"
private const val ALL_FILTER_DOUBLE_TAP_WINDOW_MS = 500L

class LibraryViewModel : ViewModel() {

    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val notificationRepository = RepositoryProvider.getNotificationRepository()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var lastAllFilterTapAt = 0L

    private val actionSheetManager = ActionSheetManager()
    val actionSheetState: StateFlow<com.emptycastle.novery.ui.screens.home.shared.ActionSheetState> = actionSheetManager.state

    init {
        initialize()

        viewModelScope.launch {
            preferencesManager.appSettings.collect { settings ->
                _uiState.update { state ->
                    state.applyLibrarySettings(
                        settings = settings,
                        spicyShelfRevealed = preferencesManager.isSpicyShelfRevealed.value
                    )
                }
                updateVisibleItems()
                applyFilters()
            }
        }

        viewModelScope.launch {
            preferencesManager.isSpicyShelfRevealed.collect { spicyShelfRevealed ->
                _uiState.update { state ->
                    state.applySpicyShelfVisibility(spicyShelfRevealed)
                }
                updateVisibleItems()
                applyFilters()
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value
                _uiState.update { state ->
                    state.applyLibrarySettings(
                        settings = settings,
                        spicyShelfRevealed = preferencesManager.isSpicyShelfRevealed.value
                    )
                }

                libraryRepository.observeLibrary().collect { items ->
                    val counts = offlineRepository.getAllDownloadCounts()
                    _uiState.update { state ->
                        state.copy(
                            downloadCounts = counts,
                            isLoading = false
                        )
                    }
                    updateVisibleItems(items)
                    applyFilters()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing library", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun updateVisibleItems(allItems: List<LibraryItem> = _uiState.value.allItems) {
        _uiState.update { state ->
            val hiddenStatuses = getHiddenStatuses(state)
            val visibleItems = allItems.filterNot { it.readingStatus in hiddenStatuses }

            state.copy(
                allItems = allItems,
                items = visibleItems,
                totalNewChapters = visibleItems.sumOf { it.newChapterCount },
                filter = if (state.filter !in state.visibleFilters) {
                    LibraryFilter.ALL
                } else {
                    state.filter
                }
            )
        }
    }

    // ================================================================
    // FILTER & SORT
    // ================================================================

    fun setFilter(filter: LibraryFilter) {
        if (filter !in _uiState.value.visibleFilters) return

        _uiState.update { it.copy(filter = filter) }
        applyFilters()
    }

    fun onFilterChipPressed(filter: LibraryFilter) {
        if (filter != LibraryFilter.ALL) {
            lastAllFilterTapAt = 0L
            setFilter(filter)
            return
        }

        val now = System.currentTimeMillis()
        val isDoubleTap = now - lastAllFilterTapAt <= ALL_FILTER_DOUBLE_TAP_WINDOW_MS
        lastAllFilterTapAt = now

        if (_uiState.value.filter != LibraryFilter.ALL) {
            _uiState.update { it.copy(filter = LibraryFilter.ALL) }
            applyFilters()
        }

        if (isDoubleTap && _uiState.value.spicyPrivacyEnabled) {
            lastAllFilterTapAt = 0L
            toggleSpicyFilterVisibility()
        }
    }

    fun setSortOrder(sortOrder: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val searchQuery = state.searchQuery.lowercase().trim()
        val downloadCounts = state.downloadCounts

        // Search filter
        val searched = if (searchQuery.isBlank()) {
            state.items
        } else {
            state.items.filter { item ->
                item.novel.name.lowercase().contains(searchQuery) ||
                        item.novel.apiName.lowercase().contains(searchQuery) ||
                        item.readingStatus.displayName().lowercase().contains(searchQuery)
            }
        }

        // Category filter
        val filtered = when (state.filter) {
            LibraryFilter.ALL -> searched
            LibraryFilter.SPICY -> searched.filter { it.readingStatus == ReadingStatus.SPICY }
            LibraryFilter.DOWNLOADED -> searched.filter {
                (downloadCounts[it.novel.url] ?: 0) > 0
            }
            LibraryFilter.READING -> searched.filter { it.readingStatus == ReadingStatus.READING }
            LibraryFilter.COMPLETED -> searched.filter { it.readingStatus == ReadingStatus.COMPLETED }
            LibraryFilter.ON_HOLD -> searched.filter { it.readingStatus == ReadingStatus.ON_HOLD }
            LibraryFilter.PLAN_TO_READ -> searched.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ }
            LibraryFilter.DROPPED -> searched.filter { it.readingStatus == ReadingStatus.DROPPED }
        }

        // Sort - NO new chapter priority except for NEW_CHAPTERS sort
        val sorted = when (state.sortOrder) {
            LibrarySortOrder.NEW_CHAPTERS -> {
                // Only this sort prioritizes new chapters
                filtered.sortedByDescending { it.newChapterCount }
            }
            LibrarySortOrder.LAST_READ -> {
                // Pure last read sorting - no new chapter priority
                filtered.sortedByDescending { it.lastReadPosition?.timestamp ?: it.addedAt }
            }
            LibrarySortOrder.TITLE_ASC -> {
                filtered.sortedBy { it.novel.name.lowercase() }
            }
            LibrarySortOrder.TITLE_DESC -> {
                filtered.sortedByDescending { it.novel.name.lowercase() }
            }
            LibrarySortOrder.DATE_ADDED -> {
                filtered.sortedByDescending { it.addedAt }
            }
            LibrarySortOrder.UNREAD_COUNT -> {
                filtered.sortedByDescending { it.unreadChapterCount }
            }
        }

        _uiState.update { it.copy(filteredItems = sorted) }
    }

    private fun toggleSpicyFilterVisibility() {
        val state = _uiState.value
        if (!state.spicyPrivacyEnabled || LibraryFilter.SPICY !in state.enabledShelfFilters) return

        preferencesManager.setSpicyShelfRevealed(LibraryFilter.SPICY !in state.visibleFilters)
    }

    // ================================================================
    // NEW CHAPTERS MANAGEMENT
    // ================================================================

    fun dismissNewChaptersCard() {
        _uiState.update { it.copy(showNewChaptersCard = false) }
    }

    fun acknowledgeNewChapters(novelUrl: String) {
        viewModelScope.launch {
            try {
                libraryRepository.acknowledgeNewChapters(novelUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging new chapters for $novelUrl", e)
            }
        }
    }

    fun acknowledgeAllNewChapters() {
        viewModelScope.launch {
            try {
                val items = _uiState.value.items.filter { it.hasNewChapters }
                items.forEach { item ->
                    libraryRepository.acknowledgeNewChapters(item.novel.url)
                }
                _uiState.update { it.copy(showNewChaptersCard = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging all new chapters", e)
            }
        }
    }

    // ================================================================
    // DOWNLOAD OPERATIONS
    // ================================================================

    fun downloadAllNewChapters(context: Context) {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value

                if (settings.autoDownloadOnWifiOnly && !isOnWifi(context)) {
                    Log.d(TAG, "Skipping download - not on WiFi")
                    return@launch
                }

                val novelsWithNew = _uiState.value.items.filter { it.hasNewChapters }
                if (novelsWithNew.isEmpty()) {
                    Log.d(TAG, "No novels with new chapters to download")
                    return@launch
                }

                _uiState.update { it.copy(isAutoDownloading = true) }

                novelsWithNew.forEachIndexed { index, item ->
                    try {
                        downloadChaptersForItem(context, item, settings.autoDownloadLimit, index, novelsWithNew.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error downloading chapters for ${item.novel.name}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadAllNewChapters", e)
            } finally {
                _uiState.update {
                    it.copy(
                        isAutoDownloading = false,
                        autoDownloadProgress = null
                    )
                }
            }
        }
    }

    private suspend fun downloadChaptersForItem(
        context: Context,
        item: LibraryItem,
        downloadLimit: Int,
        currentIndex: Int,
        totalNovels: Int
    ) {
        val provider = novelRepository.getProvider(item.novel.apiName)
        if (provider == null) {
            Log.w(TAG, "Provider not found for ${item.novel.apiName}")
            return
        }

        val detailsResult = novelRepository.loadNovelDetails(provider, item.novel.url, forceRefresh = false)
        val details = detailsResult.getOrNull()
        if (details == null) {
            Log.w(TAG, "Could not load details for ${item.novel.name}")
            return
        }

        val allChapters = details.chapters
        if (allChapters.isNullOrEmpty()) {
            Log.w(TAG, "No chapters found for ${item.novel.name}")
            return
        }

        val downloadedUrls = try {
            offlineRepository.getDownloadedChapterUrls(item.novel.url)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting downloaded URLs for ${item.novel.url}", e)
            emptySet()
        }

        val newChaptersCount = item.newChapterCount.coerceAtLeast(0)

        val newChapters = if (newChaptersCount > 0 && newChaptersCount <= allChapters.size) {
            allChapters.takeLast(newChaptersCount)
        } else {
            allChapters.asReversed()
                .filter { !downloadedUrls.contains(it.url) }
                .take(10)
        }

        val chaptersToDownload = newChapters
            .filter { chapter -> !downloadedUrls.contains(chapter.url) }
            .let { chapters ->
                if (downloadLimit > 0) {
                    chapters.take(downloadLimit)
                } else {
                    chapters
                }
            }

        if (chaptersToDownload.isEmpty()) {
            Log.d(TAG, "No new chapters to download for ${item.novel.name}")
            return
        }

        _uiState.update {
            it.copy(
                autoDownloadProgress = AutoDownloadProgress(
                    currentNovel = item.novel.name,
                    currentChapter = 0,
                    totalChapters = chaptersToDownload.size,
                    novelsCompleted = currentIndex,
                    totalNovels = totalNovels
                )
            )
        }

        val request = DownloadRequest(
            novelUrl = item.novel.url,
            novelName = item.novel.name,
            novelCoverUrl = item.novel.posterUrl,
            providerName = provider.name,
            chapterUrls = chaptersToDownload.map { it.url },
            chapterNames = chaptersToDownload.map { it.name },
            priority = DownloadPriority.NORMAL
        )

        try {
            DownloadServiceManager.startDownload(context, request)
            Log.d(TAG, "Started download for ${chaptersToDownload.size} chapters of ${item.novel.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download for ${item.novel.name}", e)
        }
    }

    fun triggerAutoDownload(context: Context) {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value
                if (!settings.autoDownloadEnabled) {
                    Log.d(TAG, "Auto-download is disabled")
                    return@launch
                }

                if (settings.autoDownloadOnWifiOnly && !isOnWifi(context)) {
                    Log.d(TAG, "Skipping auto-download - not on WiFi")
                    return@launch
                }

                val eligibleNovels = _uiState.value.items.filter { item ->
                    item.hasNewChapters &&
                            settings.autoDownloadForStatuses.contains(item.readingStatus)
                }

                if (eligibleNovels.isEmpty()) {
                    Log.d(TAG, "No eligible novels for auto-download")
                    return@launch
                }

                Log.d(TAG, "Auto-downloading for ${eligibleNovels.size} novels")

                eligibleNovels.forEach { item ->
                    try {
                        downloadChaptersForItem(
                            context = context,
                            item = item,
                            downloadLimit = settings.autoDownloadLimit,
                            currentIndex = 0,
                            totalNovels = eligibleNovels.size
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error auto-downloading for ${item.novel.name}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in triggerAutoDownload", e)
            }
        }
    }

    private fun isOnWifi(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi status", e)
            false
        }
    }

    // ================================================================
    // REFRESH
    // ================================================================

    fun refreshDownloadCounts() {
        viewModelScope.launch {
            try {
                val counts = offlineRepository.getAllDownloadCounts()
                _uiState.update { it.copy(downloadCounts = counts) }
                applyFilters()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing download counts", e)
            }
        }
    }

    fun refreshLibrary(context: Context? = null) {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            val stateAtRefreshStart = _uiState.value
            val currentFilter = _uiState.value.filter
            val currentDownloadCounts = _uiState.value.downloadCounts
            val hiddenStatuses = getHiddenStatuses(stateAtRefreshStart)
            val hiddenNovelUrlsToRefresh = getHiddenNovelUrlsToRefresh(
                state = stateAtRefreshStart,
                hiddenStatuses = hiddenStatuses
            )

            val filterDisplayName = when (currentFilter) {
                LibraryFilter.ALL -> "all novels"
                LibraryFilter.SPICY -> "spicy novels"
                LibraryFilter.DOWNLOADED -> "downloaded novels"
                LibraryFilter.READING -> "reading novels"
                LibraryFilter.COMPLETED -> "completed novels"
                LibraryFilter.ON_HOLD -> "on-hold novels"
                LibraryFilter.PLAN_TO_READ -> "plan-to-read novels"
                LibraryFilter.DROPPED -> "dropped novels"
            }

            Log.d(TAG, "Refreshing library with filter: $currentFilter ($filterDisplayName)")

            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    refreshProgress = RefreshProgress(
                        current = 0,
                        total = 0,
                        currentNovelName = "Preparing to refresh $filterDisplayName...",
                        novelsWithNewChapters = 0,
                        newChaptersFound = 0
                    ),
                    error = null
                )
            }

            try {
                var novelsWithNewChapters = 0
                var totalNewChapters = 0

                val result = libraryRepository.refreshNovelsWithFilter(
                    getProvider = { providerName ->
                        novelRepository.getProvider(providerName)
                    },
                    filter = currentFilter,
                    excludedStatuses = hiddenStatuses,
                    downloadCounts = currentDownloadCounts,
                    onProgress = { current, total, novelName ->
                        _uiState.update {
                            it.copy(
                                refreshProgress = RefreshProgress(
                                    current = current,
                                    total = total,
                                    currentNovelName = novelName,
                                    novelsWithNewChapters = novelsWithNewChapters,
                                    newChaptersFound = totalNewChapters
                                )
                            )
                        }
                    }
                )

                if (hiddenNovelUrlsToRefresh.isNotEmpty()) {
                    try {
                        libraryRepository.refreshNovelsByUrls(
                            getProvider = { providerName ->
                                novelRepository.getProvider(providerName)
                            },
                            novelUrls = hiddenNovelUrlsToRefresh,
                            onProgress = { _, _, _ -> }
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error refreshing hidden library shelf entries", e)
                    }
                }

                novelsWithNewChapters = result.updatedCount
                totalNewChapters = result.totalNewChapters

                val novelsWithNew = libraryRepository.getLibrary().filter { it.hasNewChapters }
                novelsWithNew.forEach { item ->
                    try {
                        notificationRepository.addOrUpdateNotification(
                            novelUrl = item.novel.url,
                            providerName = item.novel.apiName
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error syncing update notification for ${item.novel.url}", e)
                    }
                }

                val counts = offlineRepository.getAllDownloadCounts()

                val completionMessage = buildString {
                    append("Complete!")
                    if (result.skippedCount > 0) {
                        append(" (${result.skippedCount} skipped)")
                    }
                }

                _uiState.update {
                    it.copy(
                        downloadCounts = counts,
                        refreshProgress = RefreshProgress(
                            current = result.totalChecked,
                            total = result.totalChecked,
                            currentNovelName = completionMessage,
                            novelsWithNewChapters = novelsWithNewChapters,
                            newChaptersFound = totalNewChapters
                        ),
                        showNewChaptersCard = totalNewChapters > 0
                    )
                }

                Log.d(TAG, "Refresh complete: checked=${result.totalChecked}, skipped=${result.skippedCount}, updated=$novelsWithNewChapters, newChapters=$totalNewChapters")

                kotlinx.coroutines.delay(1500)

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        refreshProgress = null
                    )
                }

                if (context != null && totalNewChapters > 0) {
                    triggerAutoDownload(context)
                }

                applyFilters()

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing library", e)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        refreshProgress = null,
                        error = e.message ?: "Refresh failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ================================================================
    // ACTION SHEET
    // ================================================================

    fun showActionSheet(item: LibraryItem) {
        actionSheetManager.show(
            novel = item.novel,
            source = ActionSheetSource.LIBRARY,
            lastChapterName = item.lastReadPosition?.chapterName,
            libraryItem = item
        )
    }

    fun hideActionSheet() {
        actionSheetManager.hide()
    }

    fun updateReadingStatus(status: ReadingStatus) {
        actionSheetManager.updateReadingStatus(status)
    }

    fun removeFromLibrary(novelUrl: String) {
        viewModelScope.launch {
            try {
                actionSheetManager.removeFromLibrary(novelUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from library: $novelUrl", e)
            }
        }
    }

    fun getReadingPosition(novelUrl: String) = actionSheetManager.getReadingPosition(novelUrl)

    private fun getHiddenStatuses(state: LibraryUiState): Set<ReadingStatus> =
        LibraryFilter.hiddenStatuses(state.visibleFilters)

    private fun getHiddenNovelUrlsToRefresh(
        state: LibraryUiState,
        hiddenStatuses: Set<ReadingStatus>
    ): Set<String> {
        if (hiddenStatuses.isEmpty()) return emptySet()

        return state.allItems
            .asSequence()
            .filter { it.readingStatus in hiddenStatuses }
            .mapTo(mutableSetOf()) { it.novel.url }
    }

    private fun LibraryUiState.applyLibrarySettings(
        settings: com.emptycastle.novery.domain.model.AppSettings,
        spicyShelfRevealed: Boolean
    ): LibraryUiState {
        val privacyEnabled = settings.hideSpicyLibraryContent
        val showSpicyFilter = !privacyEnabled || spicyShelfRevealed
        val enabledShelfFilters = settings.enabledLibraryFilters
        val visibleFilters = LibraryFilter.visibleFilters(enabledShelfFilters, showSpicyFilter)

        return copy(
            filter = LibraryFilter.sanitizeDefault(
                settings.defaultLibraryFilter,
                enabledShelfFilters
            ),
            sortOrder = settings.defaultLibrarySort,
            spicyPrivacyEnabled = privacyEnabled,
            enabledShelfFilters = enabledShelfFilters,
            visibleFilters = visibleFilters
        )
    }

    private fun LibraryUiState.applySpicyShelfVisibility(
        spicyShelfRevealed: Boolean
    ): LibraryUiState {
        val showSpicyFilter = !spicyPrivacyEnabled || spicyShelfRevealed
        val visibleFilters = LibraryFilter.visibleFilters(enabledShelfFilters, showSpicyFilter)

        return copy(
            visibleFilters = visibleFilters,
            filter = if (filter !in visibleFilters) {
                LibraryFilter.ALL
            } else {
                filter
            }
        )
    }
}
