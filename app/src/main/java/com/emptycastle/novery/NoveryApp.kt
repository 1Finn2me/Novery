package com.emptycastle.novery

import android.app.Application
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.provider.LibReadProvider
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.provider.NovelBinProvider
import com.emptycastle.novery.service.NotificationHelper
import com.emptycastle.novery.tts.TTSManager
import com.emptycastle.novery.tts.VoiceManager

/**
 * Application class - initializes app-wide dependencies.
 */
class NoveryApp : Application() {

    // Lazy-initialized singletons
    val database: NovelDatabase by lazy { NovelDatabase.getInstance(this) }
    val preferences: PreferencesManager by lazy { PreferencesManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()

        // Initialize repository provider (only once!)
        RepositoryProvider.initialize(this)

        // Initialize TTS engine
        TTSManager.initialize(this)

        // Initialize TTS Manager
        TTSManager.initialize(this)

        // Initialize Voice Manager
        VoiceManager.initialize(this) {
            // Restore saved voice preference
            val prefs = PreferencesManager.getInstance(this)
            val savedVoiceId = prefs.getTtsVoice()
            if (savedVoiceId != null) {
                VoiceManager.selectVoice(savedVoiceId)
            }
        }

        // Register all novel providers
        registerProviders()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        TTSManager.shutdown()
    }

    private fun registerProviders() {
        // Add providers here - order determines display order
        MainProvider.register(NovelBinProvider())
        MainProvider.register(LibReadProvider())
    }
}