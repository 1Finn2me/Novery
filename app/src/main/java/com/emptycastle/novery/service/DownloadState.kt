package com.emptycastle.novery.service

import android.graphics.Bitmap
import java.util.concurrent.TimeUnit

/**
 * Represents a queued download waiting to be processed
 */
data class QueuedDownload(
    val id: String,
    val novelUrl: String,
    val novelName: String,
    val novelCoverUrl: String?,
    val chapterCount: Int,
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Represents the current state of a download operation
 */
data class DownloadState(
    // Current download info
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val novelName: String = "",
    val novelUrl: String = "",
    val novelCoverUrl: String? = null,
    val novelCoverBitmap: Bitmap? = null,
    val currentChapterName: String = "",
    val currentProgress: Int = 0,
    val totalChapters: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val error: String? = null,
    val startTimeMillis: Long = 0L,
    val bytesDownloaded: Long = 0L,
    val downloadSpeed: Long = 0L,
    val retryCount: Int = 0,

    // Queue info
    val queuedDownloads: List<QueuedDownload> = emptyList(),
    val totalQueuedChapters: Int = 0
) {
    val progressPercent: Float
        get() = if (totalChapters > 0) currentProgress.toFloat() / totalChapters else 0f

    val progressPercentInt: Int
        get() = (progressPercent * 100).toInt()

    val isComplete: Boolean
        get() = !isActive && !isPaused && currentProgress >= totalChapters && totalChapters > 0

    val hasError: Boolean
        get() = error != null

    val remainingChapters: Int
        get() = (totalChapters - currentProgress).coerceAtLeast(0)

    val hasQueue: Boolean
        get() = queuedDownloads.isNotEmpty()

    val queueSize: Int
        get() = queuedDownloads.size

    val estimatedTimeRemaining: String
        get() {
            if (downloadSpeed <= 0 || remainingChapters <= 0) return "--:--"

            val estimatedBytesRemaining = remainingChapters * 5 * 1024L
            val secondsRemaining = estimatedBytesRemaining / downloadSpeed.coerceAtLeast(1)

            return formatDuration(secondsRemaining)
        }

    val formattedSpeed: String
        get() = when {
            downloadSpeed <= 0 -> "--"
            downloadSpeed < 1024 -> "${downloadSpeed} B/s"
            downloadSpeed < 1024 * 1024 -> "${downloadSpeed / 1024} KB/s"
            else -> String.format("%.1f MB/s", downloadSpeed / (1024.0 * 1024.0))
        }

    val elapsedTime: String
        get() {
            if (startTimeMillis <= 0) return "00:00"
            val elapsed = System.currentTimeMillis() - startTimeMillis
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed)
            return formatDuration(seconds)
        }

    val statusText: String
        get() = when {
            error != null -> "Error: $error"
            isPaused -> "Paused"
            isActive -> "Downloading..."
            isComplete -> "Complete"
            else -> "Idle"
        }

    private fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                "${minutes}m ${secs}s"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
        }
    }
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
    val chapterNames: List<String>,
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val retryOnFailure: Boolean = true,
    val maxRetries: Int = 3
) {
    init {
        require(chapterUrls.size == chapterNames.size) {
            "Chapter URLs and names must have the same size"
        }
    }

    val totalChapters: Int get() = chapterUrls.size
    val id: String get() = "${novelUrl}_${System.currentTimeMillis()}"

    fun toQueuedDownload(): QueuedDownload = QueuedDownload(
        id = id,
        novelUrl = novelUrl,
        novelName = novelName,
        novelCoverUrl = novelCoverUrl,
        chapterCount = totalChapters,
        priority = priority
    )
}

enum class DownloadPriority {
    LOW, NORMAL, HIGH
}

data class DownloadResult(
    val novelUrl: String,
    val novelName: String,
    val novelCoverUrl: String?,
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val totalChapters: Int,
    val elapsedTimeMs: Long,
    val bytesDownloaded: Long
) {
    val isFullySuccessful: Boolean get() = failedCount == 0
    val successRate: Float get() = if (totalChapters > 0) successCount.toFloat() / totalChapters else 0f
}

sealed class ChapterDownloadResult {
    data class Success(val chapterUrl: String, val bytesDownloaded: Long) : ChapterDownloadResult()
    data class Failed(val chapterUrl: String, val error: String, val retryable: Boolean = true) : ChapterDownloadResult()
    data class Skipped(val chapterUrl: String, val reason: String) : ChapterDownloadResult()
}