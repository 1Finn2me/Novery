package com.emptycastle.novery.ui.screens.home.shared

import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.ReadingPosition
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.ui.components.NovelActionSheetData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActionSheetSource {
    LIBRARY, BROWSE, SEARCH, HISTORY
}

data class ActionSheetState(
    val isVisible: Boolean = false,
    val data: NovelActionSheetData? = null,
    val source: ActionSheetSource? = null,
    val historyItem: HistoryItem? = null
)

/**
 * Manager for action sheet display.
 * Each ViewModel should have its own instance.
 */
class ActionSheetManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val offlineRepository by lazy { RepositoryProvider.getOfflineRepository() }
    private val historyRepository by lazy { RepositoryProvider.getHistoryRepository() }
    private val libraryRepository by lazy { RepositoryProvider.getLibraryRepository() }

    private val _state = MutableStateFlow(ActionSheetState())
    val state: StateFlow<ActionSheetState> = _state.asStateFlow()

    // Cache for novel details
    private val detailsCache = mutableMapOf<String, NovelDetails>()
    private val readCountCache = mutableMapOf<String, Int>()
    private val downloadedCountCache = mutableMapOf<String, Int>()

    fun show(
        novel: Novel,
        source: ActionSheetSource,
        lastChapterName: String? = null,
        historyItem: HistoryItem? = null,
        libraryItem: LibraryItem? = null
    ) {
        // Prevent showing if already visible
        if (_state.value.isVisible) {
            return
        }

        val isInLibrary = LibraryStateHolder.isInLibrary(novel.url)
        val cachedDetails = detailsCache[novel.url]
        val readCount = readCountCache[novel.url]
        val downloadedCount = downloadedCountCache[novel.url]

        // Get library item if not provided
        val resolvedLibraryItem = libraryItem ?: LibraryStateHolder.getLibraryItem(novel.url)

        // Build initial data from available information
        val initialData = NovelActionSheetData(
            novel = novel,
            synopsis = cachedDetails?.synopsis,
            isInLibrary = isInLibrary,
            lastChapterName = lastChapterName
                ?: resolvedLibraryItem?.lastReadPosition?.chapterName
                ?: historyItem?.chapterName,
            providerName = novel.apiName,
            readingStatus = resolvedLibraryItem?.readingStatus,
            author = cachedDetails?.author,
            tags = cachedDetails?.tags,
            rating = cachedDetails?.rating,
            votes = cachedDetails?.peopleVoted,
            chapterCount = cachedDetails?.chapters?.size,
            readCount = readCount,
            downloadedCount = downloadedCount
        )

        _state.update {
            ActionSheetState(
                isVisible = true,
                data = initialData,
                source = source,
                historyItem = historyItem
            )
        }

        // Fetch additional data in background
        fetchAdditionalData(novel.url, resolvedLibraryItem)
    }

    fun hide() {
        _state.update { ActionSheetState() }
    }

    private fun fetchAdditionalData(novelUrl: String, libraryItem: LibraryItem?) {
        scope.launch {
            val details = detailsCache[novelUrl]
                ?: offlineRepository.getNovelDetails(novelUrl)

            if (details != null) {
                detailsCache[novelUrl] = details
            }

            val readCount = try {
                historyRepository.getReadChapterCount(novelUrl)
            } catch (e: Exception) {
                0
            }
            readCountCache[novelUrl] = readCount

            val downloadedCount = try {
                offlineRepository.getDownloadedCount(novelUrl)
            } catch (e: Exception) {
                0
            }
            downloadedCountCache[novelUrl] = downloadedCount

            // Update state if still showing this novel
            val current = _state.value
            if (current.isVisible && current.data?.novel?.url == novelUrl) {
                _state.update { state ->
                    state.copy(
                        data = state.data?.copy(
                            synopsis = details?.synopsis ?: state.data.synopsis,
                            author = details?.author ?: state.data.author,
                            tags = details?.tags ?: state.data.tags,
                            rating = details?.rating ?: state.data.rating,
                            votes = details?.peopleVoted ?: state.data.votes,
                            chapterCount = details?.chapters?.size ?: state.data.chapterCount,
                            readCount = readCount,
                            downloadedCount = downloadedCount,
                            readingStatus = libraryItem?.readingStatus ?: state.data.readingStatus
                        )
                    )
                }
            }
        }
    }

    /**
     * Update the reading status for the currently shown novel
     */
    fun updateReadingStatus(status: ReadingStatus) {
        val currentData = _state.value.data ?: return
        val novelUrl = currentData.novel.url

        scope.launch {
            try {
                libraryRepository.updateStatus(novelUrl, status)

                // Update local state immediately for responsiveness
                _state.update { state ->
                    state.copy(
                        data = state.data?.copy(readingStatus = status)
                    )
                }
            } catch (e: Exception) {
                // Handle error silently or log
            }
        }
    }

    /**
     * Refresh library status for current novel
     */
    fun refreshLibraryStatus() {
        val currentData = _state.value.data ?: return
        val novelUrl = currentData.novel.url
        val isInLibrary = LibraryStateHolder.isInLibrary(novelUrl)
        val libraryItem = LibraryStateHolder.getLibraryItem(novelUrl)

        _state.update { state ->
            state.copy(
                data = state.data?.copy(
                    isInLibrary = isInLibrary,
                    readingStatus = libraryItem?.readingStatus
                )
            )
        }
    }

    // Library actions
    suspend fun addToLibrary(novel: Novel): Boolean {
        return try {
            libraryRepository.addToLibrary(novel)
            refreshLibraryStatus()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromLibrary(novelUrl: String): Boolean {
        return try {
            libraryRepository.removeFromLibrary(novelUrl)
            offlineRepository.deleteNovelDownloads(novelUrl)
            refreshLibraryStatus()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromHistory(novelUrl: String): Boolean {
        return try {
            historyRepository.removeFromHistory(novelUrl)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Reading position helpers
    fun getReadingPosition(novelUrl: String): ReadingPosition? {
        return LibraryStateHolder.getLibraryItem(novelUrl)?.lastReadPosition
    }

    suspend fun getHistoryChapter(novelUrl: String): Pair<String, String>? {
        val item = historyRepository.getLastRead(novelUrl)
        return item?.let { Pair(it.chapterUrl, it.chapterName) }
    }

    /**
     * Get the chapter URL to continue reading from
     */
    suspend fun getContinueReadingChapter(novelUrl: String): Triple<String, String, String>? {
        val libraryItem = LibraryStateHolder.getLibraryItem(novelUrl)
        libraryItem?.lastReadPosition?.let { pos ->
            return Triple(pos.chapterUrl, novelUrl, libraryItem.novel.apiName)
        }

        val historyEntry = historyRepository.getLastRead(novelUrl)
        historyEntry?.let {
            return Triple(it.chapterUrl, novelUrl, it.apiName)
        }

        val details = detailsCache[novelUrl]
        if (details != null && details.chapters.isNotEmpty()) {
            val currentData = _state.value.data
            val providerName = currentData?.novel?.apiName ?: return null
            return Triple(details.chapters.first().url, novelUrl, providerName)
        }

        return null
    }
}