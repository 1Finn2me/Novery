package com.emptycastle.novery.data.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.emptycastle.novery.MainActivity
import com.emptycastle.novery.R
import com.emptycastle.novery.service.NotificationHelper

/**
 * Builds and posts sync progress notifications.
 */
class SyncNotifier(
    private val context: Context
) {
    fun showProgress(stage: String): Notification {
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle("Syncing library")
            .setContentText(stage)
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_notification_cancel,
                "Cancel",
                NotificationHelper.getSyncCancelPendingIntent(context)
            )
            .build()

        notify(notification)
        return notification
    }

    fun showSuccess(message: String) {
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_done)
            .setContentTitle("Sync complete")
            .setContentText(message)
            .setContentIntent(contentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    fun showError(message: String) {
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification_error)
            .setContentTitle("Sync failed")
            .setContentText(message)
            .setContentIntent(contentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notify(notification)
    }

    fun clear() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID_SYNC)
        }
    }

    private fun notify(notification: Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(NotificationHelper.NOTIFICATION_ID_SYNC, notification)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            NotificationHelper.NOTIFICATION_ID_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
