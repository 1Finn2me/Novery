package com.emptycastle.novery.ui.screens.home.tabs.library

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.EpubImportRepository
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.LibraryRepository
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.ImportedBooksDisplay
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

class LibraryViewModel : ViewModel() {

    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val notificationRepository = RepositoryProvider.getNotificationRepository()
    private val epubImportRepository by lazy { RepositoryProvider.getEpubImportRepository() }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val actionSheetManager = ActionSheetManager()
    val actionSheetState: StateFlow<com.emptycastle.novery.ui.screens.home.shared.ActionSheetState> = actionSheetManager.state

    init {
        initialize()
        observeNewChapterCount()
        loadImportSettings()

        viewModelScope.launch {
            preferencesManager.appSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        filter = settings.defaultLibraryFilter,
                        sortOrder = settings.defaultLibrarySort
                    )
                }
                applyFilters()
            }
        }
    }

    private fun loadImportSettings() {
        _uiState.update {
            it.copy(
                showImportButton = preferencesManager.getShowImportButton(),
                importedBooksDisplay = preferencesManager.getImportedBooksDisplay()
            )
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value
                _uiState.update {
                    it.copy(
                        filter = settings.defaultLibraryFilter,
                        sortOrder = settings.defaultLibrarySort
                    )
                }

                libraryRepository.observeLibrary().collect { items ->
                    val counts = offlineRepository.getAllDownloadCounts()
                    _uiState.update { state ->
                        state.copy(
                            items = items,
                            downloadCounts = counts,
                            isLoading = false
                        )
                    }
                    applyFilters()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing library", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeNewChapterCount() {
        viewModelScope.launch {
            try {
                libraryRepository.observeTotalNewChapterCount().collect { count ->
                    _uiState.update {
                        it.copy(
                            totalNewChapters = count,
                            showNewChaptersCard = if (count > 0) true else it.showNewChaptersCard
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing new chapter count", e)
            }
        }
    }

    // ================================================================
    // SECTION MODE
    // ================================================================

    fun setSelectedSection(section: LibrarySection) {
        _uiState.update { it.copy(selectedSection = section) }
        applyFilters()
    }

    // ================================================================
    // IMPORTED NOVEL HELPERS
    // ================================================================

    /**
     * Check if a novel is imported (local EPUB)
     */
    fun isImportedNovel(novelUrl: String): Boolean {
        return epubImportRepository.isImportedNovel(novelUrl)
    }

    /**
     * Check if a novel is imported by checking the apiName
     */
    fun isImportedNovelByProvider(apiName: String): Boolean {
        return apiName == LibraryRepository.IMPORTED_PROVIDER_NAME
    }

    /**
     * Get the first chapter URL for an imported novel
     */
    suspend fun getFirstChapterUrl(novelUrl: String): String? {
        val details = offlineRepository.getNovelDetails(novelUrl)
        return details?.chapters?.firstOrNull()?.url
    }

    // ================================================================
    // FILTER & SORT
    // ================================================================

    fun setFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilters()
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
        val displayMode = state.importedBooksDisplay

        // First, separate items based on display mode
        val (baseItems, shouldShowImportedFilter) = when (displayMode) {
            ImportedBooksDisplay.MIXED -> {
                // MIXED: All items together, no special filtering
                state.items to false
            }
            ImportedBooksDisplay.FILTER -> {
                // FILTER: When IMPORTED filter selected, show only imported
                // When other filters selected, exclude imported
                if (state.filter == LibraryFilter.IMPORTED) {
                    state.items.filter {
                        it.novel.apiName == LibraryRepository.IMPORTED_PROVIDER_NAME
                    } to true
                } else {
                    state.items.filter {
                        it.novel.apiName != LibraryRepository.IMPORTED_PROVIDER_NAME
                    } to true
                }
            }
            ImportedBooksDisplay.SECTION -> {
                // SECTION: Filter based on selected section
                when (state.selectedSection) {
                    LibrarySection.ONLINE -> state.items.filter {
                        it.novel.apiName != LibraryRepository.IMPORTED_PROVIDER_NAME
                    }
                    LibrarySection.LOCAL -> state.items.filter {
                        it.novel.apiName == LibraryRepository.IMPORTED_PROVIDER_NAME
                    }
                } to false
            }
        }

        // Search filter
        val searched = if (searchQuery.isBlank()) {
            baseItems
        } else {
            baseItems.filter { item ->
                item.novel.name.lowercase().contains(searchQuery) ||
                        item.novel.apiName.lowercase().contains(searchQuery) ||
                        item.readingStatus.displayName().lowercase().contains(searchQuery)
            }
        }

        // Category filter (skip IMPORTED filter in FILTER mode since we already handled it)
        val filtered = when (state.filter) {
            LibraryFilter.ALL -> searched
            LibraryFilter.DOWNLOADED -> searched.filter {
                (downloadCounts[it.novel.url] ?: 0) > 0
            }
            LibraryFilter.IMPORTED -> {
                // In FILTER mode, this is already handled above
                // In other modes, filter to imported only
                if (displayMode == ImportedBooksDisplay.FILTER) {
                    searched
                } else {
                    searched.filter {
                        it.novel.apiName == LibraryRepository.IMPORTED_PROVIDER_NAME
                    }
                }
            }
            LibraryFilter.READING -> searched.filter { it.readingStatus == ReadingStatus.READING }
            LibraryFilter.COMPLETED -> searched.filter { it.readingStatus == ReadingStatus.COMPLETED }
            LibraryFilter.ON_HOLD -> searched.filter { it.readingStatus == ReadingStatus.ON_HOLD }
            LibraryFilter.PLAN_TO_READ -> searched.filter { it.readingStatus == ReadingStatus.PLAN_TO_READ }
            LibraryFilter.DROPPED -> searched.filter { it.readingStatus == ReadingStatus.DROPPED }
        }

        // Sort
        val sorted = when (state.sortOrder) {
            LibrarySortOrder.NEW_CHAPTERS -> filtered.sortedByDescending { it.newChapterCount }
            LibrarySortOrder.LAST_READ -> filtered.sortedByDescending {
                it.lastReadPosition?.timestamp ?: it.addedAt
            }
            LibrarySortOrder.TITLE_ASC -> filtered.sortedBy { it.novel.name.lowercase() }
            LibrarySortOrder.TITLE_DESC -> filtered.sortedByDescending { it.novel.name.lowercase() }
            LibrarySortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
            LibrarySortOrder.UNREAD_COUNT -> filtered.sortedByDescending { it.unreadChapterCount }
        }

        _uiState.update { it.copy(filteredItems = sorted) }
    }

    // ================================================================
    // EPUB IMPORT
    // ================================================================

    fun importEpub(uri: Uri) {
        if (_uiState.value.isImporting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = EpubImportRepository.ImportProgress(0f, "Starting import...")
                )
            }

            try {
                val result = epubImportRepository.importEpub(uri) { progress ->
                    _uiState.update { it.copy(importProgress = progress) }
                }

                result.fold(
                    onSuccess = { novel ->
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                importProgress = null,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                importProgress = null,
                                error = error.message ?: "Import failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        error = e.message ?: "Import failed"
                    )
                }
            }
        }
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
        // Skip imported novels - they're already fully downloaded
        if (item.novel.apiName == LibraryRepository.IMPORTED_PROVIDER_NAME) {
            return
        }

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
                            item.novel.apiName != LibraryRepository.IMPORTED_PROVIDER_NAME &&
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
            val currentFilter = _uiState.value.filter
            val currentDownloadCounts = _uiState.value.downloadCounts

            val filterDisplayName = when (currentFilter) {
                LibraryFilter.ALL -> "all novels"
                LibraryFilter.DOWNLOADED -> "downloaded novels"
                LibraryFilter.IMPORTED -> "imported novels"
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

                novelsWithNewChapters = result.updatedCount
                totalNewChapters = result.totalNewChapters

                if (totalNewChapters > 0) {
                    val novelsWithNew = _uiState.value.items.filter { it.hasNewChapters }
                    novelsWithNew.forEach { item ->
                        notificationRepository.addOrUpdateNotification(
                            novelUrl = item.novel.url,
                            providerName = item.novel.apiName
                        )
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
                // If it's an imported novel, also delete the imported data
                if (isImportedNovel(novelUrl)) {
                    epubImportRepository.deleteImportedNovel(novelUrl)
                } else {
                    actionSheetManager.removeFromLibrary(novelUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from library: $novelUrl", e)
            }
        }
    }

    fun getReadingPosition(novelUrl: String) = actionSheetManager.getReadingPosition(novelUrl)
}