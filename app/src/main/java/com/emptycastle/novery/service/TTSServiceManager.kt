// service/TTSServiceManager.kt
package com.emptycastle.novery.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import com.emptycastle.novery.domain.model.Chapter
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
 * Handles service binding and provides a unified API for TTS control.
 */
object TTSServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var service: TTSService? = null
    private var isBound = false
    private var bindingContextRef: WeakReference<Context>? = null

    private var pendingRequest: PendingPlaybackRequest? = null

    private var segmentChangedJob: Job? = null
    private var playbackCompleteJob: Job? = null
    private var chapterChangedJob: Job? = null

    // Track if using system voice
    private var _useSystemVoice = false
    // Cached settings to apply when service binds
    private var _cachedSpeechRate: Float = 1.0f
    private var _cachedPitch: Float = 1.0f
    private var _cachedAutoAdvance: Boolean = true

    private val _playbackState = MutableStateFlow(TTSPlaybackState())
    val playbackState: StateFlow<TTSPlaybackState> = _playbackState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _segmentChanged = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val segmentChanged: SharedFlow<Int> = _segmentChanged.asSharedFlow()

    private val _playbackComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playbackComplete: SharedFlow<Unit> = _playbackComplete.asSharedFlow()

    private val _chapterChanged = MutableSharedFlow<TTSChapterChangeEvent>(extraBufferCapacity = 4)
    val chapterChanged: SharedFlow<TTSChapterChangeEvent> = _chapterChanged.asSharedFlow()

    val serviceState: StateFlow<TTSService.ServiceState>
        get() = TTSService.serviceState

    val isServiceRunning: Boolean
        get() = TTSService.isRunning

    private data class PendingPlaybackRequest(
        val content: TTSContent,
        val startIndex: Int,
        val cover: Bitmap?,
        val novelUrl: String?,
        val providerName: String?,
        val chapters: List<Chapter>?,
        val chapterIndex: Int
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val ttsBinder = binder as TTSService.TTSBinder
            service = ttsBinder.getService()
            isBound = true
            _isConnected.value = true

            // Apply current system voice setting
            service?.setUseSystemVoice(_useSystemVoice)

            // Apply cached playback settings (speech rate / pitch / auto-advance)
            service?.setSpeechRate(_cachedSpeechRate)
            service?.setPitch(_cachedPitch)
            service?.setAutoAdvanceEnabled(_cachedAutoAdvance)

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

            chapterChangedJob?.cancel()
            chapterChangedJob = scope.launch {
                service?.chapterChangedEvent?.collect { event ->
                    _chapterChanged.emit(event)
                }
            }

            pendingRequest?.let { request ->
                // Configure background loader if we have chapter info
                if (request.novelUrl != null && request.providerName != null && request.chapters != null) {
                    service?.configureBackgroundLoader(
                        novelUrl = request.novelUrl,
                        providerName = request.providerName,
                        chapters = request.chapters,
                        currentIndex = request.chapterIndex
                    )
                }
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
            chapterChangedJob?.cancel()
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
        chapterChangedJob?.cancel()
    }

    /**
     * Start TTS playback with optional chapter navigation support.
     * If chapters are provided, the service can auto-advance when screen is off.
     */
    fun startPlayback(
        context: Context,
        content: TTSContent,
        startIndex: Int = 0,
        cover: Bitmap? = null,
        novelUrl: String? = null,
        providerName: String? = null,
        chapters: List<Chapter>? = null,
        chapterIndex: Int = 0
    ) {
        if (content.segments.isEmpty()) return

        TTSService.start(context)

        if (!isBound) {
            pendingRequest = PendingPlaybackRequest(
                content = content,
                startIndex = startIndex,
                cover = cover,
                novelUrl = novelUrl,
                providerName = providerName,
                chapters = chapters,
                chapterIndex = chapterIndex
            )
            bind(context)
        } else {
            // Configure background loader if we have chapter info
            if (novelUrl != null && providerName != null && chapters != null) {
                service?.configureBackgroundLoader(
                    novelUrl = novelUrl,
                    providerName = providerName,
                    chapters = chapters,
                    currentIndex = chapterIndex
                )
            }
            service?.startPlayback(content, startIndex, cover)
        }
    }

    /**
     * Configure the background chapter loader.
     * Call this when starting TTS to enable auto chapter advancement.
     */
    fun configureBackgroundLoader(
        novelUrl: String,
        providerName: String,
        chapters: List<Chapter>,
        currentIndex: Int
    ) {
        service?.configureBackgroundLoader(novelUrl, providerName, chapters, currentIndex)
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

    fun nextChapter() {
        service?.nextChapter()
    }

    fun previousChapter() {
        service?.previousChapter()
    }

    fun seekToSegment(index: Int) {
        service?.seekToSegment(index)
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    fun setSpeechRate(rate: Float) {
        _cachedSpeechRate = rate
        service?.setSpeechRate(rate)
    }

    fun getSpeechRate(): Float = service?.getSpeechRate() ?: 1.0f

    fun setPitch(pitch: Float) {
        _cachedPitch = pitch
        service?.setPitch(pitch)
    }

    fun getPitch(): Float = service?.getPitch() ?: 1.0f

    fun setAutoAdvanceEnabled(enabled: Boolean) {
        _cachedAutoAdvance = enabled
        service?.setAutoAdvanceEnabled(enabled)
    }

    fun isAutoAdvanceEnabled(): Boolean = service?.isAutoAdvanceEnabled() ?: true

    /**
     * Set whether to use system default voice or app-selected voice.
     */
    fun setUseSystemVoice(useSystem: Boolean) {
        _useSystemVoice = useSystem
        service?.setUseSystemVoice(useSystem)
    }

    fun isUsingSystemVoice(): Boolean = _useSystemVoice

    /**
     * Set voice on the running service.
     */
    fun setVoice(voiceId: String): Boolean {
        if (_useSystemVoice) return false
        VoiceManager.selectVoice(voiceId)
        return service?.setVoice(voiceId) ?: false
    }

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

    fun getCurrentChapterIndex(): Int = _playbackState.value.chapterIndex

    fun getCurrentChapterUrl(): String = _playbackState.value.chapterUrl

    fun hasNextChapter(): Boolean = _playbackState.value.hasNextChapter

    fun hasPreviousChapter(): Boolean = _playbackState.value.hasPreviousChapter

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