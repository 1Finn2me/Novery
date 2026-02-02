package com.emptycastle.novery.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import com.emptycastle.novery.tts.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Singleton manager for interacting with TTSService.
 */
object TTSServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var service: TTSService? = null
    private var isBound = false
    private var bindingContextRef: WeakReference<Context>? = null

    private var pendingRequest: PendingPlaybackRequest? = null

    private var segmentChangedJob: Job? = null
    private var playbackCompleteJob: Job? = null

    // Track if using system voice
    private var _useSystemVoice = false

    private val _playbackState = MutableStateFlow(TTSPlaybackState())
    val playbackState: StateFlow<TTSPlaybackState> = _playbackState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _segmentChanged = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val segmentChanged: SharedFlow<Int> = _segmentChanged.asSharedFlow()

    private val _playbackComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playbackComplete: SharedFlow<Unit> = _playbackComplete.asSharedFlow()

    val serviceState: StateFlow<TTSService.ServiceState>
        get() = TTSService.serviceState

    val isServiceRunning: Boolean
        get() = TTSService.isRunning

    private data class PendingPlaybackRequest(
        val content: TTSContent,
        val startIndex: Int,
        val cover: Bitmap?
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val ttsBinder = binder as TTSService.TTSBinder
            service = ttsBinder.getService()
            isBound = true
            _isConnected.value = true

            // Apply current system voice setting
            service?.setUseSystemVoice(_useSystemVoice)

            scope.launch {
                service?.playbackState?.collect { state ->
                    _playbackState.value = state
                }
            }

            segmentChangedJob?.cancel()
            segmentChangedJob = scope.launch {
                service?.segmentChangedEvent?.collect { index ->
                    _segmentChanged.emit(index)
                }
            }

            playbackCompleteJob?.cancel()
            playbackCompleteJob = scope.launch {
                service?.playbackCompleteEvent?.collect {
                    _playbackComplete.emit(Unit)
                }
            }

            pendingRequest?.let { request ->
                service?.startPlayback(request.content, request.startIndex, request.cover)
                pendingRequest = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isConnected.value = false
            segmentChangedJob?.cancel()
            playbackCompleteJob?.cancel()
        }
    }

    fun bind(context: Context) {
        if (isBound) return

        bindingContextRef = WeakReference(context.applicationContext)
        val intent = Intent(context, TTSService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (!isBound) return

        try {
            bindingContextRef?.get()?.unbindService(connection)
        } catch (e: Exception) { }

        service = null
        isBound = false
        bindingContextRef = null
        _isConnected.value = false
        segmentChangedJob?.cancel()
        playbackCompleteJob?.cancel()
    }

    fun startPlayback(
        context: Context,
        content: TTSContent,
        startIndex: Int = 0,
        cover: Bitmap? = null
    ) {
        if (content.segments.isEmpty()) return

        TTSService.start(context)

        if (!isBound) {
            pendingRequest = PendingPlaybackRequest(
                content = content,
                startIndex = startIndex,
                cover = cover
            )
            bind(context)
        } else {
            service?.startPlayback(content, startIndex, cover)
        }
    }

    fun updateContent(content: TTSContent, keepSegmentIndex: Boolean = false) {
        service?.updateContent(content, keepSegmentIndex)
    }

    fun togglePlayPause() {
        service?.togglePlayPause()
    }

    fun resume() {
        service?.resume()
    }

    fun pause() {
        service?.pause()
    }

    fun stop() {
        service?.stop()
    }

    fun next() {
        service?.next()
    }

    fun previous() {
        service?.previous()
    }

    fun seekToSegment(index: Int) {
        service?.seekToSegment(index)
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    fun setSpeechRate(rate: Float) {
        service?.setSpeechRate(rate)
    }

    fun getSpeechRate(): Float = service?.getSpeechRate() ?: 1.0f

    fun setPitch(pitch: Float) {
        service?.setPitch(pitch)
    }

    fun getPitch(): Float = service?.getPitch() ?: 1.0f

    /**
     * Set whether to use system default voice or app-selected voice.
     * When true, voice selection in the app is disabled and system default is used.
     */
    fun setUseSystemVoice(useSystem: Boolean) {
        _useSystemVoice = useSystem
        service?.setUseSystemVoice(useSystem)
    }

    /**
     * Check if currently using system voice
     */
    fun isUsingSystemVoice(): Boolean = _useSystemVoice

    /**
     * Set voice on the running service. Returns true if successful.
     * Returns false if using system voice (voice selection disabled).
     */
    fun setVoice(voiceId: String): Boolean {
        if (_useSystemVoice) return false

        // Update VoiceManager's selection
        VoiceManager.selectVoice(voiceId)

        // Update the running service
        return service?.setVoice(voiceId) ?: false
    }

    /**
     * Apply current VoiceManager selection to service
     */
    fun applyCurrentVoice() {
        if (_useSystemVoice) return

        val currentVoice = VoiceManager.selectedVoice.value
        if (currentVoice != null) {
            service?.setVoice(currentVoice.id)
        }
    }

    // ================================================================
    // STATUS
    // ================================================================

    fun isPlaying(): Boolean = _playbackState.value.isPlaying

    fun isActive(): Boolean = _playbackState.value.isActive

    fun setSleepTimer(context: Context, minutes: Int) {
        TTSService.setSleepTimer(context, minutes)
    }

    fun getSleepTimerRemaining(): Int? {
        val state = serviceState.value
        return if (state is TTSService.ServiceState.SleepTimerActive) {
            state.remainingMinutes
        } else {
            null
        }
    }
}