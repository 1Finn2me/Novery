package com.emptycastle.novery.service

/**
 * Represents the current state of a download operation
 */
data class DownloadState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val novelName: String = "",
    val novelUrl: String = "",
    val currentChapterName: String = "",
    val currentProgress: Int = 0,
    val totalChapters: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val error: String? = null
) {
    val progressPercent: Float
        get() = if (totalChapters > 0) currentProgress.toFloat() / totalChapters else 0f

    val isComplete: Boolean
        get() = !isActive && !isPaused && currentProgress >= totalChapters && totalChapters > 0
}

/**
 * Request to start a download
 */
data class DownloadRequest(
    val novelUrl: String,
    val novelName: String,
    val novelCoverUrl: String?,
    val providerName: String,
    val chapterUrls: List<String>,
    val chapterNames: List<String>
) {
    init {
        require(chapterUrls.size == chapterNames.size) {
            "Chapter URLs and names must have the same size"
        }
    }

    val totalChapters: Int get() = chapterUrls.size
}