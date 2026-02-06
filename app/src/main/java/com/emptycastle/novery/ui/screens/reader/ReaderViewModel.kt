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
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.TextAlign
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
import com.emptycastle.novery.ui.screens.reader.logic.InfiniteScrollController
import com.emptycastle.novery.ui.screens.reader.logic.ProgressManager
import com.emptycastle.novery.ui.screens.reader.logic.ScrollAction
import com.emptycastle.novery.ui.screens.reader.model.ChapterContentItem
import com.emptycastle.novery.ui.screens.reader.model.ContentSegment
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.model.ResolutionMethod
import com.emptycastle.novery.ui.screens.reader.model.ScrollRestorationState
import com.emptycastle.novery.ui.screens.reader.model.SentenceHighlight
import com.emptycastle.novery.ui.screens.reader.model.TTSPosition
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

private const val TAG = "ReaderViewModel"

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
// INTERNAL TTS DATA
// =============================================================================

private data class TTSSentenceInfo(
    val text: String,
    val displayItemIndex: Int,
    val segmentIndexInChapter: Int,
    val sentenceIndexInSegment: Int,
    val chapterIndex: Int,
    val pauseAfterMs: Int
)

// =============================================================================
// VIEW MODEL
// =============================================================================

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

    // Logic managers
    private val progressManager = ProgressManager(preferencesManager)
    private val chapterLoader = ChapterLoader(novelRepository)
    private val infiniteScrollController = InfiniteScrollController()

    // =========================================================================
    // STATE
    // =========================================================================

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentProvider: MainProvider? = null
    private var currentNovelUrl: String? = null

    private val loadingMutex = Mutex()
    private var preloadJob: Job? = null

    private val _scrollRestorationState = MutableStateFlow(ScrollRestorationState())

    private var savePositionJob: Job? = null
    private val positionSaveDebounceMs = 500L

    private var isInitialLoadComplete = false

    // Flag to control scroll restoration behavior
    private var shouldRestoreScrollPosition = true

    // Load generation to prevent stale updates from affecting the UI
    private var currentLoadGeneration = 0L

    // Flag to completely block infinite scroll during initial load
    private var blockInfiniteScrollUntilStable = true

    // The chapter index that was explicitly requested by the user
    private var requestedChapterIndex = -1

    // TTS
    private val ttsEngine by lazy { TTSManager.getEngine() }
    private var appContext: Context? = null
    private var ttsSentenceList: List<TTSSentenceInfo> = emptyList()

    // Reading time tracking
    private var readingTimeTracker: ReadingTimeTracker? = null

    // Volume key navigation
    private val _volumeScrollAction = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val volumeScrollAction: SharedFlow<Boolean> = _volumeScrollAction.asSharedFlow()

    // =========================================================================
    // VISIBILITY TRACKING FOR TTS BACKGROUND SYNC
    // =========================================================================

    private var isReaderVisible = true

    /**
     * Call when reader screen becomes visible (onResume)
     */
    fun onReaderBecameVisible() {
        isReaderVisible = true

        val state = _uiState.value
        val ttsState = TTSServiceManager.playbackState.value

        // Only sync if TTS actually changed chapters while we were away
        if (ttsState.isActive &&
            ttsState.chapterIndex >= 0 &&
            ttsState.chapterIndex != state.currentChapterIndex &&
            ttsState.chapterUrl.isNotBlank()) {
            Log.d(TAG, "Reader visible - syncing to TTS chapter ${ttsState.chapterIndex}")
            syncWithTTSService()
        }
    }

    /**
     * Call when reader screen becomes invisible (onPause/onStop)
     */
    fun onReaderBecameInvisible() {
        isReaderVisible = false
    }

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
            }
        }

        viewModelScope.launch {
            preferencesManager.appSettings.collect { appSettings ->
                _uiState.update {
                    it.copy(infiniteScrollEnabled = appSettings.infiniteScroll)
                }
            }
        }
    }

    private fun observeTTSState() {
        viewModelScope.launch {
            TTSServiceManager.playbackState.collect { ttsState ->
                _uiState.update {
                    it.copy(
                        isTTSActive = ttsState.isActive,
                        ttsStatus = when {
                            ttsState.isPlaying -> TTSStatus.PLAYING
                            ttsState.isPaused -> TTSStatus.PAUSED
                            else -> TTSStatus.STOPPED
                        },
                        // Only update TTS chapter index, not main chapter index
                        currentTTSChapterIndex = if (ttsState.isActive) ttsState.chapterIndex else it.currentTTSChapterIndex
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

        viewModelScope.launch {
            TTSServiceManager.chapterChanged.collect { event ->
                handleTTSChapterChange(event)
            }
        }
    }

    /**
     * Handle chapter change events from TTS service.
     * This is called when TTS auto-advances to the next chapter while screen is off.
     */
    private fun handleTTSChapterChange(event: TTSChapterChangeEvent) {
        val chapterIndex = event.chapterIndex

        // Always track the TTS chapter position
        _uiState.update {
            it.copy(currentTTSChapterIndex = chapterIndex)
        }

        // If reader isn't visible, don't touch the main state
        // This prevents breaking infinite scroll and other reader state
        if (!isReaderVisible) {
            Log.d(TAG, "TTS chapter changed to $chapterIndex while reader invisible - deferring sync")
            return
        }

        // Reader is visible, safe to update main state
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

        // Load the chapter content for display if not already loaded
        if (!state.loadedChapters.containsKey(chapterIndex)) {
            viewModelScope.launch {
                loadChapterContent(chapterIndex, isInitialLoad = false)
            }
        }

        // Update history
        addToHistory(event.chapterUrl, event.chapterName)

        // Rebuild TTS sentence list for the new chapter
        rebuildTTSSentenceList()
    }

    /**
     * Sync ViewModel state with TTS service state when returning to the app.
     * Call this in onResume or when the reader screen becomes visible.
     */
    fun syncWithTTSService() {
        val ttsState = TTSServiceManager.playbackState.value

        if (!ttsState.isActive) return

        val currentState = _uiState.value

        // Update TTS playback status first
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

        // Check if TTS has moved to a different chapter
        if (ttsState.chapterIndex != currentState.currentChapterIndex &&
            ttsState.chapterIndex >= 0 &&
            ttsState.chapterUrl.isNotBlank()) {

            Log.d(TAG, "Syncing reader to TTS chapter ${ttsState.chapterIndex}")

            viewModelScope.launch {
                // Load the chapter content if not loaded
                if (!currentState.loadedChapters.containsKey(ttsState.chapterIndex)) {
                    loadChapterContent(ttsState.chapterIndex, isInitialLoad = false)
                }

                // Small delay for content to be ready
                delay(100)

                // Now update the main state
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

                // Rebuild TTS list for correct highlighting
                rebuildTTSSentenceList()

                // Scroll to current TTS position
                scrollToCurrentTTSPosition()
            }
        }
    }

    /**
     * Helper to scroll to current TTS sentence position
     */
    private fun scrollToCurrentTTSPosition() {
        val state = _uiState.value
        val globalIndex = state.currentGlobalSentenceIndex

        if (globalIndex >= 0 && globalIndex < ttsSentenceList.size) {
            val sentenceInfo = ttsSentenceList[globalIndex]
            _uiState.update {
                it.copy(
                    targetScrollPosition = TargetScrollPosition(
                        displayIndex = sentenceInfo.displayItemIndex,
                        offsetPixels = 0
                    )
                )
            }
        }
    }

    // =========================================================================
    // VOLUME KEY NAVIGATION
    // =========================================================================

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

    fun onReaderEnter() {
        VolumeKeyManager.setReaderActive(true)
        VolumeKeyManager.setVolumeKeyNavigationEnabled(_uiState.value.settings.volumeKeyNavigation)
    }

    fun onReaderExit() {
        VolumeKeyManager.setReaderActive(false)
    }

    // =========================================================================
    // READING TIME TRACKING
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

    // =========================================================================
    // CHAPTER LIST
    // =========================================================================

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

    private fun calculateChapterTTSProgress(globalSentenceIndex: Int): Pair<Int, Int> {
        if (ttsSentenceList.isEmpty() || globalSentenceIndex < 0) {
            return Pair(0, 0)
        }

        val currentSentenceInfo = ttsSentenceList.getOrNull(globalSentenceIndex)
            ?: return Pair(0, 0)

        val currentChapter = currentSentenceInfo.chapterIndex

        val chapterSentences = ttsSentenceList.filter { it.chapterIndex == currentChapter }
        val totalInChapter = chapterSentences.size

        val positionInChapter = chapterSentences.indexOfFirst {
            it.displayItemIndex == currentSentenceInfo.displayItemIndex &&
                    it.sentenceIndexInSegment == currentSentenceInfo.sentenceIndexInSegment
        }

        return Pair(
            (positionInChapter + 1).coerceAtLeast(1),
            totalInChapter
        )
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
        stopTTS()

        isInitialLoadComplete = false
        blockInfiniteScrollUntilStable = true
        _scrollRestorationState.value = ScrollRestorationState()

        viewModelScope.launch {
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
                    currentScrollIndex = 0,
                    currentScrollOffset = 0,
                    currentSegmentIndex = -1,
                    isTTSActive = false,
                    ttsPosition = TTSPosition(),
                    currentSentenceHighlight = null
                )
            }

            val detailsResult = novelRepository.loadNovelDetails(provider, novelUrl)

            detailsResult.onSuccess { details ->
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

                val requestedChapterUrl = allChapters.getOrNull(chapterIndex)?.url
                val requestedChapterName = allChapters.getOrNull(chapterIndex)?.name ?: ""

                val existingReadingPos = libraryRepository.getReadingPosition(novelUrl)

                if (existingReadingPos != null && requestedChapterUrl != null &&
                    existingReadingPos.chapterUrl != requestedChapterUrl
                ) {
                    preferencesManager.clearReadingPosition(existingReadingPos.chapterUrl)
                    preferencesManager.clearReadingPosition(requestedChapterUrl)

                    libraryRepository.updateReadingPosition(
                        novelUrl = novelUrl,
                        chapterUrl = requestedChapterUrl,
                        chapterName = requestedChapterName,
                        scrollIndex = 0,
                        scrollOffset = 0
                    )

                    _scrollRestorationState.value = ScrollRestorationState(
                        pendingPosition = null,
                        isWaitingForChapter = false,
                        restorationAttempts = 0,
                        lastAttemptTime = 0,
                        hasSuccessfullyRestored = true
                    )
                    _uiState.update { it.copy(hasRestoredScroll = true, targetScrollPosition = null) }
                }

                startInitialLoad(chapterIndex)

            }.onFailure { error ->
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

    private fun startInitialLoad(chapterIndex: Int) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            _uiState.update { it.copy(isPreloading = true) }

            loadChapterContent(chapterIndex, isInitialLoad = true)

            val state = _uiState.value

            if (state.infiniteScrollEnabled) {
                awaitScrollRestoration()

                val chaptersToPreload = infiniteScrollController.getChaptersToPreload(
                    currentChapterIndex = chapterIndex,
                    loadedChapterIndices = _uiState.value.loadedChapters.keys,
                    totalChapters = state.allChapters.size,
                    isEnabled = true
                ).filter { it > chapterIndex }

                for (preloadIndex in chaptersToPreload) {
                    loadChapterContent(preloadIndex, isInitialLoad = false)
                }
            }

            isInitialLoadComplete = true
            blockInfiniteScrollUntilStable = false
            _uiState.update { it.copy(isPreloading = false) }
        }
    }

    private suspend fun awaitScrollRestoration(maxWaitMs: Int = 3000) {
        var waited = 0
        while (!_uiState.value.hasRestoredScroll && waited < maxWaitMs) {
            delay(50)
            waited += 50
        }
        delay(100)
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

            val isBeforeCurrentChapter = chapterIndex < state.currentChapterIndex
            val itemCountBefore = state.displayItems.size

            val loadingChapter = chapterLoader.createLoadingChapter(chapter, chapterIndex)
            _uiState.update {
                it.copy(loadedChapters = it.loadedChapters + (chapterIndex to loadingChapter))
            }
            rebuildDisplayItems()

            when (val result = chapterLoader.loadChapter(chapter, chapterIndex)) {
                is ChapterLoadResult.Success -> {
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

                    val hasRestoredScroll = _scrollRestorationState.value.hasSuccessfullyRestored ||
                            _uiState.value.hasRestoredScroll

                    if (isBeforeCurrentChapter && !isInitialLoad && hasRestoredScroll) {
                        val itemCountAfter = _uiState.value.displayItems.size
                        val itemsAdded = itemCountAfter - itemCountBefore
                        if (itemsAdded > 0) {
                            adjustScrollPositionBy(itemsAdded)
                        }
                    }

                    onChapterLoaded(chapterIndex, isInitialLoad)

                    if (chapterIndex == _uiState.value.initialChapterIndex) {
                        addToHistory(chapter.url, chapter.name)
                        startReadingTimeTracking()
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

                    val hasRestoredScroll = _scrollRestorationState.value.hasSuccessfullyRestored ||
                            _uiState.value.hasRestoredScroll

                    if (isBeforeCurrentChapter && !isInitialLoad && hasRestoredScroll) {
                        val itemCountAfter = _uiState.value.displayItems.size
                        val itemsAdded = itemCountAfter - itemCountBefore
                        if (itemsAdded > 0) {
                            adjustScrollPositionBy(itemsAdded)
                        }
                    }
                }
            }
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

    private fun onChapterLoaded(chapterIndex: Int, isInitialLoad: Boolean) {
        val state = _uiState.value
        val restorationState = _scrollRestorationState.value

        if (restorationState.pendingPosition != null &&
            restorationState.pendingPosition.chapterIndex == chapterIndex
        ) {
            attemptScrollRestoration()
            return
        }

        if (isInitialLoad && !restorationState.hasSuccessfullyRestored && shouldRestoreScrollPosition) {
            val chapter = state.allChapters.getOrNull(chapterIndex) ?: return
            val savedPosition = progressManager.loadPosition(chapter.url, chapterIndex)

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
        } else if (isInitialLoad && !restorationState.hasSuccessfullyRestored) {
            _scrollRestorationState.update {
                it.copy(hasSuccessfullyRestored = true)
            }
            _uiState.update { it.copy(hasRestoredScroll = true) }
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

        val resolved = progressManager.resolvePosition(
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

        val chapterWordCounts = mutableMapOf<Int, Int>()

        val sortedIndices = loadedChapters.keys.sorted()

        for (chapterIndex in sortedIndices) {
            val loadedChapter = loadedChapters[chapterIndex] ?: continue
            val chapter = loadedChapter.chapter

            var chapterWordCount = 0
            var segmentIndexInChapter = 0
            var imageIndexInChapter = 0
            var ruleIndexInChapter = 0
            var breakIndexInChapter = 0
            var noteIndexInChapter = 0

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
                                        imageIndexInChapter = imageIndexInChapter,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                imageIndexInChapter++
                            }

                            is ChapterContentItem.HorizontalRule -> {
                                items.add(
                                    ReaderDisplayItem.HorizontalRule(
                                        chapterIndex = chapterIndex,
                                        rule = contentItem.rule,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                ruleIndexInChapter++
                            }

                            is ChapterContentItem.SceneBreak -> {
                                items.add(
                                    ReaderDisplayItem.SceneBreak(
                                        chapterIndex = chapterIndex,
                                        sceneBreak = contentItem.sceneBreak,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                breakIndexInChapter++
                            }

                            // NEW: Handle AuthorNote
                            is ChapterContentItem.AuthorNote -> {
                                items.add(
                                    ReaderDisplayItem.AuthorNote(
                                        chapterIndex = chapterIndex,
                                        authorNote = contentItem.authorNote,
                                        orderInChapter = orderInChapter
                                    )
                                )
                                noteIndexInChapter++
                                // Don't count author notes in word count for reading time
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

        rebuildTTSSentenceList()
    }

    private fun rebuildTTSSentenceList() {
        val state = _uiState.value
        val sentences = mutableListOf<TTSSentenceInfo>()

        val previousTTSPosition = state.ttsPosition
        val wasTTSActive = state.isTTSActive
        val previousSentenceText = state.currentSentenceHighlight?.sentence?.text

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

        val ttsContent = TTSContent(
            novelName = state.currentChapterName.split(" - ").firstOrNull() ?: "Novel",
            novelUrl = currentNovelUrl ?: "",
            chapterName = state.currentChapterName,
            chapterUrl = state.currentChapterUrl,
            segments = sentences.map { TTSSegment(it.text, it.pauseAfterMs) }
        )

        if (previousTTSPosition.segmentIndex >= 0 || previousTTSPosition.globalSentenceIndex > 0) {
            var mappedIndex = sentences.indexOfFirst { info ->
                info.displayItemIndex == previousTTSPosition.segmentIndex &&
                        info.sentenceIndexInSegment == previousTTSPosition.sentenceIndexInSegment
            }

            if (mappedIndex < 0 && !previousSentenceText.isNullOrBlank()) {
                mappedIndex = sentences.indexOfFirst { info ->
                    info.text == previousSentenceText
                }
            }

            if (mappedIndex < 0) {
                val ttsChapter = state.currentTTSChapterIndex
                if (ttsChapter >= 0) {
                    mappedIndex = sentences.indexOfFirst { info ->
                        info.chapterIndex == ttsChapter
                    }
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

                if (TTSServiceManager.isActive()) {
                    TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = true)
                    TTSServiceManager.seekToSegment(mappedIndex)
                }
            } else {
                if (wasTTSActive) {
                    if (sentences.isNotEmpty()) {
                        val fallbackIndex = previousTTSPosition.globalSentenceIndex
                            .coerceIn(0, sentences.size - 1)

                        _uiState.update {
                            val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(fallbackIndex)
                            it.copy(
                                currentGlobalSentenceIndex = fallbackIndex,
                                currentSentenceInChapter = currentInChapter,
                                totalSentencesInChapter = totalInChapter,
                                totalTTSSentences = sentences.size
                            )
                        }

                        if (TTSServiceManager.isActive()) {
                            TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = true)
                            TTSServiceManager.seekToSegment(fallbackIndex)
                        }
                    } else {
                        stopTTS()
                    }
                } else {
                    _uiState.update { it.copy(totalTTSSentences = sentences.size) }
                }
            }
        } else {
            _uiState.update { it.copy(totalTTSSentences = sentences.size) }
            if (TTSServiceManager.isActive()) {
                TTSServiceManager.updateContent(ttsContent, keepSegmentIndex = true)
            }
        }
    }

    // =========================================================================
    // INFINITE SCROLL
    // =========================================================================

    fun onApproachingEnd(lastVisibleChapterIndex: Int) {
        if (!isInitialLoadComplete || blockInfiniteScrollUntilStable) {
            Log.d(TAG, "onApproachingEnd blocked: isInitialLoadComplete=$isInitialLoadComplete, blocked=$blockInfiniteScrollUntilStable")
            return
        }

        val state = _uiState.value
        val action = infiniteScrollController.onApproachingEnd(
            lastVisibleChapterIndex = lastVisibleChapterIndex,
            loadedChapterIndices = state.loadedChapters.keys,
            totalChapters = state.allChapters.size,
            isEnabled = state.infiniteScrollEnabled
        )

        handleScrollAction(action)
    }

    fun onApproachingBeginning(firstVisibleChapterIndex: Int) {
        if (!isInitialLoadComplete || blockInfiniteScrollUntilStable) {
            Log.d(TAG, "onApproachingBeginning blocked: isInitialLoadComplete=$isInitialLoadComplete, blocked=$blockInfiniteScrollUntilStable")
            return
        }

        val state = _uiState.value
        val action = infiniteScrollController.onApproachingBeginning(
            firstVisibleChapterIndex = firstVisibleChapterIndex,
            loadedChapterIndices = state.loadedChapters.keys,
            isEnabled = state.infiniteScrollEnabled
        )

        handleScrollAction(action)
    }

    private fun handleScrollAction(action: ScrollAction) {
        when (action) {
            is ScrollAction.None -> { }

            is ScrollAction.LoadNext -> {
                viewModelScope.launch {
                    loadChapterContent(action.chapterIndex)
                    unloadChapters(action.chaptersToUnload)
                }
            }

            is ScrollAction.LoadPrevious -> {
                viewModelScope.launch {
                    loadChapterContent(action.chapterIndex)
                    unloadChapters(action.chaptersToUnload)
                }
            }
        }
    }

    private fun unloadChapters(chaptersToUnload: Set<Int>) {
        val state = _uiState.value
        if (chaptersToUnload.isEmpty()) return

        val protected = listOf(
            state.currentChapterIndex,
            state.currentTTSChapterIndex,
            requestedChapterIndex
        ).filter { it >= 0 }.toSet()

        val finalToUnload = chaptersToUnload - protected
        if (finalToUnload.isEmpty()) return

        _uiState.update {
            val updatedLoadedChapters = it.loadedChapters.toMutableMap()
            finalToUnload.forEach { index -> updatedLoadedChapters.remove(index) }
            it.copy(loadedChapters = updatedLoadedChapters)
        }

        rebuildDisplayItems()
    }

    fun updateCurrentChapter(chapterIndex: Int, chapterUrl: String, chapterName: String) {
        if (!isInitialLoadComplete || blockInfiniteScrollUntilStable) {
            Log.d(TAG, "updateCurrentChapter blocked during initial load: chapterIndex=$chapterIndex")
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
    // SCROLL POSITION TRACKING
    // =========================================================================

    fun updateCurrentScrollPosition(firstVisibleItemIndex: Int, firstVisibleItemOffset: Int) {
        _uiState.update {
            it.copy(
                currentScrollIndex = firstVisibleItemIndex,
                currentScrollOffset = firstVisibleItemOffset
            )
        }

        if (!isInitialLoadComplete || blockInfiniteScrollUntilStable) {
            return
        }

        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch {
            delay(positionSaveDebounceMs)

            val currentState = _uiState.value
            val position = progressManager.capturePosition(
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

    fun saveCurrentPosition() {
        val state = _uiState.value

        val position = progressManager.capturePosition(
            displayItems = state.displayItems,
            firstVisibleItemIndex = state.currentScrollIndex,
            firstVisibleItemOffset = state.currentScrollOffset,
            loadedChapters = state.loadedChapters
        ) ?: return

        if (position.chapterUrl.isBlank()) return

        progressManager.savePosition(position)

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

    // =========================================================================
    // HISTORY
    // =========================================================================

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
                    statsRepository.recordChapterRead(
                        novelUrl = novelUrl,
                        novelName = novelDetails.name
                    )
                }
            }
        }

        val next = _uiState.value.nextChapter ?: return
        val provider = currentProvider ?: return
        loadChapter(next.url, novelUrl ?: return, provider.name)
    }

    fun retryChapter(chapterIndex: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadedChapters = it.loadedChapters - chapterIndex)
            }
            loadChapterContent(chapterIndex)
        }
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

    fun showSettings() {
        _uiState.update { it.copy(showSettings = true, showControls = true) }
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

            _uiState.update {
                it.copy(isCurrentChapterBookmarked = !isCurrentlyBookmarked)
            }
        }
    }

    // =========================================================================
    // SETTINGS
    // =========================================================================

    fun updateSettings(settings: ReaderSettings) {
        preferencesManager.updateReaderSettings(settings)
    }

    fun updateReaderSettings(settings: ReaderSettings) {
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

    // =========================================================================
    // TTS PLAYBACK
    // =========================================================================

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

            val currentChapterIndex = state.currentChapterIndex
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

                val (currentInChapter, totalInChapter) = calculateChapterTTSProgress(sentenceIndex)

                _uiState.update {
                    it.copy(
                        currentSegmentIndex = sentenceInfo.displayItemIndex,
                        currentSentenceHighlight = highlight,
                        currentGlobalSentenceIndex = sentenceIndex,
                        currentSentenceInChapter = currentInChapter,
                        totalSentencesInChapter = totalInChapter,
                        ttsPosition = TTSPosition(
                            segmentIndex = sentenceInfo.displayItemIndex,
                            sentenceIndexInSegment = sentenceInfo.sentenceIndexInSegment,
                            globalSentenceIndex = sentenceIndex,
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

    // =========================================================================
    // CLEANUP
    // =========================================================================

    override fun onCleared() {
        super.onCleared()
        saveCurrentPosition()
        savePositionJob?.cancel()
        preloadJob?.cancel()
        readingTimeTracker?.stopTracking()
        onReaderExit()
    }
}