package com.emptycastle.novery.ui.screens.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.service.DownloadPriority
import com.emptycastle.novery.service.DownloadRequest
import com.emptycastle.novery.service.DownloadServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "NotificationViewModel"

class NotificationViewModel : ViewModel() {

    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val notificationRepository = RepositoryProvider.getNotificationRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            try {
                combine(
                    notificationRepository.observeNotificationEntries(),
                    libraryRepository.observeLibrary()
                ) { notificationEntries, libraryItems ->
                    val libraryMap = libraryItems.associateBy { it.novel.url }

                    val displayItems = notificationEntries.mapNotNull { entry ->
                        val libraryItem = libraryMap[entry.novelUrl]
                        if (libraryItem != null) {
                            NotificationDisplayItem(
                                libraryItem = libraryItem,
                                notificationEntry = entry,
                                isNew = entry.acknowledgedAt == null
                            )
                        } else {
                            viewModelScope.launch {
                                notificationRepository.removeNotification(entry.novelUrl)
                            }
                            null
                        }
                    }.sortedWith(
                        compareByDescending<NotificationDisplayItem> { it.isNew }
                            .thenByDescending { it.notificationEntry.lastUpdatedAt }
                    )

                    val totalNewChapters = displayItems.sumOf { it.libraryItem.newChapterCount }
                    val unacknowledgedCount = displayItems.count { it.isNew }

                    displayItems to Triple(totalNewChapters, displayItems.size, unacknowledgedCount)
                }.collect { (displayItems, stats) ->
                    val (totalNew, totalCount, unackCount) = stats
                    _uiState.update {
                        it.copy(
                            displayItems = displayItems,
                            totalNewChapters = totalNew,
                            totalNovelsCount = totalCount,
                            unacknowledgedCount = unackCount,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing notifications", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun markAsSeen(novelUrl: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsSeen(novelUrl)
                libraryRepository.acknowledgeNewChapters(novelUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking as seen: $novelUrl", e)
            }
        }
    }

    fun markAllAsSeen() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMarkingAllSeen = true) }
                notificationRepository.markAllAsSeen()
                _uiState.value.displayItems.forEach { item ->
                    libraryRepository.acknowledgeNewChapters(item.libraryItem.novel.url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all as seen", e)
            } finally {
                _uiState.update { it.copy(isMarkingAllSeen = false) }
            }
        }
    }

    fun removeFromNotifications(novelUrl: String) {
        viewModelScope.launch {
            try {
                notificationRepository.removeNotification(novelUrl)
                libraryRepository.acknowledgeNewChapters(novelUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from notifications: $novelUrl", e)
            }
        }
    }

    fun requestClearAll() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            try {
                notificationRepository.clearAllNotifications()
                _uiState.value.displayItems.forEach { item ->
                    libraryRepository.acknowledgeNewChapters(item.libraryItem.novel.url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all notifications", e)
            } finally {
                _uiState.update { it.copy(showClearConfirmation = false) }
            }
        }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun downloadNewChapters(context: Context, item: LibraryItem) {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value
                if (settings.autoDownloadOnWifiOnly && !isOnWifi(context)) {
                    return@launch
                }

                _uiState.update {
                    it.copy(downloadingNovelUrls = it.downloadingNovelUrls + item.novel.url)
                }

                downloadChaptersForItem(context, item, settings.autoDownloadLimit)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading chapters for ${item.novel.name}", e)
            } finally {
                _uiState.update {
                    it.copy(downloadingNovelUrls = it.downloadingNovelUrls - item.novel.url)
                }
            }
        }
    }

    fun downloadAllNewChapters(context: Context) {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value
                if (settings.autoDownloadOnWifiOnly && !isOnWifi(context)) {
                    return@launch
                }

                val items = _uiState.value.displayItems.map { it.libraryItem }
                if (items.isEmpty()) return@launch

                _uiState.update { it.copy(isDownloadingAll = true) }

                items.forEach { item ->
                    try {
                        _uiState.update {
                            it.copy(downloadingNovelUrls = it.downloadingNovelUrls + item.novel.url)
                        }
                        downloadChaptersForItem(context, item, settings.autoDownloadLimit)
                    } finally {
                        _uiState.update {
                            it.copy(downloadingNovelUrls = it.downloadingNovelUrls - item.novel.url)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadAllNewChapters", e)
            } finally {
                _uiState.update { it.copy(isDownloadingAll = false) }
            }
        }
    }

    private suspend fun downloadChaptersForItem(
        context: Context,
        item: LibraryItem,
        downloadLimit: Int
    ) {
        val provider = novelRepository.getProvider(item.novel.apiName) ?: return
        val details = novelRepository.loadNovelDetails(provider, item.novel.url, false).getOrNull() ?: return
        val allChapters = details.chapters ?: return

        val downloadedUrls = try {
            offlineRepository.getDownloadedChapterUrls(item.novel.url)
        } catch (e: Exception) {
            emptySet()
        }

        val newChaptersCount = item.newChapterCount.coerceAtLeast(0)
        val newChapters = if (newChaptersCount > 0 && newChaptersCount <= allChapters.size) {
            allChapters.takeLast(newChaptersCount)
        } else {
            allChapters.asReversed().filter { !downloadedUrls.contains(it.url) }.take(10)
        }

        val chaptersToDownload = newChapters
            .filter { !downloadedUrls.contains(it.url) }
            .let { if (downloadLimit > 0) it.take(downloadLimit) else it }

        if (chaptersToDownload.isEmpty()) return

        val request = DownloadRequest(
            novelUrl = item.novel.url,
            novelName = item.novel.name,
            novelCoverUrl = item.novel.posterUrl,
            providerName = provider.name,
            chapterUrls = chaptersToDownload.map { it.url },
            chapterNames = chaptersToDownload.map { it.name },
            priority = DownloadPriority.NORMAL
        )

        DownloadServiceManager.startDownload(context, request)
    }

    private fun isOnWifi(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    fun getReadingPosition(novelUrl: String) = _uiState.value.displayItems
        .find { it.libraryItem.novel.url == novelUrl }
        ?.libraryItem?.lastReadPosition
}