package com.emptycastle.novery.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
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
 * Provides easy access to playback state and control methods.
 */
object TTSServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var service: TTSService? = null
    private var isBound = false
    private var bindingContextRef: WeakReference<Context>? = null

    // Pending request
    private var pendingRequest: PendingPlaybackRequest? = null

    // Collection jobs
    private var segmentChangedJob: Job? = null
    private var playbackCompleteJob: Job? = null

    // Observable state
    private val _playbackState = MutableStateFlow(TTSPlaybackState())
    val playbackState: StateFlow<TTSPlaybackState> = _playbackState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Event flows for external observation
    private val _segmentChanged = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val segmentChanged: SharedFlow<Int> = _segmentChanged.asSharedFlow()

    private val _playbackComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playbackComplete: SharedFlow<Unit> = _playbackComplete.asSharedFlow()

    // Service state (from companion)
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

            // Observe service playback state
            scope.launch {
                service?.playbackState?.collect { state ->
                    _playbackState.value = state
                }
            }

            // Observe segment changed events from service
            segmentChangedJob?.cancel()
            segmentChangedJob = scope.launch {
                service?.segmentChangedEvent?.collect { index ->
                    _segmentChanged.emit(index)
                }
            }

            // Observe playback complete events from service
            playbackCompleteJob?.cancel()
            playbackCompleteJob = scope.launch {
                service?.playbackCompleteEvent?.collect {
                    _playbackComplete.emit(Unit)
                }
            }

            // Execute pending request
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

    /**
     * Bind to the TTS service
     */
    fun bind(context: Context) {
        if (isBound) return

        bindingContextRef = WeakReference(context.applicationContext)
        val intent = Intent(context, TTSService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbind from the TTS service
     */
    fun unbind() {
        if (!isBound) return

        try {
            bindingContextRef?.get()?.unbindService(connection)
        } catch (e: Exception) {
            // Not bound
        }

        service = null
        isBound = false
        bindingContextRef = null
        _isConnected.value = false
        segmentChangedJob?.cancel()
        playbackCompleteJob?.cancel()
    }

    /**
     * Start TTS playback
     */
    fun startPlayback(
        context: Context,
        content: TTSContent,
        startIndex: Int = 0,
        cover: Bitmap? = null
    ) {
        if (content.segments.isEmpty()) return

        // Start service first
        TTSService.start(context)

        if (!isBound) {
            // Store pending request
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

    /**
     * Update content (e.g., when scrolling to new chapter)
     */
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

    fun setSpeechRate(rate: Float) {
        service?.setSpeechRate(rate)
    }

    fun getSpeechRate(): Float = service?.getSpeechRate() ?: 1.0f

    fun isPlaying(): Boolean = _playbackState.value.isPlaying

    fun isActive(): Boolean = _playbackState.value.isActive

    /**
     * Set sleep timer (0 to cancel)
     */
    fun setSleepTimer(context: Context, minutes: Int) {
        TTSService.setSleepTimer(context, minutes)
    }

    /**
     * Get sleep timer remaining minutes
     */
    fun getSleepTimerRemaining(): Int? {
        val state = serviceState.value
        return if (state is TTSService.ServiceState.SleepTimerActive) {
            state.remainingMinutes
        } else {
            null
        }
    }
}