package com.emptycastle.novery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emptycastle.novery.MainActivity
import com.emptycastle.novery.R
import com.emptycastle.novery.util.NotificationImageLoader
import kotlinx.coroutines.runBlocking

object NotificationHelper {

    const val CHANNEL_DOWNLOAD = "novery_download_channel"
    const val CHANNEL_DOWNLOAD_COMPLETE = "novery_download_complete_channel"
    const val CHANNEL_TTS = "novery_tts_channel"

    const val NOTIFICATION_ID_DOWNLOAD = 1001
    const val NOTIFICATION_ID_DOWNLOAD_COMPLETE = 1002
    const val NOTIFICATION_ID_TTS = 1003

    const val ACTION_DOWNLOAD_PAUSE = "com.emptycastle.novery.action.DOWNLOAD_PAUSE"
    const val ACTION_DOWNLOAD_RESUME = "com.emptycastle.novery.action.DOWNLOAD_RESUME"
    const val ACTION_DOWNLOAD_CANCEL = "com.emptycastle.novery.action.DOWNLOAD_CANCEL"

    const val ACTION_TTS_PLAY = "com.emptycastle.novery.action.TTS_PLAY"
    const val ACTION_TTS_PAUSE = "com.emptycastle.novery.action.TTS_PAUSE"
    const val ACTION_TTS_STOP = "com.emptycastle.novery.action.TTS_STOP"
    const val ACTION_TTS_NEXT = "com.emptycastle.novery.action.TTS_NEXT"
    const val ACTION_TTS_PREVIOUS = "com.emptycastle.novery.action.TTS_PREVIOUS"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows chapter download progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val downloadCompleteChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_COMPLETE,
                "Download Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when downloads complete or fail"
                setShowBadge(true)
            }

            val ttsChannel = NotificationChannel(
                CHANNEL_TTS,
                "Text-to-Speech",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "TTS playback controls"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannels(
                listOf(downloadChannel, downloadCompleteChannel, ttsChannel)
            )
        }
    }

    private fun getMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows chapter download progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // ================================================================
    // DOWNLOAD NOTIFICATIONS
    // ================================================================

    fun buildPreparingNotification(context: Context): Notification {
        ensureChannelExists(context)

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle("Preparing download")
            .setContentText("Please wait...")
            .setSmallIcon(R.drawable.ic_notification_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(context.getColor(R.color.notification_accent))
            .setShowWhen(false)
            .build()
    }

    fun buildDownloadProgressNotification(
        context: Context,
        state: DownloadState,
        coverBitmap: Bitmap?
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val percentage = if (state.totalChapters > 0) {
            (state.currentProgress * 100) / state.totalChapters
        } else 0

        // Clean, professional title
        val title = when {
            state.isPaused -> "Download paused"
            state.queueSize > 0 -> "Downloading • ${state.queueSize} in queue"
            else -> "Downloading"
        }

        // Content: Novel name with percentage
        val contentText = "${state.novelName} • $percentage%"

        // SubText: Chapter progress and stats
        val subText = buildCompactStats(state)

        // Color based on state
        val notificationColor = when {
            state.isPaused -> context.getColor(R.color.notification_paused)
            state.failedCount > 0 -> context.getColor(R.color.notification_warning)
            else -> context.getColor(R.color.notification_accent)
        }

        // Icon based on state
        val smallIcon = when {
            state.isPaused -> R.drawable.ic_notification_pause
            else -> R.drawable.ic_notification_download
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(subText)
            .setSmallIcon(smallIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setProgress(100, percentage, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(notificationColor)
            .setShowWhen(false)
            .setSilent(true)

        // Large icon (cover)
        (coverBitmap ?: state.novelCoverBitmap)?.let {
            builder.setLargeIcon(it)
        }

        // Expanded style with detailed info
        builder.setStyle(buildExpandedProgressStyle(state, percentage))

        // Action buttons
        if (state.isPaused) {
            builder.addAction(
                R.drawable.ic_notification_play,
                "Resume",
                getActionPendingIntent(context, ACTION_DOWNLOAD_RESUME, 1)
            )
        } else {
            builder.addAction(
                R.drawable.ic_notification_pause,
                "Pause",
                getActionPendingIntent(context, ACTION_DOWNLOAD_PAUSE, 1)
            )
        }

        builder.addAction(
            R.drawable.ic_notification_cancel,
            "Cancel",
            getActionPendingIntent(context, ACTION_DOWNLOAD_CANCEL, 2)
        )

        return builder.build()
    }

    private fun buildCompactStats(state: DownloadState): String {
        return buildString {
            append("${state.currentProgress}/${state.totalChapters}")

            if (!state.isPaused) {
                if (state.formattedSpeed.isNotBlank() && state.formattedSpeed != "--") {
                    append(" • ${state.formattedSpeed}")
                }
                if (state.estimatedTimeRemaining.isNotBlank() && state.estimatedTimeRemaining != "--:--") {
                    append(" • ${state.estimatedTimeRemaining}")
                }
            }
        }
    }

    private fun buildExpandedProgressStyle(
        state: DownloadState,
        percentage: Int
    ): NotificationCompat.BigTextStyle {
        val expandedText = buildString {
            // Novel name
            append(state.novelName)
            appendLine()
            appendLine()

            // Progress line
            append("Progress: ${state.currentProgress} of ${state.totalChapters} chapters ($percentage%)")

            // Statistics
            if (state.successCount > 0 || state.failedCount > 0 || state.skippedCount > 0) {
                appendLine()
                val stats = mutableListOf<String>()
                if (state.successCount > 0) stats.add("${state.successCount} saved")
                if (state.skippedCount > 0) stats.add("${state.skippedCount} skipped")
                if (state.failedCount > 0) stats.add("${state.failedCount} failed")
                append(stats.joinToString(" • "))
            }

            // Speed and ETA
            if (!state.isPaused) {
                val hasSpeed = state.formattedSpeed.isNotBlank() && state.formattedSpeed != "--"
                val hasEta = state.estimatedTimeRemaining.isNotBlank() && state.estimatedTimeRemaining != "--:--"

                if (hasSpeed || hasEta) {
                    appendLine()
                    val metrics = mutableListOf<String>()
                    if (hasSpeed) metrics.add("Speed: ${state.formattedSpeed}")
                    if (hasEta) metrics.add("Remaining: ${state.estimatedTimeRemaining}")
                    append(metrics.joinToString(" • "))
                }
            }

            // Current chapter
            val chapterName = state.currentChapterName
            if (chapterName.isNotBlank() &&
                !chapterName.startsWith("Starting") &&
                !chapterName.startsWith("Downloading:")
            ) {
                appendLine()
                appendLine()
                val displayName = chapterName.ellipsize(50)
                append("Current: $displayName")
            }

            // Queue info
            if (state.queueSize > 0) {
                appendLine()
                appendLine()
                val novelLabel = if (state.queueSize == 1) "novel" else "novels"
                append("${state.queueSize} more $novelLabel queued")
            }
        }

        val bigTitle = if (state.isPaused) "Download paused" else "Downloading..."

        return NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .setBigContentTitle(bigTitle)
    }

    fun buildDownloadCompleteNotification(
        context: Context,
        novelName: String,
        novelCoverUrl: String?,
        chaptersDownloaded: Int,
        totalChapters: Int,
        failedCount: Int,
        elapsedTimeMs: Long,
        queueRemaining: Int = 0
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val completelyFailed = failedCount == totalChapters && chaptersDownloaded == 0
        val hasErrors = failedCount > 0

        val title = when {
            completelyFailed -> "Download failed"
            hasErrors -> "Download complete with errors"
            else -> "Download complete"
        }

        val contentText = novelName

        val subText = buildString {
            append("$chaptersDownloaded chapters")
            if (failedCount > 0) append(" • $failedCount failed")
            append(" • ${formatDuration(elapsedTimeMs)}")
        }

        val iconRes = when {
            completelyFailed -> R.drawable.ic_notification_error
            hasErrors -> R.drawable.ic_notification_warning
            else -> R.drawable.ic_notification_done
        }

        val colorRes = when {
            completelyFailed -> R.color.notification_error
            hasErrors -> R.color.notification_warning
            else -> R.color.notification_success
        }

        val expandedText = buildString {
            append(novelName)
            appendLine()
            appendLine()

            append("Downloaded: $chaptersDownloaded of $totalChapters chapters")
            if (failedCount > 0) {
                appendLine()
                append("Failed: $failedCount chapters")
            }
            appendLine()
            append("Duration: ${formatDuration(elapsedTimeMs)}")

            if (queueRemaining > 0) {
                appendLine()
                appendLine()
                val label = if (queueRemaining == 1) "download" else "downloads"
                append("$queueRemaining more $label in progress")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_COMPLETE)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(subText)
            .setSmallIcon(iconRes)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(context.getColor(colorRes))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(title)
            )

        if (!novelCoverUrl.isNullOrBlank()) {
            try {
                val bitmap = runBlocking {
                    NotificationImageLoader.loadImage(context, novelCoverUrl, rounded = true)
                }
                bitmap?.let { builder.setLargeIcon(it) }
            } catch (_: Exception) { }
        }

        return builder.build()
    }

    fun buildDownloadErrorNotification(
        context: Context,
        novelName: String,
        novelCoverUrl: String?,
        errorMessage: String,
        chaptersCompleted: Int = 0,
        totalChapters: Int = 0
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val expandedText = buildString {
            append(novelName)
            appendLine()
            appendLine()
            append("Error: $errorMessage")

            if (chaptersCompleted > 0 && totalChapters > 0) {
                appendLine()
                appendLine()
                append("Progress saved: $chaptersCompleted of $totalChapters chapters")
                appendLine()
                append("You can resume this download later.")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_COMPLETE)
            .setContentTitle("Download failed")
            .setContentText(novelName)
            .setSubText(errorMessage.ellipsize(40))
            .setSmallIcon(R.drawable.ic_notification_error)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setColor(context.getColor(R.color.notification_error))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle("Download failed")
            )

        if (!novelCoverUrl.isNullOrBlank()) {
            try {
                val bitmap = runBlocking {
                    NotificationImageLoader.loadImage(context, novelCoverUrl, rounded = true)
                }
                bitmap?.let { builder.setLargeIcon(it) }
            } catch (_: Exception) { }
        }

        return builder.build()
    }

    // ================================================================
    // TTS NOTIFICATIONS
    // ================================================================

    fun buildTTSNotification(
        context: Context,
        novelName: String,
        chapterName: String,
        novelCoverUrl: String?,
        isPlaying: Boolean,
        currentSegment: Int,
        totalSegments: Int
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val title = if (isPlaying) "Now playing" else "Paused"
        val subText = "Segment ${currentSegment + 1} of $totalSegments"

        val expandedText = buildString {
            append(novelName)
            appendLine()
            append(chapterName)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TTS)
            .setContentTitle(title)
            .setContentText(novelName)
            .setSubText(subText)
            .setSmallIcon(R.drawable.ic_notification_tts)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(context.getColor(R.color.notification_tts))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(title)
                    .setSummaryText(subText)
            )

        if (!novelCoverUrl.isNullOrBlank()) {
            try {
                val bitmap = runBlocking {
                    NotificationImageLoader.loadImage(context, novelCoverUrl, rounded = true)
                }
                bitmap?.let { builder.setLargeIcon(it) }
            } catch (_: Exception) { }
        }

        builder.addAction(
            R.drawable.ic_notification_previous,
            "Previous",
            getActionPendingIntent(context, ACTION_TTS_PREVIOUS, 10)
        )

        if (isPlaying) {
            builder.addAction(
                R.drawable.ic_notification_pause,
                "Pause",
                getActionPendingIntent(context, ACTION_TTS_PAUSE, 11)
            )
        } else {
            builder.addAction(
                R.drawable.ic_notification_play,
                "Play",
                getActionPendingIntent(context, ACTION_TTS_PLAY, 11)
            )
        }

        builder.addAction(
            R.drawable.ic_notification_next,
            "Next",
            getActionPendingIntent(context, ACTION_TTS_NEXT, 12)
        )

        builder.setDeleteIntent(
            getActionPendingIntent(context, ACTION_TTS_STOP, 13)
        )

        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
        )

        return builder.build()
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val m = seconds / 60
                val s = seconds % 60
                if (s > 0) "${m}m ${s}s" else "${m}m"
            }
            else -> {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            }
        }
    }

    private fun String.ellipsize(maxLength: Int): String {
        return if (length > maxLength) take(maxLength - 3) + "..." else this
    }

    fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        getNotificationManager(context).cancel(notificationId)
    }

    fun cancelAllNotifications(context: Context) {
        getNotificationManager(context).cancelAll()
    }
}