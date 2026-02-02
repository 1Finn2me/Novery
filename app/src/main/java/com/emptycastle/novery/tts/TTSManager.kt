package com.emptycastle.novery.tts

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * Simple settings manager for TTS preferences.
 * Actual playback is handled by TTSService.
 * Voice management is handled by VoiceManager.
 */
object TTSManager {

    private const val PREFS_NAME = "tts_settings"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_PITCH = "pitch"
    private const val KEY_VOLUME = "volume"
    private const val KEY_VOICE_ID = "voice_id"
    private const val KEY_LANGUAGE = "language"

    private var prefs: SharedPreferences? = null
    private var lightweightEngine: TTSEngine? = null

    // Cached settings
    private var cachedRate = 1.0f
    private var cachedPitch = 1.0f
    private var cachedVolume = 1.0f
    private var cachedVoiceId: String? = null

    /**
     * Initialize preferences. Call early in app lifecycle.
     */
    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadCachedSettings()
        }
    }

    private fun loadCachedSettings() {
        prefs?.let { p ->
            cachedRate = p.getFloat(KEY_SPEECH_RATE, 1.0f)
            cachedPitch = p.getFloat(KEY_PITCH, 1.0f)
            cachedVolume = p.getFloat(KEY_VOLUME, 1.0f)
            cachedVoiceId = p.getString(KEY_VOICE_ID, null)
        }
    }

    /**
     * Get lightweight engine for quick TTS (not for reader playback)
     */
    fun getEngine(): TTSEngine {
        return lightweightEngine ?: throw IllegalStateException(
            "TTSManager not initialized. Call initialize() first."
        )
    }

    /**
     * Create engine lazily if needed
     */
    fun getOrCreateEngine(context: Context): TTSEngine {
        return lightweightEngine ?: TTSEngine.getInstance(context).also {
            lightweightEngine = it
        }
    }

    fun isInitialized(): Boolean = prefs != null

    // Settings with persistence
    fun setRate(rate: Float) {
        cachedRate = rate.coerceIn(0.5f, 2.5f)
        prefs?.edit()?.putFloat(KEY_SPEECH_RATE, cachedRate)?.apply()
    }

    fun getRate(): Float = cachedRate

    fun setPitch(pitch: Float) {
        cachedPitch = pitch.coerceIn(0.5f, 2.0f)
        prefs?.edit()?.putFloat(KEY_PITCH, cachedPitch)?.apply()
    }

    fun getPitch(): Float = cachedPitch

    fun setVolume(volume: Float) {
        cachedVolume = volume.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat(KEY_VOLUME, cachedVolume)?.apply()
    }

    fun getVolume(): Float = cachedVolume

    fun setVoice(voiceId: String) {
        cachedVoiceId = voiceId
        prefs?.edit()?.putString(KEY_VOICE_ID, voiceId)?.apply()
        VoiceManager.selectVoice(voiceId)
    }

    fun getVoiceId(): String? = cachedVoiceId

    fun setLanguage(locale: Locale) {
        prefs?.edit()?.putString(KEY_LANGUAGE, locale.toLanguageTag())?.apply()
    }

    fun getAvailableRates(): List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f)

    fun resetSettings() {
        prefs?.edit()?.clear()?.apply()
        cachedRate = 1.0f
        cachedPitch = 1.0f
        cachedVolume = 1.0f
        cachedVoiceId = null
    }

    fun shutdown() {
        lightweightEngine?.shutdown()
        lightweightEngine = null
        prefs = null
    }
}