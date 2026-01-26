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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "NotificationViewModel"

class NotificationViewModel : ViewModel() {

    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNovelsWithUpdates()
    }

    private fun loadNovelsWithUpdates() {
        viewModelScope.launch {
            try {
                libraryRepository.observeLibrary().collect { items ->
                    val novelsWithNew = items.filter { it.hasNewChapters }
                    val totalNew = novelsWithNew.sumOf { it.newChapterCount }

                    _uiState.update {
                        it.copy(
                            novelsWithUpdates = novelsWithNew,
                            totalNewChapters = totalNew,
                            novelsCount = novelsWithNew.size,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading novels with updates", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ================================================================
    // MARK AS SEEN
    // ================================================================

    fun markAsSeen(novelUrl: String) {
        viewModelScope.launch {
            try {
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

                val items = _uiState.value.novelsWithUpdates
                items.forEach { item ->
                    libraryRepository.acknowledgeNewChapters(item.novel.url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all as seen", e)
            } finally {
                _uiState.update { it.copy(isMarkingAllSeen = false) }
            }
        }
    }

    // ================================================================
    // DOWNLOAD
    // ================================================================

    fun downloadNewChapters(context: Context, item: LibraryItem) {
        viewModelScope.launch {
            try {
                val settings = preferencesManager.appSettings.value

                if (settings.autoDownloadOnWifiOnly && !isOnWifi(context)) {
                    Log.d(TAG, "Skipping download - not on WiFi")
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
                    Log.d(TAG, "Skipping download - not on WiFi")
                    return@launch
                }

                val novelsWithNew = _uiState.value.novelsWithUpdates
                if (novelsWithNew.isEmpty()) {
                    Log.d(TAG, "No novels with new chapters to download")
                    return@launch
                }

                _uiState.update { it.copy(isDownloadingAll = true) }

                novelsWithNew.forEach { item ->
                    try {
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
    // READING POSITION
    // ================================================================

    fun getReadingPosition(novelUrl: String) = _uiState.value.novelsWithUpdates
        .find { it.novel.url == novelUrl }
        ?.lastReadPosition
}