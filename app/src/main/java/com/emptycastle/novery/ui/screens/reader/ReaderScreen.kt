package com.emptycastle.novery.ui.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.ProgressStyle
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ScrollMode
import com.emptycastle.novery.domain.model.TapAction
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.ui.components.ChapterListSheet
import com.emptycastle.novery.ui.components.ReaderBottomBar
import com.emptycastle.novery.ui.components.TTSPlayer
import com.emptycastle.novery.ui.components.TTSSettingsPanel
import com.emptycastle.novery.ui.screens.reader.components.KeepScreenOnEffect
import com.emptycastle.novery.ui.screens.reader.components.PagedReaderContainer
import com.emptycastle.novery.ui.screens.reader.components.ReaderContainer
import com.emptycastle.novery.ui.screens.reader.components.ReaderErrorState
import com.emptycastle.novery.ui.screens.reader.components.ReaderTopBar
import com.emptycastle.novery.ui.screens.reader.components.ScrollUtils
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.model.SentenceBoundsInSegment
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.screens.reader.theme.ReaderDefaults
import com.emptycastle.novery.util.ImmersiveModeEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// =============================================================================
// MAIN SCREEN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterUrl: String,
    novelUrl: String,
    providerName: String,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsScrollLocked by viewModel.ttsScrollLocked.collectAsState()
    val ensureVisibleIndex by viewModel.ttsShouldEnsureVisible.collectAsState()
    val sentenceBounds by viewModel.sentenceBounds.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val chapterListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Auto-hide controls timer
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    // Initialize context for TTS
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    // Register/unregister with volume key manager
    DisposableEffect(Unit) {
        viewModel.onReaderEnter()
        onDispose {
            viewModel.onReaderExit()
        }
    }

    // Get preferences
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    // Keep screen on based on settings
    KeepScreenOnEffect(enabled = uiState.settings.keepScreenOn || appSettings.keepScreenOn)

    // Apply brightness setting
    BrightnessEffect(brightness = uiState.settings.brightness)

    // Get theme colors
    val colors = remember(uiState.settings.theme) {
        ReaderColors.fromTheme(uiState.settings.theme)
    }

    // Immersive mode control
    val showSystemBars = uiState.showControls ||
            uiState.isTTSActive ||
            uiState.shouldShowLoadingOverlay ||
            (uiState.error != null && uiState.isContentReady) ||
            uiState.showTTSSettings ||
            uiState.showChapterList
    ImmersiveModeEffect(showSystemBars = showSystemBars)

    // Auto-hide controls
    LaunchedEffect(uiState.showControls, uiState.settings.autoHideControlsDelay) {
        autoHideJob?.cancel()

        if (uiState.showControls &&
            uiState.settings.autoHideControlsDelay > 0 &&
            !uiState.isTTSActive &&
            !uiState.showTTSSettings &&
            !uiState.showChapterList
        ) {
            autoHideJob = scope.launch {
                delay(uiState.settings.autoHideControlsDelay)
                viewModel.hideControls()
            }
        }
    }

    // Lifecycle handling for reading time tracking AND TTS visibility sync
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onPauseReading()
                    viewModel.onReaderBecameInvisible()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResumeReading()
                    viewModel.onReaderBecameVisible()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load chapter on first composition
    LaunchedEffect(chapterUrl, novelUrl, providerName) {
        viewModel.loadChapter(chapterUrl, novelUrl, providerName)
    }

    // Restore scroll position when content is loaded and ready
    LaunchedEffect(uiState.targetScrollPosition, uiState.isContentReady) {
        val targetPosition = uiState.targetScrollPosition ?: return@LaunchedEffect
        if (!uiState.isContentReady) return@LaunchedEffect

        delay(150)

        try {
            val targetIndex = targetPosition.displayIndex.coerceIn(
                0,
                maxOf(0, uiState.displayItems.size - 1)
            )
            listState.scrollToItem(
                index = targetIndex,
                scrollOffset = targetPosition.offsetPixels
            )
        } catch (e: Exception) {
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) { }
        }

        viewModel.markScrollRestored()
    }

    // Track scroll position changes - only when content is ready
    LaunchedEffect(listState, uiState.isContentReady) {
        if (!uiState.isContentReady) return@LaunchedEffect

        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (index, offset) ->
            viewModel.updateCurrentScrollPosition(index, offset)
        }
    }

    // Track current chapter based on visible items - only when content is ready
    LaunchedEffect(listState, uiState.isContentReady) {
        if (!uiState.isContentReady) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                val displayItems = uiState.displayItems
                if (displayItems.isEmpty()) return@collect

                val item = displayItems.getOrNull(firstVisibleIndex)
                val chapterIndex = when (item) {
                    is ReaderDisplayItem.ChapterHeader -> item.chapterIndex
                    is ReaderDisplayItem.Segment -> item.chapterIndex
                    is ReaderDisplayItem.Image -> item.chapterIndex
                    is ReaderDisplayItem.HorizontalRule -> item.chapterIndex
                    is ReaderDisplayItem.SceneBreak -> item.chapterIndex
                    is ReaderDisplayItem.AuthorNote -> item.chapterIndex
                    is ReaderDisplayItem.ChapterDivider -> item.chapterIndex
                    is ReaderDisplayItem.Table -> item.chapterIndex
                    is ReaderDisplayItem.List -> item.chapterIndex
                    is ReaderDisplayItem.LoadingIndicator -> item.chapterIndex
                    is ReaderDisplayItem.ErrorIndicator -> item.chapterIndex
                    null -> return@collect
                }

                val chapter = uiState.allChapters.getOrNull(chapterIndex)
                if (chapter != null) {
                    viewModel.updateCurrentChapter(chapterIndex, chapter.url, chapter.name)
                }
            }
    }

    // Infinite scroll handlers - only when content is ready
    LaunchedEffect(listState, uiState.isContentReady) {
        if (!uiState.isContentReady) return@LaunchedEffect

        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        }.collect { firstVisibleIndex ->
            if (firstVisibleIndex <= ReaderDefaults.PRELOAD_THRESHOLD_ITEMS) {
                val displayItems = uiState.displayItems
                val firstItem = displayItems.getOrNull(firstVisibleIndex)
                val chapterIndex = when (firstItem) {
                    is ReaderDisplayItem.ChapterHeader -> firstItem.chapterIndex
                    is ReaderDisplayItem.Segment -> firstItem.chapterIndex
                    is ReaderDisplayItem.Image -> firstItem.chapterIndex
                    is ReaderDisplayItem.HorizontalRule -> firstItem.chapterIndex
                    is ReaderDisplayItem.SceneBreak -> firstItem.chapterIndex
                    is ReaderDisplayItem.AuthorNote -> firstItem.chapterIndex
                    is ReaderDisplayItem.ChapterDivider -> firstItem.chapterIndex
                    is ReaderDisplayItem.Table -> firstItem.chapterIndex
                    is ReaderDisplayItem.List -> firstItem.chapterIndex
                    is ReaderDisplayItem.LoadingIndicator -> firstItem.chapterIndex
                    is ReaderDisplayItem.ErrorIndicator -> firstItem.chapterIndex
                    null -> return@collect
                }
                viewModel.onApproachingBeginning(chapterIndex)
            }
        }
    }

    LaunchedEffect(listState, uiState.isContentReady) {
        if (!uiState.isContentReady) return@LaunchedEffect

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Pair(lastVisibleIndex, totalItems)
        }.collect { (lastVisibleIndex, totalItems) ->
            if (totalItems > 0 && lastVisibleIndex >= totalItems - ReaderDefaults.PRELOAD_THRESHOLD_ITEMS) {
                val displayItems = uiState.displayItems
                val lastItem = displayItems.getOrNull(lastVisibleIndex)
                val chapterIndex = when (lastItem) {
                    is ReaderDisplayItem.Segment -> lastItem.chapterIndex
                    is ReaderDisplayItem.Image -> lastItem.chapterIndex
                    is ReaderDisplayItem.HorizontalRule -> lastItem.chapterIndex
                    is ReaderDisplayItem.SceneBreak -> lastItem.chapterIndex
                    is ReaderDisplayItem.AuthorNote -> lastItem.chapterIndex
                    is ReaderDisplayItem.ChapterDivider -> lastItem.chapterIndex
                    is ReaderDisplayItem.ChapterHeader -> lastItem.chapterIndex
                    is ReaderDisplayItem.Table -> lastItem.chapterIndex
                    is ReaderDisplayItem.List -> lastItem.chapterIndex
                    is ReaderDisplayItem.LoadingIndicator -> lastItem.chapterIndex
                    is ReaderDisplayItem.ErrorIndicator -> lastItem.chapterIndex
                    null -> return@collect
                }
                viewModel.onApproachingEnd(chapterIndex)
            }
        }
    }

    // Auto-scroll to current segment during TTS (when NOT using bounded scroll)
    // When bounded scroll is active, the ReaderContainer handles keeping the item visible
    LaunchedEffect(uiState.currentSegmentIndex, uiState.isTTSActive, uiState.ttsSettings.autoScroll, ttsScrollLocked) {
        if (uiState.isTTSActive &&
            uiState.currentSegmentIndex >= 0 &&
            uiState.ttsSettings.autoScroll &&
            !ttsScrollLocked  // Only auto-scroll when NOT locked (bounded scroll handles locked case)
        ) {
            try {
                if (uiState.settings.smoothScroll && !uiState.settings.reduceMotion) {
                    listState.animateScrollToItem(
                        index = uiState.currentSegmentIndex,
                        scrollOffset = ReaderDefaults.SCROLL_OFFSET_PX
                    )
                } else {
                    listState.scrollToItem(
                        index = uiState.currentSegmentIndex,
                        scrollOffset = ReaderDefaults.SCROLL_OFFSET_PX
                    )
                }
            } catch (_: Exception) { }
        }
    }

    // Volume key scroll handling
    LaunchedEffect(Unit) {
        viewModel.volumeScrollAction.collectLatest { goForward ->
            ScrollUtils.scrollByPage(
                scope = scope,
                listState = listState,
                forward = goForward,
                smoothScroll = uiState.settings.smoothScroll,
                reduceMotion = uiState.settings.reduceMotion,
                sensitivity = uiState.settings.scrollSensitivity,
                totalItems = uiState.displayItems.size
            )
        }
    }

    // Save position when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePositionOnExit()
        }
    }

    // Calculate CHAPTER-SPECIFIC reading progress - only when content is ready
    val chapterProgress by remember(uiState.currentChapterIndex, uiState.displayItems, uiState.isContentReady) {
        derivedStateOf {
            if (!uiState.isContentReady) 0f
            else calculateChapterProgress(
                listState = listState,
                displayItems = uiState.displayItems,
                currentChapterIndex = uiState.currentChapterIndex
            )
        }
    }

    // Update progress in ViewModel
    LaunchedEffect(chapterProgress, uiState.isContentReady) {
        if (uiState.isContentReady) {
            viewModel.updateChapterProgress(chapterProgress)
        }
    }

    // Calculate estimated reading time for CURRENT CHAPTER
    val estimatedTimeLeft by remember(chapterProgress, uiState.currentChapterWordCount, uiState.settings, uiState.isContentReady) {
        derivedStateOf {
            if (!uiState.isContentReady) ""
            else calculateEstimatedTimeLeft(
                progress = chapterProgress,
                totalWords = uiState.currentChapterWordCount,
                settings = uiState.settings
            )
        }
    }

    // Derive highlighted display index for bounded scrolling
    val highlightedDisplayIndex by remember {
        derivedStateOf {
            uiState.currentSentenceHighlight?.segmentDisplayIndex ?: -1
        }
    }

    // Determine if scroll should be bounded
    val isScrollBounded = uiState.isTTSActive && ttsScrollLocked

    // Scroll by page function for tap zones and volume keys
    val scrollByPage: (Boolean) -> Unit = remember(
        listState,
        uiState.settings.smoothScroll,
        uiState.settings.reduceMotion,
        uiState.settings.scrollSensitivity,
        uiState.displayItems.size
    ) {
        { forward: Boolean ->
            ScrollUtils.scrollByPage(
                scope = scope,
                listState = listState,
                forward = forward,
                smoothScroll = uiState.settings.smoothScroll,
                reduceMotion = uiState.settings.reduceMotion,
                sensitivity = uiState.settings.scrollSensitivity,
                totalItems = uiState.displayItems.size
            )
        }
    }

    // Chapter list sheet
    if (uiState.showChapterList) {
        ChapterListSheet(
            chapters = uiState.allChapters,
            currentChapterIndex = uiState.currentChapterIndex,
            readChapterUrls = uiState.readChapterUrls,
            onChapterSelected = { index, _ ->
                viewModel.navigateToChapter(index)
            },
            onDismiss = { viewModel.hideChapterList() },
            sheetState = chapterListSheetState
        )
    }

    ReaderScreenContent(
        uiState = uiState,
        colors = colors,
        listState = listState,
        chapterProgress = chapterProgress,
        estimatedTimeLeft = if (uiState.settings.showReadingTime) estimatedTimeLeft else null,
        isScrollBounded = isScrollBounded,
        highlightedDisplayIndex = highlightedDisplayIndex,
        ensureVisibleIndex = ensureVisibleIndex,
        ttsScrollLocked = ttsScrollLocked,
        currentSentenceBounds = sentenceBounds,
        onSentenceBoundsUpdated = viewModel::updateSentenceBounds,
        onEnsureVisibleHandled = { viewModel.clearTTSEnsureVisible() },
        onTapAction = { action ->
            when (action) {
                TapAction.TOGGLE_CONTROLS -> viewModel.toggleControls()
                TapAction.PREVIOUS_PAGE -> scrollByPage(false)
                TapAction.NEXT_PAGE -> scrollByPage(true)
                TapAction.BOOKMARK -> viewModel.toggleBookmark()
                TapAction.OPEN_SETTINGS -> viewModel.toggleControls()
                TapAction.OPEN_CHAPTERS -> viewModel.toggleChapterList()
                TapAction.START_TTS -> viewModel.startTTS()
                TapAction.SCROLL_UP -> scrollByPage(false)
                TapAction.SCROLL_DOWN -> scrollByPage(true)
                TapAction.TOGGLE_FULLSCREEN -> viewModel.toggleControls()
                TapAction.NONE -> { }
            }
        },
        onBack = onBack,
        onRetry = { viewModel.loadChapter(chapterUrl, novelUrl, providerName) },
        onRetryChapter = { chapterIndex -> viewModel.retryChapter(chapterIndex) },
        onToggleControls = viewModel::toggleControls,
        onToggleBookmark = viewModel::toggleBookmark,
        onToggleChapterList = viewModel::toggleChapterList,
        onToggleTTSSettings = viewModel::toggleTTSSettings,
        onHideTTSSettings = viewModel::hideTTSSettings,
        onSettingsChange = viewModel::updateReaderSettings,
        onNavigateToSettings = onNavigateToSettings,
        onStartTTS = viewModel::startTTS,
        onPauseTTS = viewModel::pauseTTS,
        onResumeTTS = viewModel::resumeTTS,
        onStopTTS = viewModel::stopTTS,
        onTTSNext = viewModel::nextSegment,
        onTTSPrevious = viewModel::previousSegment,
        onTTSSpeedChange = viewModel::updateTTSSpeed,
        onTTSPitchChange = viewModel::updateTTSPitch,
        onTTSVoiceSelected = viewModel::updateTTSVoice,
        onTTSAutoScrollChange = viewModel::updateTTSAutoScroll,
        onTTSHighlightChange = viewModel::updateTTSHighlightSentence,
        onTTSLockScrollChange = viewModel::setTTSScrollLock,
        onTTSUseSystemVoiceChange = viewModel::updateTTSUseSystemVoice,
        onPrevious = viewModel::navigateToPrevious,
        onNext = viewModel::navigateToNext,
        onConfirmScrollReset = viewModel::confirmScrollReset
    )
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Calculate progress within the current chapter only
 */
private fun calculateChapterProgress(
    listState: LazyListState,
    displayItems: List<ReaderDisplayItem>,
    currentChapterIndex: Int
): Float {
    if (displayItems.isEmpty()) return 0f

    var chapterFirstIndex = -1
    var chapterLastIndex = -1

    displayItems.forEachIndexed { index, item ->
        val itemChapterIndex = when (item) {
            is ReaderDisplayItem.ChapterHeader -> item.chapterIndex
            is ReaderDisplayItem.Segment -> item.chapterIndex
            is ReaderDisplayItem.Image -> item.chapterIndex
            is ReaderDisplayItem.HorizontalRule -> item.chapterIndex
            is ReaderDisplayItem.SceneBreak -> item.chapterIndex
            is ReaderDisplayItem.AuthorNote -> item.chapterIndex
            is ReaderDisplayItem.ChapterDivider -> item.chapterIndex
            is ReaderDisplayItem.Table -> item.chapterIndex
            is ReaderDisplayItem.List -> item.chapterIndex
            is ReaderDisplayItem.LoadingIndicator -> item.chapterIndex
            is ReaderDisplayItem.ErrorIndicator -> item.chapterIndex
        }

        if (itemChapterIndex == currentChapterIndex) {
            if (chapterFirstIndex == -1) {
                chapterFirstIndex = index
            }
            chapterLastIndex = index
        }
    }

    if (chapterFirstIndex == -1 || chapterLastIndex == -1) return 0f

    val chapterItemCount = chapterLastIndex - chapterFirstIndex + 1
    if (chapterItemCount <= 1) return 0f

    val currentIndex = listState.firstVisibleItemIndex
    val positionInChapter = (currentIndex - chapterFirstIndex).coerceIn(0, chapterItemCount - 1)

    return (positionInChapter.toFloat() / (chapterItemCount - 1)).coerceIn(0f, 1f)
}

/**
 * Calculate estimated time left to read the current chapter
 */
private fun calculateEstimatedTimeLeft(
    progress: Float,
    totalWords: Int,
    settings: ReaderSettings
): String {
    if (totalWords <= 0) return ""

    val baseWpm = 250

    val fontSizeModifier = when {
        settings.fontSize < 14 -> 1.1f
        settings.fontSize > 20 -> 0.9f
        else -> 1.0f
    }

    val lineHeightModifier = when {
        settings.lineHeight < 1.4f -> 1.05f
        settings.lineHeight > 2.0f -> 0.95f
        else -> 1.0f
    }

    val wpm = (baseWpm * fontSizeModifier * lineHeightModifier).toInt()

    val remainingProgress = (1f - progress).coerceIn(0f, 1f)
    val remainingWords = (totalWords * remainingProgress).toInt()
    val minutes = remainingWords / wpm

    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
        }
    }
}

// =============================================================================
// BRIGHTNESS EFFECT
// =============================================================================

@Composable
private fun BrightnessEffect(brightness: Float) {
    val context = LocalContext.current

    LaunchedEffect(brightness) {
        if (brightness != ReaderSettings.BRIGHTNESS_SYSTEM && brightness >= 0f) {
            try {
                val activity = context as? android.app.Activity
                activity?.window?.let { window ->
                    val layoutParams = window.attributes
                    layoutParams.screenBrightness = brightness
                    window.attributes = layoutParams
                }
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val activity = context as? android.app.Activity
                activity?.window?.let { window ->
                    val layoutParams = window.attributes
                    layoutParams.screenBrightness = -1f
                    window.attributes = layoutParams
                }
            } catch (_: Exception) { }
        }
    }
}

// =============================================================================
// SCREEN CONTENT
// =============================================================================

@Composable
private fun ReaderScreenContent(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    chapterProgress: Float,
    estimatedTimeLeft: String?,
    isScrollBounded: Boolean,
    highlightedDisplayIndex: Int,
    ensureVisibleIndex: Int?,
    ttsScrollLocked: Boolean,
    currentSentenceBounds: SentenceBoundsInSegment = SentenceBoundsInSegment.INVALID,
    onSentenceBoundsUpdated: (Int, Float, Float) -> Unit = { _, _, _ -> },
    onEnsureVisibleHandled: () -> Unit,
    onTapAction: (TapAction) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onToggleControls: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleChapterList: () -> Unit,
    onToggleTTSSettings: () -> Unit,
    onHideTTSSettings: () -> Unit,
    onSettingsChange: (ReaderSettings) -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartTTS: () -> Unit,
    onPauseTTS: () -> Unit,
    onResumeTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onTTSNext: () -> Unit,
    onTTSPrevious: () -> Unit,
    onTTSSpeedChange: (Float) -> Unit,
    onTTSPitchChange: (Float) -> Unit,
    onTTSVoiceSelected: (VoiceInfo) -> Unit,
    onTTSAutoScrollChange: (Boolean) -> Unit,
    onTTSHighlightChange: (Boolean) -> Unit,
    onTTSLockScrollChange: (Boolean) -> Unit,
    onTTSUseSystemVoiceChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onConfirmScrollReset: () -> Unit
) {
    val tapZones = uiState.settings.tapZones
    val isContinuousMode = uiState.settings.scrollMode == ScrollMode.CONTINUOUS

    // Track if we've completed scroll reset for this content
    var hasCompletedScrollReset by remember(uiState.currentChapterUrl) {
        mutableStateOf(false)
    }

    // Handle scroll reset when content is ready but scroll pending
    LaunchedEffect(
        uiState.isContentReady,
        uiState.pendingScrollReset,
        uiState.displayItems.size,
        uiState.targetScrollPosition
    ) {
        if (uiState.isContentReady &&
            uiState.pendingScrollReset &&
            uiState.displayItems.isNotEmpty() &&
            !hasCompletedScrollReset
        ) {
            val targetPosition = uiState.targetScrollPosition
            val targetIndex = (targetPosition?.displayIndex ?: 0)
                .coerceIn(0, uiState.displayItems.size - 1)
            val targetOffset = targetPosition?.offsetPixels ?: 0

            try {
                // Scroll immediately without animation
                listState.scrollToItem(index = targetIndex, scrollOffset = targetOffset)
            } catch (e: Exception) {
                try {
                    listState.scrollToItem(0)
                } catch (_: Exception) { }
            }

            hasCompletedScrollReset = true
            onConfirmScrollReset()
        }
    }

    // Reset the flag when chapter changes
    LaunchedEffect(uiState.currentChapterUrl) {
        hasCompletedScrollReset = false
    }

    // Determine if content should be visible
    // Content is rendered but invisible while scroll reset is pending
    val contentVisible = !uiState.shouldShowLoadingOverlay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Render content (potentially invisible) to allow scroll positioning
        if (uiState.displayItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (contentVisible) 1f else 0f)
            ) {
                if (isContinuousMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(tapZones, contentVisible) {
                                if (contentVisible) {
                                    detectTapGestures(
                                        onDoubleTap = { offset ->
                                            onTapAction(tapZones.doubleTapAction)
                                        },
                                        onTap = { offset ->
                                            val width = size.width.toFloat()
                                            val height = size.height.toFloat()

                                            val leftZoneWidth = width * tapZones.horizontalZoneRatio
                                            val rightZoneStart = width * (1 - tapZones.horizontalZoneRatio)
                                            val topZoneHeight = height * tapZones.verticalZoneRatio
                                            val bottomZoneStart = height * (1 - tapZones.verticalZoneRatio)

                                            val action = when {
                                                offset.y < topZoneHeight -> tapZones.topZoneAction
                                                offset.y > bottomZoneStart -> tapZones.bottomZoneAction
                                                offset.x < leftZoneWidth -> tapZones.leftZoneAction
                                                offset.x > rightZoneStart -> tapZones.rightZoneAction
                                                else -> tapZones.centerZoneAction
                                            }

                                            onTapAction(action)
                                        }
                                    )
                                }
                            }
                    ) {
                        ReaderContainer(
                            uiState = uiState,
                            colors = colors,
                            listState = listState,
                            isScrollBounded = isScrollBounded,
                            highlightedDisplayIndex = highlightedDisplayIndex,
                            ensureVisibleIndex = ensureVisibleIndex,
                            onEnsureVisibleHandled = onEnsureVisibleHandled,
                            onSentenceBoundsUpdated = { displayIndex, top, bottom ->
                                onSentenceBoundsUpdated(displayIndex, top, bottom)
                            },
                            currentSentenceBounds = currentSentenceBounds,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onBack = onBack,
                            onRetryChapter = onRetryChapter
                        )
                    }
                } else {
                    PagedReaderContainer(
                        uiState = uiState,
                        colors = colors,
                        initialPage = 0,
                        onPageChanged = { _, _ -> },
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onBack = onBack,
                        onRetryChapter = onRetryChapter,
                        onTapCenter = onToggleControls
                    )
                }

                // Controls overlay (only interactive when visible)
                if (contentVisible) {
                    ControlsOverlay(
                        uiState = uiState,
                        colors = colors,
                        chapterProgress = chapterProgress,
                        estimatedTimeLeft = estimatedTimeLeft,
                        onBack = onBack,
                        onToggleBookmark = onToggleBookmark,
                        onToggleChapterList = onToggleChapterList,
                        onSettingsChange = onSettingsChange,
                        onNavigateToSettings = onNavigateToSettings,
                        onStartTTS = onStartTTS,
                        onPauseTTS = onPauseTTS,
                        onResumeTTS = onResumeTTS,
                        onStopTTS = onStopTTS,
                        onTTSNext = onTTSNext,
                        onTTSPrevious = onTTSPrevious,
                        onToggleTTSSettings = onToggleTTSSettings
                    )

                    // TTS Settings Panel
                    AnimatedVisibility(
                        visible = uiState.showTTSSettings,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        TTSSettingsPanel(
                            speed = uiState.ttsSettings.speed,
                            pitch = uiState.ttsSettings.pitch,
                            selectedVoiceId = uiState.ttsSettings.voiceId,
                            autoScroll = uiState.ttsSettings.autoScroll,
                            lockScrollDuringTTS = ttsScrollLocked,
                            highlightSentence = uiState.ttsSettings.highlightSentence,
                            useSystemVoice = uiState.ttsSettings.useSystemVoice,
                            onSpeedChange = onTTSSpeedChange,
                            onPitchChange = onTTSPitchChange,
                            onVoiceSelected = onTTSVoiceSelected,
                            onAutoScrollChange = onTTSAutoScrollChange,
                            onHighlightChange = onTTSHighlightChange,
                            onLockScrollChange = onTTSLockScrollChange,
                            onUseSystemVoiceChange = onTTSUseSystemVoiceChange,
                            onDismiss = onHideTTSSettings,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Error state
        if (uiState.error != null && uiState.isContentReady && uiState.displayItems.isEmpty()) {
            ReaderErrorState(
                message = uiState.error,
                colors = colors,
                onRetry = onRetry,
                onBack = onBack
            )
        }

        // Loading overlay
        if (uiState.shouldShowLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading chapter...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text
                    )
                }
            }
        }

        // Warmth filter overlay
        if (uiState.settings.warmthFilter > 0f) {
            WarmthOverlay(
                warmth = uiState.settings.warmthFilter,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// =============================================================================
// CONTROLS OVERLAY
// =============================================================================

@Composable
private fun ControlsOverlay(
    uiState: ReaderUiState,
    colors: ReaderColors,
    chapterProgress: Float,
    estimatedTimeLeft: String?,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleChapterList: () -> Unit,
    onSettingsChange: (ReaderSettings) -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartTTS: () -> Unit,
    onPauseTTS: () -> Unit,
    onResumeTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onTTSNext: () -> Unit,
    onTTSPrevious: () -> Unit,
    onToggleTTSSettings: () -> Unit
) {
    val animationDuration = if (uiState.settings.reduceMotion) 0 else ReaderDefaults.ControlsAnimationDuration

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = animationDuration)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = animationDuration)
            ) + fadeOut()
        ) {
            ReaderTopBar(
                chapterTitle = if (uiState.settings.showChapterTitle) uiState.currentChapterName else "",
                chapterNumber = uiState.currentChapterIndex + 1,
                totalChapters = uiState.allChapters.size,
                isBookmarked = uiState.isCurrentChapterBookmarked,
                chapterProgress = chapterProgress,
                estimatedTimeLeft = estimatedTimeLeft,
                colors = colors,
                progressStyle = if (uiState.settings.showProgress) uiState.settings.progressStyle else ProgressStyle.NONE,
                largerTouchTargets = uiState.settings.largerTouchTargets,
                onBack = onBack,
                onBookmarkClick = onToggleBookmark
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls
        AnimatedVisibility(
            visible = uiState.showControls || uiState.isTTSActive,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = animationDuration)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = animationDuration)
            ) + fadeOut()
        ) {
            AnimatedContent(
                targetState = uiState.isTTSActive,
                transitionSpec = {
                    val enter = slideInVertically(
                        initialOffsetY = { if (targetState) it else -it },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(tween(200))
                    val exit = slideOutVertically(
                        targetOffsetY = { if (targetState) -it else it },
                        animationSpec = tween(150)
                    ) + fadeOut(tween(100))
                    enter togetherWith exit
                },
                label = "bottomBarSwitch"
            ) { ttsActive ->
                if (ttsActive) {
                    // TTS Player
                    TTSPlayer(
                        isPlaying = uiState.ttsStatus == TTSStatus.PLAYING,
                        canGoPrevious = uiState.currentGlobalSentenceIndex > 0,
                        canGoNext = uiState.currentGlobalSentenceIndex < uiState.totalTTSSentences - 1,
                        currentSentenceInChapter = uiState.currentSentenceInChapter,
                        totalSentencesInChapter = uiState.totalSentencesInChapter,
                        chapterNumber = uiState.currentChapterIndex + 1,
                        totalChapters = uiState.allChapters.size,
                        speechRate = uiState.ttsSettings.speed,
                        onPlayPause = {
                            if (uiState.ttsStatus == TTSStatus.PLAYING) onPauseTTS() else onResumeTTS()
                        },
                        onNext = onTTSNext,
                        onPrevious = onTTSPrevious,
                        onStop = onStopTTS,
                        onOpenSettings = onToggleTTSSettings
                    )
                } else {
                    // Reader bottom bar with inline settings
                    ReaderBottomBar(
                        settings = uiState.settings,
                        onSettingsChange = onSettingsChange,
                        onOpenChapterList = onToggleChapterList,
                        onStartTTS = onStartTTS,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

// =============================================================================
// WARMTH OVERLAY
// =============================================================================

@Composable
private fun WarmthOverlay(
    warmth: Float,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = warmth * 0.3f,
        label = "warmth_alpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier.background(Color(0xFFFF9800).copy(alpha = alpha))
        )
    }
}