package com.emptycastle.novery.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays a silent audio track to maintain an active media session.
 * This enables Bluetooth headphone controls and lock screen media controls
 * to work with TTS playback.
 *
 * Compatible with Android API 21+ (targeting API 28/Android 9 and above)
 */
class SilentAudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "SilentAudioPlayer"

        // Audio configuration
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // Buffer duration in milliseconds - longer buffer means less CPU wake-ups
        private const val BUFFER_DURATION_MS = 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val isPlaying = AtomicBoolean(false)

    // Silent audio buffer (all zeros = silence)
    private val silentBuffer: ByteArray by lazy {
        val bufferSize = (SAMPLE_RATE * 2 * BUFFER_DURATION_MS) / 1000 // 2 bytes per sample for 16-bit
        ByteArray(bufferSize) // Initialized to zeros = silence
    }

    /**
     * Start playing silent audio. This maintains an active audio session
     * that enables media button controls.
     */
    fun start() {
        if (isPlaying.getAndSet(true)) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            val bufferSize = maxOf(minBufferSize, silentBuffer.size)

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG)
                            .setEncoding(AUDIO_FORMAT)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            audioTrack?.play()

            // Start writing silence in a loop
            playbackJob = scope.launch {
                while (isActive && isPlaying.get()) {
                    try {
                        audioTrack?.write(silentBuffer, 0, silentBuffer.size)
                    } catch (e: Exception) {
                        // Handle write errors gracefully
                        if (isPlaying.get()) {
                            delay(100)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying.set(false)
        }
    }

    /**
     * Pause silent audio playback (keeps resources allocated)
     */
    fun pause() {
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resume silent audio playback
     */
    fun resume() {
        try {
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop and release all resources
     */
    fun stop() {
        isPlaying.set(false)
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    /**
     * Check if silent audio is currently playing
     */
    fun isPlaying(): Boolean = isPlaying.get()

    /**
     * Get the audio session ID (useful for routing)
     */
    fun getAudioSessionId(): Int = audioTrack?.audioSessionId ?: 0

    /**
     * Release all resources and cancel scope
     */
    fun release() {
        stop()
        scope.cancel()
    }
}