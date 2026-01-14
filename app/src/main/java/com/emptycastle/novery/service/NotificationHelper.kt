package com.emptycastle.novery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emptycastle.novery.MainActivity

/**
 * Helper object for creating and managing notifications.
 * Handles notification channels and builders for downloads and TTS.
 */
object NotificationHelper {

    // Channel IDs
    const val CHANNEL_DOWNLOAD = "novery_download_channel"
    const val CHANNEL_TTS = "novery_tts_channel"

    // Notification IDs
    const val NOTIFICATION_ID_DOWNLOAD = 1001
    const val NOTIFICATION_ID_DOWNLOAD_COMPLETE = 1002
    const val NOTIFICATION_ID_TTS = 1003

    // Download Actions
    const val ACTION_DOWNLOAD_PAUSE = "com.emptycastle.novery.action.DOWNLOAD_PAUSE"
    const val ACTION_DOWNLOAD_RESUME = "com.emptycastle.novery.action.DOWNLOAD_RESUME"
    const val ACTION_DOWNLOAD_CANCEL = "com.emptycastle.novery.action.DOWNLOAD_CANCEL"

    // TTS Actions
    const val ACTION_TTS_PLAY = "com.emptycastle.novery.action.TTS_PLAY"
    const val ACTION_TTS_PAUSE = "com.emptycastle.novery.action.TTS_PAUSE"
    const val ACTION_TTS_STOP = "com.emptycastle.novery.action.TTS_STOP"
    const val ACTION_TTS_NEXT = "com.emptycastle.novery.action.TTS_NEXT"
    const val ACTION_TTS_PREVIOUS = "com.emptycastle.novery.action.TTS_PREVIOUS"

    /**
     * Create notification channels. Call this in Application.onCreate()
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Download channel - low importance (no sound)
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Chapter download progress and status"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // TTS channel - low importance (no sound, controls only)
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

            notificationManager.createNotificationChannels(listOf(downloadChannel, ttsChannel))
        }
    }

    /**
     * Get PendingIntent to open the main activity
     */
    fun getMainActivityPendingIntent(context: Context): PendingIntent {
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

    /**
     * Create a PendingIntent for a broadcast action
     */
    fun getActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
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

    // ================================================================
    // DOWNLOAD NOTIFICATIONS
    // ================================================================

    /**
     * Build a download progress notification
     */
    fun buildDownloadProgressNotification(
        context: Context,
        novelName: String,
        chapterName: String,
        progress: Int,
        total: Int,
        isPaused: Boolean
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle(if (isPaused) "Download Paused" else "Downloading")
            .setContentText(novelName)
            .setSubText("$progress / $total chapters")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setProgress(total, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        // Add info about current chapter
        if (chapterName.isNotBlank()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$novelName\n$chapterName")
                    .setSummaryText("$progress / $total chapters")
            )
        }

        // Add Pause/Resume action
        if (isPaused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                getActionPendingIntent(context, ACTION_DOWNLOAD_RESUME, 1)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                getActionPendingIntent(context, ACTION_DOWNLOAD_PAUSE, 1)
            )
        }

        // Add Cancel action
        builder.addAction(
            android.R.drawable.ic_delete,
            "Cancel",
            getActionPendingIntent(context, ACTION_DOWNLOAD_CANCEL, 2)
        )

        return builder.build()
    }

    /**
     * Build a download complete notification
     */
    fun buildDownloadCompleteNotification(
        context: Context,
        novelName: String,
        chaptersDownloaded: Int,
        totalChapters: Int
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle("Download Complete")
            .setContentText(novelName)
            .setSubText("$chaptersDownloaded chapters downloaded")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    /**
     * Build a download error notification
     */
    fun buildDownloadErrorNotification(
        context: Context,
        novelName: String,
        errorMessage: String
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setContentTitle("Download Failed")
            .setContentText(novelName)
            .setSubText(errorMessage)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
    }

    // ================================================================
    // TTS NOTIFICATIONS
    // ================================================================

    /**
     * Build a TTS playback notification
     */
    fun buildTTSNotification(
        context: Context,
        novelName: String,
        chapterName: String,
        isPlaying: Boolean,
        currentSegment: Int,
        totalSegments: Int
    ): Notification {
        val contentIntent = getMainActivityPendingIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_TTS)
            .setContentTitle(novelName)
            .setContentText(chapterName)
            .setSubText("Segment ${currentSegment + 1} / $totalSegments")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Previous action
        builder.addAction(
            android.R.drawable.ic_media_previous,
            "Previous",
            getActionPendingIntent(context, ACTION_TTS_PREVIOUS, 10)
        )

        // Play/Pause action
        if (isPlaying) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                getActionPendingIntent(context, ACTION_TTS_PAUSE, 11)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play",
                getActionPendingIntent(context, ACTION_TTS_PLAY, 11)
            )
        }

        // Next action
        builder.addAction(
            android.R.drawable.ic_media_next,
            "Next",
            getActionPendingIntent(context, ACTION_TTS_NEXT, 12)
        )

        // Stop action (as delete intent)
        builder.setDeleteIntent(
            getActionPendingIntent(context, ACTION_TTS_STOP, 13)
        )

        // Use MediaStyle for better media controls on lock screen
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2) // Show all 3 actions in compact view
        )

        return builder.build()
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    /**
     * Get the NotificationManager
     */
    fun getNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Cancel a notification by ID
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        getNotificationManager(context).cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        getNotificationManager(context).cancelAll()
    }
}