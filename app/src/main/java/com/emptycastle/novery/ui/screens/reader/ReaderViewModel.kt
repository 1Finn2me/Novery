package com.emptycastle.novery.ui.screens.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.VolumeKeyDirection
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.service.TTSChapterChangeEvent
import com.emptycastle.novery.service.TTSContent
import com.emptycastle.novery.service.TTSSegment
import com.emptycastle.novery.service.TTSServiceManager
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.tts.TTSManager
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.tts.VoiceManager
import com.emptycastle.novery.ui.screens.reader.logic.ChapterLoadResult
import com.emptycastle.novery.ui.screens.reader.logic.ChapterLoader
import com.emptycastle.novery.ui.screens.reader.model.ChapterCharacterMap
import com.emptycastle.novery.ui.screens.reader.model.ChapterContentItem
import com.emptycastle.novery.ui.screens.reader.model.ContentSegment
import com.emptycastle.novery.ui.screens.reader.model.PositionResolution
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.model.SentenceBoundsInSegment
import com.emptycastle.novery.ui.screens.reader.model.SentenceHighlight
import com.emptycastle.novery.ui.screens.reader.model.StableScrollPosition
import com.emptycastle.novery.ui.screens.reader.model.TTSPosition
import com.emptycastle.novery.ui.screens.reader.model.TTSScrollEdge
import com.emptycastle.novery.ui.screens.reader.model.TTSSettingsState
import com.emptycastle.novery.ui.screens.reader.model.TargetScrollPosition
import com.emptycastle.novery.util.ReadingTimeTracker
import com.emptycastle.novery.util.VolumeKeyEvent
import com.emptycastle.novery.util.VolumeKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "ReaderViewModel"

private suspend fun loadCoverBitmap(coverUrl: String?): Bitmap? {
    if (coverUrl.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(coverUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            BitmapFactory.decodeStream(connection.getInputStream())
        } catch (e: Exception) {
            null
        }
    }
}

// =============================================================================
// STABLE TTS DATA STRUCTURES
// =============================================================================

/**
 * Stable coordinates for a TTS sentence position.
 * These coordinates do NOT change when chapters are loaded/unloaded.
 */
private data class StableTTSCoordinate(
    val chapterIndex: Int,
    val segmentIndexInChapter: Int,
    val sentenceIndexInSegment: Int
) {
    val isValid: Boolean get() = chapterIndex >= 0 && segmentIndexInChapter >= 0 && sentenceIndexInSegment >= 0

    companion object {
        val INVALID = StableTTSCoordinate(-1, -1, -1)
    }
}

/**
 * TTS sentence info with stable coordinates.
 * The globalIndex is used for TTS service communication but may become stale.
 */
private data class TTSSentenceInfo(
    val text: String,
    // Stable coordinates (never change unless chapter content changes)
    val chapterIndex: Int,
    val segmentIndexInChapter: Int,
    val sentenceIndexInSegment: Int,
    // Pause after this sentence
    val pauseAfterMs: Int
) {
    val coordinate: StableTTSCoordinate
        get() = StableTTSCoordinate(chapterIndex, segmentIndexInChapter, sentenceIndexInSegment)
}

/**
 * Navigation source for determining scroll restoration behavior.
 */
private enum class NavigationSource {
    CONTINUE,       // Continuing from last position (restore scroll)
    CHAPTER_LIST,   // User selected from chapter list (start fresh)
    NAVIGATION,     // Previous/Next buttons (start fresh)
    TTS_AUTO        // TTS auto-advanced (TTS will set position)
}

class ReaderViewModel : ViewModel() {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val historyRepository = RepositoryProvider.getHistoryRepository()
    private val offlineRepository = RepositoryProvider.getOfflineRepository()
    private val libraryRepository = RepositoryProvider.getLibraryRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val statsRepository = RepositoryProvider.getStatsRepository()
    private val bookmarkRepository = RepositoryProvider.getBookmarkRepository()

    private val chapterLoader = ChapterLoader(novelRepository)

    // =========================================================================
    // STATE
    // =========================================================================

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentProvider: MainProvider? = null
    private var currentNovelUrl: String? = null

    // Thread-safe flags
    private val isTransitioning = AtomicBoolean(false)
    private val blockInfiniteScroll = AtomicBoolean(true)
    private val blockTTSUpdates = AtomicBoolean(true)
    private val blockTTSSync = AtomicBoolean(false)

    // TTS rebuild lock - prevents concurrent rebuilds
    private val ttsRebuildMutex = Mutex()
    private val ttsOperationMutex = Mutex()

    // Generation counter for load operations
    private val loadGeneration = AtomicLong(0L)

    // Mutex for state modifications
    private val stateMutex = Mutex()

    // Track which chapters are currently loading
    private val loadingChapters = mutableSetOf<Int>()
    private val loadingChaptersMutex = Mutex()

    // Stable position tracking
    private var desiredScrollPosition: StableScrollPosition? = null
    private val characterMaps = mutableMapOf<Int, ChapterCharacterMap>()

    // Preload configuration
    private var preloadBefore: Int = 1
    private var preloadAfter: Int = 2

    private var requestedChapterIndex = -1
    private var preloadJob: Job? = null
    private var savePositionJob: Job? = null
    private val positionSaveDebounceMs = 500L

    // Navigation tracking
    private var lastNavigationSource: NavigationSource = NavigationSource.CONTINUE

    // =========================================================================
    // TTS STATE - USING STABLE COORDINATES
    // =========================================================================

    private val ttsEngine by lazy { TTSManager.getEngine() }
    private var appContext: Context? = null

    // The master sentence list - rebuilt when chapters load
    private var ttsSentenceList: List<TTSSentenceInfo> = emptyList()

    // Current TTS position using STABLE coordinates (survives display index shifts)
    private var currentTTSCoordinate: StableTTSCoordinate = StableTTSCoordinate.INVALID

    // Cached sentence text for fallback matching after rebuilds
    private var currentTTSSentenceText: String = ""

    // TTS sentence bounds tracking
    private var currentSentenceBounds: SentenceBoundsInSegment = SentenceBoundsInSegment.INVALID

    private val _sentenceBounds = MutableStateFlow(SentenceBoundsInSegment.INVALID)
    val sentenceBounds: StateFlow<SentenceBoundsInSegment> = _sentenceBounds.asStateFlow()

    // Reading time tracking
    private var readingTimeTracker: ReadingTimeTracker? = null

    // Volume key navigation
    private val _volumeScrollAction = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val volumeScrollAction: SharedFlow<Boolean> = _volumeScrollAction.asSharedFlow()

    // TTS scroll lock
    private val _ttsScrollLocked = MutableStateFlow(true)
    val ttsScrollLocked: StateFlow<Boolean> = _ttsScrollLocked.asStateFlow()

    // TTS ensure visible
    private val _ttsShouldEnsureVisible = MutableStateFlow<Int?>(null)
    val ttsShouldEnsureVisible: StateFlow<Int?> = _ttsShouldEnsureVisible.asStateFlow()

    // Visibility tracking
    private var isReaderVisible = true
    private var ttsWasActiveOnLeave = false
    private var ttsChapterOnLeave = -1

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    init {
        loadTTSSettings()
        observeSettings()
        observeTTSState()
        observeVolumeKeys()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferencesManager.readerSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                VolumeKeyManager.setVolumeKeyNavigationEnabled(settings.volumeKeyNavigation)
                _ttsScrollLocked.value = settings.lockScrollDuringTTS
            }
        }

        viewModelScope.launch {
            preferencesManager.appSettings.collect { appSettings ->
                _uiState.update { it.copy(infiniteScrollEnabled = appSettings.infiniteScroll) }
            }
        }
    }

    private fun observeTTSState() {
        viewModelScope.launch {
            TTSServiceManager.playbackState.collect { ttsState ->
                if (blockTTSUpdates.get() || blockTTSSync.get()) {
                    Log.d(TAG, "TTS state update blocked")
                    return@collect
                }

                _uiState.update {
                    it.copy(
                        isTTSActive = ttsState.isActive,
                        ttsStatus = when {
                            ttsState.isPlaying -> TTSStatus.PLAYING
                            ttsState.isPaused -> TTSStatus.PAUSED
                            else -> TTSStatus.STOPPED
                        },
                        currentTTSChapterIndex = if (ttsState.isActive) ttsState.chapterIndex else it.currentTTSChapterIndex
                    )
                }
            }
        }

        viewModelScope.launch {
            TTSServiceManager.segmentChanged.collect { sentenceIndex ->
                if (blockTTSUpdates.get() || blockTTSSync.get()) {
                    Log.d(TAG, "TTS segment change blocked")
                    return@collect
                }
                handleTTSSegmentChange(sentenceIndex)
            }
        }

        viewModelScope.launch {
            TTSServiceManager.playbackComplete.collect {
                if (!blockTTSUpdates.get() && !blockTTSSync.get()) {
                    handleTTSPlaybackComplete()
                }
            }
        }

        viewModelScope.launch {
            TTSServiceManager.chapterChanged.collect { event ->
                if (blockTTSUpdates.get() || blockTTSSync.get()) {
                    Log.d(TAG, "TTS chapter change blocked during transition")
                    return@collect
                }
                handleTTSChapterChange(event)
            }
        }
    }

    private fun observeVolumeKeys() {
        viewModelScope.launch {
            VolumeKeyManager.volumeKeyEvents.collect { event ->
                handleVolumeKeyEvent(event)
            }
        }
    }

    private fun handleVolumeKeyEvent(event: VolumeKeyEvent) {
        val settings = _uiState.value.settings
        if (!settings.volumeKeyNavigation) return

        val goForward = when (settings.volumeKeyDirection) {
            VolumeKeyDirection.NATURAL -> event == VolumeKeyEvent.VOLUME_DOWN
            VolumeKeyDirection.INVERTED -> event == VolumeKeyEvent.VOLUME_UP
        }

        _volumeScrollAction.tryEmit(goForward)
    }

    // =========================================================================
    // TTS SCROLL LOCK
    // =========================================================================

    fun setTTSScrollLock(locked: Boolean) {
        _ttsScrollLocked.value = locked
        val currentSettings = _uiState.value.settings
        preferencesManager.updateReaderSettings(currentSettings.copy(lockScrollDuringTTS = locked))
    }

    fun toggleTTSScrollLock() {
        setTTSScrollLock(!_ttsScrollLocked.value)
    }

    fun isScrollBounded(): Boolean {
        val state = _uiState.value
        return state.isTTSActive && _ttsScrollLocked.value
    }

    fun getCurrentHighlightedDisplayIndex(): Int {
        return _uiState.value.currentSentenceHighlight?.segmentDisplayIndex ?: -1
    }

    fun clearTTSEnsureVisible() {
        _ttsShouldEnsureVisible.value = null
    }

    fun scrollToCurrentTTSPosition() {
        val displayIndex = _uiState.value.currentSentenceHighlight?.segmentDisplayIndex
        if (displayIndex != null && displayIndex >= 0) {
            _ttsShouldEnsureVisible.value = displayIndex
        }
    }

    // =========================================================================
    // TTS SENTENCE BOUNDS
    // =========================================================================

    fun updateSentenceBounds(displayIndex: Int, topOffset: Float, bottomOffset: Float) {
        val currentHighlightIndex = _uiState.value.currentSentenceHighlight?.segmentDisplayIndex

        if (displayIndex == currentHighlightIndex) {
            val bounds = SentenceBoundsInSegment(
                topOffset = topOffset,
                bottomOffset = bottomOffset,
                height = bottomOffset - topOffset
            )
            currentSentenceBounds = bounds
            _sentenceBounds.value = bounds

            _uiState.update { state ->
                state.currentSentenceHighlight?.let { highlight ->
                    state.copy(
                        currentSentenceHighlight = highlight.copy(boundsInSegment = bounds)
                    )
                } ?: state
            }
        }
    }

    // =========================================================================
    // VISIBILITY & TTS SYNC
    // =========================================================================

    fun onReaderBecameVisible() {
        isReaderVisible = true

        val ttsState = TTSServiceManager.playbackState.value
        val state = _uiState.value

        if (ttsState.isActive &&
            ttsWasActiveOnLeave &&
            ttsState.chapterIndex >= 0 &&
            ttsState.chapterIndex != state.currentChapterIndex &&
            !isTransitioning.get() &&
            !blockTTSSync.get()) {
            Log.d(TAG, "Reader visible - TTS moved from chapter ${state.currentChapterIndex} to ${ttsState.chapterIndex}")
            syncWithTTSService()
        }

        ttsWasActiveOnLeave = false
        ttsChapterOnLeave = -1
    }

    fun onReaderBecameInvisible() {
        isReaderVisible = false

        val ttsState = TTSServiceManager.playbackState.value
        ttsWasActiveOnLeave = ttsState.isActive
        ttsChapterOnLeave = ttsState.chapterIndex
    }

    fun syncWithTTSService() {
        if (blockTTSSync.get() || isTransitioning.get()) {
            Log.d(TAG, "TTS sync blocked")
            return
        }

        val ttsState = TTSServiceManager.playbackState.value
        if (!ttsState.isActive) return

        val currentState = _uiState.value

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

        if (ttsState.chapterIndex != currentState.currentChapterIndex &&
            ttsState.chapterIndex >= 0 &&
            ttsState.chapterUrl.isNotBlank()) {

            Log.d(TAG, "Syncing to TTS chapter ${ttsState.chapterIndex}")
            lastNavigationSource = NavigationSource.TTS_AUTO

            viewModelScope.launch {
                if (!currentState.loadedChapters.containsKey(ttsState.chapterIndex)) {
                    loadChapterContent(ttsState.chapterIndex, isInitialLoad = false)

                    var attempts = 0
                    while (!_uiState.value.loadedChapters.containsKey(ttsState.chapterIndex) && attempts < 50) {
                        delay(100)
                        attempts++
                    }

                    if (!_uiState.value.loadedChapters.containsKey(ttsState.chapterIndex)) {
                        Log.e(TAG, "Failed to load chapter ${ttsState.chapterIndex} for TTS sync")
                        return@launch
                    }
                }

                _uiState.update {
                    it.copy(
                        currentChapterIndex = ttsState.chapterIndex,
                        currentChapterUrl = ttsState.chapterUrl,
                        currentChapterName = ttsState.chapterName,
                        currentTTSChapterIndex = ttsState.chapterIndex,
                        previousChapter = it.allChapters.getOrNull(ttsState.chapterIndex - 1),
                        nextChapter = it.allChapters.getOrNull(ttsState.chapterIndex + 1)
                    )
                }

                rebuildTTSSentenceListSafe()

                // Restore TTS position
                if (currentTTSCoordinate.isValid) {
                    updateHighlightFromCoordinate(currentTTSCoordinate)
                }
            }
        } else if (currentTTSCoordinate.isValid) {
            // Same chapter, just update highlight
            updateHighlightFromCoordinate(currentTTSCoordinate)
        }
    }

    // =========================================================================
    // TTS CORE LOGIC - STABLE COORDINATE BASED
    // =========================================================================

    /**
     * Find the global sentence index for a stable coordinate.
     * Returns -1 if not found.
     */
    private fun findSentenceIndexByCoordinate(coord: StableTTSCoordinate): Int {
        if (!coord.isValid) return -1
        return ttsSentenceList.indexOfFirst { it.coordinate == coord }
    }

    /**
     * Find the display index for a stable coordinate.
     * Computed on-demand, always fresh.
     */
    private fun findDisplayIndexForCoordinate(coord: StableTTSCoordinate): Int {
        if (!coord.isValid) return -1
        val displayItems = _uiState.value.displayItems
        return displayItems.indexOfFirst { item ->
            item is ReaderDisplayItem.Segment &&
                    item.chapterIndex == coord.chapterIndex &&
                    item.segmentIndexInChapter == coord.segmentIndexInChapter
        }
    }

    /**
     * Get the stable coordinate for a global sentence index.
     */
    private fun getCoordinateForIndex(index: Int): StableTTSCoordinate {
        val info = ttsSentenceList.getOrNull(index) ?: return StableTTSCoordinate.INVALID
        return info.coordinate
    }

    /**
     * Handle segment change from TTS service.
     * Converts global index to stable coordinate and updates highlight.
     */
    private fun handleTTSSegmentChange(sentenceIndex: Int) {
        if (sentenceIndex < 0 || sentenceIndex >= ttsSentenceList.size) {
            Log.w(TAG, "Invalid sentence index from TTS: $sentenceIndex (list size: ${ttsSentenceList.size})")
            return
        }

        val sentenceInfo = ttsSentenceList[sentenceIndex]

        // Update stable coordinate
        currentTTSCoordinate = sentenceInfo.coordinate
        currentTTSSentenceText = sentenceInfo.text

        // Update highlight using the coordinate (computes fresh display index)
        updateHighlightFromCoordinate(currentTTSCoordinate)
    }

    /**
     * Update UI highlight from a stable coordinate.
     * This computes the display index fresh each time.
     */
    private fun updateHighlightFromCoordinate(coord: StableTTSCoordinate) {
        if (!coord.isValid) {
            clearTTSHighlightOnly()
            return
        }

        val state = _uiState.value

        // Check if this coordinate is for a loaded chapter
        if (!state.loadedChapters.containsKey(coord.chapterIndex)) {
            Log.d(TAG, "Chapter ${coord.chapterIndex} not loaded, deferring highlight")
            return
        }

        // Find display index fresh
        val displayIndex = findDisplayIndexForCoordinate(coord)
        if (displayIndex < 0) {
            Log.w(TAG, "Could not find display index for coordinate $coord")
            return
        }

        val displayItem = state.displayItems.getOrNull(displayIndex)
        if (displayItem !is ReaderDisplayItem.Segment) {
            Log.w(TAG, "Display item at $displayIndex is not a segment")
            return
        }

        val sentence = displayItem.segment.getSentence(coord.sentenceIndexInSegment)
        if (sentence == null) {
            Log.w(TAG, "Could not find sentence ${coord.sentenceIndexInSegment} in segment")
            return
        }

        // Determine scroll edge based on direction
        val previousIndex = state.currentGlobalSentenceIndex
        val globalIndex = findSentenceIndexByCoordinate(coord)
        val scrollEdge = when {
            globalIndex > previousIndex -> TTSScrollEdge.BOTTOM
            globalIndex < previousIndex -> TTSScrollEdge.TOP
            else -> state.lastTTSScrollEdge
        }

        val highlight = SentenceHighlight(
            segmentDisplayIndex = displayIndex,
            sentenceIndex = coord.sentenceIndexInSegment,
            sentence = sentence,
            boundsInSegment = currentSentenceBounds
        )

        val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(coord)

        _uiState.update {
            it.copy(
                currentSegmentIndex = displayIndex,
                currentSentenceHighlight = highlight,
                currentGlobalSentenceIndex = globalIndex,
                currentSentenceInChapter = currentInChapter,
                totalSentencesInChapter = totalInChapter,
                lastTTSScrollEdge = scrollEdge,
                ttsPosition = TTSPosition(
                    segmentIndex = displayIndex,
                    sentenceIndexInSegment = coord.sentenceIndexInSegment,
                    globalSentenceIndex = globalIndex,
                )
            )
        }

        _sentenceBounds.value = SentenceBoundsInSegment.INVALID
        _ttsShouldEnsureVisible.value = displayIndex
    }

    /**
     * Calculate chapter-local TTS progress from stable coordinate.
     */
    private fun calculateChapterTTSProgress(coord: StableTTSCoordinate): Pair<Int, Int> {
        if (!coord.isValid || ttsSentenceList.isEmpty()) {
            return Pair(0, 0)
        }

        val chapterSentences = ttsSentenceList.filter { it.chapterIndex == coord.chapterIndex }
        val totalInChapter = chapterSentences.size

        val positionInChapter = chapterSentences.indexOfFirst { it.coordinate == coord }

        return Pair((positionInChapter + 1).coerceAtLeast(1), totalInChapter)
    }

    /**
     * Thread-safe rebuild of TTS sentence list.
     * Preserves current position using stable coordinates.
     */
    private suspend fun rebuildTTSSentenceListSafe() {
        ttsRebuildMutex.withLock {
            rebuildTTSSentenceListInternal()
        }
    }

    /**
     * Internal rebuild - must be called under ttsRebuildMutex.
     */
    private fun rebuildTTSSentenceListInternal() {
        if (blockTTSUpdates.get() || blockTTSSync.get()) {
            Log.d(TAG, "TTS sentence list rebuild blocked")
            return
        }

        val state = _uiState.value

        // Save current position
        val savedCoordinate = currentTTSCoordinate
        val savedText = currentTTSSentenceText
        val wasTTSActive = state.isTTSActive && state.ttsStatus != TTSStatus.STOPPED

        // Build new list
        val sentences = mutableListOf<TTSSentenceInfo>()

        state.displayItems.forEach { item ->
            if (item is ReaderDisplayItem.Segment) {
                item.segment.sentences.forEachIndexed { sentenceIndex, sentence ->
                    sentences.add(
                        TTSSentenceInfo(
                            text = sentence.text,
                            chapterIndex = item.chapterIndex,
                            segmentIndexInChapter = item.segmentIndexInChapter,
                            sentenceIndexInSegment = sentenceIndex,
                            pauseAfterMs = sentence.pauseAfterMs
                        )
                    )
                }
            }
        }

        ttsSentenceList = sentences

        Log.d(TAG, "Rebuilt TTS sentence list: ${sentences.size} sentences")

        // Update total count
        _uiState.update { it.copy(totalTTSSentences = sentences.size) }

        // Restore position if TTS was active
        if (wasTTSActive && savedCoordinate.isValid) {
            // Try to find by coordinate first
            var restoredIndex = findSentenceIndexByCoordinate(savedCoordinate)

            // Fallback: find by text match in same chapter
            if (restoredIndex < 0 && savedText.isNotBlank()) {
                restoredIndex = sentences.indexOfFirst { info ->
                    info.chapterIndex == savedCoordinate.chapterIndex &&
                            info.text == savedText
                }
            }

            if (restoredIndex >= 0) {
                Log.d(TAG, "Restored TTS position to index $restoredIndex")

                // Update coordinate from found position
                currentTTSCoordinate = sentences[restoredIndex].coordinate
                currentTTSSentenceText = sentences[restoredIndex].text

                // Update highlight
                updateHighlightFromCoordinate(currentTTSCoordinate)

                // Update TTS service
                if (TTSServiceManager.isActive()) {
                    val ttsContent = buildTTSContent(state, sentences)
                    TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = false)
                    TTSServiceManager.seekToSegment(restoredIndex)
                }
            } else {
                Log.w(TAG, "Could not restore TTS position for coordinate $savedCoordinate")

                // Try to find first sentence of the same chapter
                val chapterStart = sentences.indexOfFirst { it.chapterIndex == savedCoordinate.chapterIndex }
                if (chapterStart >= 0) {
                    currentTTSCoordinate = sentences[chapterStart].coordinate
                    currentTTSSentenceText = sentences[chapterStart].text
                    updateHighlightFromCoordinate(currentTTSCoordinate)

                    if (TTSServiceManager.isActive()) {
                        val ttsContent = buildTTSContent(state, sentences)
                        TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = false)
                        TTSServiceManager.seekToSegment(chapterStart)
                    }
                }
            }
        } else if (TTSServiceManager.isActive()) {
            // TTS active but no saved position - update content without seek
            val ttsContent = buildTTSContent(state, sentences)
            TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = true)
        }
    }

    /**
     * Non-suspend version for use in rebuildDisplayItems
     */
    private fun rebuildTTSSentenceList() {
        viewModelScope.launch {
            rebuildTTSSentenceListSafe()
        }
    }

    private fun buildTTSContent(state: ReaderUiState, sentences: List<TTSSentenceInfo>): TTSContent {
        return TTSContent(
            novelName = state.currentChapterName.split(" - ").firstOrNull() ?: "Novel",
            novelUrl = currentNovelUrl ?: "",
            chapterName = state.currentChapterName,
            chapterUrl = state.currentChapterUrl,
            segments = sentences.map { TTSSegment(it.text, it.pauseAfterMs) },
            chapterIndex = state.currentChapterIndex,
            totalChapters = state.allChapters.size,
            hasNextChapter = state.currentChapterIndex < state.allChapters.size - 1,
            hasPreviousChapter = state.currentChapterIndex > 0
        )
    }

    // =========================================================================
    // TTS PLAYBACK
    // =========================================================================

    fun startTTS() {
        if (blockTTSUpdates.get() || blockTTSSync.get() || isTransitioning.get()) {
            Log.d(TAG, "Cannot start TTS during transition")
            return
        }

        val context = appContext ?: return
        val state = _uiState.value

        if (state.displayItems.isEmpty()) {
            Log.d(TAG, "Cannot start TTS - no display items")
            return
        }

        viewModelScope.launch {
            ttsOperationMutex.withLock {
                // Ensure sentence list is current
                rebuildTTSSentenceListSafe()

                if (ttsSentenceList.isEmpty()) {
                    Log.d(TAG, "Cannot start TTS - no sentences")
                    return@withLock
                }

                val currentChapterIndex = state.currentChapterIndex
                val sentencesForCurrentChapter = ttsSentenceList.filter { it.chapterIndex == currentChapterIndex }
                if (sentencesForCurrentChapter.isEmpty()) {
                    Log.d(TAG, "Cannot start TTS - no sentences for current chapter $currentChapterIndex")
                    return@withLock
                }

                // Find the first visible sentence (not just segment)
                val startCoordinate = findFirstVisibleSentence(state)
                val startSentenceIndex = if (startCoordinate.isValid) {
                    findSentenceIndexByCoordinate(startCoordinate)
                } else {
                    ttsSentenceList.indexOfFirst { it.chapterIndex == currentChapterIndex }
                }.coerceAtLeast(0)

                // Set current coordinate
                if (startSentenceIndex >= 0 && startSentenceIndex < ttsSentenceList.size) {
                    currentTTSCoordinate = ttsSentenceList[startSentenceIndex].coordinate
                    currentTTSSentenceText = ttsSentenceList[startSentenceIndex].text
                }

                val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(currentTTSCoordinate)

                val novelUrl = currentNovelUrl ?: ""
                val providerName = currentProvider?.name ?: ""
                val novelDetails = offlineRepository.getNovelDetails(novelUrl)
                val coverUrl = novelDetails?.posterUrl
                val coverBitmap = loadCoverBitmap(coverUrl)

                val novelName = novelDetails?.name
                    ?: state.currentChapterName.split(" - ").firstOrNull()
                    ?: state.currentChapterName.ifBlank { "Novel" }

                val allChapters = state.allChapters

                val content = TTSContent(
                    novelName = novelName,
                    novelUrl = novelUrl,
                    chapterName = state.currentChapterName,
                    chapterUrl = state.currentChapterUrl,
                    segments = ttsSentenceList.map { TTSSegment(it.text, it.pauseAfterMs) },
                    coverUrl = coverUrl,
                    chapterIndex = currentChapterIndex,
                    totalChapters = allChapters.size,
                    hasNextChapter = currentChapterIndex < allChapters.size - 1,
                    hasPreviousChapter = currentChapterIndex > 0
                )

                updateHighlightFromCoordinate(currentTTSCoordinate)

                _uiState.update {
                    it.copy(
                        isTTSActive = true,
                        currentGlobalSentenceIndex = startSentenceIndex,
                        currentSentenceInChapter = currentInChapter,
                        totalSentencesInChapter = totalInChapter,
                        totalTTSSentences = ttsSentenceList.size,
                        currentTTSChapterIndex = currentChapterIndex
                    )
                }

                TTSServiceManager.setSpeechRate(state.ttsSettings.speed)
                // Ensure service uses user's auto-advance preference (persisted in reader settings)
                TTSServiceManager.setAutoAdvanceEnabled(state.settings.ttsAutoAdvanceChapter)

                TTSServiceManager.startPlayback(
                    context = context,
                    content = content,
                    startIndex = startSentenceIndex,
                    cover = coverBitmap,
                    novelUrl = novelUrl,
                    providerName = providerName,
                    chapters = allChapters,
                    chapterIndex = currentChapterIndex
                )
            }
        }
    }

    /**
     * Find the first visible sentence, considering scroll offset.
     * This is more precise than finding just the first visible segment.
     */
    private fun findFirstVisibleSentence(state: ReaderUiState): StableTTSCoordinate {
        val scrollIndex = state.currentScrollIndex
        val scrollOffset = state.currentScrollOffset
        val displayItems = state.displayItems

        // Find the first segment at or after scroll position
        for (i in scrollIndex until displayItems.size) {
            val item = displayItems[i]
            if (item is ReaderDisplayItem.Segment) {
                // If this is the item at scroll position, consider offset
                if (i == scrollIndex && scrollOffset > 0) {
                    // We're partially scrolled into this segment
                    // Estimate which sentence is visible based on offset
                    // For now, use simple heuristic: if offset > 100px, start from sentence 1
                    // A more sophisticated approach would use measured sentence heights
                    val sentenceIndex = estimateSentenceFromOffset(item.segment, scrollOffset)
                    return StableTTSCoordinate(
                        chapterIndex = item.chapterIndex,
                        segmentIndexInChapter = item.segmentIndexInChapter,
                        sentenceIndexInSegment = sentenceIndex
                    )
                } else {
                    // Start from first sentence of this segment
                    return StableTTSCoordinate(
                        chapterIndex = item.chapterIndex,
                        segmentIndexInChapter = item.segmentIndexInChapter,
                        sentenceIndexInSegment = 0
                    )
                }
            }
        }

        // Fallback: search backwards
        for (i in scrollIndex downTo 0) {
            val item = displayItems[i]
            if (item is ReaderDisplayItem.Segment) {
                return StableTTSCoordinate(
                    chapterIndex = item.chapterIndex,
                    segmentIndexInChapter = item.segmentIndexInChapter,
                    sentenceIndexInSegment = 0
                )
            }
        }

        return StableTTSCoordinate.INVALID
    }

    /**
     * Estimate which sentence is visible based on pixel offset.
     * This is an approximation - proper implementation would need measured heights.
     */
    private fun estimateSentenceFromOffset(segment: ContentSegment, pixelOffset: Int): Int {
        if (segment.sentenceCount <= 1) return 0

        // Estimate average sentence height (rough approximation)
        // Assume ~60dp per sentence on average (will vary based on font size, line height)
        val estimatedSentenceHeight = 60 * 3 // ~180 pixels for 3 lines

        // Calculate which sentence the offset corresponds to
        val sentenceIndex = (pixelOffset / estimatedSentenceHeight).coerceIn(0, segment.sentenceCount - 1)

        return sentenceIndex
    }

    /**
     * Handle TTS playback completion.
     */
    private fun handleTTSPlaybackComplete() {
        val state = _uiState.value

        Log.d(TAG, "TTS playback complete - chapter ${state.currentTTSChapterIndex}")

        val isLastChapter = state.currentTTSChapterIndex >= state.allChapters.size - 1

        // Check if at end of sentences
        val hasNoMoreSentences = ttsSentenceList.isEmpty() ||
                !ttsSentenceList.any { it.chapterIndex > state.currentTTSChapterIndex }

        if (isLastChapter && hasNoMoreSentences) {
            Log.d(TAG, "Reached end of novel, fully stopping TTS")
            stopTTSInternal()

            _uiState.update {
                it.copy(
                    isTTSActive = false,
                    ttsStatus = TTSStatus.STOPPED,
                    currentSentenceHighlight = null,
                    currentSegmentIndex = -1,
                    ttsPosition = TTSPosition(),
                    currentTTSChapterIndex = -1
                )
            }

            currentTTSCoordinate = StableTTSCoordinate.INVALID
            currentTTSSentenceText = ""
        } else {
            Log.d(TAG, "Chapter ended, waiting for TTS service to advance")
            clearTTSHighlightOnly()
        }
    }

    /**
     * Handle chapter change events from TTS service.
     */
    private fun handleTTSChapterChange(event: TTSChapterChangeEvent) {
        val chapterIndex = event.chapterIndex

        Log.d(TAG, "TTS chapter changed to $chapterIndex")

        _uiState.update { it.copy(currentTTSChapterIndex = chapterIndex) }

        // Update coordinate to start of new chapter
        currentTTSCoordinate = StableTTSCoordinate(
            chapterIndex = chapterIndex,
            segmentIndexInChapter = 0,
            sentenceIndexInSegment = 0
        )

        if (!isReaderVisible) {
            Log.d(TAG, "Reader invisible - deferring TTS chapter change handling")
            return
        }

        lastNavigationSource = NavigationSource.TTS_AUTO

        val state = _uiState.value

        viewModelScope.launch {
            // Load chapter if needed
            if (!state.loadedChapters.containsKey(chapterIndex)) {
                Log.d(TAG, "Loading chapter $chapterIndex for TTS continuation")
                loadChapterContent(chapterIndex, isInitialLoad = false)

                var attempts = 0
                while (!_uiState.value.loadedChapters.containsKey(chapterIndex) && attempts < 50) {
                    delay(100)
                    attempts++
                }

                if (!_uiState.value.loadedChapters.containsKey(chapterIndex)) {
                    Log.e(TAG, "Failed to load chapter $chapterIndex for TTS - stopping")
                    stopTTSInternal()
                    return@launch
                }
            }

            // Update state
            _uiState.update {
                it.copy(
                    currentChapterIndex = chapterIndex,
                    currentChapterUrl = event.chapterUrl,
                    currentChapterName = event.chapterName,
                    previousChapter = it.allChapters.getOrNull(chapterIndex - 1),
                    nextChapter = it.allChapters.getOrNull(chapterIndex + 1)
                )
            }

            addToHistory(event.chapterUrl, event.chapterName)

            // Rebuild TTS list (this will pick up the new chapter)
            rebuildTTSSentenceListSafe()

            // Find first sentence of new chapter
            val firstSentenceIndex = ttsSentenceList.indexOfFirst { it.chapterIndex == chapterIndex }
            if (firstSentenceIndex >= 0) {
                currentTTSCoordinate = ttsSentenceList[firstSentenceIndex].coordinate
                currentTTSSentenceText = ttsSentenceList[firstSentenceIndex].text

                Log.d(TAG, "Starting TTS at sentence $firstSentenceIndex of chapter $chapterIndex")
                updateHighlightFromCoordinate(currentTTSCoordinate)

                val ttsContent = buildTTSContent(_uiState.value, ttsSentenceList)
                TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = false)
                TTSServiceManager.seekToSegment(firstSentenceIndex)
            } else {
                Log.e(TAG, "No sentences found for chapter $chapterIndex")
                stopTTSInternal()
            }
        }
    }

    private fun clearTTSHighlightOnly() {
        currentSentenceBounds = SentenceBoundsInSegment.INVALID
        _sentenceBounds.value = SentenceBoundsInSegment.INVALID

        _uiState.update {
            it.copy(
                currentSegmentIndex = -1,
                currentSentenceHighlight = null,
                ttsPosition = TTSPosition()
            )
        }
        _ttsShouldEnsureVisible.value = null
    }

    private fun clearTTSHighlight() {
        clearTTSHighlightOnly()
        currentTTSCoordinate = StableTTSCoordinate.INVALID
        currentTTSSentenceText = ""

        _uiState.update {
            it.copy(
                currentGlobalSentenceIndex = 0,
                currentSentenceInChapter = 0,
                totalSentencesInChapter = 0
            )
        }
    }

    private fun deactivateTTS() {
        clearTTSHighlight()
        ttsSentenceList = emptyList()

        _uiState.update {
            it.copy(
                isTTSActive = false,
                ttsStatus = TTSStatus.STOPPED,
                totalTTSSentences = 0,
                currentTTSChapterIndex = -1
            )
        }
    }

    private fun stopTTSInternal() {
        TTSServiceManager.stop()
        deactivateTTS()
    }

    fun stopTTS() {
        stopTTSInternal()
    }

    fun pauseTTS() {
        TTSServiceManager.pause()
    }

    fun resumeTTS() {
        TTSServiceManager.resume()
    }

    fun nextSegment() {
        viewModelScope.launch {
            ttsOperationMutex.withLock {
                val currentIndex = findSentenceIndexByCoordinate(currentTTSCoordinate)
                val nextIndex = currentIndex + 1

                if (nextIndex >= 0 && nextIndex < ttsSentenceList.size) {
                    currentTTSCoordinate = ttsSentenceList[nextIndex].coordinate
                    currentTTSSentenceText = ttsSentenceList[nextIndex].text

                    TTSServiceManager.seekToSegment(nextIndex)
                    updateHighlightFromCoordinate(currentTTSCoordinate)
                }
            }
        }
    }

    fun previousSegment() {
        viewModelScope.launch {
            ttsOperationMutex.withLock {
                val currentIndex = findSentenceIndexByCoordinate(currentTTSCoordinate)
                val prevIndex = (currentIndex - 1).coerceAtLeast(0)

                if (prevIndex >= 0 && prevIndex < ttsSentenceList.size) {
                    currentTTSCoordinate = ttsSentenceList[prevIndex].coordinate
                    currentTTSSentenceText = ttsSentenceList[prevIndex].text

                    TTSServiceManager.seekToSegment(prevIndex)
                    updateHighlightFromCoordinate(currentTTSCoordinate)
                }
            }
        }
    }

    // =========================================================================
    // TTS SETTINGS
    // =========================================================================

    private fun loadTTSSettings() {
        val settings = TTSSettingsState(
            speed = preferencesManager.getTtsSpeed(),
            pitch = preferencesManager.getTtsPitch(),
            volume = preferencesManager.getTtsVolume(),
            voiceId = preferencesManager.getTtsVoice(),
            autoScroll = preferencesManager.getTtsAutoScroll(),
            highlightSentence = preferencesManager.getTtsHighlightSentence(),
            pauseOnCalls = preferencesManager.getTtsPauseOnCalls(),
            useSystemVoice = preferencesManager.getTtsUseSystemVoice()
        )
        _uiState.update { it.copy(ttsSettings = settings) }
    }

    fun updateTTSUseSystemVoice(useSystem: Boolean) {
        preferencesManager.setTtsUseSystemVoice(useSystem)
        _uiState.update { it.copy(ttsSettings = it.ttsSettings.copy(useSystemVoice = useSystem)) }
        TTSServiceManager.setUseSystemVoice(useSystem)
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
        TTSServiceManager.setPitch(clampedPitch)
    }

    fun updateTTSVoice(voice: VoiceInfo) {
        VoiceManager.selectVoice(voice.id)
        TTSServiceManager.setVoice(voice.id)
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

    // =========================================================================
    // CHAPTER LOADING
    // =========================================================================

    private fun loadChapterInternal(
        chapterUrl: String,
        novelUrl: String,
        providerName: String,
        source: NavigationSource
    ) {
        currentNovelUrl = novelUrl
        currentProvider = novelRepository.getProvider(providerName)
        lastNavigationSource = source

        val provider = currentProvider ?: run {
            _uiState.update { it.copy(error = "Provider not found", isLoading = false) }
            return
        }

        chapterLoader.configure(provider)

        // Block EVERYTHING
        isTransitioning.set(true)
        blockInfiniteScroll.set(true)
        blockTTSUpdates.set(true)
        blockTTSSync.set(true)

        stopTTSInternal()
        savePositionJob?.cancel()

        val thisGeneration = loadGeneration.incrementAndGet()

        viewModelScope.launch {
            stateMutex.withLock {
                characterMaps.clear()
                loadingChapters.clear()
            }

            desiredScrollPosition = null
            currentTTSCoordinate = StableTTSCoordinate.INVALID
            currentTTSSentenceText = ""
            ttsSentenceList = emptyList()
            currentSentenceBounds = SentenceBoundsInSegment.INVALID
            _sentenceBounds.value = SentenceBoundsInSegment.INVALID
            _ttsShouldEnsureVisible.value = null

            val shouldRestorePosition = when (source) {
                NavigationSource.CONTINUE -> true
                NavigationSource.CHAPTER_LIST -> false
                NavigationSource.NAVIGATION -> false
                NavigationSource.TTS_AUTO -> false
            }

            if (!shouldRestorePosition) {
                Log.d(TAG, "Clearing saved position for $chapterUrl (source: $source)")
                preferencesManager.clearReadingPosition(chapterUrl)
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    isContentReady = false,
                    error = null,
                    currentChapterUrl = chapterUrl,
                    loadedChapters = emptyMap(),
                    displayItems = emptyList(),
                    targetScrollPosition = null,
                    hasRestoredScroll = !shouldRestorePosition,
                    pendingScrollReset = true,
                    currentScrollIndex = 0,
                    currentScrollOffset = 0,
                    currentSegmentIndex = -1,
                    isTTSActive = false,
                    ttsStatus = TTSStatus.STOPPED,
                    ttsPosition = TTSPosition(),
                    currentSentenceHighlight = null,
                    currentGlobalSentenceIndex = 0,
                    currentSentenceInChapter = 0,
                    totalSentencesInChapter = 0,
                    totalTTSSentences = 0,
                    currentTTSChapterIndex = -1
                )
            }

            val detailsResult = novelRepository.loadNovelDetails(provider, novelUrl)

            detailsResult.onSuccess { details ->
                if (thisGeneration != loadGeneration.get()) return@onSuccess

                val allChapters = details.chapters
                val chapterIndex = allChapters.indexOfFirst { it.url == chapterUrl }
                    .takeIf { it >= 0 } ?: 0

                requestedChapterIndex = chapterIndex

                _uiState.update {
                    it.copy(
                        allChapters = allChapters,
                        initialChapterIndex = chapterIndex,
                        currentChapterIndex = chapterIndex,
                        previousChapter = allChapters.getOrNull(chapterIndex - 1),
                        nextChapter = allChapters.getOrNull(chapterIndex + 1)
                    )
                }

                startInitialLoad(chapterIndex, thisGeneration, shouldRestorePosition)

            }.onFailure { error ->
                if (thisGeneration == loadGeneration.get()) {
                    isTransitioning.set(false)
                    blockInfiniteScroll.set(false)
                    blockTTSUpdates.set(false)
                    blockTTSSync.set(false)

                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to load novel details",
                            isLoading = false,
                            isContentReady = true
                        )
                    }
                }
            }
        }
    }

    fun loadChapter(
        chapterUrl: String,
        novelUrl: String,
        providerName: String
    ) {
        loadChapterInternal(chapterUrl, novelUrl, providerName, NavigationSource.CONTINUE)
    }

    private fun startInitialLoad(
        chapterIndex: Int,
        generation: Long,
        shouldRestorePosition: Boolean
    ) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            if (generation != loadGeneration.get()) return@launch

            _uiState.update { it.copy(isPreloading = true) }

            loadChapterContent(chapterIndex, isInitialLoad = true)

            if (generation != loadGeneration.get()) return@launch

            val state = _uiState.value
            val chapter = state.allChapters.getOrNull(chapterIndex)

            if (chapter != null) {
                val targetPosition = if (shouldRestorePosition) {
                    val savedPosition = loadSavedStablePosition(chapter.url, chapterIndex)
                    desiredScrollPosition = savedPosition ?: StableScrollPosition.chapterStart(chapterIndex)
                    resolveStablePosition(desiredScrollPosition!!)
                } else {
                    desiredScrollPosition = StableScrollPosition.chapterStart(chapterIndex)
                    PositionResolution.Found(0, 0, 1.0f)
                }

                _uiState.update {
                    it.copy(
                        targetScrollPosition = when (targetPosition) {
                            is PositionResolution.Found -> TargetScrollPosition(
                                displayIndex = targetPosition.displayIndex,
                                offsetPixels = targetPosition.pixelOffset
                            )
                            else -> TargetScrollPosition(0, 0)
                        }
                    )
                }
            }

            if (state.infiniteScrollEnabled) {
                val chaptersToPreload = getChaptersToPreload(chapterIndex)
                for (preloadIndex in chaptersToPreload) {
                    if (generation != loadGeneration.get()) return@launch
                    loadChapterContent(preloadIndex, isInitialLoad = false)
                }
            }

            chapter?.let {
                addToHistory(it.url, it.name)
                startReadingTimeTracking()
            }

            _uiState.update { it.copy(isPreloading = false) }

            Log.d(TAG, "Initial load complete, unblocking all tracking")
            isTransitioning.set(false)
            blockInfiniteScroll.set(false)
            blockTTSUpdates.set(false)
            blockTTSSync.set(false)
        }
    }

    private suspend fun loadChapterContent(
        chapterIndex: Int,
        isInitialLoad: Boolean = false
    ) {
        val state = _uiState.value
        val allChapters = state.allChapters

        if (chapterIndex < 0 || chapterIndex >= allChapters.size) return

        val shouldLoad = loadingChaptersMutex.withLock {
            if (state.loadedChapters.containsKey(chapterIndex)) return
            if (loadingChapters.contains(chapterIndex)) return
            loadingChapters.add(chapterIndex)
            true
        }

        if (!shouldLoad) return

        val chapter = allChapters[chapterIndex]
        val isBeforeCurrentChapter = chapterIndex < state.currentChapterIndex
        val itemCountBefore = state.displayItems.size

        // Save TTS state before rebuild
        val ttsWasActive = _uiState.value.isTTSActive && _uiState.value.ttsStatus != TTSStatus.STOPPED
        val savedTTSCoordinate = currentTTSCoordinate
        val savedTTSText = currentTTSSentenceText

        val loadingChapter = chapterLoader.createLoadingChapter(chapter, chapterIndex)
        _uiState.update {
            it.copy(loadedChapters = it.loadedChapters + (chapterIndex to loadingChapter))
        }
        rebuildDisplayItemsWithTTSPreserve(ttsWasActive, savedTTSCoordinate, savedTTSText)

        try {
            val result = chapterLoader.loadChapter(chapter, chapterIndex)

            when (result) {
                is ChapterLoadResult.Success -> {
                    stateMutex.withLock {
                        characterMaps[chapterIndex] = ChapterCharacterMap.build(
                            result.loadedChapter.segments,
                            chapterIndex
                        )
                    }

                    _uiState.update {
                        it.copy(
                            loadedChapters = it.loadedChapters + (chapterIndex to result.loadedChapter),
                            isLoading = if (chapterIndex == it.initialChapterIndex) false else it.isLoading,
                            isContentReady = if (chapterIndex == it.initialChapterIndex) true else it.isContentReady,
                            currentChapterName = if (chapterIndex == it.currentChapterIndex) chapter.name else it.currentChapterName,
                            isOfflineMode = result.loadedChapter.isFromCache && it.isOfflineMode
                        )
                    }

                    rebuildDisplayItemsWithTTSPreserve(ttsWasActive, savedTTSCoordinate, savedTTSText)

                    if (isBeforeCurrentChapter && !isInitialLoad && !isTransitioning.get()) {
                        val itemCountAfter = _uiState.value.displayItems.size
                        val itemsAdded = itemCountAfter - itemCountBefore
                        if (itemsAdded > 0) {
                            adjustScrollPositionBy(itemsAdded)
                        }
                    }
                }

                is ChapterLoadResult.Error -> {
                    val errorChapter = chapterLoader.createErrorChapter(chapter, chapterIndex, result.message)
                    _uiState.update {
                        it.copy(
                            loadedChapters = it.loadedChapters + (chapterIndex to errorChapter),
                            isLoading = if (chapterIndex == it.initialChapterIndex) false else it.isLoading,
                            isContentReady = if (chapterIndex == it.initialChapterIndex) true else it.isContentReady,
                            error = if (chapterIndex == it.initialChapterIndex) result.message else it.error
                        )
                    }
                    rebuildDisplayItemsWithTTSPreserve(ttsWasActive, savedTTSCoordinate, savedTTSText)
                }
            }
        } finally {
            loadingChaptersMutex.withLock {
                loadingChapters.remove(chapterIndex)
            }
        }
    }

    /**
     * Rebuild display items while preserving TTS state.
     */
    private fun rebuildDisplayItemsWithTTSPreserve(
        ttsWasActive: Boolean,
        savedCoordinate: StableTTSCoordinate,
        savedText: String
    ) {
        rebuildDisplayItemsInternal()

        // Restore TTS if it was active
        if (ttsWasActive && !blockTTSUpdates.get() && !blockTTSSync.get()) {
            viewModelScope.launch {
                // Rebuild sentence list with preserved position
                ttsRebuildMutex.withLock {
                    currentTTSCoordinate = savedCoordinate
                    currentTTSSentenceText = savedText
                    rebuildTTSSentenceListInternal()
                }
            }
        }
    }

    private fun rebuildDisplayItemsInternal() {
        val state = _uiState.value
        val allChapters = state.allChapters
        val loadedChapters = state.loadedChapters

        if (allChapters.isEmpty()) return

        val items = mutableListOf<ReaderDisplayItem>()
        var globalSegmentIndex = 0
        val chapterWordCounts = mutableMapOf<Int, Int>()

        val sortedIndices = loadedChapters.keys.sorted()

        for (chapterIndex in sortedIndices) {
            val loadedChapter = loadedChapters[chapterIndex] ?: continue
            val chapter = loadedChapter.chapter

            var chapterWordCount = 0
            var segmentIndexInChapter = 0

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
                    loadedChapter.contentItems.forEachIndexed { orderInChapter, contentItem ->
                        when (contentItem) {
                            is ChapterContentItem.Text -> {
                                val segment = contentItem.segment
                                items.add(
                                    ReaderDisplayItem.Segment(
                                        chapterIndex = chapterIndex,
                                        chapterUrl = chapter.url,
                                        segment = segment,
                                        segmentIndexInChapter = segmentIndexInChapter,
                                        globalSegmentIndex = globalSegmentIndex,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                globalSegmentIndex++
                                segmentIndexInChapter++
                                chapterWordCount += segment.text.split("\\s+".toRegex()).size
                            }

                            is ChapterContentItem.Image -> {
                                items.add(
                                    ReaderDisplayItem.Image(
                                        chapterIndex = chapterIndex,
                                        chapterUrl = chapter.url,
                                        image = contentItem.image,
                                        imageIndexInChapter = orderInChapter,
                                        orderInChapter = orderInChapter
                                    )
                                )
                            }

                            is ChapterContentItem.HorizontalRule -> {
                                items.add(
                                    ReaderDisplayItem.HorizontalRule(
                                        chapterIndex = chapterIndex,
                                        rule = contentItem.rule,
                                        orderInChapter = orderInChapter
                                    )
                                )
                            }

                            is ChapterContentItem.SceneBreak -> {
                                items.add(
                                    ReaderDisplayItem.SceneBreak(
                                        chapterIndex = chapterIndex,
                                        sceneBreak = contentItem.sceneBreak,
                                        orderInChapter = orderInChapter
                                    )
                                )
                            }

                            is ChapterContentItem.AuthorNote -> {
                                items.add(
                                    ReaderDisplayItem.AuthorNote(
                                        chapterIndex = chapterIndex,
                                        authorNote = contentItem.authorNote,
                                        orderInChapter = orderInChapter
                                    )
                                )
                            }

                            is ChapterContentItem.Table -> {
                                items.add(
                                    ReaderDisplayItem.Table(
                                        chapterIndex = chapterIndex,
                                        table = contentItem.table,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                chapterWordCount += contentItem.table.plainText.split("\\s+".toRegex()).size
                            }

                            is ChapterContentItem.List -> {
                                items.add(
                                    ReaderDisplayItem.List(
                                        chapterIndex = chapterIndex,
                                        list = contentItem.list,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                chapterWordCount += contentItem.list.plainText.split("\\s+".toRegex()).size
                            }
                        }
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

            chapterWordCounts[chapterIndex] = chapterWordCount
        }

        val currentChapterWordCount = chapterWordCounts[state.currentChapterIndex] ?: 0

        _uiState.update {
            it.copy(
                displayItems = items,
                currentChapterWordCount = currentChapterWordCount
            )
        }
    }

    fun retryChapter(chapterIndex: Int) {
        viewModelScope.launch {
            val canRetry = loadingChaptersMutex.withLock {
                if (loadingChapters.contains(chapterIndex)) {
                    false
                } else {
                    loadingChapters.add(chapterIndex)
                    true
                }
            }

            if (!canRetry) return@launch

            try {
                stateMutex.withLock {
                    characterMaps.remove(chapterIndex)
                }
                _uiState.update { it.copy(loadedChapters = it.loadedChapters - chapterIndex) }

                loadingChaptersMutex.withLock {
                    loadingChapters.remove(chapterIndex)
                }

                loadChapterContent(chapterIndex)
            } catch (e: Exception) {
                loadingChaptersMutex.withLock {
                    loadingChapters.remove(chapterIndex)
                }
                throw e
            }
        }
    }

    // =========================================================================
    // POSITION TRACKING
    // =========================================================================

    private fun getChaptersToPreload(centerIndex: Int): List<Int> {
        val state = _uiState.value
        val totalChapters = state.allChapters.size
        val indices = mutableListOf<Int>()

        for (offset in 1..preloadAfter) {
            val idx = centerIndex + offset
            if (idx < totalChapters && !state.loadedChapters.containsKey(idx)) {
                indices.add(idx)
            }
        }

        for (offset in 1..preloadBefore) {
            val idx = centerIndex - offset
            if (idx >= 0 && !state.loadedChapters.containsKey(idx)) {
                indices.add(idx)
            }
        }

        return indices
    }

    private fun loadSavedStablePosition(chapterUrl: String, chapterIndex: Int): StableScrollPosition? {
        val saved = preferencesManager.getReadingPosition(chapterUrl) ?: return null
        return StableScrollPosition(
            chapterIndex = chapterIndex,
            characterOffset = 0,
            segmentIndex = saved.segmentIndex,
            pixelOffset = saved.offset
        )
    }

    private fun resolveStablePosition(position: StableScrollPosition): PositionResolution {
        val displayItems = _uiState.value.displayItems
        if (displayItems.isEmpty()) return PositionResolution.NotFound

        if (!_uiState.value.loadedChapters.containsKey(position.chapterIndex)) {
            return PositionResolution.ChapterNotLoaded(position.chapterIndex)
        }

        val charMap = characterMaps[position.chapterIndex]

        val targetSegmentIndex = if (position.characterOffset > 0 && charMap != null) {
            charMap.findSegmentByCharOffset(position.characterOffset)
        } else {
            position.segmentIndex
        }

        val displayIndex = displayItems.indexOfFirst { item ->
            when (item) {
                is ReaderDisplayItem.Segment ->
                    item.chapterIndex == position.chapterIndex &&
                            item.segmentIndexInChapter >= targetSegmentIndex

                is ReaderDisplayItem.ChapterHeader ->
                    item.chapterIndex == position.chapterIndex &&
                            targetSegmentIndex == 0 && position.pixelOffset == 0

                else -> false
            }
        }

        return if (displayIndex >= 0) {
            PositionResolution.Found(displayIndex, position.pixelOffset, 1.0f)
        } else {
            val headerIndex = displayItems.indexOfFirst { item ->
                item is ReaderDisplayItem.ChapterHeader &&
                        item.chapterIndex == position.chapterIndex
            }
            if (headerIndex >= 0) {
                PositionResolution.Found(headerIndex, 0, 0.5f)
            } else {
                PositionResolution.NotFound
            }
        }
    }

    private fun displayIndexToStablePosition(
        displayIndex: Int,
        pixelOffset: Int
    ): StableScrollPosition? {
        val displayItems = _uiState.value.displayItems
        val item = displayItems.getOrNull(displayIndex) ?: return null

        return when (item) {
            is ReaderDisplayItem.Segment -> {
                val charMap = characterMaps[item.chapterIndex]
                val charOffset = charMap?.getCharOffsetForSegment(item.segmentIndexInChapter) ?: 0

                StableScrollPosition(
                    chapterIndex = item.chapterIndex,
                    characterOffset = charOffset,
                    segmentIndex = item.segmentIndexInChapter,
                    pixelOffset = pixelOffset
                )
            }

            is ReaderDisplayItem.ChapterHeader -> {
                StableScrollPosition.chapterStart(item.chapterIndex)
                    .copy(pixelOffset = pixelOffset)
            }

            is ReaderDisplayItem.ChapterDivider -> {
                val chapter = _uiState.value.loadedChapters[item.chapterIndex]
                val totalChars = characterMaps[item.chapterIndex]?.totalCharacters ?: 0

                StableScrollPosition(
                    chapterIndex = item.chapterIndex,
                    characterOffset = totalChars,
                    segmentIndex = chapter?.segments?.size ?: 0,
                    pixelOffset = pixelOffset
                )
            }

            else -> null
        }
    }

    fun updateCurrentScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        if (isTransitioning.get()) {
            return
        }

        _uiState.update {
            it.copy(
                currentScrollIndex = firstVisibleItemIndex,
                currentScrollOffset = firstVisibleItemOffset
            )
        }

        val stablePosition = displayIndexToStablePosition(firstVisibleItemIndex, firstVisibleItemOffset)
        if (stablePosition != null) {
            desiredScrollPosition = stablePosition

            if (stablePosition.chapterIndex != _uiState.value.currentChapterIndex) {
                val chapter = _uiState.value.allChapters.getOrNull(stablePosition.chapterIndex)
                if (chapter != null) {
                    updateCurrentChapter(stablePosition.chapterIndex, chapter.url, chapter.name)
                }
            }

            // Only save if not TTS-driven scroll
            if (_ttsShouldEnsureVisible.value == null) {
                debouncedSavePosition(stablePosition)
            }
        }
    }

    private fun debouncedSavePosition(position: StableScrollPosition) {
        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            delay(positionSaveDebounceMs)
            saveStablePosition(position)
        }
    }

    private fun saveStablePosition(position: StableScrollPosition) {
        val chapter = _uiState.value.allChapters.getOrNull(position.chapterIndex) ?: return

        preferencesManager.saveReadingPosition(
            chapterUrl = chapter.url,
            segmentId = "seg-${position.segmentIndex}",
            segmentIndex = position.segmentIndex,
            progress = 0f,
            offset = position.pixelOffset,
            chapterIndex = position.chapterIndex
        )

        viewModelScope.launch {
            val novelUrl = currentNovelUrl ?: return@launch
            if (libraryRepository.isFavorite(novelUrl)) {
                libraryRepository.updateReadingPosition(
                    novelUrl = novelUrl,
                    chapterUrl = chapter.url,
                    chapterName = chapter.name,
                    scrollIndex = position.segmentIndex,
                    scrollOffset = position.pixelOffset
                )
            }
        }
    }

    fun saveCurrentPosition() {
        desiredScrollPosition?.let { saveStablePosition(it) }
    }

    fun savePositionOnExit() = saveCurrentPosition()

    fun confirmScrollReset() {
        _uiState.update {
            it.copy(
                pendingScrollReset = false,
                hasRestoredScroll = true,
                targetScrollPosition = null
            )
        }
    }

    fun markScrollRestored() {
        _uiState.update {
            it.copy(
                targetScrollPosition = null,
                hasRestoredScroll = true,
                pendingScrollReset = false
            )
        }
    }

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

    // =========================================================================
    // INFINITE SCROLL
    // =========================================================================

    fun onApproachingEnd(lastVisibleChapterIndex: Int) {
        if (isTransitioning.get() || blockInfiniteScroll.get()) {
            return
        }

        val state = _uiState.value
        if (!state.infiniteScrollEnabled) return

        val maxLoadedIndex = state.loadedChapters.keys.maxOrNull() ?: return
        if (lastVisibleChapterIndex < maxLoadedIndex - preloadAfter) return

        val indexToLoad = maxLoadedIndex + 1
        val totalChapters = state.allChapters.size

        if (indexToLoad < totalChapters && !state.loadedChapters.containsKey(indexToLoad)) {
            viewModelScope.launch {
                loadChapterContent(indexToLoad, isInitialLoad = false)
                unloadDistantChapters()
            }
        }
    }

    fun onApproachingBeginning(firstVisibleChapterIndex: Int) {
        if (isTransitioning.get() || blockInfiniteScroll.get()) {
            return
        }

        val state = _uiState.value
        if (!state.infiniteScrollEnabled) return

        val minLoadedIndex = state.loadedChapters.keys.minOrNull() ?: return
        if (firstVisibleChapterIndex > minLoadedIndex + preloadBefore) return

        val indexToLoad = minLoadedIndex - 1

        if (indexToLoad >= 0 && !state.loadedChapters.containsKey(indexToLoad)) {
            viewModelScope.launch {
                loadChapterContent(indexToLoad, isInitialLoad = false)
                unloadDistantChapters()
            }
        }
    }

    private suspend fun unloadDistantChapters() {
        val state = _uiState.value
        val center = state.currentChapterIndex
        val keepRange = (center - preloadBefore - 1)..(center + preloadAfter + 1)

        // CRITICAL: Also protect TTS chapter
        val protected = setOf(
            state.currentChapterIndex,
            state.currentTTSChapterIndex,
            currentTTSCoordinate.chapterIndex,  // Extra protection
            requestedChapterIndex
        ).filter { it >= 0 }

        val toUnload = stateMutex.withLock {
            state.loadedChapters.keys.filter { it !in keepRange && it !in protected }
        }

        if (toUnload.isNotEmpty()) {
            Log.d(TAG, "Unloading chapters: $toUnload (protected: $protected)")

            stateMutex.withLock {
                toUnload.forEach { idx ->
                    characterMaps.remove(idx)
                }
            }

            _uiState.update {
                val updated = it.loadedChapters.toMutableMap()
                toUnload.forEach { idx -> updated.remove(idx) }
                it.copy(loadedChapters = updated)
            }

            // Rebuild with TTS preservation
            val ttsActive = _uiState.value.isTTSActive
            rebuildDisplayItemsWithTTSPreserve(ttsActive, currentTTSCoordinate, currentTTSSentenceText)
        }
    }

    fun updateCurrentChapter(chapterIndex: Int, chapterUrl: String, chapterName: String) {
        if (isTransitioning.get() || blockInfiniteScroll.get()) {
            return
        }

        val state = _uiState.value
        if (state.currentChapterIndex != chapterIndex) {
            val loadedChapter = state.loadedChapters[chapterIndex]
            val wordCount = loadedChapter?.segments?.sumOf { segment ->
                segment.text.split("\\s+".toRegex()).size
            } ?: 0

            _uiState.update {
                it.copy(
                    currentChapterIndex = chapterIndex,
                    currentChapterUrl = chapterUrl,
                    currentChapterName = chapterName,
                    previousChapter = it.allChapters.getOrNull(chapterIndex - 1),
                    nextChapter = it.allChapters.getOrNull(chapterIndex + 1),
                    currentChapterWordCount = wordCount
                )
            }

            addToHistory(chapterUrl, chapterName)
        }
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    fun navigateToPrevious() {
        saveCurrentPosition()
        val previous = _uiState.value.previousChapter ?: return
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return
        loadChapterInternal(previous.url, novelUrl, provider.name, NavigationSource.NAVIGATION)
    }

    fun navigateToNext() {
        saveCurrentPosition()

        val novelUrl = currentNovelUrl
        if (novelUrl != null) {
            viewModelScope.launch {
                val novelDetails = offlineRepository.getNovelDetails(novelUrl)
                if (novelDetails != null) {
                    statsRepository.recordChapterRead(novelUrl, novelDetails.name)
                }
            }
        }

        val next = _uiState.value.nextChapter ?: return
        val provider = currentProvider ?: return
        loadChapterInternal(next.url, novelUrl ?: return, provider.name, NavigationSource.NAVIGATION)
    }

    fun navigateToChapter(chapterIndex: Int) {
        val state = _uiState.value
        val allChapters = state.allChapters

        if (chapterIndex < 0 || chapterIndex >= allChapters.size) return

        val chapter = allChapters[chapterIndex]
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return

        if (chapterIndex == state.currentChapterIndex) {
            saveCurrentPosition()
        }

        stopTTS()
        _uiState.update { it.copy(showChapterList = false) }

        loadChapterInternal(chapter.url, novelUrl, provider.name, NavigationSource.CHAPTER_LIST)
    }

    // =========================================================================
    // UI CONTROLS
    // =========================================================================

    fun toggleControls() {
        _uiState.update {
            it.copy(
                showControls = !it.showControls,
                showSettings = if (!it.showControls) false else it.showSettings,
                showTTSSettings = if (!it.showControls) false else it.showTTSSettings
            )
        }
    }

    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun toggleChapterList() {
        _uiState.update { it.copy(showChapterList = !it.showChapterList) }
    }

    fun hideChapterList() {
        _uiState.update { it.copy(showChapterList = false) }
    }

    fun showSettings() {
        _uiState.update { it.copy(showSettings = true, showControls = true) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun updateChapterProgress(progress: Float) {
        _uiState.update { it.copy(chapterProgress = progress) }
    }

    fun toggleBookmark() {
        val state = _uiState.value
        val chapterUrl = state.currentChapterUrl
        val novelUrl = currentNovelUrl ?: return

        viewModelScope.launch {
            val isCurrentlyBookmarked = state.isCurrentChapterBookmarked
            if (isCurrentlyBookmarked) {
                bookmarkRepository.removeBookmark(novelUrl, chapterUrl)
            } else {
                bookmarkRepository.addBookmark(
                    novelUrl = novelUrl,
                    chapterUrl = chapterUrl,
                    chapterName = state.currentChapterName,
                    position = state.currentScrollIndex,
                    timestamp = System.currentTimeMillis()
                )
            }

            _uiState.update { it.copy(isCurrentChapterBookmarked = !isCurrentlyBookmarked) }
        }
    }

    fun updateReaderSettings(settings: ReaderSettings) {
        preferencesManager.updateReaderSettings(settings)
    }

    // =========================================================================
    // READING TIME & HISTORY
    // =========================================================================

    private fun startReadingTimeTracking() {
        val novelUrl = currentNovelUrl ?: return

        viewModelScope.launch {
            val novelDetails = offlineRepository.getNovelDetails(novelUrl)
            val novelName = novelDetails?.name ?: "Unknown Novel"

            if (readingTimeTracker == null) {
                readingTimeTracker = ReadingTimeTracker(statsRepository, viewModelScope)
            }
            readingTimeTracker?.startTracking(novelUrl, novelName)
        }
    }

    fun onPauseReading() {
        readingTimeTracker?.pauseTracking()
    }

    fun onResumeReading() {
        readingTimeTracker?.resumeTracking()
    }

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

    // =========================================================================
    // VOLUME KEYS
    // =========================================================================

    fun onReaderEnter() {
        VolumeKeyManager.setReaderActive(true)
        VolumeKeyManager.setVolumeKeyNavigationEnabled(_uiState.value.settings.volumeKeyNavigation)
    }

    fun onReaderExit() {
        VolumeKeyManager.setReaderActive(false)
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    override fun onCleared() {
        super.onCleared()
        stopTTS()
        saveCurrentPosition()
        savePositionJob?.cancel()
        preloadJob?.cancel()
        readingTimeTracker?.stopTracking()
        onReaderExit()
    }
}