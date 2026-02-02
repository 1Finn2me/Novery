package com.emptycastle.novery.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Lightweight TTS utility for quick speech synthesis without the full service.
 * Use TTSService for background playback with notifications and media controls.
 *
 * Use cases:
 * - Quick text-to-speech for accessibility
 * - Voice preview
 * - Short announcements
 */
class TTSEngine private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: TTSEngine? = null

        fun getInstance(context: Context): TTSEngine {
            return instance ?: synchronized(this) {
                instance ?: TTSEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeak: String? = null

    private var speechRate = 1.0f
    private var pitch = 1.0f

    var onStart: (() -> Unit)? = null
    var onDone: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                setupListener()
                isReady = true

                pendingSpeak?.let {
                    speak(it)
                    pendingSpeak = null
                }
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStart?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onError?.invoke("TTS error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onError?.invoke("TTS error: $errorCode")
            }
        })
    }

    /**
     * Speak text immediately
     */
    fun speak(text: String) {
        if (!isReady) {
            pendingSpeak = text
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Add text to queue
     */
    fun enqueue(text: String) {
        if (!isReady) return

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitchValue: Float) {
        pitch = pitchValue.coerceIn(0.5f, 2.0f)
        tts?.setPitch(pitch)
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        instance = null
    }
}