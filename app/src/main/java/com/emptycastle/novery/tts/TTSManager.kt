package com.emptycastle.novery.tts

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Singleton manager for TTS Engine with settings persistence.
 */
object TTSManager {

    private const val PREFS_NAME = "tts_settings"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_PITCH = "pitch"
    private const val KEY_VOLUME = "volume"
    private const val KEY_VOICE_ID = "voice_id"
    private const val KEY_LANGUAGE = "language"

    private var engine: TTSEngine? = null
    private var prefs: SharedPreferences? = null

    /**
     * Initialize the TTS engine with settings persistence
     */
    fun initialize(context: Context) {
        if (engine == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            engine = TTSEngine(context.applicationContext)
            restoreSettings()
        }
    }

    /**
     * Get the TTS engine instance
     */
    fun getEngine(): TTSEngine {
        return engine ?: throw IllegalStateException(
            "TTSManager not initialized. Call initialize() first."
        )
    }

    /**
     * Check if engine is initialized
     */
    fun isInitialized(): Boolean = engine != null

    /**
     * Get engine status flow
     */
    fun getStatusFlow(): StateFlow<TTSStatus>? = engine?.status

    /**
     * Get current voice flow
     */
    fun getCurrentVoiceFlow(): StateFlow<TTSVoice?>? = engine?.currentVoice

    /**
     * Get available voices flow
     */
    fun getVoicesFlow(): StateFlow<List<TTSVoice>>? = engine?.voices

    // ================================================================
    // QUICK ACCESS METHODS
    // ================================================================

    /**
     * Speak text immediately
     */
    fun speak(text: String) {
        engine?.speak(text)
    }

    /**
     * Stop speaking
     */
    fun stop() {
        engine?.stop()
    }

    /**
     * Pause speaking
     */
    fun pause() {
        engine?.pause()
    }

    /**
     * Resume speaking
     */
    fun resume() {
        engine?.resume()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        engine?.togglePlayPause()
    }

    /**
     * Check if speaking
     */
    fun isSpeaking(): Boolean = engine?.isSpeaking() == true

    /**
     * Check if ready
     */
    fun isReady(): Boolean = engine?.isReady() == true

    // ================================================================
    // SETTINGS WITH PERSISTENCE
    // ================================================================

    /**
     * Set speech rate and persist
     */
    fun setRate(rate: Float) {
        engine?.setRate(rate)
        prefs?.edit()?.putFloat(KEY_SPEECH_RATE, rate)?.apply()
    }

    /**
     * Get current speech rate
     */
    fun getRate(): Float = engine?.getRate() ?: 1.0f

    /**
     * Set pitch and persist
     */
    fun setPitch(pitch: Float) {
        engine?.setPitch(pitch)
        prefs?.edit()?.putFloat(KEY_PITCH, pitch)?.apply()
    }

    /**
     * Get current pitch
     */
    fun getPitch(): Float = engine?.getPitch() ?: 1.0f

    /**
     * Set volume and persist
     */
    fun setVolume(volume: Float) {
        engine?.setVolume(volume)
        prefs?.edit()?.putFloat(KEY_VOLUME, volume)?.apply()
    }

    /**
     * Get current volume
     */
    fun getVolume(): Float = engine?.getVolume() ?: 1.0f

    /**
     * Set voice and persist
     */
    fun setVoice(voiceId: String): Boolean {
        val success = engine?.setVoice(voiceId) ?: false
        if (success) {
            prefs?.edit()?.putString(KEY_VOICE_ID, voiceId)?.apply()
        }
        return success
    }

    /**
     * Get current voice ID
     */
    fun getVoiceId(): String? = engine?.currentVoice?.value?.id

    /**
     * Set language and persist
     */
    fun setLanguage(locale: Locale): Boolean {
        val success = engine?.setLanguage(locale) ?: false
        if (success) {
            prefs?.edit()?.putString(KEY_LANGUAGE, locale.toLanguageTag())?.apply()
        }
        return success
    }

    /**
     * Get available languages
     */
    fun getAvailableLanguages(): List<Locale> = engine?.getAvailableLanguages() ?: emptyList()

    /**
     * Get voices for a specific language
     */
    fun getVoicesForLanguage(languageCode: String): List<TTSVoice> {
        return engine?.getVoicesForLanguage(languageCode) ?: emptyList()
    }

    // ================================================================
    // RATE PRESETS
    // ================================================================

    /**
     * Get available rate presets
     */
    fun getAvailableRates(): List<Float> = SpeechRatePreset.getAvailableRates()

    /**
     * Increase rate by one step
     */
    fun increaseRate() {
        val currentRate = getRate()
        val rates = getAvailableRates()
        val nextRate = rates.firstOrNull { it > currentRate } ?: rates.last()
        setRate(nextRate)
    }

    /**
     * Decrease rate by one step
     */
    fun decreaseRate() {
        val currentRate = getRate()
        val rates = getAvailableRates()
        val prevRate = rates.lastOrNull { it < currentRate } ?: rates.first()
        setRate(prevRate)
    }

    // ================================================================
    // SETTINGS PERSISTENCE
    // ================================================================

    private fun restoreSettings() {
        prefs?.let { p ->
            val rate = p.getFloat(KEY_SPEECH_RATE, 1.0f)
            val pitch = p.getFloat(KEY_PITCH, 1.0f)
            val volume = p.getFloat(KEY_VOLUME, 1.0f)
            val voiceId = p.getString(KEY_VOICE_ID, null)
            val languageTag = p.getString(KEY_LANGUAGE, null)

            engine?.setRate(rate)
            engine?.setPitch(pitch)
            engine?.setVolume(volume)

            voiceId?.let { engine?.setVoice(it) }
            languageTag?.let {
                try {
                    engine?.setLanguage(Locale.forLanguageTag(it))
                } catch (e: Exception) {
                    // Ignore invalid locale
                }
            }
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun resetSettings() {
        prefs?.edit()?.clear()?.apply()
        engine?.setRate(1.0f)
        engine?.setPitch(1.0f)
        engine?.setVolume(1.0f)
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    /**
     * Shutdown and release resources
     */
    fun shutdown() {
        engine?.shutdown()
        engine = null
        prefs = null
    }
}