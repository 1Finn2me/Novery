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
import com.emptycastle.novery.ui.screens.reader.model.StableTTSPosition
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

private data class TTSSentenceInfo(
    val text: String,
    val displayItemIndex: Int,
    val segmentIndexInChapter: Int,
    val sentenceIndexInSegment: Int,
    val chapterIndex: Int,
    val pauseAfterMs: Int
)

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

    // Generation counter for load operations
    private val loadGeneration = AtomicLong(0L)

    // Mutex for state modifications (NOT held during IO)
    private val stateMutex = Mutex()

    // Track which chapters are currently loading (prevents duplicate loads)
    private val loadingChapters = mutableSetOf<Int>()
    private val loadingChaptersMutex = Mutex()

    // Stable position tracking
    private var desiredScrollPosition: StableScrollPosition? = null
    private var desiredTTSPosition: StableTTSPosition = StableTTSPosition.INVALID
    private val characterMaps = mutableMapOf<Int, ChapterCharacterMap>()

    // Preload configuration
    private var preloadBefore: Int = 1
    private var preloadAfter: Int = 2

    private var requestedChapterIndex = -1
    private var preloadJob: Job? = null
    private var savePositionJob: Job? = null
    private val positionSaveDebounceMs = 500L

    // TTS
    private val ttsEngine by lazy { TTSManager.getEngine() }
    private var appContext: Context? = null
    private var ttsSentenceList: List<TTSSentenceInfo> = emptyList()

    // TTS sentence bounds tracking
    private var currentSentenceBounds: SentenceBoundsInSegment = SentenceBoundsInSegment.INVALID

    private val _sentenceBounds = MutableStateFlow(SentenceBoundsInSegment.INVALID)
    val sentenceBounds: StateFlow<SentenceBoundsInSegment> = _sentenceBounds.asStateFlow()

    // Reading time tracking
    private var readingTimeTracker: ReadingTimeTracker? = null

    // Volume key navigation
    private val _volumeScrollAction = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val volumeScrollAction: SharedFlow<Boolean> = _volumeScrollAction.asSharedFlow()

    // TTS scroll lock - controls bounded scrolling during TTS
    private val _ttsScrollLocked = MutableStateFlow(true)
    val ttsScrollLocked: StateFlow<Boolean> = _ttsScrollLocked.asStateFlow()

    // TTS ensure visible - triggers auto-scroll to highlighted item
    private val _ttsShouldEnsureVisible = MutableStateFlow<Int?>(null)
    val ttsShouldEnsureVisible: StateFlow<Int?> = _ttsShouldEnsureVisible.asStateFlow()

    // Visibility tracking
    private var isReaderVisible = true

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
                // Sync TTS scroll lock setting
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
                if (blockTTSUpdates.get()) {
                    Log.d(TAG, "TTS state update blocked during transition")
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
                if (blockTTSUpdates.get()) {
                    Log.d(TAG, "TTS segment change blocked during transition")
                    return@collect
                }
                updateTTSHighlight(sentenceIndex)
            }
        }

        viewModelScope.launch {
            TTSServiceManager.playbackComplete.collect {
                if (!blockTTSUpdates.get()) {
                    clearTTSHighlight()
                }
            }
        }

        viewModelScope.launch {
            TTSServiceManager.chapterChanged.collect { event ->
                if (blockTTSUpdates.get()) {
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
    // TTS SCROLL LOCK (BOUNDED SCROLLING)
    // =========================================================================

    /**
     * Set the TTS scroll lock state. When enabled, scrolling is bounded to keep
     * the highlighted text visible on screen.
     */
    fun setTTSScrollLock(locked: Boolean) {
        _ttsScrollLocked.value = locked
        // Persist to settings
        val currentSettings = _uiState.value.settings
        preferencesManager.updateReaderSettings(currentSettings.copy(lockScrollDuringTTS = locked))
        Log.d(TAG, "TTS scroll lock set to: $locked")
    }

    /**
     * Toggle the TTS scroll lock state.
     */
    fun toggleTTSScrollLock() {
        setTTSScrollLock(!_ttsScrollLocked.value)
    }

    /**
     * Check if scroll should be bounded (not completely locked, but constrained).
     * Returns true when TTS is active and scroll lock is enabled.
     */
    fun isScrollBounded(): Boolean {
        val state = _uiState.value
        return state.isTTSActive && _ttsScrollLocked.value
    }

    /**
     * Get the current highlighted display index for scroll bounding.
     */
    fun getCurrentHighlightedDisplayIndex(): Int {
        return _uiState.value.currentSentenceHighlight?.segmentDisplayIndex ?: -1
    }

    /**
     * Clear the ensure visible trigger after handling.
     */
    fun clearTTSEnsureVisible() {
        _ttsShouldEnsureVisible.value = null
    }

    /**
     * Scroll to current TTS position by triggering ensure visible.
     */
    fun scrollToCurrentTTSPosition() {
        val displayIndex = _uiState.value.currentSentenceHighlight?.segmentDisplayIndex
        if (displayIndex != null && displayIndex >= 0) {
            _ttsShouldEnsureVisible.value = displayIndex
            Log.d(TAG, "Triggered ensure visible for display index: $displayIndex")
        }
    }

    // =========================================================================
    // TTS SENTENCE BOUNDS
    // =========================================================================

    /**
     * Update sentence bounds from UI. Called when SegmentItem reports calculated
     * sentence line bounds from TextLayoutResult.
     */
    fun updateSentenceBounds(displayIndex: Int, topOffset: Float, bottomOffset: Float) {
        val currentHighlightIndex = _uiState.value.currentSentenceHighlight?.segmentDisplayIndex

        // Only update if this is for the currently highlighted segment
        if (displayIndex == currentHighlightIndex) {
            val bounds = SentenceBoundsInSegment(
                topOffset = topOffset,
                bottomOffset = bottomOffset,
                height = bottomOffset - topOffset
            )
            currentSentenceBounds = bounds
            _sentenceBounds.value = bounds

            // Update the highlight with bounds
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
        val state = _uiState.value
        val ttsState = TTSServiceManager.playbackState.value

        if (ttsState.isActive &&
            ttsState.chapterIndex >= 0 &&
            ttsState.chapterIndex != state.currentChapterIndex &&
            ttsState.chapterUrl.isNotBlank()) {
            Log.d(TAG, "Reader visible - syncing to TTS chapter ${ttsState.chapterIndex}")
            syncWithTTSService()
        }
    }

    fun onReaderBecameInvisible() {
        isReaderVisible = false
    }

    fun syncWithTTSService() {
        val ttsState = TTSServiceManager.playbackState.value
        if (!ttsState.isActive) return

        val currentState = _uiState.value

        _uiState.update {
            it.copy(
                isTTSActive = ttsState.isActive,
                currentGlobalSentenceIndex = ttsState.currentSegmentIndex,
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

            viewModelScope.launch {
                if (!currentState.loadedChapters.containsKey(ttsState.chapterIndex)) {
                    loadChapterContent(ttsState.chapterIndex, isInitialLoad = false)
                }

                delay(100)

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

                rebuildTTSSentenceList()
                scrollToCurrentTTSPosition()
            }
        }
    }

    private fun handleTTSChapterChange(event: TTSChapterChangeEvent) {
        val chapterIndex = event.chapterIndex

        _uiState.update { it.copy(currentTTSChapterIndex = chapterIndex) }

        if (!isReaderVisible) {
            Log.d(TAG, "TTS chapter changed to $chapterIndex while reader invisible - deferring sync")
            return
        }

        Log.d(TAG, "TTS chapter changed to $chapterIndex while reader visible - updating state")

        val state = _uiState.value

        _uiState.update {
            it.copy(
                currentChapterIndex = chapterIndex,
                currentChapterUrl = event.chapterUrl,
                currentChapterName = event.chapterName,
                previousChapter = it.allChapters.getOrNull(chapterIndex - 1),
                nextChapter = it.allChapters.getOrNull(chapterIndex + 1)
            )
        }

        if (!state.loadedChapters.containsKey(chapterIndex)) {
            viewModelScope.launch {
                loadChapterContent(chapterIndex, isInitialLoad = false)
            }
        }

        addToHistory(event.chapterUrl, event.chapterName)
        rebuildTTSSentenceList()
    }

    // =========================================================================
    // CHAPTER LOADING
    // =========================================================================

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

        chapterLoader.configure(provider)

        // Set flags BEFORE stopping TTS
        isTransitioning.set(true)
        blockInfiniteScroll.set(true)
        blockTTSUpdates.set(true)

        // NOW stop TTS (callbacks will be blocked)
        stopTTSInternal()

        // Increment generation
        val thisGeneration = loadGeneration.incrementAndGet()

        viewModelScope.launch {
            // Reset state atomically
            stateMutex.withLock {
                characterMaps.clear()
                loadingChapters.clear()
            }

            desiredScrollPosition = null
            desiredTTSPosition = StableTTSPosition.INVALID
            ttsSentenceList = emptyList()
            currentSentenceBounds = SentenceBoundsInSegment.INVALID
            _sentenceBounds.value = SentenceBoundsInSegment.INVALID
            _ttsShouldEnsureVisible.value = null

            _uiState.update {
                it.copy(
                    isLoading = true,
                    isContentReady = false,
                    error = null,
                    currentChapterUrl = chapterUrl,
                    loadedChapters = emptyMap(),
                    displayItems = emptyList(),
                    targetScrollPosition = null,
                    hasRestoredScroll = false,
                    pendingScrollReset = true,
                    currentScrollIndex = 0,
                    currentScrollOffset = 0,
                    // Fully reset TTS state
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
                if (thisGeneration != loadGeneration.get()) {
                    Log.d(TAG, "Stale load generation, ignoring")
                    return@onSuccess
                }

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

                startInitialLoad(chapterIndex, thisGeneration)

            }.onFailure { error ->
                if (thisGeneration == loadGeneration.get()) {
                    isTransitioning.set(false)
                    blockInfiniteScroll.set(false)
                    blockTTSUpdates.set(false)
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

    private fun startInitialLoad(chapterIndex: Int, generation: Long) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            if (generation != loadGeneration.get()) return@launch

            _uiState.update { it.copy(isPreloading = true) }

            loadChapterContent(chapterIndex, isInitialLoad = true)

            if (generation != loadGeneration.get()) return@launch

            val state = _uiState.value
            val chapter = state.allChapters.getOrNull(chapterIndex)

            if (chapter != null) {
                val savedPosition = loadSavedStablePosition(chapter.url, chapterIndex)
                desiredScrollPosition = savedPosition ?: StableScrollPosition.chapterStart(chapterIndex)

                val targetPosition = resolveStablePosition(desiredScrollPosition!!)

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

            // Unblock AFTER everything is ready
            Log.d(TAG, "Initial load complete, unblocking all tracking")
            isTransitioning.set(false)
            blockInfiniteScroll.set(false)
            blockTTSUpdates.set(false)
        }
    }

    private suspend fun loadChapterContent(
        chapterIndex: Int,
        isInitialLoad: Boolean = false
    ) {
        val state = _uiState.value
        val allChapters = state.allChapters

        if (chapterIndex < 0 || chapterIndex >= allChapters.size) return

        // PHASE 1: Check and claim (quick, under mutex)
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

        // Add loading placeholder
        val loadingChapter = chapterLoader.createLoadingChapter(chapter, chapterIndex)
        _uiState.update {
            it.copy(loadedChapters = it.loadedChapters + (chapterIndex to loadingChapter))
        }
        rebuildDisplayItems()

        try {
            // PHASE 2: IO (NO mutex held!)
            val result = chapterLoader.loadChapter(chapter, chapterIndex)

            // PHASE 3: Apply results (quick operations)
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

                    rebuildDisplayItems()

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
                    rebuildDisplayItems()
                }
            }
        } finally {
            // Always remove from loading set
            loadingChaptersMutex.withLock {
                loadingChapters.remove(chapterIndex)
            }
        }
    }

    fun retryChapter(chapterIndex: Int) {
        viewModelScope.launch {
            // Atomic check-and-claim
            val canRetry = loadingChaptersMutex.withLock {
                if (loadingChapters.contains(chapterIndex)) {
                    false
                } else {
                    loadingChapters.add(chapterIndex)
                    true
                }
            }

            if (!canRetry) {
                Log.d(TAG, "Chapter $chapterIndex already loading, skipping retry")
                return@launch
            }

            try {
                // Remove from state
                stateMutex.withLock {
                    characterMaps.remove(chapterIndex)
                }
                _uiState.update { it.copy(loadedChapters = it.loadedChapters - chapterIndex) }

                // Remove from loading set before calling loadChapterContent
                loadingChaptersMutex.withLock {
                    loadingChapters.remove(chapterIndex)
                }

                // Now load
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

        // FIX: When characterOffset is 0 (restored from prefs where it wasn't saved),
        // use segmentIndex directly instead of computing from characterOffset
        val targetSegmentIndex = if (position.characterOffset > 0 && charMap != null) {
            charMap.findSegmentByCharOffset(position.characterOffset)
        } else {
            // characterOffset is 0, so use the saved segmentIndex
            position.segmentIndex
        }

        val displayIndex = displayItems.indexOfFirst { item ->
            when (item) {
                is ReaderDisplayItem.Segment ->
                    item.chapterIndex == position.chapterIndex &&
                            item.segmentIndexInChapter >= targetSegmentIndex

                // FIX: Only match header for true chapter start (segment 0, no offset)
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

    /**
     * Update current scroll position. This is called from the UI as the user scrolls.
     * Note: When TTS is active with bounded scrolling, this is still called but the
     * scroll bounds are enforced at the UI layer via NestedScrollConnection.
     */
    fun updateCurrentScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        if (isTransitioning.get()) {
            Log.d(TAG, "Scroll tracking blocked - transitioning")
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

            debouncedSavePosition(stablePosition)
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
            Log.d(TAG, "onApproachingEnd blocked")
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
            Log.d(TAG, "onApproachingBeginning blocked")
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

        val protected = setOf(
            state.currentChapterIndex,
            state.currentTTSChapterIndex,
            requestedChapterIndex
        ).filter { it >= 0 }

        val toUnload = stateMutex.withLock {
            state.loadedChapters.keys.filter { it !in keepRange && it !in protected }
        }

        if (toUnload.isNotEmpty()) {
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

            rebuildDisplayItems()
        }
    }

    fun updateCurrentChapter(chapterIndex: Int, chapterUrl: String, chapterName: String) {
        if (isTransitioning.get() || blockInfiniteScroll.get()) {
            Log.d(TAG, "updateCurrentChapter blocked during transition")
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
    // DISPLAY ITEMS BUILDING
    // =========================================================================

    private fun rebuildDisplayItems() {
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
                                // Count words in table for reading time estimation
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
                                // Count words in list for reading time estimation
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
                totalTTSSentences = items
                    .filterIsInstance<ReaderDisplayItem.Segment>()
                    .sumOf { seg -> seg.segment.sentenceCount },
                currentChapterWordCount = currentChapterWordCount
            )
        }

        if (!blockTTSUpdates.get()) {
            rebuildTTSSentenceList()
        }
    }

    // =========================================================================
    // TTS
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

    private fun rebuildTTSSentenceList() {
        if (blockTTSUpdates.get()) {
            Log.d(TAG, "TTS sentence list rebuild blocked")
            return
        }

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
                            chapterIndex = item.chapterIndex,
                            pauseAfterMs = sentence.pauseAfterMs
                        )
                    )
                }
            }
        }

        ttsSentenceList = sentences

        val previousTTSPosition = state.ttsPosition
        val wasTTSActive = state.isTTSActive && state.ttsStatus != TTSStatus.STOPPED
        val ttsChapterMatchesCurrent = state.currentTTSChapterIndex == state.currentChapterIndex

        if (wasTTSActive && ttsChapterMatchesCurrent && previousTTSPosition.isValid) {
            val previousSentenceText = state.currentSentenceHighlight?.sentence?.text

            var mappedIndex = sentences.indexOfFirst { info ->
                info.chapterIndex == state.currentChapterIndex &&
                        info.displayItemIndex == previousTTSPosition.segmentIndex &&
                        info.sentenceIndexInSegment == previousTTSPosition.sentenceIndexInSegment
            }

            if (mappedIndex < 0 && !previousSentenceText.isNullOrBlank()) {
                mappedIndex = sentences.indexOfFirst { info ->
                    info.chapterIndex == state.currentChapterIndex &&
                            info.text == previousSentenceText
                }
            }

            if (mappedIndex >= 0) {
                val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(mappedIndex)
                _uiState.update {
                    it.copy(
                        currentGlobalSentenceIndex = mappedIndex,
                        currentSentenceInChapter = currentInChapter,
                        totalSentencesInChapter = totalInChapter,
                        totalTTSSentences = sentences.size
                    )
                }

                val ttsContent = buildTTSContent(state, sentences)
                if (TTSServiceManager.isActive()) {
                    TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = true)
                    TTSServiceManager.seekToSegment(mappedIndex)
                }
            } else {
                Log.d(TAG, "TTS position not found in current chapter, resetting")
                _uiState.update {
                    it.copy(
                        totalTTSSentences = sentences.size,
                        currentGlobalSentenceIndex = 0,
                        currentSentenceInChapter = 0,
                        ttsPosition = TTSPosition()
                    )
                }
            }
        } else {
            _uiState.update { it.copy(totalTTSSentences = sentences.size) }

            if (TTSServiceManager.isActive()) {
                val ttsContent = buildTTSContent(state, sentences)
                TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = false)
            }
        }
    }

    private fun buildTTSContent(state: ReaderUiState, sentences: List<TTSSentenceInfo>): TTSContent {
        return TTSContent(
            novelName = state.currentChapterName.split(" - ").firstOrNull() ?: "Novel",
            novelUrl = currentNovelUrl ?: "",
            chapterName = state.currentChapterName,
            chapterUrl = state.currentChapterUrl,
            segments = sentences.map { TTSSegment(it.text, it.pauseAfterMs) }
        )
    }

    private fun calculateChapterTTSProgress(globalSentenceIndex: Int): Pair<Int, Int> {
        if (ttsSentenceList.isEmpty() || globalSentenceIndex < 0) {
            return Pair(0, 0)
        }

        val currentSentenceInfo = ttsSentenceList.getOrNull(globalSentenceIndex) ?: return Pair(0, 0)
        val currentChapter = currentSentenceInfo.chapterIndex

        val chapterSentences = ttsSentenceList.filter { it.chapterIndex == currentChapter }
        val totalInChapter = chapterSentences.size

        val positionInChapter = chapterSentences.indexOfFirst {
            it.displayItemIndex == currentSentenceInfo.displayItemIndex &&
                    it.sentenceIndexInSegment == currentSentenceInfo.sentenceIndexInSegment
        }

        return Pair((positionInChapter + 1).coerceAtLeast(1), totalInChapter)
    }

    fun startTTS() {
        if (blockTTSUpdates.get() || isTransitioning.get()) {
            Log.d(TAG, "Cannot start TTS during transition")
            return
        }

        val context = appContext ?: return
        val state = _uiState.value

        if (state.displayItems.isEmpty()) {
            Log.d(TAG, "Cannot start TTS - no display items")
            return
        }

        val startingDisplayIndex = findFirstVisibleSegmentIndex()

        if (ttsSentenceList.isEmpty()) {
            rebuildTTSSentenceList()
        }
        if (ttsSentenceList.isEmpty()) {
            Log.d(TAG, "Cannot start TTS - no sentences")
            return
        }

        val currentChapterIndex = state.currentChapterIndex
        val sentencesForCurrentChapter = ttsSentenceList.filter { it.chapterIndex == currentChapterIndex }
        if (sentencesForCurrentChapter.isEmpty()) {
            Log.d(TAG, "Cannot start TTS - no sentences for current chapter $currentChapterIndex")
            return
        }

        val startSentenceIndex = if (startingDisplayIndex >= 0) {
            ttsSentenceList.indexOfFirst {
                it.displayItemIndex >= startingDisplayIndex &&
                        it.chapterIndex == currentChapterIndex
            }.takeIf { it >= 0 } ?: ttsSentenceList.indexOfFirst { it.chapterIndex == currentChapterIndex }
        } else {
            ttsSentenceList.indexOfFirst { it.chapterIndex == currentChapterIndex }
        }.coerceAtLeast(0)

        val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(startSentenceIndex)

        viewModelScope.launch {
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

            updateTTSHighlight(startSentenceIndex)

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

    private fun findFirstVisibleSegmentIndex(): Int {
        val state = _uiState.value
        val scrollIndex = state.currentScrollIndex
        val displayItems = state.displayItems

        for (i in scrollIndex until displayItems.size) {
            if (displayItems[i] is ReaderDisplayItem.Segment) return i
        }
        for (i in scrollIndex downTo 0) {
            if (displayItems[i] is ReaderDisplayItem.Segment) return i
        }
        return -1
    }

    private fun updateTTSHighlight(sentenceIndex: Int) {
        if (blockTTSUpdates.get()) {
            Log.d(TAG, "TTS highlight update blocked")
            return
        }

        if (sentenceIndex < 0 || sentenceIndex >= ttsSentenceList.size) {
            clearTTSHighlight()
            return
        }

        val sentenceInfo = ttsSentenceList[sentenceIndex]
        val state = _uiState.value

        if (sentenceInfo.chapterIndex != state.currentChapterIndex) {
            Log.d(TAG, "TTS highlight for wrong chapter, ignoring")
            return
        }

        val displayItems = state.displayItems
        val displayItem = displayItems.getOrNull(sentenceInfo.displayItemIndex)

        if (displayItem is ReaderDisplayItem.Segment) {
            val sentence = displayItem.segment.getSentence(sentenceInfo.sentenceIndexInSegment)

            if (sentence != null) {
                // Determine scroll edge based on direction of sentence change
                val previousIndex = state.currentGlobalSentenceIndex
                val scrollEdge = when {
                    sentenceIndex > previousIndex -> TTSScrollEdge.BOTTOM // Moving forward, might need to scroll down
                    sentenceIndex < previousIndex -> TTSScrollEdge.TOP    // Moving backward, might need to scroll up
                    else -> state.lastTTSScrollEdge
                }

                val highlight = SentenceHighlight(
                    segmentDisplayIndex = sentenceInfo.displayItemIndex,
                    sentenceIndex = sentenceInfo.sentenceIndexInSegment,
                    sentence = sentence,
                    boundsInSegment = currentSentenceBounds // Will be updated by UI
                )

                val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(sentenceIndex)

                _uiState.update {
                    it.copy(
                        currentSegmentIndex = sentenceInfo.displayItemIndex,
                        currentSentenceHighlight = highlight,
                        currentGlobalSentenceIndex = sentenceIndex,
                        currentSentenceInChapter = currentInChapter,
                        totalSentencesInChapter = totalInChapter,
                        lastTTSScrollEdge = scrollEdge,
                        ttsPosition = TTSPosition(
                            segmentIndex = sentenceInfo.displayItemIndex,
                            sentenceIndexInSegment = sentenceInfo.sentenceIndexInSegment,
                            globalSentenceIndex = sentenceIndex,
                        )
                    )
                }

                // Reset bounds (will be recalculated by UI)
                _sentenceBounds.value = SentenceBoundsInSegment.INVALID

                // Trigger ensure visible to auto-scroll to the highlighted item
                _ttsShouldEnsureVisible.value = sentenceInfo.displayItemIndex
            }
        }
    }

    private fun clearTTSHighlight() {
        currentSentenceBounds = SentenceBoundsInSegment.INVALID
        _sentenceBounds.value = SentenceBoundsInSegment.INVALID

        _uiState.update {
            it.copy(
                currentSegmentIndex = -1,
                currentSentenceHighlight = null,
                ttsPosition = TTSPosition(),
                currentGlobalSentenceIndex = 0,
                currentSentenceInChapter = 0,
                totalSentencesInChapter = 0
            )
        }
        _ttsShouldEnsureVisible.value = null
    }

    private fun deactivateTTS() {
        clearTTSHighlight()
        ttsSentenceList = emptyList()
        desiredTTSPosition = StableTTSPosition.INVALID

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

    // TTS Settings
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
    // NAVIGATION
    // =========================================================================

    fun navigateToPrevious() {
        saveCurrentPosition()
        val previous = _uiState.value.previousChapter ?: return
        val novelUrl = currentNovelUrl ?: return
        val provider = currentProvider ?: return
        loadChapter(previous.url, novelUrl, provider.name)
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
        loadChapter(next.url, novelUrl ?: return, provider.name)
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