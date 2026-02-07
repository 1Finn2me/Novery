package com.emptycastle.novery.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.service.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadedNovel(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String?,
    val sourceName: String,
    val downloadedChapters: Int,
    val totalChapters: Int = 0
)

data class ActiveDownload(
    val novelUrl: String,
    val novelName: String,
    val coverUrl: String?,
    val currentChapterName: String,
    val downloadedCount: Int,
    val totalCount: Int,
    val progress: Float,
    val isPaused: Boolean = false,
    val speed: String = "",
    val eta: String = ""
)

data class DownloadsUiState(
    val isLoading: Boolean = true,
    val downloadedNovels: List<DownloadedNovel> = emptyList(),
    val activeDownloads: List<ActiveDownload> = emptyList(),
    val totalStorageUsed: String = "0 MB"
)

class DownloadsViewModel : ViewModel() {

    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        observeActiveDownloads()
    }

    private fun observeActiveDownloads() {
        viewModelScope.launch {
            DownloadServiceManager.downloadState.collect { downloadState ->
                updateActiveDownloads(downloadState)
            }
        }
    }

    private fun updateActiveDownloads(downloadState: DownloadState) {
        val activeList = mutableListOf<ActiveDownload>()

        // Current download
        if (downloadState.isActive || downloadState.isPaused) {
            activeList.add(
                ActiveDownload(
                    novelUrl = downloadState.novelUrl,
                    novelName = downloadState.novelName,
                    coverUrl = downloadState.novelCoverUrl,
                    currentChapterName = downloadState.currentChapterName,
                    downloadedCount = downloadState.currentProgress,
                    totalCount = downloadState.totalChapters,
                    progress = downloadState.progressPercent,
                    isPaused = downloadState.isPaused,
                    speed = downloadState.formattedSpeed,
                    eta = downloadState.estimatedTimeRemaining
                )
            )
        }

        // Queued downloads
        downloadState.queuedDownloads.forEach { queued ->
            activeList.add(
                ActiveDownload(
                    novelUrl = queued.novelUrl,
                    novelName = queued.novelName,
                    coverUrl = queued.novelCoverUrl,
                    currentChapterName = "Queued",
                    downloadedCount = 0,
                    totalCount = queued.chapterCount,
                    progress = 0f,
                    isPaused = false
                )
            )
        }

        _uiState.update { it.copy(activeDownloads = activeList) }

        // Refresh downloaded novels when a download completes
        if (!downloadState.isActive && !downloadState.isPaused && _uiState.value.downloadedNovels.isEmpty()) {
            loadDownloads()
        }
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get all novels with download counts
                val downloadCounts = offlineRepository.getAllDownloadCounts()

                val downloadedNovels = downloadCounts.mapNotNull { (novelUrl, count) ->
                    if (count <= 0) return@mapNotNull null

                    // Try to get novel details from offline cache first
                    val offlineDetails = offlineRepository.getNovelDetails(novelUrl)

                    // If not in offline cache, try library
                    val libraryItem = libraryRepository.getLibraryItem(novelUrl)

                    // Get the best available info
                    val novelName = offlineDetails?.name
                        ?: libraryItem?.novel?.name
                        ?: extractNameFromUrl(novelUrl)

                    val coverUrl = offlineDetails?.posterUrl
                        ?: libraryItem?.novel?.posterUrl

                    val sourceName = libraryItem?.novel?.apiName
                        ?: extractSourceFromUrl(novelUrl)

                    DownloadedNovel(
                        novelUrl = novelUrl,
                        novelName = novelName,
                        coverUrl = coverUrl,
                        sourceName = sourceName,
                        downloadedChapters = count
                    )
                }.sortedByDescending { it.downloadedChapters }

                // Calculate approximate storage (rough estimate: ~10KB per chapter average)
                val totalChapters = downloadCounts.values.sum()
                val estimatedMB = (totalChapters * 10) / 1024.0
                val storageString = if (estimatedMB < 1) {
                    "${(estimatedMB * 1024).toInt()} KB"
                } else {
                    String.format("%.1f MB", estimatedMB)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadedNovels = downloadedNovels,
                        totalStorageUsed = storageString
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteNovelDownloads(novelUrl: String) {
        viewModelScope.launch {
            try {
                offlineRepository.deleteNovelDownloads(novelUrl)
                loadDownloads()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseDownload() {
        DownloadServiceManager.pauseDownload()
    }

    fun resumeDownload() {
        DownloadServiceManager.resumeDownload()
    }

    fun cancelDownload() {
        DownloadServiceManager.cancelDownload()
    }

    fun removeFromQueue(novelUrl: String) {
        DownloadServiceManager.removeFromQueue(novelUrl)
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            url.substringAfterLast("/")
                .replace("-", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        } catch (e: Exception) {
            "Unknown Novel"
        }
    }

    private fun extractSourceFromUrl(url: String): String {
        return try {
            val host = url.removePrefix("https://").removePrefix("http://")
                .substringBefore("/")
                .removePrefix("www.")
            host.substringBefore(".").replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }
}