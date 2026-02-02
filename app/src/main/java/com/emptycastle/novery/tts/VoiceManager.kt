package com.emptycastle.novery.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Voice quality tier based on voice characteristics
 */
enum class VoiceQuality(val displayName: String, val priority: Int) {
    PREMIUM("Premium", 0),
    HIGH("High Quality", 1),
    NORMAL("Standard", 2),
    LOW("Basic", 3),
    UNKNOWN("Unknown", 4)
}

/**
 * Voice gender classification
 */
enum class VoiceGender(val displayName: String) {
    FEMALE("Female"),
    MALE("Male"),
    NEUTRAL("Neutral"),
    UNKNOWN("Unknown")
}

/**
 * Processed voice information with metadata
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val locale: Locale,
    val languageCode: String,
    val languageDisplayName: String,
    val countryDisplayName: String,
    val isLocal: Boolean,
    val isNetworkRequired: Boolean,
    val quality: VoiceQuality,
    val gender: VoiceGender,
    val isInstalled: Boolean = true
) {
    val shortName: String
        get() = displayName.split("•").firstOrNull()?.trim() ?: displayName

    val fullDescription: String
        get() = buildString {
            append(displayName)
            if (!isLocal) append(" ☁️")
            if (quality == VoiceQuality.PREMIUM) append(" ⭐")
        }
}

/**
 * Language group containing voices
 */
data class LanguageGroup(
    val languageCode: String,
    val displayName: String,
    val flag: String?,
    val voices: List<VoiceInfo>
) {
    val voiceCount: Int get() = voices.size
    val hasLocalVoices: Boolean get() = voices.any { it.isLocal }
    val hasPremiumVoices: Boolean get() = voices.any { it.quality == VoiceQuality.PREMIUM }
}

/**
 * Manages TTS voices with lazy initialization and efficient resource usage.
 *
 * The TTS instance is created lazily and only kept alive during voice operations.
 * For playback, TTSService maintains its own instance for reliability.
 */
object VoiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Lazy TTS instance for voice detection/preview
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initializationInProgress = false

    // State
    private val _voices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val voices: StateFlow<List<VoiceInfo>> = _voices.asStateFlow()

    private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
    val languageGroups: StateFlow<List<LanguageGroup>> = _languageGroups.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceInfo?>(null)
    val selectedVoice: StateFlow<VoiceInfo?> = _selectedVoice.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Language code to flag emoji mapping
    private val languageFlags = mapOf(
        "en" to "🇺🇸", "en-US" to "🇺🇸", "en-GB" to "🇬🇧", "en-AU" to "🇦🇺", "en-IN" to "🇮🇳",
        "es" to "🇪🇸", "es-ES" to "🇪🇸", "es-MX" to "🇲🇽", "es-US" to "🇺🇸",
        "fr" to "🇫🇷", "fr-FR" to "🇫🇷", "fr-CA" to "🇨🇦",
        "de" to "🇩🇪", "de-DE" to "🇩🇪",
        "it" to "🇮🇹", "it-IT" to "🇮🇹",
        "pt" to "🇵🇹", "pt-BR" to "🇧🇷", "pt-PT" to "🇵🇹",
        "ru" to "🇷🇺", "ru-RU" to "🇷🇺",
        "ja" to "🇯🇵", "ja-JP" to "🇯🇵",
        "ko" to "🇰🇷", "ko-KR" to "🇰🇷",
        "zh" to "🇨🇳", "zh-CN" to "🇨🇳", "zh-TW" to "🇹🇼",
        "hi" to "🇮🇳", "hi-IN" to "🇮🇳",
        "ar" to "🇸🇦", "ar-SA" to "🇸🇦",
        "nl" to "🇳🇱", "nl-NL" to "🇳🇱",
        "pl" to "🇵🇱", "pl-PL" to "🇵🇱",
        "tr" to "🇹🇷", "tr-TR" to "🇹🇷",
        "vi" to "🇻🇳", "vi-VN" to "🇻🇳",
        "th" to "🇹🇭", "th-TH" to "🇹🇭",
        "id" to "🇮🇩", "id-ID" to "🇮🇩",
        "sv" to "🇸🇪", "sv-SE" to "🇸🇪",
        "da" to "🇩🇰", "da-DK" to "🇩🇰",
        "no" to "🇳🇴", "nb-NO" to "🇳🇴",
        "fi" to "🇫🇮", "fi-FI" to "🇫🇮",
        "cs" to "🇨🇿", "cs-CZ" to "🇨🇿",
        "el" to "🇬🇷", "el-GR" to "🇬🇷",
        "he" to "🇮🇱", "he-IL" to "🇮🇱",
        "uk" to "🇺🇦", "uk-UA" to "🇺🇦",
        "ro" to "🇷🇴", "ro-RO" to "🇷🇴",
        "hu" to "🇭🇺", "hu-HU" to "🇭🇺",
        "sk" to "🇸🇰", "sk-SK" to "🇸🇰",
        "bg" to "🇧🇬", "bg-BG" to "🇧🇬",
        "hr" to "🇭🇷", "hr-HR" to "🇭🇷",
        "ms" to "🇲🇾", "ms-MY" to "🇲🇾",
        "fil" to "🇵🇭", "tl-PH" to "🇵🇭"
    )

    /**
     * Initialize the voice manager and load available voices.
     * Uses suspend function for clean async handling.
     */
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        if (isInitialized || initializationInProgress) {
            onComplete?.invoke()
            return
        }

        initializationInProgress = true
        _isLoading.value = true

        scope.launch {
            val success = initializeTTS(context)
            if (success) {
                loadVoices()
                isInitialized = true
            }
            initializationInProgress = false
            _isLoading.value = false
            onComplete?.invoke()
        }
    }

    private suspend fun initializeTTS(context: Context): Boolean = suspendCancellableCoroutine { cont ->
        tts = TextToSpeech(context.applicationContext) { status ->
            cont.resume(status == TextToSpeech.SUCCESS)
        }

        cont.invokeOnCancellation {
            tts?.shutdown()
            tts = null
        }
    }

    /**
     * Ensure TTS is initialized before performing operations
     */
    private suspend fun ensureInitialized(context: Context): Boolean {
        if (isInitialized && tts != null) return true

        return suspendCancellableCoroutine { cont ->
            initialize(context) {
                cont.resume(isInitialized)
            }
        }
    }

    private suspend fun loadVoices() {
        val engine = tts ?: return

        withContext(Dispatchers.Default) {
            try {
                val rawVoices = engine.voices ?: emptySet()

                val processedVoices = rawVoices
                    .filter { isVoiceUsable(it) }
                    .map { processVoice(it) }
                    .sortedWith(voiceComparator())

                _voices.value = processedVoices

                val groups = processedVoices
                    .groupBy { it.languageCode }
                    .map { (langCode, voices) ->
                        val firstVoice = voices.first()
                        LanguageGroup(
                            languageCode = langCode,
                            displayName = firstVoice.languageDisplayName,
                            flag = getFlag(langCode, firstVoice.locale),
                            voices = voices.sortedWith(voiceComparator())
                        )
                    }
                    .sortedWith(languageGroupComparator())

                _languageGroups.value = groups

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isVoiceUsable(voice: Voice): Boolean = true

    private fun processVoice(voice: Voice): VoiceInfo {
        val name = voice.name
        val locale = voice.locale

        return VoiceInfo(
            id = name,
            name = name,
            displayName = formatVoiceDisplayName(voice),
            locale = locale,
            languageCode = locale.language,
            languageDisplayName = locale.displayLanguage,
            countryDisplayName = locale.displayCountry,
            isLocal = !voice.isNetworkConnectionRequired,
            isNetworkRequired = voice.isNetworkConnectionRequired,
            quality = detectVoiceQuality(voice),
            gender = detectVoiceGender(voice),
            isInstalled = !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        )
    }

    private fun formatVoiceDisplayName(voice: Voice): String {
        val name = voice.name

        var cleanName = name
            .replace(Regex("^[a-z]{2}-[a-z]{2}-x-"), "")
            .replace(Regex("-local$"), "")
            .replace(Regex("-network$"), " (Network)")
            .replace(Regex("#.+$"), "")
            .replace("_", " ")
            .replace("-", " ")

        cleanName = cleanName.split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

        val country = voice.locale.displayCountry
        val language = voice.locale.displayLanguage

        return if (country.isNotBlank() && country != language) {
            "$cleanName • $country"
        } else {
            cleanName
        }
    }

    private fun detectVoiceQuality(voice: Voice): VoiceQuality {
        val name = voice.name.lowercase()
        val features = voice.features

        return when {
            name.contains("wavenet") -> VoiceQuality.PREMIUM
            name.contains("neural") -> VoiceQuality.PREMIUM
            name.contains("premium") -> VoiceQuality.PREMIUM
            name.contains("studio") -> VoiceQuality.PREMIUM
            name.contains("enhanced") -> VoiceQuality.HIGH
            name.contains("hd") -> VoiceQuality.HIGH
            name.contains("high") -> VoiceQuality.HIGH
            features.contains("highQuality") -> VoiceQuality.HIGH
            voice.isNetworkConnectionRequired -> VoiceQuality.LOW
            else -> VoiceQuality.NORMAL
        }
    }

    private fun detectVoiceGender(voice: Voice): VoiceGender {
        val name = voice.name.lowercase()

        return when {
            name.contains("female") -> VoiceGender.FEMALE
            name.contains("male") && !name.contains("female") -> VoiceGender.MALE
            name.contains("-f-") -> VoiceGender.FEMALE
            name.contains("-m-") -> VoiceGender.MALE
            name.contains("woman") -> VoiceGender.FEMALE
            name.contains("man") && !name.contains("woman") -> VoiceGender.MALE
            // Common voice names
            name.matches(Regex(".*(samantha|victoria|karen|susan|emma|amy|joanna|salli|kimberly|ivy).*")) -> VoiceGender.FEMALE
            name.matches(Regex(".*(daniel|james|david|matthew|brian|joey|justin).*")) -> VoiceGender.MALE
            else -> VoiceGender.UNKNOWN
        }
    }

    private fun getFlag(languageCode: String, locale: Locale): String? {
        val localeTag = locale.toLanguageTag()
        return languageFlags[localeTag]
            ?: languageFlags[languageCode]
            ?: languageFlags[locale.language]
    }

    private fun voiceComparator(): Comparator<VoiceInfo> {
        return compareBy(
            { !it.isLocal },
            { it.quality.priority },
            { !it.isInstalled },
            { it.displayName }
        )
    }

    private fun languageGroupComparator(): Comparator<LanguageGroup> {
        return compareBy(
            { it.languageCode != "en" },
            { !it.hasLocalVoices },
            { !it.hasPremiumVoices },
            { it.displayName }
        )
    }

    /**
     * Select a voice by ID. Updates the stored selection.
     */
    fun selectVoice(voiceId: String): Boolean {
        val voice = _voices.value.find { it.id == voiceId }
        if (voice != null) {
            _selectedVoice.value = voice

            // Apply to local TTS instance if available (for preview)
            tts?.let { engine ->
                val androidVoice = engine.voices?.find { it.name == voiceId }
                if (androidVoice != null) {
                    engine.voice = androidVoice
                    return true
                }
            }
            // Even if TTS isn't available, we store the selection
            // TTSService will apply it when it initializes
            return true
        }
        return false
    }

    /**
     * Select best voice for a language
     */
    fun selectBestVoiceForLanguage(languageCode: String): VoiceInfo? {
        val voicesForLanguage = _voices.value.filter { it.languageCode == languageCode }

        val bestVoice = voicesForLanguage
            .filter { it.isLocal && it.isInstalled }
            .minByOrNull { it.quality.priority }
            ?: voicesForLanguage.firstOrNull()

        bestVoice?.let { selectVoice(it.id) }
        return bestVoice
    }

    /**
     * Get recommended voices
     */
    fun getRecommendedVoices(): List<VoiceInfo> {
        val allVoices = _voices.value

        return allVoices.filter { it.isLocal && it.languageCode == "en" }.take(5) +
                allVoices.filter { it.isLocal && it.languageCode != "en" }.take(5)
    }

    /**
     * Get voices for a specific language
     */
    fun getVoicesForLanguage(languageCode: String): List<VoiceInfo> {
        return _voices.value.filter {
            it.languageCode.equals(languageCode, ignoreCase = true)
        }
    }

    /**
     * Search voices
     */
    fun searchVoices(query: String): List<VoiceInfo> {
        if (query.isBlank()) return _voices.value

        val lowerQuery = query.lowercase()
        return _voices.value.filter { voice ->
            voice.displayName.lowercase().contains(lowerQuery) ||
                    voice.languageDisplayName.lowercase().contains(lowerQuery) ||
                    voice.countryDisplayName.lowercase().contains(lowerQuery)
        }
    }

    fun getSelectedVoiceId(): String? = _selectedVoice.value?.id

    fun isVoiceSelected(voiceId: String): Boolean = _selectedVoice.value?.id == voiceId

    /**
     * Preview a voice with sample text
     */
    fun previewVoice(voiceId: String, sampleText: String = "This is a preview of how this voice sounds.") {
        tts?.let { engine ->
            val voice = engine.voices?.find { it.name == voiceId }
            if (voice != null) {
                engine.stop()
                engine.voice = voice
                engine.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "preview_$voiceId")
            }
        }
    }

    fun stopPreview() {
        tts?.stop()
    }

    /**
     * Release TTS resources. Call when voice selection UI is dismissed.
     * The TTS will be re-initialized on next use if needed.
     */
    fun releasePreviewTTS() {
        // Don't release if we're the only TTS instance
        // In production, you might want to keep it alive for faster previews
    }

    /**
     * Full shutdown - call on app exit
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _voices.value = emptyList()
        _languageGroups.value = emptyList()
        // Don't clear _selectedVoice - it should persist
    }
}