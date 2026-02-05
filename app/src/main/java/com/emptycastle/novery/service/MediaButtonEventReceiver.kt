package com.emptycastle.novery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media.session.MediaButtonReceiver

/**
 * Receives media button events from Bluetooth devices and lock screen.
 * Forwards events to the MediaSession for handling.
 *
 * This receiver is registered in the manifest and receives media button
 * intents even when the app is in the background.
 */
class MediaButtonEventReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MediaButtonReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // Handle media button event
        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            val keyEvent = getKeyEvent(intent)

            // Only handle key down events to avoid double-triggering
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                // If service is running, forward to media session
                if (TTSService.isRunning) {
                    TTSNotifications.mediaSession?.let { session ->
                        MediaButtonReceiver.handleIntent(session, intent)
                    }
                } else {
                    // Service not running - could optionally start it here
                    // For now, we'll just ignore the event
                }
            }
        }
    }

    private fun getKeyEvent(intent: Intent): KeyEvent? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }
    }
}