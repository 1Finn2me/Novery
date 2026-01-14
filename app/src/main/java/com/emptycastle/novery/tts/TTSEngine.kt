package com.emptycastle.novery.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * TTS playback status
 */
enum class TTSStatus {
    IDLE,
    INITIALIZING,
    READY,
    PLAYING,
    PAUSED,
    ERROR
}

/**
 * Voice information with additional metadata
 */
data class TTSVoice(
    val id: String,
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isNetworkRequired: Boolean,
    val quality: VoiceQuality = VoiceQuality.NORMAL,
    val gender: VoiceGender = VoiceGender.UNKNOWN
) {
    enum class VoiceQuality { LOW, NORMAL, HIGH, VERY_HIGH }
    enum class VoiceGender { MALE, FEMALE, NEUTRAL, UNKNOWN }
}

/**
 * Speech rate presets
 */
enum class SpeechRatePreset(val rate: Float, val displayName: String) {
    VERY_SLOW(0.5f, "0.5x"),
    SLOW(0.75f, "0.75x"),
    NORMAL(1.0f, "1x"),
    FAST(1.25f, "1.25x"),
    FASTER(1.5f, "1.5x"),
    VERY_FAST(1.75f, "1.75x"),
    ULTRA_FAST(2.0f, "2x"),
    MAX(2.5f, "2.5x");

    companion object {
        fun fromRate(rate: Float): SpeechRatePreset {
            return entries.minByOrNull { kotlin.math.abs(it.rate - rate) } ?: NORMAL
        }

        fun getAvailableRates(): List<Float> = listOf(
            0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f
        )
    }
}

/**
 * Word highlight event for UI synchronization
 */
data class WordHighlightEvent(
    val utteranceId: String,
    val word: String,
    val startIndex: Int,
    val endIndex: Int,
    val sentenceIndex: Int
)

/**
 * Utterance completion event
 */
sealed class TTSEvent {
    data class Started(val utteranceId: String) : TTSEvent()
    data class Completed(val utteranceId: String) : TTSEvent()
    data class Error(val utteranceId: String, val error: String) : TTSEvent()
    data class WordBoundary(val event: WordHighlightEvent) : TTSEvent()
    data class SentenceCompleted(val sentenceIndex: Int, val totalSentences: Int) : TTSEvent()
    object QueueEmpty : TTSEvent()
}

/**
 * Speech item in queue
 */
data class SpeechItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sentences: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Advanced Text-to-Speech Engine with sentence-level control,
 * word highlighting, and queue management.
 */
class TTSEngine(private val context: Context) {

    companion object {
        private const val TAG = "TTSEngine"

        // Sentence splitting regex
        private val SENTENCE_REGEX = Regex("""(?<=[.!?])\s+(?=[A-Z"'])""")

        // Max characters per utterance (Android limit is around 4000)
        private const val MAX_UTTERANCE_LENGTH = 3500

        // Retry settings
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 500L
    }

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var processingJob: Job? = null

    // TTS Engine
    private var tts: TextToSpeech? = null
    private var initializationRetries = 0

    // State
    private val _status = MutableStateFlow(TTSStatus.IDLE)
    val status: StateFlow<TTSStatus> = _status.asStateFlow()

    private val _voices = MutableStateFlow<List<TTSVoice>>(emptyList())
    val voices: StateFlow<List<TTSVoice>> = _voices.asStateFlow()

    private val _currentVoice = MutableStateFlow<TTSVoice?>(null)
    val currentVoice: StateFlow<TTSVoice?> = _currentVoice.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Locale.US)
    val currentLanguage: StateFlow<Locale> = _currentLanguage.asStateFlow()

    // Events
    private val _events = MutableSharedFlow<TTSEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<TTSEvent> = _events.asSharedFlow()

    // Settings
    private var speechRate: Float = 1.0f
    private var pitch: Float = 1.0f
    private var volume: Float = 1.0f

    // Queue
    private val speechQueue = ConcurrentLinkedQueue<SpeechItem>()
    private var currentItem: SpeechItem? = null
    private var currentSentenceIndex = 0
    private var isPaused = false
    private var pausedAtSentence = 0

    // Current utterance tracking
    private var currentUtteranceId: String = ""
    private var currentText: String = ""

    // Callbacks (for compatibility with existing code)
    var onStart: (() -> Unit)? = null
    var onDone: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSentenceComplete: ((Int, Int) -> Unit)? = null // (current, total)

    init {
        initializeTTS()
    }

    // ================================================================
    // INITIALIZATION
    // ================================================================

    private fun initializeTTS() {
        _status.value = TTSStatus.INITIALIZING

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initializationRetries = 0
                setupTTS()
                loadVoices()
                _status.value = TTSStatus.READY
            } else {
                handleInitializationError()
            }
        }
    }

    private fun handleInitializationError() {
        if (initializationRetries < MAX_RETRIES) {
            initializationRetries++
            scope.launch {
                delay(RETRY_DELAY_MS * initializationRetries)
                tts?.shutdown()
                initializeTTS()
            }
        } else {
            _status.value = TTSStatus.ERROR
            onError?.invoke("TTS initialization failed after $MAX_RETRIES attempts")
            scope.launch { _events.emit(TTSEvent.Error("init", "TTS initialization failed")) }
        }
    }

    private fun setupTTS() {
        tts?.let { engine ->
            // Set default language
            val result = engine.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }
            _currentLanguage.value = engine.voice?.locale ?: Locale.US

            // Apply saved settings
            engine.setSpeechRate(speechRate)
            engine.setPitch(pitch)

            // Set up listener
            engine.setOnUtteranceProgressListener(createUtteranceListener())
        }
    }

    private fun createUtteranceListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) {
                    _status.value = TTSStatus.PLAYING
                    onStart?.invoke()
                    scope.launch {
                        _events.emit(TTSEvent.Started(utteranceId ?: ""))
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) {
                    handleUtteranceComplete()
                }
            }

            override fun onError(utteranceId: String?) {
                handleUtteranceError(utteranceId, "Unknown error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val errorMessage = when (errorCode) {
                    TextToSpeech.ERROR_SYNTHESIS -> "Synthesis error"
                    TextToSpeech.ERROR_SERVICE -> "Service error"
                    TextToSpeech.ERROR_OUTPUT -> "Output error"
                    TextToSpeech.ERROR_NETWORK -> "Network error"
                    TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
                    TextToSpeech.ERROR_NOT_INSTALLED_YET -> "TTS not installed"
                    else -> "Error code: $errorCode"
                }
                handleUtteranceError(utteranceId, errorMessage)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (interrupted && utteranceId == currentUtteranceId) {
                    // User stopped, don't auto-continue
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Word boundary callback
                if (utteranceId == currentUtteranceId && start >= 0 && end > start) {
                    val word = currentText.substring(
                        start.coerceAtMost(currentText.length),
                        end.coerceAtMost(currentText.length)
                    )
                    scope.launch {
                        _events.emit(
                            TTSEvent.WordBoundary(
                                WordHighlightEvent(
                                    utteranceId = utteranceId ?: "",
                                    word = word,
                                    startIndex = start,
                                    endIndex = end,
                                    sentenceIndex = currentSentenceIndex
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleUtteranceComplete() {
        val item = currentItem ?: return

        currentSentenceIndex++

        scope.launch {
            _events.emit(
                TTSEvent.SentenceCompleted(
                    currentSentenceIndex,
                    item.sentences.size
                )
            )
        }

        onSentenceComplete?.invoke(currentSentenceIndex, item.sentences.size)

        if (currentSentenceIndex < item.sentences.size && !isPaused) {
            // More sentences to speak
            speakSentence(currentSentenceIndex)
        } else if (currentSentenceIndex >= item.sentences.size) {
            // Item complete, move to next in queue
            scope.launch {
                _events.emit(TTSEvent.Completed(item.id))
            }
            onDone?.invoke()
            processNextInQueue()
        }
    }

    private fun handleUtteranceError(utteranceId: String?, errorMessage: String) {
        if (utteranceId != currentUtteranceId) return

        scope.launch {
            _events.emit(TTSEvent.Error(utteranceId ?: "", errorMessage))
        }

        // Try to recover by skipping to next sentence
        val item = currentItem
        if (item != null && currentSentenceIndex < item.sentences.size - 1) {
            currentSentenceIndex++
            speakSentence(currentSentenceIndex)
        } else {
            _status.value = TTSStatus.ERROR
            onError?.invoke(errorMessage)
            processNextInQueue()
        }
    }

    // ================================================================
    // VOICE MANAGEMENT
    // ================================================================

    private fun loadVoices() {
        tts?.let { engine ->
            try {
                val availableVoices = engine.voices ?: return

                val voiceList = availableVoices
                    .filter { isVoiceUsable(it) }
                    .map { voice -> mapToTTSVoice(voice) }
                    .sortedWith(voiceComparator())

                _voices.value = voiceList

                // Set default voice
                selectDefaultVoice(voiceList)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isVoiceUsable(voice: Voice): Boolean {
        // Filter out unusable voices
        if (voice.isNetworkConnectionRequired) {
            // Allow network voices but deprioritize them
            return voice.features.contains("networkTimeoutMs")
        }
        return true
    }

    private fun mapToTTSVoice(voice: Voice): TTSVoice {
        val name = voice.name
        val quality = when {
            name.contains("wavenet", ignoreCase = true) -> TTSVoice.VoiceQuality.VERY_HIGH
            name.contains("neural", ignoreCase = true) -> TTSVoice.VoiceQuality.HIGH
            name.contains("enhanced", ignoreCase = true) -> TTSVoice.VoiceQuality.HIGH
            else -> TTSVoice.VoiceQuality.NORMAL
        }

        val gender = when {
            name.contains("female", ignoreCase = true) -> TTSVoice.VoiceGender.FEMALE
            name.contains("male", ignoreCase = true) -> TTSVoice.VoiceGender.MALE
            name.contains("-f-", ignoreCase = true) -> TTSVoice.VoiceGender.FEMALE
            name.contains("-m-", ignoreCase = true) -> TTSVoice.VoiceGender.MALE
            else -> TTSVoice.VoiceGender.UNKNOWN
        }

        return TTSVoice(
            id = voice.name,
            name = voice.name,
            displayName = formatVoiceDisplayName(voice),
            locale = voice.locale,
            isNetworkRequired = voice.isNetworkConnectionRequired,
            quality = quality,
            gender = gender
        )
    }

    private fun formatVoiceDisplayName(voice: Voice): String {
        val name = voice.name
            .replace(Regex("^[a-z]{2}-[a-z]{2}-x-"), "")
            .replace(Regex("-local$"), "")
            .replace("-network", " ★")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { it.uppercase() }

        val languageDisplay = voice.locale.displayLanguage
        val countryDisplay = voice.locale.displayCountry

        val location = if (countryDisplay.isNotBlank() && countryDisplay != languageDisplay) {
            "$languageDisplay ($countryDisplay)"
        } else {
            languageDisplay
        }

        return "$name • $location"
    }

    private fun voiceComparator(): Comparator<TTSVoice> {
        return compareBy(
            // 1. Local voices first
            { it.isNetworkRequired },
            // 2. Higher quality first
            { -it.quality.ordinal },
            // 3. English voices first
            { !it.locale.language.startsWith("en") },
            // 4. Alphabetically by display name
            { it.displayName }
        )
    }

    private fun selectDefaultVoice(voices: List<TTSVoice>) {
        val defaultVoice = voices.firstOrNull {
            it.locale.language == "en" &&
                    !it.isNetworkRequired &&
                    it.quality >= TTSVoice.VoiceQuality.NORMAL
        } ?: voices.firstOrNull()

        defaultVoice?.let { setVoice(it.id) }
    }

    /**
     * Get voices filtered by language
     */
    fun getVoicesForLanguage(languageCode: String): List<TTSVoice> {
        return _voices.value.filter {
            it.locale.language.equals(languageCode, ignoreCase = true)
        }
    }

    /**
     * Get available languages
     */
    fun getAvailableLanguages(): List<Locale> {
        return _voices.value
            .map { it.locale }
            .distinctBy { it.language }
            .sortedBy { it.displayLanguage }
    }

    // ================================================================
    // PUBLIC SPEAKING API
    // ================================================================

    /**
     * Speak text immediately, clearing the queue
     */
    fun speak(text: String, metadata: Map<String, Any> = emptyMap()) {
        clearQueue()
        enqueue(text, metadata)
    }

    /**
     * Add text to the speech queue
     */
    fun enqueue(text: String, metadata: Map<String, Any> = emptyMap()) {
        val cleanedText = cleanTextForSpeech(text)
        if (cleanedText.isBlank()) {
            onDone?.invoke()
            return
        }

        val sentences = splitIntoSentences(cleanedText)
        val item = SpeechItem(
            text = cleanedText,
            sentences = sentences,
            metadata = metadata
        )

        speechQueue.offer(item)

        if (_status.value == TTSStatus.READY || _status.value == TTSStatus.IDLE) {
            processNextInQueue()
        }
    }

    /**
     * Speak a list of texts in sequence
     */
    fun speakAll(texts: List<String>, metadata: Map<String, Any> = emptyMap()) {
        clearQueue()
        texts.forEach { text ->
            val cleanedText = cleanTextForSpeech(text)
            if (cleanedText.isNotBlank()) {
                val sentences = splitIntoSentences(cleanedText)
                speechQueue.offer(
                    SpeechItem(
                        text = cleanedText,
                        sentences = sentences,
                        metadata = metadata
                    )
                )
            }
        }
        processNextInQueue()
    }

    private fun processNextInQueue() {
        val nextItem = speechQueue.poll()

        if (nextItem == null) {
            currentItem = null
            _status.value = TTSStatus.READY
            scope.launch { _events.emit(TTSEvent.QueueEmpty) }
            return
        }

        currentItem = nextItem
        currentSentenceIndex = 0
        isPaused = false

        speakSentence(0)
    }

    private fun speakSentence(index: Int) {
        val item = currentItem ?: return

        if (index >= item.sentences.size) {
            processNextInQueue()
            return
        }

        val sentence = item.sentences[index]
        currentText = sentence
        currentUtteranceId = "${item.id}_$index"
        currentSentenceIndex = index

        tts?.let { engine ->
            engine.setSpeechRate(speechRate)
            engine.setPitch(pitch)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, currentUtteranceId)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }

            engine.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, currentUtteranceId)
        }
    }

    // ================================================================
    // PLAYBACK CONTROL
    // ================================================================

    /**
     * Pause speech with position memory
     */
    fun pause() {
        if (_status.value != TTSStatus.PLAYING) return

        isPaused = true
        pausedAtSentence = currentSentenceIndex
        tts?.stop()
        _status.value = TTSStatus.PAUSED
    }

    /**
     * Resume speech from paused position
     */
    fun resume() {
        if (_status.value != TTSStatus.PAUSED) return

        isPaused = false

        if (currentItem != null) {
            // Resume from the sentence we were at
            speakSentence(pausedAtSentence)
        } else {
            processNextInQueue()
        }
    }

    /**
     * Toggle between play and pause
     */
    fun togglePlayPause() {
        when (_status.value) {
            TTSStatus.PLAYING -> pause()
            TTSStatus.PAUSED -> resume()
            TTSStatus.READY -> processNextInQueue()
            else -> { /* Ignore */ }
        }
    }

    /**
     * Stop speech completely and clear queue
     */
    fun stop() {
        tts?.stop()
        clearQueue()
        currentItem = null
        currentSentenceIndex = 0
        isPaused = false
        _status.value = TTSStatus.READY
    }

    /**
     * Skip to the next sentence
     */
    fun skipToNextSentence() {
        val item = currentItem ?: return

        tts?.stop()

        if (currentSentenceIndex < item.sentences.size - 1) {
            currentSentenceIndex++
            speakSentence(currentSentenceIndex)
        } else {
            processNextInQueue()
        }
    }

    /**
     * Skip to the previous sentence
     */
    fun skipToPreviousSentence() {
        tts?.stop()

        if (currentSentenceIndex > 0) {
            currentSentenceIndex--
            speakSentence(currentSentenceIndex)
        } else {
            // At the beginning, just restart
            speakSentence(0)
        }
    }

    /**
     * Skip to a specific sentence
     */
    fun skipToSentence(index: Int) {
        val item = currentItem ?: return

        if (index in 0 until item.sentences.size) {
            tts?.stop()
            currentSentenceIndex = index
            speakSentence(index)
        }
    }

    /**
     * Clear the speech queue
     */
    fun clearQueue() {
        speechQueue.clear()
    }

    /**
     * Get current queue size
     */
    fun getQueueSize(): Int = speechQueue.size

    // ================================================================
    // SETTINGS
    // ================================================================

    /**
     * Set speech rate (0.25 to 4.0, default 1.0)
     */
    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.25f, 4.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun getRate(): Float = speechRate

    /**
     * Increase speech rate by step
     */
    fun increaseRate(step: Float = 0.25f) {
        setRate(speechRate + step)
    }

    /**
     * Decrease speech rate by step
     */
    fun decreaseRate(step: Float = 0.25f) {
        setRate(speechRate - step)
    }

    /**
     * Set rate from preset
     */
    fun setRatePreset(preset: SpeechRatePreset) {
        setRate(preset.rate)
    }

    /**
     * Get current rate as preset
     */
    fun getRatePreset(): SpeechRatePreset = SpeechRatePreset.fromRate(speechRate)

    /**
     * Set pitch (0.25 to 4.0, default 1.0)
     */
    fun setPitch(pitchValue: Float) {
        pitch = pitchValue.coerceIn(0.25f, 4.0f)
        tts?.setPitch(pitch)
    }

    fun getPitch(): Float = pitch

    /**
     * Set volume (0.0 to 1.0, default 1.0)
     */
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    fun getVolume(): Float = volume

    /**
     * Set voice by ID
     */
    fun setVoice(voiceId: String): Boolean {
        return tts?.let { engine ->
            val voice = engine.voices?.find { it.name == voiceId }
            if (voice != null) {
                engine.voice = voice
                _currentVoice.value = _voices.value.find { it.id == voiceId }
                _currentLanguage.value = voice.locale
                true
            } else {
                false
            }
        } ?: false
    }

    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        return tts?.let { engine ->
            val result = engine.setLanguage(locale)
            val success = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (success) {
                _currentLanguage.value = locale
            }
            success
        } ?: false
    }

    // ================================================================
    // STATUS & INFO
    // ================================================================

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    /**
     * Check if TTS is initialized and ready
     */
    fun isReady(): Boolean = _status.value == TTSStatus.READY ||
            _status.value == TTSStatus.PLAYING ||
            _status.value == TTSStatus.PAUSED

    /**
     * Get current sentence index
     */
    fun getCurrentSentenceIndex(): Int = currentSentenceIndex

    /**
     * Get total sentences in current item
     */
    fun getTotalSentences(): Int = currentItem?.sentences?.size ?: 0

    /**
     * Get progress as percentage (0.0 to 1.0)
     */
    fun getProgress(): Float {
        val total = currentItem?.sentences?.size ?: return 0f
        if (total == 0) return 0f
        return currentSentenceIndex.toFloat() / total
    }

    /**
     * Get current item metadata
     */
    fun getCurrentMetadata(): Map<String, Any> = currentItem?.metadata ?: emptyMap()

    // ================================================================
    // TEXT PROCESSING
    // ================================================================

    /**
     * Split text into sentences for better control
     */
    private fun splitIntoSentences(text: String): List<String> {
        // First, split by paragraph breaks
        val paragraphs = text.split(Regex("\n{2,}"))

        val sentences = mutableListOf<String>()

        for (paragraph in paragraphs) {
            // Split paragraph into sentences
            val paragraphSentences = SENTENCE_REGEX.split(paragraph.trim())
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Handle very long sentences by chunking them
            for (sentence in paragraphSentences) {
                if (sentence.length > MAX_UTTERANCE_LENGTH) {
                    sentences.addAll(chunkLongSentence(sentence))
                } else {
                    sentences.add(sentence)
                }
            }
        }

        return sentences.ifEmpty { listOf(text) }
    }

    /**
     * Chunk a very long sentence into smaller pieces at natural break points
     */
    private fun chunkLongSentence(sentence: String): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = sentence

        while (remaining.length > MAX_UTTERANCE_LENGTH) {
            // Find a good break point
            val breakPoint = findBreakPoint(remaining, MAX_UTTERANCE_LENGTH)
            chunks.add(remaining.substring(0, breakPoint).trim())
            remaining = remaining.substring(breakPoint).trim()
        }

        if (remaining.isNotBlank()) {
            chunks.add(remaining)
        }

        return chunks
    }

    /**
     * Find a natural break point in text
     */
    private fun findBreakPoint(text: String, maxLength: Int): Int {
        // Prefer breaking at these points (in order of preference)
        val breakChars = listOf(". ", "! ", "? ", "; ", ", ", " - ", " ")

        for (breakChar in breakChars) {
            val lastIndex = text.lastIndexOf(breakChar, maxLength)
            if (lastIndex > maxLength / 2) {
                return lastIndex + breakChar.length
            }
        }

        // Fall back to space
        val lastSpace = text.lastIndexOf(' ', maxLength)
        return if (lastSpace > 0) lastSpace + 1 else maxLength
    }

    /**
     * Clean text for TTS reading
     */
    fun cleanTextForSpeech(text: String): String {
        var cleaned = text

        // Remove HTML tags
        cleaned = cleaned.replace(Regex("<[^>]*>"), " ")

        // Convert HTML entities
        cleaned = cleaned
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&#160;", " ")
            .replace("&hellip;", "...")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")

        // Remove URLs
        cleaned = cleaned.replace(Regex("https?://\\S+"), "")
        cleaned = cleaned.replace(Regex("www\\.\\S+"), "")

        // Remove email addresses
        cleaned = cleaned.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "")

        // Handle common book formatting
        cleaned = cleaned.replace(Regex("\\*{2,}"), " ") // Bold markers
        cleaned = cleaned.replace(Regex("_{2,}"), " ") // Italic markers
        cleaned = cleaned.replace(Regex("~{2,}"), " ") // Strikethrough markers
        cleaned = cleaned.replace(Regex("#{1,6}\\s"), "") // Markdown headers

        // Remove translator/editor notes
        cleaned = cleaned.replace(Regex("\\[T/N:.*?\\]", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("\\[E/N:.*?\\]", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("\\[A/N:.*?\\]", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("Translator:.*?(?=\\n|$)", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("Editor:.*?(?=\\n|$)", RegexOption.IGNORE_CASE), "")

        // Handle excessive punctuation
        cleaned = cleaned.replace(Regex("[.]{3,}"), "...")
        cleaned = cleaned.replace(Regex("[-–—]{3,}"), " — ")
        cleaned = cleaned.replace(Regex("[!]{2,}"), "!")
        cleaned = cleaned.replace(Regex("[?]{2,}"), "?")

        // Clean up quotes for better reading
        cleaned = cleaned.replace(""", "\"")
        cleaned = cleaned.replace(""", "\"")
        cleaned = cleaned.replace("'", "'")
        cleaned = cleaned.replace("'", "'")

        // Normalize whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ")

        // Remove leading/trailing whitespace
        cleaned = cleaned.trim()

        return cleaned
    }

    /**
     * Detect likely language of text
     */
    fun detectLanguage(text: String): Locale {
        // Simple heuristic based on character ranges
        val sample = text.take(500)

        return when {
            sample.any { it in '\u4e00'..'\u9fff' } -> Locale.CHINESE
            sample.any { it in '\u3040'..'\u30ff' } -> Locale.JAPANESE
            sample.any { it in '\uac00'..'\ud7af' } -> Locale.KOREAN
            sample.any { it in '\u0600'..'\u06ff' } -> Locale("ar")
            sample.any { it in '\u0400'..'\u04ff' } -> Locale("ru")
            sample.any { it in '\u0e00'..'\u0e7f' } -> Locale("th")
            else -> Locale.ENGLISH
        }
    }

    // ================================================================
    // CLEANUP
    // ================================================================

    /**
     * Clean up resources
     */
    fun shutdown() {
        processingJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        clearQueue()
        _status.value = TTSStatus.IDLE
    }
}