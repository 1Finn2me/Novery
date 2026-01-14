package com.emptycastle.novery.ui.screens.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.TextAlign
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.service.TTSContent
import com.emptycastle.novery.service.TTSServiceManager
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.tts.TTSManager
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.tts.VoiceManager
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.ParsedSentence
import com.emptycastle.novery.util.SentenceParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL

private suspend fun loadCoverBitmap(coverUrl: String?): Bitmap? {
    if (coverUrl.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(coverUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

data class ContentSegment(
    val id: String,
    val html: String,
    val text: String,
    val sentences: List<ParsedSentence> = emptyList()
) {
    val sentenceCount: Int get() = sentences.size
    fun getSentenceText(index: Int): String? = sentences.getOrNull(index)?.text
    fun getSentence(index: Int): ParsedSentence? = sentences.getOrNull(index)
}

data class ReadingPosition(
    val chapterUrl: String,
    val chapterIndex: Int,
    val segmentId: String,
    val segmentIndexInChapter: Int,
    val approximateProgress: Float,
    val offsetPixels: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromHeader(chapterUrl: String, chapterIndex: Int): ReadingPosition {
            return ReadingPosition(
                chapterUrl = chapterUrl,
                chapterIndex = chapterIndex,
                segmentId = "header",
                segmentIndexInChapter = -1,
                approximateProgress = 0f,
                offsetPixels = 0
            )
        }
    }
}

data class ResolvedScrollPosition(
    val displayIndex: Int,
    val offsetPixels: Int,
    val resolutionMethod: ResolutionMethod,
    val confidence: Float
)

enum class ResolutionMethod {
    EXACT_SEGMENT_ID,
    SEGMENT_INDEX,
    PROGRESS_ESTIMATE,
    CHAPTER_START,
    NOT_FOUND
}

data class ScrollRestorationState(
    val pendingPosition: ReadingPosition? = null,
    val isWaitingForChapter: Boolean = false,
    val restorationAttempts: Int = 0,
    val lastAttemptTime: Long = 0,
    val hasSuccessfullyRestored: Boolean = false
) {
    val maxAttempts = 5
    val shouldRetry: Boolean
        get() = pendingPosition != null &&
                restorationAttempts < maxAttempts &&
                !hasSuccessfullyRestored
}

data class TargetScrollPosition(
    val displayIndex: Int,
    val offsetPixels: Int,
    val id: Long = System.currentTimeMillis()
)

data class LoadedChapter(
    val chapter: Chapter,
    val chapterIndex: Int,
    val segments: List<ContentSegment>,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalSentences: Int get() = segments.sumOf { it.sentenceCount }

    // Calculate display items count for this chapter (header + segments + divider/loading/error)
    val displayItemCount: Int get() = when {
        isLoading -> 2 // header + loading indicator
        error != null -> 2 // header + error indicator
        else -> 1 + segments.size + 1 // header + segments + divider
    }
}

data class TTSPosition(
    val segmentIndex: Int = -1,
    val sentenceIndexInSegment: Int = 0,
    val globalSentenceIndex: Int = 0
) {
    val isValid: Boolean get() = segmentIndex >= 0
}

sealed class ReaderDisplayItem(open val itemId: String) {
    data class ChapterHeader(
        val chapterIndex: Int,
        val chapterName: String,
        val chapterNumber: Int,
        val totalChapters: Int
    ) : ReaderDisplayItem("header_$chapterIndex")

    data class Segment(
        val chapterIndex: Int,
        val chapterUrl: String,
        val segment: ContentSegment,
        val segmentIndexInChapter: Int,
        val globalSegmentIndex: Int = 0
    ) : ReaderDisplayItem("segment_${chapterIndex}_${segment.id}")

    data class ChapterDivider(
        val chapterIndex: Int,
        val chapterName: String,
        val chapterNumber: Int,
        val totalChapters: Int,
        val hasNextChapter: Boolean
    ) : ReaderDisplayItem("divider_$chapterIndex")

    data class LoadingIndicator(
        val chapterIndex: Int
    ) : ReaderDisplayItem("loading_$chapterIndex")

    data class ErrorIndicator(
        val chapterIndex: Int,
        val error: String
    ) : ReaderDisplayItem("error_$chapterIndex")
}

data class SentenceHighlight(
    val segmentDisplayIndex: Int,
    val sentenceIndex: Int,
    val sentence: ParsedSentence
)

data class TTSSettingsState(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val voiceId: String? = null,
    val autoScroll: Boolean = true,
    val highlightSentence: Boolean = true,
    val pauseOnCalls: Boolean = true
)

data class ReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allChapters: List<Chapter> = emptyList(),
    val loadedChapters: Map<Int, LoadedChapter> = emptyMap(),
    val initialChapterIndex: Int = 0,
    val displayItems: List<ReaderDisplayItem> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentChapterUrl: String = "",
    val currentChapterName: String = "",
    val targetScrollPosition: TargetScrollPosition? = null,
    val hasRestoredScroll: Boolean = false,
    val currentScrollIndex: Int = 0,
    val currentScrollOffset: Int = 0,
    val previousChapter: Chapter? = null,
    val nextChapter: Chapter? = null,
    val settings: ReaderSettings = ReaderSettings(),
    val showControls: Boolean = true,
    val showSettings: Boolean = false,
    val infiniteScrollEnabled: Boolean = false,
    val isPreloading: Boolean = false,
    val readingProgress: Float = 0f,
    val chapterProgress: Float = 0f,
    val isTTSActive: Boolean = false,
    val ttsStatus: TTSStatus = TTSStatus.STOPPED,
    val currentSegmentIndex: Int = -1,
    val currentTTSChapterIndex: Int = -1,
    val ttsPosition: TTSPosition = TTSPosition(),
    val currentSentenceHighlight: SentenceHighlight? = null,
    val totalTTSSentences: Int = 0,
    val currentGlobalSentenceIndex: Int = 0,
    val ttsSettings: TTSSettingsState = TTSSettingsState(),
    val showTTSSettings: Boolean = false,
    val showChapterList: Boolean = false,
    val readChapterUrls: Set<String> = emptySet()
) {
    val savedScrollPosition: TargetScrollPosition? get() = targetScrollPosition

    fun getAllSegments(): List<ReaderDisplayItem.Segment> {
        return displayItems.filterIsInstance<ReaderDisplayItem.Segment>()
    }

    fun getTotalSentenceCount(): Int {
        return getAllSegments().sumOf { it.segment.sentenceCount }
    }
}

// =============================================================================
// SCROLL POSITION MANAGER
// =============================================================================

class ScrollPositionManager(
    private val preferencesManager: PreferencesManager
) {
    fun capturePosition(
        displayItems: List<ReaderDisplayItem>,
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Int,
        loadedChapters: Map<Int, LoadedChapter>
    ): ReadingPosition? {
        if (displayItems.isEmpty() || firstVisibleItemIndex >= displayItems.size) {
            return null
        }

        val visibleItem = displayItems[firstVisibleItemIndex]

        return when (visibleItem) {
            is ReaderDisplayItem.Segment -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                val totalSegments = chapter?.segments?.size ?: 1
                val progress = if (totalSegments > 0) {
                    (visibleItem.segmentIndexInChapter.toFloat() / totalSegments).coerceIn(0f, 1f)
                } else 0f

                ReadingPosition(
                    chapterUrl = visibleItem.chapterUrl,
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = visibleItem.segment.id,
                    segmentIndexInChapter = visibleItem.segmentIndexInChapter,
                    approximateProgress = progress,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            is ReaderDisplayItem.ChapterHeader -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                ReadingPosition.fromHeader(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex
                ).copy(offsetPixels = firstVisibleItemOffset)
            }

            is ReaderDisplayItem.ChapterDivider -> {
                val chapter = loadedChapters[visibleItem.chapterIndex]
                ReadingPosition(
                    chapterUrl = chapter?.chapter?.url ?: "",
                    chapterIndex = visibleItem.chapterIndex,
                    segmentId = "divider",
                    segmentIndexInChapter = chapter?.segments?.lastIndex ?: 0,
                    approximateProgress = 1f,
                    offsetPixels = firstVisibleItemOffset
                )
            }

            else -> null
        }
    }

    fun resolvePosition(
        savedPosition: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition {
        val chapter = loadedChapters.values.find {
            it.chapter.url == savedPosition.chapterUrl && !it.isLoading
        }

        if (chapter == null) {
            return ResolvedScrollPosition(
                displayIndex = 0,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.NOT_FOUND,
                confidence = 0f
            )
        }

        findBySegmentId(savedPosition, displayItems)?.let { return it }
        findBySegmentIndex(savedPosition, displayItems)?.let { return it }
        findByProgress(savedPosition, displayItems, loadedChapters)?.let { return it }
        findChapterStart(savedPosition, displayItems, loadedChapters)?.let { return it }

        return ResolvedScrollPosition(
            displayIndex = 0,
            offsetPixels = 0,
            resolutionMethod = ResolutionMethod.NOT_FOUND,
            confidence = 0f
        )
    }

    private fun findBySegmentId(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>
    ): ResolvedScrollPosition? {
        if (position.segmentId == "header") {
            val index = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterHeader &&
                        item.chapterIndex == position.chapterIndex
            }
            if (index >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = index,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                    confidence = 1f
                )
            }
        }

        if (position.segmentId == "divider") {
            val index = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterDivider &&
                        item.chapterIndex == position.chapterIndex
            }
            if (index >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = index,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                    confidence = 1f
                )
            }
        }

        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segment.id == position.segmentId
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = position.offsetPixels,
                resolutionMethod = ResolutionMethod.EXACT_SEGMENT_ID,
                confidence = 1f
            )
        } else null
    }

    private fun findBySegmentIndex(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>
    ): ResolvedScrollPosition? {
        if (position.segmentIndexInChapter < 0) {
            val headerIndex = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterHeader &&
                        item.chapterIndex == position.chapterIndex
            }
            if (headerIndex >= 0) {
                return ResolvedScrollPosition(
                    displayIndex = headerIndex,
                    offsetPixels = position.offsetPixels,
                    resolutionMethod = ResolutionMethod.SEGMENT_INDEX,
                    confidence = 0.9f
                )
            }
        }

        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segmentIndexInChapter == position.segmentIndexInChapter
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = position.offsetPixels,
                resolutionMethod = ResolutionMethod.SEGMENT_INDEX,
                confidence = 0.85f
            )
        } else null
    }

    private fun findByProgress(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition? {
        val chapter = loadedChapters.values.find { it.chapter.url == position.chapterUrl }
            ?: return null

        if (chapter.segments.isEmpty()) return null

        val targetSegmentIndex = (position.approximateProgress * chapter.segments.size)
            .toInt()
            .coerceIn(0, chapter.segments.lastIndex)

        val index = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl &&
                    item.segmentIndexInChapter >= targetSegmentIndex
        }

        return if (index >= 0) {
            ResolvedScrollPosition(
                displayIndex = index,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.PROGRESS_ESTIMATE,
                confidence = 0.6f
            )
        } else null
    }

    private fun findChapterStart(
        position: ReadingPosition,
        displayItems: List<ReaderDisplayItem>,
        loadedChapters: Map<Int, LoadedChapter>
    ): ResolvedScrollPosition? {
        val headerIndex = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.ChapterHeader &&
                    loadedChapters[item.chapterIndex]?.chapter?.url == position.chapterUrl
        }

        if (headerIndex >= 0) {
            return ResolvedScrollPosition(
                displayIndex = headerIndex,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.CHAPTER_START,
                confidence = 0.4f
            )
        }

        val firstSegmentIndex = displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterUrl == position.chapterUrl
        }

        return if (firstSegmentIndex >= 0) {
            ResolvedScrollPosition(
                displayIndex = firstSegmentIndex,
                offsetPixels = 0,
                resolutionMethod = ResolutionMethod.CHAPTER_START,
                confidence = 0.4f
            )
        } else null
    }

    fun savePosition(position: ReadingPosition) {
        preferencesManager.saveReadingPosition(
            chapterUrl = position.chapterUrl,
            segmentId = position.segmentId,
            segmentIndex = position.segmentIndexInChapter,
            progress = position.approximateProgress,
            offset = position.offsetPixels
        )
    }

    fun loadPosition(chapterUrl: String, chapterIndex: Int): ReadingPosition? {
        return preferencesManager.getReadingPosition(chapterUrl)?.let { saved ->
            ReadingPosition(
                chapterUrl = chapterUrl,
                chapterIndex = chapterIndex,
                segmentId = saved.segmentId,
                segmentIndexInChapter = saved.segmentIndex,
                approximateProgress = saved.progress,
                offsetPixels = saved.offset,
                timestamp = saved.timestamp
            )
        }
    }
}

// =============================================================================
// VIEW MODEL
// =============================================================================

class ReaderViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentProvider: MainProvider? = null
    private var currentNovelUrl: String? = null

    private val loadingMutex = Mutex()
    private var preloadJob: Job? = null

    private val scrollPositionManager = ScrollPositionManager(preferencesManager)
    private val _scrollRestorationState = MutableStateFlow(ScrollRestorationState())

    private var savePositionJob: Job? = null
    private val positionSaveDebounceMs = 500L

    // Flag to prevent preloading during initial scroll restoration
    private var isInitialLoadComplete = false

    private val ttsEngine by lazy { TTSManager.getEngine() }
    private var appContext: Context? = null
    private var ttsSentenceList: List<TTSSentenceInfo> = emptyList()

    private data class TTSSentenceInfo(
        val text: String,
        val displayItemIndex: Int,
        val segmentIndexInChapter: Int,
        val sentenceIndexInSegment: Int,
        val chapterIndex: Int
    )

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    init {
        loadTTSSettings()

        viewModelScope.launch {
            preferencesManager.readerSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            preferencesManager.appSettings.collect { appSettings ->
                _uiState.update {
                    it.copy(infiniteScrollEnabled = appSettings.infiniteScroll)
                }
            }
        }

        viewModelScope.launch {
            TTSServiceManager.playbackState.collect { ttsState ->
                _uiState.update {
                    it.copy(
                        isTTSActive = ttsState.isActive,
                        ttsStatus = when {
                            ttsState.isPlaying -> TTSStatus.PLAYING
                            ttsState.isPaused -> TTSStatus.PAUSED
                            else -> TTSStatus.STOPPED
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            TTSServiceManager.segmentChanged.collect { sentenceIndex ->
                updateTTSHighlight(sentenceIndex)
            }
        }

        viewModelScope.launch {
            TTSServiceManager.playbackComplete.collect {
                clearTTSHighlight()
            }
        }
    }

    // ================================================================
    // CHAPTER LIST
    // ================================================================

    fun toggleChapterList() {
        _uiState.update { it.copy(showChapterList = !it.showChapterList) }
    }

    fun hideChapterList() {
        _uiState.update { it.copy(showChapterList = false) }
    }

    fun navigateToChapter(chapterIndex: Int) {
        val state = _uiState.value
        val allChapters = state.allChapters

        if (chapterIndex < 0 || chapterIndex >= allChapters.size) return

        val chapter = allChapters[chapterIndex]
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return

        saveCurrentPosition()
        stopTTS()

        _uiState.update { it.copy(showChapterList = false) }

        loadChapter(chapter.url, novelUrl, provider.name)
    }

    // ================================================================
    // TTS SETTINGS
    // ================================================================

    private fun loadTTSSettings() {
        val settings = TTSSettingsState(
            speed = preferencesManager.getTtsSpeed(),
            pitch = preferencesManager.getTtsPitch(),
            volume = preferencesManager.getTtsVolume(),
            voiceId = preferencesManager.getTtsVoice(),
            autoScroll = preferencesManager.getTtsAutoScroll(),
            highlightSentence = preferencesManager.getTtsHighlightSentence(),
            pauseOnCalls = preferencesManager.getTtsPauseOnCalls()
        )
        _uiState.update { it.copy(ttsSettings = settings) }
    }

    fun updateTTSSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.5f)
        preferencesManager.setTtsSpeed(clampedSpeed)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(speed = clampedSpeed)) }
        TTSServiceManager.setSpeechRate(clampedSpeed)
    }

    fun updateTTSPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        preferencesManager.setTtsPitch(clampedPitch)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(pitch = clampedPitch)) }
    }

    fun updateTTSVoice(voice: VoiceInfo) {
        VoiceManager.selectVoice(voice.id)
        preferencesManager.setTtsVoice(voice.id)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(voiceId = voice.id)) }
    }

    fun updateTTSAutoScroll(enabled: Boolean) {
        preferencesManager.setTtsAutoScroll(enabled)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(autoScroll = enabled)) }
    }

    fun updateTTSHighlightSentence(enabled: Boolean) {
        preferencesManager.setTtsHighlightSentence(enabled)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(highlightSentence = enabled)) }
    }

    fun toggleTTSSettings() {
        _uiState.update { it.copy(showTTSSettings = !it.showTTSSettings) }
    }

    fun hideTTSSettings() {
        _uiState.update { it.copy(showTTSSettings = false) }
    }

    // ================================================================
    // LOAD CHAPTER
    // ================================================================

    fun loadChapter(
        chapterUrl: String,
        novelUrl: String,
        providerName: String
    ) {
        currentNovelUrl = novelUrl
        currentProvider = novelRepository.getProvider(providerName)

        val provider = currentProvider ?: run {
            _uiState.update { it.copy(error = "Provider not found", isLoading = false) }
            return
        }

        stopTTS()

        // Reset all state for new chapter load
        isInitialLoadComplete = false
        _scrollRestorationState.value = ScrollRestorationState()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentChapterUrl = chapterUrl,
                    loadedChapters = emptyMap(),
                    displayItems = emptyList(),
                    targetScrollPosition = null,
                    hasRestoredScroll = false,
                    currentScrollIndex = 0,
                    currentScrollOffset = 0,
                    currentSegmentIndex = -1,
                    isTTSActive = false,
                    ttsPosition = TTSPosition(),
                    currentSentenceHighlight = null
                )
            }

            val detailsResult = novelRepository.loadNovelDetails(provider, novelUrl)

            detailsResult.fold(
                onSuccess = { details ->
                    val allChapters = details.chapters
                    val chapterIndex = allChapters.indexOfFirst { it.url == chapterUrl }
                        .takeIf { it >= 0 } ?: 0

                    _uiState.update {
                        it.copy(
                            allChapters = allChapters,
                            initialChapterIndex = chapterIndex,
                            currentChapterIndex = chapterIndex,
                            previousChapter = if (chapterIndex > 0) allChapters[chapterIndex - 1] else null,
                            nextChapter = if (chapterIndex < allChapters.size - 1) allChapters[chapterIndex + 1] else null
                        )
                    }

                    // Start the phased loading process
                    startInitialLoad(chapterIndex)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to load novel details",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    /**
     * PHASE 1: Load only the current chapter
     * PHASE 2: Wait for scroll restoration
     * PHASE 3: Preload adjacent chapters (next only initially to avoid scroll shift)
     */
    private fun startInitialLoad(chapterIndex: Int) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            _uiState.update { it.copy(isPreloading = true) }

            // PHASE 1: Load only the current chapter
            loadChapterContent(chapterIndex, isInitialLoad = true)

            // PHASE 2: Wait for scroll restoration to complete
            val infiniteScrollEnabled = _uiState.value.infiniteScrollEnabled

            if (infiniteScrollEnabled) {
                // Wait for scroll restoration with timeout
                var waited = 0
                val maxWait = 3000 // 3 seconds max
                while (!_uiState.value.hasRestoredScroll && waited < maxWait) {
                    delay(50)
                    waited += 50
                }

                // Additional delay for layout stability
                delay(100)

                // PHASE 3: Only preload NEXT chapter (not previous!)
                // Loading previous chapter would shift all indices and break scroll position
                val allChapters = _uiState.value.allChapters
                if (chapterIndex < allChapters.size - 1) {
                    loadChapterContent(chapterIndex + 1, isInitialLoad = false)
                }
            }

            isInitialLoadComplete = true
            _uiState.update { it.copy(isPreloading = false) }
        }
    }

    private suspend fun loadChapterContent(
        chapterIndex: Int,
        isInitialLoad: Boolean = false
    ) {
        loadingMutex.withLock {
            val state = _uiState.value
            val allChapters = state.allChapters

            if (chapterIndex < 0 || chapterIndex >= allChapters.size) return
            if (state.loadedChapters.containsKey(chapterIndex)) return

            val chapter = allChapters[chapterIndex]
            val provider = currentProvider ?: return

            // Track if this chapter is before the current viewing position
            val isBeforeCurrentChapter = chapterIndex < state.currentChapterIndex
            val itemCountBefore = state.displayItems.size

            _uiState.update {
                val loadingChapter = LoadedChapter(
                    chapter = chapter,
                    chapterIndex = chapterIndex,
                    segments = emptyList(),
                    isLoading = true
                )
                it.copy(
                    loadedChapters = it.loadedChapters + (chapterIndex to loadingChapter)
                )
            }

            rebuildDisplayItems()

            val result = novelRepository.loadChapterContent(provider, chapter.url)

            result.fold(
                onSuccess = { content ->
                    val segments = parseContentToSegments(content)

                    val loadedChapter = LoadedChapter(
                        chapter = chapter,
                        chapterIndex = chapterIndex,
                        segments = segments,
                        isLoading = false
                    )

                    _uiState.update {
                        it.copy(
                            loadedChapters = it.loadedChapters + (chapterIndex to loadedChapter),
                            isLoading = if (chapterIndex == it.initialChapterIndex) false else it.isLoading,
                            currentChapterName = if (chapterIndex == it.currentChapterIndex) chapter.name else it.currentChapterName
                        )
                    }

                    rebuildDisplayItems()

                    // Adjust scroll position if we loaded a chapter BEFORE the current position
                    // (This only happens during non-initial loads, e.g., when user scrolls up)
                    if (isBeforeCurrentChapter && !isInitialLoad && isInitialLoadComplete) {
                        val itemCountAfter = _uiState.value.displayItems.size
                        val itemsAdded = itemCountAfter - itemCountBefore

                        if (itemsAdded > 0) {
                            adjustScrollPositionBy(itemsAdded)
                        }
                    }

                    // Trigger scroll restoration for initial load
                    onChapterLoaded(chapterIndex, isInitialLoad)

                    if (chapterIndex == _uiState.value.initialChapterIndex) {
                        addToHistory(chapter.url, chapter.name)
                    }
                },
                onFailure = { error ->
                    val errorChapter = LoadedChapter(
                        chapter = chapter,
                        chapterIndex = chapterIndex,
                        segments = emptyList(),
                        isLoading = false,
                        error = error.message ?: "Failed to load chapter"
                    )

                    _uiState.update {
                        it.copy(
                            loadedChapters = it.loadedChapters + (chapterIndex to errorChapter),
                            isLoading = if (chapterIndex == it.initialChapterIndex) false else it.isLoading,
                            error = if (chapterIndex == it.initialChapterIndex) error.message else it.error
                        )
                    }

                    rebuildDisplayItems()
                }
            )
        }
    }

    /**
     * Adjust scroll position when items are added before current position
     */
    private fun adjustScrollPositionBy(itemsAdded: Int) {
        _uiState.update {
            val newIndex = it.currentScrollIndex + itemsAdded
            it.copy(
                currentScrollIndex = newIndex,
                targetScrollPosition = TargetScrollPosition(
                    displayIndex = newIndex,
                    offsetPixels = it.currentScrollOffset
                )
            )
        }
    }

    private fun onChapterLoaded(chapterIndex: Int, isInitialLoad: Boolean) {
        val state = _uiState.value
        val restorationState = _scrollRestorationState.value

        if (restorationState.pendingPosition != null &&
            restorationState.pendingPosition.chapterIndex == chapterIndex
        ) {
            attemptScrollRestoration()
            return
        }

        if (isInitialLoad && !restorationState.hasSuccessfullyRestored) {
            val chapter = state.allChapters.getOrNull(chapterIndex) ?: return
            val savedPosition = scrollPositionManager.loadPosition(chapter.url, chapterIndex)

            if (savedPosition != null) {
                _scrollRestorationState.update {
                    it.copy(
                        pendingPosition = savedPosition,
                        isWaitingForChapter = false
                    )
                }
                attemptScrollRestoration()
            } else {
                _scrollRestorationState.update {
                    it.copy(hasSuccessfullyRestored = true)
                }
                _uiState.update { it.copy(hasRestoredScroll = true) }
            }
        }
    }

    private fun attemptScrollRestoration() {
        val state = _uiState.value
        val restorationState = _scrollRestorationState.value
        val pendingPosition = restorationState.pendingPosition ?: return

        if (restorationState.hasSuccessfullyRestored) return
        if (restorationState.restorationAttempts >= restorationState.maxAttempts) {
            _scrollRestorationState.update {
                it.copy(
                    pendingPosition = null,
                    hasSuccessfullyRestored = true
                )
            }
            _uiState.update { it.copy(hasRestoredScroll = true) }
            return
        }

        val resolved = scrollPositionManager.resolvePosition(
            savedPosition = pendingPosition,
            displayItems = state.displayItems,
            loadedChapters = state.loadedChapters
        )

        when (resolved.resolutionMethod) {
            ResolutionMethod.NOT_FOUND -> {
                _scrollRestorationState.update {
                    it.copy(
                        restorationAttempts = it.restorationAttempts + 1,
                        isWaitingForChapter = true,
                        lastAttemptTime = System.currentTimeMillis()
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        targetScrollPosition = TargetScrollPosition(
                            displayIndex = resolved.displayIndex,
                            offsetPixels = resolved.offsetPixels
                        )
                    )
                }

                _scrollRestorationState.update {
                    it.copy(
                        pendingPosition = null,
                        hasSuccessfullyRestored = true,
                        isWaitingForChapter = false
                    )
                }
            }
        }
    }

    private fun rebuildDisplayItems() {
        val state = _uiState.value
        val allChapters = state.allChapters
        val loadedChapters = state.loadedChapters

        if (allChapters.isEmpty()) return

        val items = mutableListOf<ReaderDisplayItem>()
        var globalSegmentIndex = 0

        val sortedIndices = loadedChapters.keys.sorted()

        for (chapterIndex in sortedIndices) {
            val loadedChapter = loadedChapters[chapterIndex] ?: continue
            val chapter = loadedChapter.chapter

            items.add(
                ReaderDisplayItem.ChapterHeader(
                    chapterIndex = chapterIndex,
                    chapterName = chapter.name,
                    chapterNumber = chapterIndex + 1,
                    totalChapters = allChapters.size
                )
            )

            when {
                loadedChapter.isLoading -> {
                    items.add(ReaderDisplayItem.LoadingIndicator(chapterIndex))
                }

                loadedChapter.error != null -> {
                    items.add(ReaderDisplayItem.ErrorIndicator(chapterIndex, loadedChapter.error))
                }

                else -> {
                    loadedChapter.segments.forEachIndexed { segmentIndex, segment ->
                        items.add(
                            ReaderDisplayItem.Segment(
                                chapterIndex = chapterIndex,
                                chapterUrl = chapter.url,
                                segment = segment,
                                segmentIndexInChapter = segmentIndex,
                                globalSegmentIndex = globalSegmentIndex
                            )
                        )
                        globalSegmentIndex++
                    }

                    items.add(
                        ReaderDisplayItem.ChapterDivider(
                            chapterIndex = chapterIndex,
                            chapterName = chapter.name,
                            chapterNumber = chapterIndex + 1,
                            totalChapters = allChapters.size,
                            hasNextChapter = chapterIndex < allChapters.size - 1
                        )
                    )
                }
            }
        }

        _uiState.update {
            it.copy(
                displayItems = items,
                totalTTSSentences = items
                    .filterIsInstance<ReaderDisplayItem.Segment>()
                    .sumOf { seg -> seg.segment.sentenceCount }
            )
        }

        rebuildTTSSentenceList()
    }

    private fun rebuildTTSSentenceList() {
        val state = _uiState.value
        val sentences = mutableListOf<TTSSentenceInfo>()

        state.displayItems.forEachIndexed { displayIndex, item ->
            if (item is ReaderDisplayItem.Segment) {
                item.segment.sentences.forEachIndexed { sentenceIndex, sentence ->
                    sentences.add(
                        TTSSentenceInfo(
                            text = sentence.text,
                            displayItemIndex = displayIndex,
                            segmentIndexInChapter = item.segmentIndexInChapter,
                            sentenceIndexInSegment = sentenceIndex,
                            chapterIndex = item.chapterIndex
                        )
                    )
                }
            }
        }

        ttsSentenceList = sentences
    }

    // ================================================================
    // CONTENT PARSING
    // ================================================================

    private fun parseContentToSegments(html: String): List<ContentSegment> {
        val cleanedHtml = HtmlUtils.sanitize(html)
        val segments = mutableListOf<ContentSegment>()

        val doc = org.jsoup.Jsoup.parse(cleanedHtml)
        val elements = doc.body().children()

        elements.forEachIndexed { index, element ->
            val elementHtml = element.outerHtml()
            val elementText = element.text()

            if (elementText.isNotBlank() || element.tagName() == "img") {
                val parsedParagraph = SentenceParser.parse(elementText)

                segments.add(
                    ContentSegment(
                        id = "seg-$index",
                        html = elementHtml,
                        text = elementText,
                        sentences = parsedParagraph.sentences
                    )
                )
            }
        }

        if (segments.isEmpty() && cleanedHtml.isNotBlank()) {
            val text = HtmlUtils.extractText(cleanedHtml)
            val parsedParagraph = SentenceParser.parse(text)

            segments.add(
                ContentSegment(
                    id = "seg-0",
                    html = cleanedHtml,
                    text = text,
                    sentences = parsedParagraph.sentences
                )
            )
        }

        return segments
    }

    // ================================================================
    // PRELOADING & CHAPTER MANAGEMENT
    // ================================================================

    /**
     * Called when user is approaching the end of loaded content (scrolling down)
     */
    fun onApproachingEnd(lastVisibleChapterIndex: Int) {
        if (!_uiState.value.infiniteScrollEnabled) return
        if (!isInitialLoadComplete) return // Don't preload during initial load

        val state = _uiState.value
        val allChapters = state.allChapters
        val loadedChapters = state.loadedChapters

        val maxLoadedIndex = loadedChapters.keys.maxOrNull() ?: return

        if (lastVisibleChapterIndex >= maxLoadedIndex - 1) {
            val nextToLoad = maxLoadedIndex + 1
            if (nextToLoad < allChapters.size && !loadedChapters.containsKey(nextToLoad)) {
                viewModelScope.launch {
                    // Unload oldest chapters to maintain window of 3
                    val chaptersToKeep = (nextToLoad - 2..nextToLoad).toSet()
                    unloadChaptersExcept(chaptersToKeep)

                    loadChapterContent(nextToLoad)
                }
            }
        }
    }

    /**
     * Called when user is approaching the beginning of loaded content (scrolling up)
     */
    fun onApproachingBeginning(firstVisibleChapterIndex: Int) {
        if (!_uiState.value.infiniteScrollEnabled) return
        if (!isInitialLoadComplete) return // Don't preload during initial load

        val state = _uiState.value
        val loadedChapters = state.loadedChapters

        val minLoadedIndex = loadedChapters.keys.minOrNull() ?: return

        if (firstVisibleChapterIndex <= minLoadedIndex + 1) {
            val prevToLoad = minLoadedIndex - 1
            if (prevToLoad >= 0 && !loadedChapters.containsKey(prevToLoad)) {
                viewModelScope.launch {
                    // Unload chapters that are too far ahead
                    val chaptersToKeep = (prevToLoad..prevToLoad + 2).toSet()
                    unloadChaptersExcept(chaptersToKeep)

                    // Load previous chapter (scroll adjustment handled in loadChapterContent)
                    loadChapterContent(prevToLoad)
                }
            }
        }
    }

    private fun unloadChaptersExcept(chaptersToKeep: Set<Int>) {
        val loadedChapters = _uiState.value.loadedChapters
        val chaptersToUnload = loadedChapters.keys - chaptersToKeep

        if (chaptersToUnload.isNotEmpty()) {
            val updatedLoadedChapters = loadedChapters.toMutableMap()
            chaptersToUnload.forEach { updatedLoadedChapters.remove(it) }
            _uiState.update { it.copy(loadedChapters = updatedLoadedChapters) }
            rebuildDisplayItems()
        }
    }

    fun updateCurrentChapter(chapterIndex: Int, chapterUrl: String, chapterName: String) {
        val state = _uiState.value
        if (state.currentChapterIndex != chapterIndex) {
            _uiState.update {
                it.copy(
                    currentChapterIndex = chapterIndex,
                    currentChapterUrl = chapterUrl,
                    currentChapterName = chapterName,
                    previousChapter = if (chapterIndex > 0) it.allChapters.getOrNull(chapterIndex - 1) else null,
                    nextChapter = if (chapterIndex < it.allChapters.size - 1) it.allChapters.getOrNull(chapterIndex + 1) else null
                )
            }

            addToHistory(chapterUrl, chapterName)
        }
    }

    // ================================================================
    // SCROLL POSITION TRACKING
    // ================================================================

    fun updateCurrentScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        _uiState.update {
            it.copy(
                currentScrollIndex = firstVisibleItemIndex,
                currentScrollOffset = firstVisibleItemOffset
            )
        }

        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            delay(positionSaveDebounceMs)

            val currentState = _uiState.value
            val position = scrollPositionManager.capturePosition(
                displayItems = currentState.displayItems,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemOffset = firstVisibleItemOffset,
                loadedChapters = currentState.loadedChapters
            )

            if (position != null && position.chapterUrl.isNotBlank()) {
                if (position.chapterIndex != currentState.currentChapterIndex) {
                    val chapter = currentState.allChapters.getOrNull(position.chapterIndex)
                    if (chapter != null) {
                        updateCurrentChapter(position.chapterIndex, chapter.url, chapter.name)
                    }
                }
            }
        }
    }

    fun markScrollRestored() {
        _uiState.update {
            it.copy(
                targetScrollPosition = null,
                hasRestoredScroll = true
            )
        }
    }

    fun saveCurrentPosition() {
        val state = _uiState.value

        val position = scrollPositionManager.capturePosition(
            displayItems = state.displayItems,
            firstVisibleItemIndex = state.currentScrollIndex,
            firstVisibleItemOffset = state.currentScrollOffset,
            loadedChapters = state.loadedChapters
        ) ?: return

        if (position.chapterUrl.isBlank()) return

        scrollPositionManager.savePosition(position)

        val novelUrl = currentNovelUrl ?: return
        viewModelScope.launch {
            if (libraryRepository.isFavorite(novelUrl)) {
                libraryRepository.updateReadingPosition(
                    novelUrl = novelUrl,
                    chapterUrl = position.chapterUrl,
                    chapterName = state.currentChapterName,
                    scrollIndex = position.segmentIndexInChapter,
                    scrollOffset = position.offsetPixels
                )
            }
        }
    }

    fun savePositionOnExit() = saveCurrentPosition()

    // ================================================================
    // HISTORY
    // ================================================================

    private fun addToHistory(chapterUrl: String, chapterTitle: String) {
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return

        viewModelScope.launch {
            val details = offlineRepository.getNovelDetails(novelUrl)

            if (details != null) {
                val novel = Novel(
                    name = details.name,
                    url = details.url,
                    posterUrl = details.posterUrl,
                    apiName = provider.name
                )

                val chapter = Chapter(name = chapterTitle, url = chapterUrl)
                historyRepository.addToHistory(novel, chapter)
                libraryRepository.updateLastChapter(novelUrl, chapter)
            }
        }
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    fun navigateToPrevious() {
        saveCurrentPosition()
        val previous = _uiState.value.previousChapter ?: return
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return
        loadChapter(previous.url, novelUrl, provider.name)
    }

    fun navigateToNext() {
        saveCurrentPosition()
        val next = _uiState.value.nextChapter ?: return
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return
        loadChapter(next.url, novelUrl, provider.name)
    }

    fun retryChapter(chapterIndex: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadedChapters = it.loadedChapters - chapterIndex)
            }
            loadChapterContent(chapterIndex)
        }
    }

    // ================================================================
    // UI CONTROLS
    // ================================================================

    fun toggleControls() {
        _uiState.update {
            it.copy(
                showControls = !it.showControls,
                showSettings = if (!it.showControls) false else it.showSettings,
                showTTSSettings = if (!it.showControls) false else it.showTTSSettings
            )
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun updateReadingProgress(progress: Float) {
        _uiState.update { it.copy(readingProgress = progress) }
    }

    fun updateChapterProgress(progress: Float) {
        _uiState.update { it.copy(chapterProgress = progress) }
    }

    // ================================================================
    // SETTINGS
    // ================================================================

    fun updateSettings(settings: ReaderSettings) {
        preferencesManager.updateReaderSettings(settings)
    }

    fun updateFontSize(size: Int) {
        val current = _uiState.value.settings
        updateSettings(current.copy(fontSize = size.coerceIn(12, 32)))
    }

    fun updateFontFamily(family: FontFamily) {
        val current = _uiState.value.settings
        updateSettings(current.copy(fontFamily = family))
    }

    fun updateTheme(theme: ReaderTheme) {
        val current = _uiState.value.settings
        updateSettings(current.copy(theme = theme))
    }

    fun updateTextAlign(align: TextAlign) {
        val current = _uiState.value.settings
        updateSettings(current.copy(textAlign = align))
    }

    fun updateLineHeight(height: Float) {
        val current = _uiState.value.settings
        updateSettings(current.copy(lineHeight = height))
    }

    // ================================================================
    // TTS
    // ================================================================

    fun getTTSEngine() = ttsEngine

    fun startTTS() {
        val context = appContext ?: return
        val state = _uiState.value

        val startingDisplayIndex = findFirstVisibleSegmentIndex()

        if (ttsSentenceList.isEmpty()) {
            rebuildTTSSentenceList()
        }

        if (ttsSentenceList.isEmpty()) return

        val startSentenceIndex = if (startingDisplayIndex >= 0) {
            ttsSentenceList.indexOfFirst { it.displayItemIndex >= startingDisplayIndex }
                .coerceAtLeast(0)
        } else {
            0
        }

        viewModelScope.launch {
            val novelUrl = currentNovelUrl ?: ""
            val novelDetails = offlineRepository.getNovelDetails(novelUrl)
            val coverUrl = novelDetails?.posterUrl
            val coverBitmap = loadCoverBitmap(coverUrl)

            val novelName = novelDetails?.name
                ?: state.currentChapterName.split(" - ").firstOrNull()
                ?: state.currentChapterName.ifBlank { "Novel" }

            val content = TTSContent(
                novelName = novelName,
                novelUrl = novelUrl,
                chapterName = state.currentChapterName,
                chapterUrl = state.currentChapterUrl,
                segments = ttsSentenceList.map { it.text },
                coverUrl = coverUrl
            )

            updateTTSHighlight(startSentenceIndex)

            _uiState.update {
                it.copy(
                    isTTSActive = true,
                    currentGlobalSentenceIndex = startSentenceIndex,
                    totalTTSSentences = ttsSentenceList.size
                )
            }

            TTSServiceManager.setSpeechRate(state.ttsSettings.speed)

            TTSServiceManager.startPlayback(
                context = context,
                content = content,
                startIndex = startSentenceIndex,
                cover = coverBitmap
            )
        }
    }

    private fun findFirstVisibleSegmentIndex(): Int {
        val state = _uiState.value
        val scrollIndex = state.currentScrollIndex
        val displayItems = state.displayItems

        for (i in scrollIndex until displayItems.size) {
            if (displayItems[i] is ReaderDisplayItem.Segment) {
                return i
            }
        }

        for (i in scrollIndex downTo 0) {
            if (displayItems[i] is ReaderDisplayItem.Segment) {
                return i
            }
        }

        return -1
    }

    private fun updateTTSHighlight(sentenceIndex: Int) {
        if (sentenceIndex < 0 || sentenceIndex >= ttsSentenceList.size) {
            clearTTSHighlight()
            return
        }

        val sentenceInfo = ttsSentenceList[sentenceIndex]
        val displayItems = _uiState.value.displayItems
        val displayItem = displayItems.getOrNull(sentenceInfo.displayItemIndex)

        if (displayItem is ReaderDisplayItem.Segment) {
            val sentence = displayItem.segment.getSentence(sentenceInfo.sentenceIndexInSegment)

            if (sentence != null) {
                val highlight = SentenceHighlight(
                    segmentDisplayIndex = sentenceInfo.displayItemIndex,
                    sentenceIndex = sentenceInfo.sentenceIndexInSegment,
                    sentence = sentence
                )

                _uiState.update {
                    it.copy(
                        currentSegmentIndex = sentenceInfo.displayItemIndex,
                        currentSentenceHighlight = highlight,
                        currentGlobalSentenceIndex = sentenceIndex,
                        ttsPosition = TTSPosition(
                            segmentIndex = sentenceInfo.displayItemIndex,
                            sentenceIndexInSegment = sentenceInfo.sentenceIndexInSegment,
                            globalSentenceIndex = sentenceIndex
                        )
                    )
                }
            }
        }
    }

    private fun clearTTSHighlight() {
        _uiState.update {
            it.copy(
                isTTSActive = false,
                currentSegmentIndex = -1,
                currentSentenceHighlight = null,
                ttsPosition = TTSPosition(),
                currentGlobalSentenceIndex = 0
            )
        }
    }

    fun stopTTS() {
        TTSServiceManager.stop()
        clearTTSHighlight()
    }

    fun pauseTTS() {
        TTSServiceManager.pause()
    }

    fun resumeTTS() {
        TTSServiceManager.resume()
    }

    fun nextSegment() {
        val currentIndex = _uiState.value.currentGlobalSentenceIndex
        val nextIndex = currentIndex + 1

        if (nextIndex < ttsSentenceList.size) {
            TTSServiceManager.seekToSegment(nextIndex)
            updateTTSHighlight(nextIndex)
        }
    }

    fun previousSegment() {
        val currentIndex = _uiState.value.currentGlobalSentenceIndex
        val prevIndex = (currentIndex - 1).coerceAtLeast(0)

        TTSServiceManager.seekToSegment(prevIndex)
        updateTTSHighlight(prevIndex)
    }

    fun setCurrentSegment(index: Int) {
        val displayItems = _uiState.value.displayItems
        val item = displayItems.getOrNull(index)

        if (item is ReaderDisplayItem.Segment) {
            val sentenceIndex = ttsSentenceList.indexOfFirst {
                it.displayItemIndex == index
            }

            if (sentenceIndex >= 0) {
                _uiState.update {
                    it.copy(
                        currentSegmentIndex = index,
                        currentTTSChapterIndex = item.chapterIndex
                    )
                }

                TTSServiceManager.seekToSegment(sentenceIndex)
                updateTTSHighlight(sentenceIndex)
            }
        }
    }

    fun seekToSentence(displayIndex: Int, sentenceIndex: Int) {
        val globalIndex = ttsSentenceList.indexOfFirst {
            it.displayItemIndex == displayIndex &&
                    it.sentenceIndexInSegment == sentenceIndex
        }

        if (globalIndex >= 0) {
            TTSServiceManager.seekToSegment(globalIndex)
            updateTTSHighlight(globalIndex)
        }
    }

    fun getCurrentSegmentText(): String {
        val state = _uiState.value
        val highlight = state.currentSentenceHighlight
        return highlight?.sentence?.text ?: ""
    }

    fun getHighlightedText(
        segment: ContentSegment,
        displayIndex: Int,
        highlightColor: Color
    ): AnnotatedString {
        val state = _uiState.value
        val currentHighlight = state.currentSentenceHighlight

        val shouldHighlight = state.isTTSActive &&
                state.ttsSettings.highlightSentence &&
                currentHighlight != null &&
                currentHighlight.segmentDisplayIndex == displayIndex

        return buildAnnotatedString {
            append(segment.text)

            if (shouldHighlight && currentHighlight != null) {
                val sentence = currentHighlight.sentence
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = sentence.startIndex.coerceAtMost(segment.text.length),
                    end = sentence.endIndex.coerceAtMost(segment.text.length)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentPosition()
        savePositionJob?.cancel()
        preloadJob?.cancel()
    }
}