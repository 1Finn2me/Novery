package com.emptycastle.novery.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.ui.components.ChapterListSheet
import com.emptycastle.novery.ui.components.ReaderBottomBar
import com.emptycastle.novery.ui.components.TTSSettingsPanel
import com.emptycastle.novery.ui.screens.reader.components.KeepScreenOnEffect
import com.emptycastle.novery.ui.screens.reader.components.ReaderContainer
import com.emptycastle.novery.ui.screens.reader.components.ReaderErrorState
import com.emptycastle.novery.ui.screens.reader.components.ReaderLoadingState
import com.emptycastle.novery.ui.screens.reader.components.ReaderTopBar
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.screens.reader.theme.ReaderDefaults
import com.emptycastle.novery.util.ImmersiveModeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val chapterListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Initialize context for TTS
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
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

    val listState = rememberLazyListState()

    // Immersive mode control - show system bars when controls are visible or in special states
    val showSystemBars = uiState.showControls ||
            uiState.isTTSActive ||
            uiState.isLoading ||
            uiState.error != null ||
            uiState.showTTSSettings ||
            uiState.showChapterList
    ImmersiveModeEffect(showSystemBars = showSystemBars)

    // Lifecycle handling for reading time tracking
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onPauseReading()
                Lifecycle.Event.ON_RESUME -> viewModel.onResumeReading()
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

    // Restore scroll position when content is loaded
    LaunchedEffect(uiState.targetScrollPosition) {
        val targetPosition = uiState.targetScrollPosition ?: return@LaunchedEffect

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

    // Track scroll position changes
    LaunchedEffect(listState) {
        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (index, offset) ->
            viewModel.updateCurrentScrollPosition(index, offset)
        }
    }

    // Track current chapter based on visible items
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                val displayItems = uiState.displayItems
                if (displayItems.isEmpty()) return@collect

                val item = displayItems.getOrNull(firstVisibleIndex)
                val chapterIndex = when (item) {
                    is ReaderDisplayItem.ChapterHeader -> item.chapterIndex
                    is ReaderDisplayItem.Segment -> item.chapterIndex
                    is ReaderDisplayItem.ChapterDivider -> item.chapterIndex
                    is ReaderDisplayItem.LoadingIndicator -> item.chapterIndex
                    is ReaderDisplayItem.ErrorIndicator -> item.chapterIndex
                    else -> return@collect
                }

                val chapter = uiState.allChapters.getOrNull(chapterIndex)
                if (chapter != null) {
                    viewModel.updateCurrentChapter(chapterIndex, chapter.url, chapter.name)
                }
            }
    }

    // Infinite scroll: approaching beginning
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        }.collect { firstVisibleIndex ->
            if (firstVisibleIndex <= ReaderDefaults.PRELOAD_THRESHOLD_ITEMS) {
                val displayItems = uiState.displayItems
                val firstItem = displayItems.getOrNull(firstVisibleIndex)
                val chapterIndex = when (firstItem) {
                    is ReaderDisplayItem.ChapterHeader -> firstItem.chapterIndex
                    is ReaderDisplayItem.Segment -> firstItem.chapterIndex
                    else -> return@collect
                }
                viewModel.onApproachingBeginning(chapterIndex)
            }
        }
    }

    // Infinite scroll: approaching end
    LaunchedEffect(listState) {
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
                    is ReaderDisplayItem.ChapterDivider -> lastItem.chapterIndex
                    else -> return@collect
                }
                viewModel.onApproachingEnd(chapterIndex)
            }
        }
    }

    // Auto-scroll to current segment during TTS
    LaunchedEffect(uiState.currentSegmentIndex, uiState.isTTSActive, uiState.ttsSettings.autoScroll) {
        if (uiState.isTTSActive && uiState.currentSegmentIndex >= 0 && uiState.ttsSettings.autoScroll) {
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

    // Save position when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePositionOnExit()
        }
    }

    // Calculate reading progress
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) 0f
            else (listState.firstVisibleItemIndex.toFloat() / totalItems).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(readingProgress) {
        viewModel.updateReadingProgress(readingProgress)
    }

    // Calculate estimated reading time
    val estimatedTimeLeft by remember(readingProgress, uiState.estimatedTotalWords) {
        derivedStateOf {
            calculateEstimatedTimeLeft(
                progress = readingProgress,
                totalWords = uiState.estimatedTotalWords,
                settings = uiState.settings
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
        estimatedTimeLeft = if (uiState.settings.showReadingTime) estimatedTimeLeft else null,
        onBack = onBack,
        onRetry = { viewModel.loadChapter(chapterUrl, novelUrl, providerName) },
        onRetryChapter = { chapterIndex -> viewModel.retryChapter(chapterIndex) },
        onToggleControls = viewModel::toggleControls,
        onToggleBookmark = viewModel::toggleBookmark,
        onToggleChapterList = viewModel::toggleChapterList,
        onToggleSettings = viewModel::toggleSettings,
        onToggleTTSSettings = viewModel::toggleTTSSettings,
        onHideTTSSettings = viewModel::hideTTSSettings,

        // Quick settings
        onFontSizeChange = { delta ->
            val newSize = (uiState.settings.fontSize + delta).coerceIn(
                ReaderSettings.MIN_FONT_SIZE,
                ReaderSettings.MAX_FONT_SIZE
            )
            viewModel.updateReaderSettings(uiState.settings.copy(fontSize = newSize))
        },
        onThemeChange = { theme ->
            viewModel.updateReaderSettings(uiState.settings.copy(theme = theme))
        },
        onLineHeightChange = { newLineHeight ->
            viewModel.updateReaderSettings(uiState.settings.copy(lineHeight = newLineHeight))
        },
        onNavigateToSettings = onNavigateToSettings,

        // TTS
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

        // Navigation
        onPrevious = viewModel::navigateToPrevious,
        onNext = viewModel::navigateToNext,
        onSegmentClick = viewModel::setCurrentSegment,
        onSentenceClick = viewModel::seekToSentence
    )
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

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

    val remainingProgress = 1f - progress
    val remainingWords = (totalWords * remainingProgress).toInt()
    val minutes = remainingWords / wpm

    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60}h ${minutes % 60}m"
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
    estimatedTimeLeft: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onToggleControls: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleChapterList: () -> Unit,
    onToggleSettings: () -> Unit,
    onToggleTTSSettings: () -> Unit,
    onHideTTSSettings: () -> Unit,

    // Quick settings
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineHeightChange: (Float) -> Unit,  // Changed from toggle to value
    onNavigateToSettings: () -> Unit,

    // TTS
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

    // Navigation
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSegmentClick: (Int) -> Unit,
    onSentenceClick: (Int, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when {
            uiState.isLoading && uiState.displayItems.isEmpty() -> {
                ReaderLoadingState(colors = colors)
            }

            uiState.error != null && uiState.displayItems.isEmpty() -> {
                ReaderErrorState(
                    message = uiState.error,
                    colors = colors,
                    onRetry = onRetry,
                    onBack = onBack
                )
            }

            else -> {
                // Main content with tap detection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    // Simple: tap anywhere toggles controls
                                    onToggleControls()
                                }
                            )
                        }
                ) {
                    ReaderContainer(
                        uiState = uiState,
                        colors = colors,
                        listState = listState,
                        onSegmentClick = onSegmentClick,
                        onSentenceClick = onSentenceClick,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onBack = onBack,
                        onRetryChapter = onRetryChapter
                    )
                }

                // Overlay controls
                ControlsOverlay(
                    uiState = uiState,
                    colors = colors,
                    estimatedTimeLeft = estimatedTimeLeft,
                    onBack = onBack,
                    onToggleBookmark = onToggleBookmark,
                    onToggleChapterList = onToggleChapterList,
                    onFontSizeChange = onFontSizeChange,
                    onThemeChange = onThemeChange,
                    onLineHeightChange = onLineHeightChange,
                    onNavigateToSettings = onNavigateToSettings,
                    onStartTTS = onStartTTS,
                    onPauseTTS = onPauseTTS,
                    onResumeTTS = onResumeTTS,
                    onStopTTS = onStopTTS,
                    onTTSNext = onTTSNext,
                    onTTSPrevious = onTTSPrevious,
                    onToggleTTSSettings = onToggleTTSSettings
                )

                // TTS Settings Panel (slides up from bottom)
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
                        highlightSentence = uiState.ttsSettings.highlightSentence,
                        onSpeedChange = onTTSSpeedChange,
                        onPitchChange = onTTSPitchChange,
                        onVoiceSelected = onTTSVoiceSelected,
                        onAutoScrollChange = onTTSAutoScrollChange,
                        onHighlightChange = onTTSHighlightChange,
                        onDismiss = onHideTTSSettings,
                        modifier = Modifier.padding(16.dp)
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
    estimatedTimeLeft: String?,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleChapterList: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineHeightChange: (Float) -> Unit,  // Changed from toggle to value
    onNavigateToSettings: () -> Unit,
    onStartTTS: () -> Unit,
    onPauseTTS: () -> Unit,
    onResumeTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onTTSNext: () -> Unit,
    onTTSPrevious: () -> Unit,
    onToggleTTSSettings: () -> Unit
) {
    val animationDuration =
        if (uiState.settings.reduceMotion) 0 else ReaderDefaults.ControlsAnimationDuration

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
                chapterTitle = if (uiState.settings.showChapterTitle)
                    uiState.currentChapterName
                else "",
                chapterNumber = uiState.currentChapterIndex + 1,
                totalChapters = uiState.allChapters.size,
                isBookmarked = uiState.isCurrentChapterBookmarked,
                readingProgress = uiState.readingProgress,
                estimatedTimeLeft = estimatedTimeLeft,
                colors = colors,
                onBack = onBack,
                onBookmarkClick = onToggleBookmark
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Bar - visible when controls shown OR when TTS is active
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
            ReaderBottomBar(
                // TTS State
                isTTSActive = uiState.isTTSActive,
                isTTSPlaying = uiState.ttsStatus == TTSStatus.PLAYING,
                currentSentenceIndex = uiState.currentGlobalSentenceIndex,
                totalSentences = uiState.totalTTSSentences,
                chapterName = uiState.currentChapterName,
                speechRate = uiState.ttsSettings.speed,

                // Quick Settings State
                fontSize = uiState.settings.fontSize,
                currentTheme = uiState.settings.theme,
                lineHeight = uiState.settings.lineHeight,

                // Callbacks - Normal Mode
                onFontSizeDecrease = { onFontSizeChange(-1) },
                onFontSizeIncrease = { onFontSizeChange(1) },
                onThemeChange = onThemeChange,
                onLineHeightChange = onLineHeightChange,  // Now passes value directly
                onOpenChapterList = onToggleChapterList,
                onOpenSettings = onNavigateToSettings,

                // Callbacks - TTS
                onStartTTS = onStartTTS,
                onPauseTTS = onPauseTTS,
                onResumeTTS = onResumeTTS,
                onStopTTS = onStopTTS,
                onNextSentence = onTTSNext,
                onPreviousSentence = onTTSPrevious,
                onOpenTTSSettings = onToggleTTSSettings
            )
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