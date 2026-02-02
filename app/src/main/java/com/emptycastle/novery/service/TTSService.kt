package com.emptycastle.novery.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.emptycastle.novery.tts.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance TTS Service with sentence pre-queuing for gapless playback.
 */
class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSService"

        private const val QUEUE_AHEAD_COUNT = 3
        private const val MIN_QUEUE_THRESHOLD = 1

        const val ACTION_START = "com.emptycastle.novery.ACTION_TTS_START"
        const val ACTION_STOP = "com.emptycastle.novery.ACTION_TTS_STOP"
        const val ACTION_SET_SLEEP_TIMER = "com.emptycastle.novery.ACTION_SET_SLEEP_TIMER"
        const val ACTION_CANCEL_SLEEP_TIMER = "com.emptycastle.novery.ACTION_CANCEL_SLEEP_TIMER"

        const val EXTRA_SLEEP_TIMER_MINUTES = "sleep_timer_minutes"

        private val _isRunning = AtomicBoolean(false)
        private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Stopped)

        val isRunning: Boolean get() = _isRunning.get()
        val serviceState: StateFlow<ServiceState> get() = _serviceState.asStateFlow()

        fun start(context: Context) {
            if (_isRunning.get()) return
            _serviceState.value = ServiceState.Starting

            val intent = Intent(context, TTSService::class.java).apply {
                action = ACTION_START
            }

            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
                _serviceState.value = ServiceState.Error(e.message ?: "Failed to start service")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TTSService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun setSleepTimer(context: Context, minutes: Int) {
            val intent = Intent(context, TTSService::class.java).apply {
                action = if (minutes > 0) ACTION_SET_SLEEP_TIMER else ACTION_CANCEL_SLEEP_TIMER
                putExtra(EXTRA_SLEEP_TIMER_MINUTES, minutes)
            }
            context.startService(intent)
        }

        fun getStopPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, TTSService::class.java).apply {
                action = ACTION_STOP
            }
            return PendingIntent.getService(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    sealed class ServiceState {
        object Stopped : ServiceState()
        object Starting : ServiceState()
        object Running : ServiceState()
        object Paused : ServiceState()
        data class Error(val message: String) : ServiceState()
        data class SleepTimerActive(val remainingMinutes: Int) : ServiceState()
    }

    private val binder = TTSBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var pendingPlayRequest: PendingPlayRequest? = null

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false
    private var wasPlayingBeforePhoneCall = false

    private var wakeLock: PowerManager.WakeLock? = null
    private var sleepTimerJob: Job? = null

    private var currentContent: TTSContent? = null
    private var coverBitmap: Bitmap? = null
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var useSystemVoice = false // Flag to use system default voice

    private val currentSentenceIndex = AtomicInteger(0)
    private val queuedUtterances = ConcurrentHashMap<String, Int>()
    private var highestQueuedIndex = -1
    private var isPlaying = false
    private var isPausedByUser = false

    private val _playbackState = MutableStateFlow(TTSPlaybackState())
    val playbackState: StateFlow<TTSPlaybackState> = _playbackState.asStateFlow()

    private val _segmentChangedEvent = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val segmentChangedEvent: SharedFlow<Int> = _segmentChangedEvent.asSharedFlow()

    private val _playbackCompleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playbackCompleteEvent: SharedFlow<Unit> = _playbackCompleteEvent.asSharedFlow()

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private var telephonyCallback: TelephonyCallback? = null

    private var actionReceiver: BroadcastReceiver? = null
    private var headphoneReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    private data class PendingPlayRequest(
        val content: TTSContent,
        val startIndex: Int,
        val cover: Bitmap?
    )

    inner class TTSBinder : Binder() {
        fun getService(): TTSService = this@TTSService
        fun isPlaying(): Boolean = _playbackState.value.isPlaying
        fun setSleepTimer(minutes: Int) = handleSetSleepTimer(minutes)
        fun cancelSleepTimer() = handleCancelSleepTimer()
        fun getSleepTimerRemaining(): Int? {
            val state = _serviceState.value
            return if (state is ServiceState.SleepTimerActive) state.remainingMinutes else null
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.set(true)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)

        registerActionReceiver()
        registerHeadphoneReceiver()
        registerBluetoothReceiver()
        registerPhoneStateListener()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> { }
            ACTION_STOP -> handleStop()
            ACTION_SET_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra(EXTRA_SLEEP_TIMER_MINUTES, 0)
                handleSetSleepTimer(minutes)
            }
            ACTION_CANCEL_SLEEP_TIMER -> handleCancelSleepTimer()
            else -> {
                TTSNotifications.mediaSession?.let { session ->
                    MediaButtonReceiver.handleIntent(session, intent)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.setSpeechRate(speechRate)
            tts?.setPitch(pitch)

            // Only apply saved voice if not using system voice
            if (!useSystemVoice) {
                applySavedVoice()
            }

            setupUtteranceListener()

            ttsInitialized = true

            pendingPlayRequest?.let { request ->
                startPlaybackInternal(request.content, request.startIndex, request.cover)
                pendingPlayRequest = null
            }
        } else {
            _playbackState.value = _playbackState.value.copy(
                error = "Failed to initialize TTS engine"
            )
            _serviceState.value = ServiceState.Error("TTS initialization failed")
        }
    }

    private fun applySavedVoice() {
        if (useSystemVoice) return // Don't apply if using system voice

        val selectedVoice = VoiceManager.selectedVoice.value
        if (selectedVoice != null) {
            val voice = tts?.voices?.find { it.name == selectedVoice.id }
            if (voice != null) {
                tts?.voice = voice
            }
        }
    }

    /**
     * Set whether to use system default voice or app-selected voice
     */
    fun setUseSystemVoice(useSystem: Boolean) {
        useSystemVoice = useSystem

        if (useSystem) {
            // Reset to system default voice by setting language (which uses default voice)
            tts?.let { engine ->
                val currentLocale = engine.voice?.locale ?: Locale.getDefault()
                engine.setLanguage(currentLocale)
            }
        } else {
            // Apply the saved voice from VoiceManager
            applySavedVoice()
        }

        // If playing, restart from current position to apply voice change
        if (isPlaying && !isPausedByUser) {
            val currentIndex = currentSentenceIndex.get()
            queueSentences(currentIndex, clearExisting = true)
        }
    }

    fun isUsingSystemVoice(): Boolean = useSystemVoice

    fun setVoice(voiceId: String): Boolean {
        if (useSystemVoice) return false // Don't change voice if using system voice

        val voice = tts?.voices?.find { it.name == voiceId }
        if (voice != null) {
            tts?.voice = voice

            // If playing, restart from current position to apply new voice
            if (isPlaying && !isPausedByUser) {
                val currentIndex = currentSentenceIndex.get()
                queueSentences(currentIndex, clearExisting = true)
            }
            return true
        }
        return false
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 3.0f)
        tts?.setSpeechRate(speechRate)
        _playbackState.value = _playbackState.value.copy(speechRate = speechRate)

        TTSNotifications.updateSpeechRate(speechRate)
        updateNotification()
    }

    fun getSpeechRate(): Float = speechRate

    fun setPitch(pitchValue: Float) {
        pitch = pitchValue.coerceIn(0.5f, 2.0f)
        tts?.setPitch(pitch)

        // If playing, restart from current position to apply new pitch
        if (isPlaying && !isPausedByUser) {
            val currentIndex = currentSentenceIndex.get()
            queueSentences(currentIndex, clearExisting = true)
        }
    }

    fun getPitch(): Float = pitch

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) {
                val sentenceIndex = extractSentenceIndex(utteranceId) ?: return

                serviceScope.launch(Dispatchers.Main) {
                    currentSentenceIndex.set(sentenceIndex)
                    updatePlaybackStateForSentence(sentenceIndex)
                    _segmentChangedEvent.emit(sentenceIndex)
                }
            }

            override fun onDone(utteranceId: String?) {
                val completedIndex = extractSentenceIndex(utteranceId) ?: return

                serviceScope.launch(Dispatchers.Main) {
                    onUtteranceComplete(completedIndex)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                val index = extractSentenceIndex(utteranceId) ?: return
                serviceScope.launch(Dispatchers.Main) {
                    onUtteranceComplete(index)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val index = extractSentenceIndex(utteranceId) ?: return
                serviceScope.launch(Dispatchers.Main) {
                    handleTTSError(errorCode, index)
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                utteranceId?.let { queuedUtterances.remove(it) }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            }
        })
    }

    private fun extractSentenceIndex(utteranceId: String?): Int? {
        return utteranceId?.removePrefix("s_")?.toIntOrNull()
    }

    private fun onUtteranceComplete(completedIndex: Int) {
        val utteranceId = "s_$completedIndex"
        queuedUtterances.remove(utteranceId)

        val content = currentContent ?: return

        if (completedIndex >= content.totalSegments - 1) {
            onPlaybackComplete()
            return
        }

        val queueSize = queuedUtterances.size
        if (queueSize < MIN_QUEUE_THRESHOLD && isPlaying && !isPausedByUser) {
            refillQueue()
        }
    }

    private fun handleTTSError(errorCode: Int, sentenceIndex: Int) {
        val errorMessage = when (errorCode) {
            TextToSpeech.ERROR_SYNTHESIS -> "Synthesis error"
            TextToSpeech.ERROR_SERVICE -> "TTS service error"
            TextToSpeech.ERROR_OUTPUT -> "Output error"
            TextToSpeech.ERROR_NETWORK -> "Network error"
            TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
            TextToSpeech.ERROR_NOT_INSTALLED_YET -> "TTS not installed"
            else -> "Unknown TTS error"
        }

        _playbackState.value = _playbackState.value.copy(error = errorMessage)
        onUtteranceComplete(sentenceIndex)
    }

    private fun queueSentences(fromIndex: Int, clearExisting: Boolean = true) {
        val content = currentContent ?: return
        val engine = tts ?: return

        if (clearExisting) {
            engine.stop()
            queuedUtterances.clear()
            highestQueuedIndex = fromIndex - 1
        }

        val endIndex = (fromIndex + QUEUE_AHEAD_COUNT).coerceAtMost(content.totalSegments)

        for (i in fromIndex until endIndex) {
            if (i <= highestQueuedIndex) continue
            queueSentence(i, isFirst = (i == fromIndex && clearExisting))
        }
    }

    private fun queueSentence(index: Int, isFirst: Boolean = false): Boolean {
        val content = currentContent ?: return false
        val engine = tts ?: return false

        if (index >= content.totalSegments) return false

        val segment = content.getSegment(index) ?: return false
        val text = segment.text
        val cleanText = cleanTextForSpeech(text)

        if (cleanText.isBlank()) {
            return queueSentence(index + 1, isFirst)
        }

        val utteranceId = "s_$index"
        val mode = if (isFirst) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = engine.speak(cleanText, mode, params, utteranceId)

        if (result == TextToSpeech.SUCCESS) {
            queuedUtterances[utteranceId] = index
            highestQueuedIndex = maxOf(highestQueuedIndex, index)

            // Enqueue a silent utterance to represent the parser-provided pause
            val pauseMs = segment.pauseAfterMs.coerceAtLeast(0)
            if (pauseMs > 0) {
                // Use a separate utterance id for pauses (ignored by index extraction)
                val pauseId = "p_$index"
                engine.playSilentUtterance(pauseMs.toLong(), TextToSpeech.QUEUE_ADD, pauseId)
            }

            return true
        }

        return false
    }

    private fun refillQueue() {
        val content = currentContent ?: return

        val nextToQueue = highestQueuedIndex + 1
        if (nextToQueue >= content.totalSegments) return

        val currentQueueSize = queuedUtterances.size
        val toQueue = QUEUE_AHEAD_COUNT - currentQueueSize

        for (i in 0 until toQueue) {
            val index = nextToQueue + i
            if (index >= content.totalSegments) break
            queueSentence(index, isFirst = false)
        }
    }

    fun startPlayback(content: TTSContent, startIndex: Int = 0, cover: Bitmap? = null) {
        if (content.segments.isEmpty()) return

        if (!ttsInitialized) {
            pendingPlayRequest = PendingPlayRequest(content, startIndex, cover)
            return
        }

        startPlaybackInternal(content, startIndex, cover)
    }

    private fun startPlaybackInternal(content: TTSContent, startIndex: Int, cover: Bitmap?) {
        currentContent = content
        coverBitmap = cover

        val validStartIndex = startIndex.coerceIn(0, content.totalSegments - 1)
        currentSentenceIndex.set(validStartIndex)

        _playbackState.value = TTSPlaybackState(
            isActive = true,
            isPlaying = true,
            isPaused = false,
            novelName = content.novelName,
            chapterName = content.chapterName,
            currentSegmentIndex = validStartIndex,
            totalSegments = content.totalSegments,
            currentText = content.getSegment(validStartIndex)?.text ?: "",
            speechRate = speechRate
        )

        if (!requestAudioFocus()) {
            _playbackState.value = _playbackState.value.copy(
                error = "Could not obtain audio focus"
            )
            return
        }

        acquireWakeLock()
        isPlaying = true
        isPausedByUser = false

        // Apply current settings
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        TTSNotifications.updateSpeechRate(speechRate)

        val currentServiceState = _serviceState.value
        if (currentServiceState is ServiceState.SleepTimerActive) {
            TTSNotifications.updateSleepTimer(currentServiceState.remainingMinutes)
        } else {
            TTSNotifications.updateSleepTimer(null)
        }

        TTSNotifications.initializeMediaSession(this, content, this)
        startForegroundWithNotification()
        _serviceState.value = ServiceState.Running

        queueSentences(validStartIndex, clearExisting = true)
    }

    fun updateContent(content: TTSContent, keepSegmentIndex: Boolean = false) {
        currentContent = content

        val newIndex = if (keepSegmentIndex) {
            currentSentenceIndex.get().coerceIn(0, content.totalSegments - 1)
        } else {
            0
        }
        currentSentenceIndex.set(newIndex)

        _playbackState.value = _playbackState.value.copy(
            novelName = content.novelName,
            chapterName = content.chapterName,
            totalSegments = content.totalSegments,
            currentSegmentIndex = newIndex,
            currentText = content.getSegment(newIndex)?.text ?: ""
        )

        TTSNotifications.updateMetadata(content, coverBitmap)
        updateNotification()

        if (isPlaying && !isPausedByUser) {
            queueSentences(newIndex, clearExisting = true)
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun resume() {
        if (!_playbackState.value.isActive) return
        if (_playbackState.value.isPlaying) return
        if (!requestAudioFocus()) return

        isPausedByUser = false
        isPlaying = true

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPaused = false
        )

        _serviceState.value = if (sleepTimerJob?.isActive == true) {
            _serviceState.value
        } else {
            ServiceState.Running
        }

        acquireWakeLock()
        updateNotification()

        queueSentences(currentSentenceIndex.get(), clearExisting = true)
    }

    fun pause() {
        isPausedByUser = true
        isPlaying = false

        tts?.stop()
        queuedUtterances.clear()

        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = true
        )

        _serviceState.value = ServiceState.Paused

        releaseWakeLock()
        updateNotification()
    }

    fun stop() {
        handleStop()
    }

    private fun handleStop() {
        isPlaying = false
        isPausedByUser = false

        tts?.stop()
        queuedUtterances.clear()

        abandonAudioFocus()
        releaseWakeLock()

        sleepTimerJob?.cancel()
        sleepTimerJob = null

        _playbackState.value = TTSPlaybackState()
        _serviceState.value = ServiceState.Stopped

        currentContent = null
        currentSentenceIndex.set(0)
        highestQueuedIndex = -1

        serviceScope.launch {
            _playbackCompleteEvent.emit(Unit)
        }

        TTSNotifications.cancelNotification(this)
        TTSNotifications.releaseMediaSession()

        stopForegroundCompat()
        stopSelf()
    }

    fun next() {
        val content = currentContent ?: return
        val current = currentSentenceIndex.get()

        if (current < content.totalSegments - 1) {
            seekToSegment(current + 1)
        }
    }

    fun previous() {
        val current = currentSentenceIndex.get()
        if (current > 0) {
            seekToSegment(current - 1)
        }
    }

    fun seekToSegment(index: Int) {
        val content = currentContent ?: return

        if (index !in 0 until content.totalSegments) return

        currentSentenceIndex.set(index)
        updatePlaybackStateForSentence(index)

        serviceScope.launch {
            _segmentChangedEvent.emit(index)
        }

        if (isPlaying && !isPausedByUser) {
            queueSentences(index, clearExisting = true)
        }
    }

    private fun updatePlaybackStateForSentence(index: Int) {
        val content = currentContent ?: return

        _playbackState.value = _playbackState.value.copy(
            currentSegmentIndex = index,
            currentText = content.getSegment(index)?.text ?: ""
        )

        updateNotification()
    }

    private fun onPlaybackComplete() {
        isPlaying = false

        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false
        )

        releaseWakeLock()
        updateNotification()

        serviceScope.launch {
            _playbackCompleteEvent.emit(Unit)
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[*#@~`|]"), "")
            .trim()
    }

    private fun registerActionReceiver() {
        actionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    TTSNotifications.ACTION_PLAY -> resume()
                    TTSNotifications.ACTION_PAUSE -> pause()
                    TTSNotifications.ACTION_STOP -> stop()
                    TTSNotifications.ACTION_NEXT -> next()
                    TTSNotifications.ACTION_PREVIOUS -> previous()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(TTSNotifications.ACTION_PLAY)
            addAction(TTSNotifications.ACTION_PAUSE)
            addAction(TTSNotifications.ACTION_STOP)
            addAction(TTSNotifications.ACTION_NEXT)
            addAction(TTSNotifications.ACTION_PREVIOUS)
        }

        ContextCompat.registerReceiver(
            this,
            actionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun registerHeadphoneReceiver() {
        headphoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    if (_playbackState.value.isPlaying) {
                        pause()
                    }
                }
            }
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        ContextCompat.registerReceiver(
            this,
            headphoneReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    @SuppressLint("MissingPermission")
    private fun registerBluetoothReceiver() {
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    if (_playbackState.value.isPlaying) {
                        pause()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }

        try {
            ContextCompat.registerReceiver(
                this,
                bluetoothReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerPhoneStateListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handlePhoneStateChanged(state)
                }
            }

            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handlePhoneStateChanged(state)
                }
            }

            try {
                @Suppress("DEPRECATION")
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun handlePhoneStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                wasPlayingBeforePhoneCall = _playbackState.value.isPlaying
                if (wasPlayingBeforePhoneCall) {
                    pause()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasPlayingBeforePhoneCall) {
                    serviceScope.launch {
                        delay(500)
                        resume()
                        wasPlayingBeforePhoneCall = false
                    }
                }
            }
        }
    }

    private fun unregisterReceivers() {
        listOf(actionReceiver, headphoneReceiver, bluetoothReceiver).forEach { receiver ->
            receiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (e: Exception) { }
            }
        }
        actionReceiver = null
        headphoneReceiver = null
        bluetoothReceiver = null
    }

    private fun unregisterPhoneStateListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            phoneStateListener = null
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    resume()
                    wasPlayingBeforeFocusLoss = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = _playbackState.value.isPlaying
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                wasPlayingBeforeFocusLoss = _playbackState.value.isPlaying
                pause()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(true)
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }

        hasAudioFocus = false
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock != null) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Novery:TTSWakeLock"
        ).apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun handleSetSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            handleCancelSleepTimer()
            return
        }

        sleepTimerJob?.cancel()
        _serviceState.value = ServiceState.SleepTimerActive(minutes)

        TTSNotifications.updateSleepTimer(minutes)
        updateNotification()

        sleepTimerJob = serviceScope.launch {
            var remainingMinutes = minutes

            while (remainingMinutes > 0) {
                delay(TimeUnit.MINUTES.toMillis(1))
                remainingMinutes--
                _serviceState.value = ServiceState.SleepTimerActive(remainingMinutes)

                TTSNotifications.updateSleepTimer(remainingMinutes)
                updateNotification()
            }

            stop()
        }
    }

    private fun handleCancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null

        TTSNotifications.updateSleepTimer(null)

        if (_serviceState.value is ServiceState.SleepTimerActive) {
            _serviceState.value = ServiceState.Running
            updateNotification()
        }
    }

    private fun startForegroundWithNotification() {
        val content = currentContent ?: return
        val state = _playbackState.value

        val notification = TTSNotifications.createNotification(
            context = this,
            content = content,
            status = TTSStatus.PLAYING,
            currentSegment = state.currentSegmentIndex,
            coverBitmap = coverBitmap
        ) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TTSNotifications.TTS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(TTSNotifications.TTS_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val content = currentContent ?: return
        val state = _playbackState.value

        if (!state.isActive) return

        val status = when {
            state.isPlaying -> TTSStatus.PLAYING
            state.isPaused -> TTSStatus.PAUSED
            else -> TTSStatus.STOPPED
        }

        TTSNotifications.notify(
            context = this,
            content = content,
            status = status,
            currentSegment = state.currentSegmentIndex,
            coverBitmap = coverBitmap
        )
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun cleanup() {
        _isRunning.set(false)
        _serviceState.value = ServiceState.Stopped

        sleepTimerJob?.cancel()
        serviceScope.cancel()

        unregisterReceivers()
        unregisterPhoneStateListener()

        abandonAudioFocus()
        releaseWakeLock()

        tts?.stop()
        tts?.shutdown()
        tts = null

        queuedUtterances.clear()

        TTSNotifications.releaseMediaSession()
        TTSNotifications.cancelNotification(this)
    }
}