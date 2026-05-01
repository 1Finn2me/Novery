package com.emptycastle.novery.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emptycastle.novery.service.NotificationHelper

/**
 * Handles sync notification actions.
 */
class SyncActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != NotificationHelper.ACTION_SYNC_CANCEL) {
            return
        }

        val appContext = context.applicationContext
        SyncWorker.cancel(appContext)
        SyncStatusTracker.finishCancelled()
        SyncNotifier(appContext).clear()
    }
}
