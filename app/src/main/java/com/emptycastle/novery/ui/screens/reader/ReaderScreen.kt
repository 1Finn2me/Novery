package com.emptycastle.novery.ui.screens.reader

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.service.TTSStatus
import com.emptycastle.novery.tts.TTSEngine
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.ui.components.ChapterListSheet
import com.emptycastle.novery.ui.components.ErrorMessage
import com.emptycastle.novery.ui.components.FilterChip
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.components.ReaderBottomBar
import com.emptycastle.novery.ui.components.TTSPlayer
import com.emptycastle.novery.ui.components.TTSSettingsPanel
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Orange600
import com.emptycastle.novery.ui.theme.ReaderDarkBackground
import com.emptycastle.novery.ui.theme.ReaderDarkText
import com.emptycastle.novery.ui.theme.ReaderLightBackground
import com.emptycastle.novery.ui.theme.ReaderLightText
import com.emptycastle.novery.ui.theme.ReaderSepiaBackground
import com.emptycastle.novery.ui.theme.ReaderSepiaText
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.util.ImmersiveModeEffect
import kotlinx.coroutines.flow.distinctUntilChanged

// =============================================================================
// CONSTANTS
// =============================================================================

private object ReaderDefaults {
    const val MIN_FONT_SIZE = 12
    const val MAX_FONT_SIZE = 32
    const val SCROLL_OFFSET_PX = -100

    val ContentHorizontalPadding = 24.dp
    val ContentVerticalPadding = 16.dp
    val SegmentSpacing = 8.dp
    val ProgressBarHeight = 3.dp
    val FabBottomPadding = 32.dp
    val TopBarElevation = 4.dp
    val ThemeButtonHeight = 40.dp
    val CornerRadius = 8.dp
    val ActiveSegmentBorderWidth = 2.dp

    val ChapterDividerVerticalPadding = 48.dp

    const val ControlsBackgroundAlpha = 0.98f
    const val LabelAlpha = 0.6f
    const val ActiveSegmentBackgroundAlpha = 0.1f
    const val ActiveSegmentBorderAlpha = 0.3f
    const val TtsIndicatorAlpha = 0.1f
}

// =============================================================================
// THEME COLORS
// =============================================================================

data class ReaderColors(
    val background: Color,
    val text: Color,
    val accent: Color = Orange500,
    val accentDark: Color = Orange600,
    val divider: Color = Zinc700,
    val sentenceHighlight: Color = Orange500.copy(alpha = 0.3f)
) {
    companion object {
        fun fromTheme(theme: ReaderTheme): ReaderColors = when (theme) {
            ReaderTheme.LIGHT -> ReaderColors(
                background = ReaderLightBackground,
                text = ReaderLightText,
                divider = Color.LightGray.copy(alpha = 0.3f),
                sentenceHighlight = Orange500.copy(alpha = 0.25f)
            )
            ReaderTheme.SEPIA -> ReaderColors(
                background = ReaderSepiaBackground,
                text = ReaderSepiaText,
                divider = Color(0xFFD4C4A8),
                sentenceHighlight = Color(0xFFD4A574).copy(alpha = 0.35f)
            )
            ReaderTheme.DARK -> ReaderColors(
                background = ReaderDarkBackground,
                text = ReaderDarkText,
                divider = Zinc700,
                sentenceHighlight = Orange500.copy(alpha = 0.35f)
            )
        }
    }
}

// =============================================================================
// KEEP SCREEN ON EFFECT
// =============================================================================

@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        val window = (context as? ComponentActivity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

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
    viewModel: ReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Chapter list sheet state
    val chapterListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    KeepScreenOnEffect(enabled = appSettings.keepScreenOn)

    val colors = remember(uiState.settings.theme) {
        ReaderColors.fromTheme(uiState.settings.theme)
    }

    val listState = rememberLazyListState()

    val showSystemBars = uiState.showControls || uiState.isTTSActive || uiState.isLoading || uiState.error != null
    ImmersiveModeEffect(showSystemBars = showSystemBars)

    // Load chapter on first composition
    LaunchedEffect(chapterUrl, novelUrl, providerName) {
        viewModel.loadChapter(chapterUrl, novelUrl, providerName)
    }

    // Restore scroll position when content is loaded
    LaunchedEffect(uiState.targetScrollPosition) {
        val targetPosition = uiState.targetScrollPosition ?: return@LaunchedEffect

        // Wait a bit for layout to stabilize
        kotlinx.coroutines.delay(150)

        try {
            listState.scrollToItem(
                index = targetPosition.displayIndex.coerceIn(0, maxOf(0, uiState.displayItems.size - 1)),
                scrollOffset = targetPosition.offsetPixels
            )
        } catch (e: Exception) {
            // If scroll fails, try scrolling to start
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
    // Detect approaching BEGINNING for infinite scroll (scrolling UP)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            firstVisibleIndex
        }.collect { firstVisibleIndex ->
            if (firstVisibleIndex <= 5) { // Near the top
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

    // Detect approaching end for infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Pair(lastVisibleIndex, totalItems)
        }.collect { (lastVisibleIndex, totalItems) ->
            if (totalItems > 0 && lastVisibleIndex >= totalItems - 5) {
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

    // Auto-scroll to current segment during TTS (only if auto-scroll is enabled)
    LaunchedEffect(uiState.currentSegmentIndex, uiState.isTTSActive, uiState.ttsSettings.autoScroll) {
        if (uiState.isTTSActive && uiState.currentSegmentIndex >= 0 && uiState.ttsSettings.autoScroll) {
            try {
                listState.animateScrollToItem(
                    index = uiState.currentSegmentIndex,
                    scrollOffset = ReaderDefaults.SCROLL_OFFSET_PX
                )
            } catch (e: Exception) {
                // Ignore scroll exceptions
            }
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
        onBack = onBack,
        onRetry = { viewModel.loadChapter(chapterUrl, novelUrl, providerName) },
        onRetryChapter = { chapterIndex -> viewModel.retryChapter(chapterIndex) },
        callbacks = ReaderCallbacks(
            onToggleControls = viewModel::toggleControls,
            onToggleSettings = viewModel::toggleSettings,
            onFontSizeChange = viewModel::updateFontSize,
            onFontFamilyChange = viewModel::updateFontFamily,
            onThemeChange = viewModel::updateTheme,
            onPrevious = viewModel::navigateToPrevious,
            onNext = viewModel::navigateToNext,
            onSegmentClick = viewModel::setCurrentSegment,
            onSentenceClick = viewModel::seekToSentence,
            onStartTTS = viewModel::startTTS,
            onPauseTTS = viewModel::pauseTTS,
            onResumeTTS = viewModel::resumeTTS,
            onStopTTS = viewModel::stopTTS,
            onTTSNext = viewModel::nextSegment,
            onTTSPrevious = viewModel::previousSegment,
            getTTSEngine = viewModel::getTTSEngine,
            onToggleTTSSettings = viewModel::toggleTTSSettings,
            onHideTTSSettings = viewModel::hideTTSSettings,
            onTTSSpeedChange = viewModel::updateTTSSpeed,
            onTTSPitchChange = viewModel::updateTTSPitch,
            onTTSVoiceSelected = viewModel::updateTTSVoice,
            onTTSAutoScrollChange = viewModel::updateTTSAutoScroll,
            onTTSHighlightChange = viewModel::updateTTSHighlightSentence,
            onToggleChapterList = viewModel::toggleChapterList,
            onHideChapterList = viewModel::hideChapterList,
            onNavigateToChapter = viewModel::navigateToChapter
        )
    )
}


// =============================================================================
// CALLBACKS CONTAINER
// =============================================================================

data class ReaderCallbacks(
    val onToggleControls: () -> Unit,
    val onToggleSettings: () -> Unit,
    val onFontSizeChange: (Int) -> Unit,
    val onFontFamilyChange: (com.emptycastle.novery.domain.model.FontFamily) -> Unit,
    val onThemeChange: (ReaderTheme) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSegmentClick: (Int) -> Unit,
    val onSentenceClick: (Int, Int) -> Unit,
    val onStartTTS: () -> Unit,
    val onPauseTTS: () -> Unit,
    val onResumeTTS: () -> Unit,
    val onStopTTS: () -> Unit,
    val onTTSNext: () -> Unit,
    val onTTSPrevious: () -> Unit,
    val getTTSEngine: () -> TTSEngine,
    // TTS Settings callbacks
    val onToggleTTSSettings: () -> Unit,
    val onHideTTSSettings: () -> Unit,
    val onTTSSpeedChange: (Float) -> Unit,
    val onTTSPitchChange: (Float) -> Unit,
    val onTTSVoiceSelected: (VoiceInfo) -> Unit,
    val onTTSAutoScrollChange: (Boolean) -> Unit,
    val onTTSHighlightChange: (Boolean) -> Unit,
    // Chapter list callbacks
    val onToggleChapterList: () -> Unit,
    val onHideChapterList: () -> Unit,
    val onNavigateToChapter: (Int) -> Unit
)


// =============================================================================
// SCREEN CONTENT
// =============================================================================

@Composable
private fun ReaderScreenContent(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    callbacks: ReaderCallbacks
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when {
            uiState.isLoading && uiState.displayItems.isEmpty() -> {
                LoadingState(textColor = colors.text)
            }

            uiState.error != null && uiState.displayItems.isEmpty() -> {
                ErrorState(
                    message = uiState.error,
                    onRetry = onRetry,
                    onBack = onBack
                )
            }

            else -> {
                ContentState(
                    uiState = uiState,
                    colors = colors,
                    listState = listState,
                    onBack = onBack,
                    onRetryChapter = onRetryChapter,
                    callbacks = callbacks
                )
            }
        }
    }
}

// =============================================================================
// LOADING STATE
// =============================================================================

@Composable
private fun LoadingState(textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading chapter" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Orange500)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading Chapter...",
                color = textColor.copy(alpha = ReaderDefaults.LabelAlpha)
            )
        }
    }
}

// =============================================================================
// ERROR STATE
// =============================================================================

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ErrorMessage(
                message = message,
                onRetry = onRetry
            )
            Spacer(modifier = Modifier.height(16.dp))
            GhostButton(
                text = "Go Back",
                onClick = onBack
            )
        }
    }
}

// =============================================================================
// CONTENT STATE
// =============================================================================

@Composable
private fun ContentState(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    callbacks: ReaderCallbacks
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main scrollable content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    callbacks.onToggleControls()
                }
        ) {
            ReaderContent(
                uiState = uiState,
                colors = colors,
                listState = listState,
                onSegmentClick = callbacks.onSegmentClick,
                onSentenceClick = callbacks.onSentenceClick,
                onPrevious = callbacks.onPrevious,
                onNext = callbacks.onNext,
                onBack = onBack,
                onRetryChapter = onRetryChapter
            )
        }

        // Overlay controls
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = uiState.showControls,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                ReaderTopBar(
                    chapterTitle = uiState.currentChapterName,
                    chapterNumber = uiState.currentChapterIndex + 1,
                    totalChapters = uiState.allChapters.size,
                    isTTSActive = uiState.isTTSActive,
                    isPreloading = uiState.isPreloading,
                    backgroundColor = colors.background,
                    onBack = onBack,
                    onSettingsClick = callbacks.onToggleSettings
                )
            }

            AnimatedVisibility(
                visible = uiState.showSettings && uiState.showControls,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ReaderSettingsPanel(
                    settings = uiState.settings,
                    colors = colors,
                    onFontSizeChange = callbacks.onFontSizeChange,
                    onFontFamilyChange = callbacks.onFontFamilyChange,
                    onThemeChange = callbacks.onThemeChange
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Reading progress bar
        LinearProgressIndicator(
            progress = { uiState.readingProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(ReaderDefaults.ProgressBarHeight)
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Above the bottom bar
                .alpha(if (uiState.showControls) 1f else 0.3f),
            color = colors.accentDark,
            trackColor = Color.Transparent
        )

        // Bottom Bar (Transforms between controls and TTS player)
        AnimatedVisibility(
            visible = uiState.showControls || uiState.isTTSActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomBar(
                isTTSActive = uiState.isTTSActive,
                isTTSPlaying = uiState.ttsStatus == TTSStatus.PLAYING,
                currentSentenceIndex = uiState.currentGlobalSentenceIndex,
                totalSentences = uiState.totalTTSSentences,
                chapterName = uiState.currentChapterName,
                speechRate = uiState.ttsSettings.speed,
                onPlayTTS = callbacks.onStartTTS,
                onPauseTTS = callbacks.onPauseTTS,
                onResumeTTS = callbacks.onResumeTTS,
                onStopTTS = callbacks.onStopTTS,
                onNextSentence = callbacks.onTTSNext,
                onPreviousSentence = callbacks.onTTSPrevious,
                onOpenChapterList = callbacks.onToggleChapterList,
                onOpenSettings = callbacks.onToggleSettings,
                onOpenTTSSettings = callbacks.onToggleTTSSettings
            )
        }

        // TTS Settings Panel (overlay)
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
                onSpeedChange = callbacks.onTTSSpeedChange,
                onPitchChange = callbacks.onTTSPitchChange,
                onVoiceSelected = callbacks.onTTSVoiceSelected,
                onAutoScrollChange = callbacks.onTTSAutoScrollChange,
                onHighlightChange = callbacks.onTTSHighlightChange,
                onDismiss = callbacks.onHideTTSSettings,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// =============================================================================
// TTS CONTROLS
// =============================================================================

@Composable
private fun TTSControls(
    isTTSActive: Boolean,
    isContentAvailable: Boolean,
    showControls: Boolean,
    currentSegmentIndex: Int,
    totalSegments: Int,
    currentSentenceIndex: Int,
    totalSentences: Int,
    onStartTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = isTTSActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            TTSPlayer(
                currentSegmentIndex = currentSentenceIndex,
                totalSegments = totalSentences,
                onNext = onNext,
                onPrevious = onPrevious,
                onClose = onStopTTS
            )
        }

        AnimatedVisibility(
            visible = !isTTSActive && isContentAvailable && showControls,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.padding(bottom = ReaderDefaults.FabBottomPadding)
        ) {
            ExtendedFloatingActionButton(
                onClick = onStartTTS,
                containerColor = Orange600,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "Listen",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

// =============================================================================
// TOP BAR
// =============================================================================

@Composable
private fun ReaderTopBar(
    chapterTitle: String,
    chapterNumber: Int,
    totalChapters: Int,
    isTTSActive: Boolean,
    isPreloading: Boolean,
    backgroundColor: Color,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor.copy(alpha = ReaderDefaults.ControlsBackgroundAlpha),
        shadowElevation = ReaderDefaults.TopBarElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = Zinc400
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Text(
                    text = "Chapter $chapterNumber of $totalChapters",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc500
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPreloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Orange500
                    )
                }

                if (isTTSActive) {
                    TTSIndicatorChip()
                }
            }

            // Spacer to balance the back button
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun TTSIndicatorChip() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Orange500.copy(alpha = ReaderDefaults.TtsIndicatorAlpha)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Orange500
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Listening",
                style = MaterialTheme.typography.labelSmall,
                color = Orange500
            )
        }
    }
}

// =============================================================================
// SETTINGS PANEL
// =============================================================================

@Composable
private fun ReaderSettingsPanel(
    settings: ReaderSettings,
    colors: ReaderColors,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (com.emptycastle.novery.domain.model.FontFamily) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.background.copy(alpha = ReaderDefaults.ControlsBackgroundAlpha)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsLabel(text = "Theme", color = colors.text)
            ThemeSelector(
                selectedTheme = settings.theme,
                onThemeChange = onThemeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            FontSizeControl(
                fontSize = settings.fontSize,
                textColor = colors.text,
                onFontSizeChange = onFontSizeChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsLabel(text = "Font", color = colors.text)
            FontFamilySelector(
                selectedFamily = settings.fontFamily,
                onFontFamilyChange = onFontFamilyChange
            )
        }
    }
}

@Composable
private fun SettingsLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color.copy(alpha = ReaderDefaults.LabelAlpha)
    )
}

@Composable
private fun ThemeSelector(
    selectedTheme: ReaderTheme,
    onThemeChange: (ReaderTheme) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReaderTheme.entries.forEach { theme ->
            ThemeButton(
                theme = theme,
                isSelected = selectedTheme == theme,
                onClick = { onThemeChange(theme) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeButton(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = remember(theme) { ReaderColors.fromTheme(theme) }
    val icon = remember(theme) {
        when (theme) {
            ReaderTheme.LIGHT -> Icons.Default.LightMode
            ReaderTheme.SEPIA -> Icons.Default.Coffee
            ReaderTheme.DARK -> Icons.Default.DarkMode
        }
    }

    Surface(
        modifier = modifier
            .height(ReaderDefaults.ThemeButtonHeight)
            .clip(RoundedCornerShape(ReaderDefaults.CornerRadius))
            .clickable(onClick = onClick),
        color = themeColors.background,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Orange500 else Zinc700
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "${theme.displayName()} theme",
                tint = themeColors.text
            )
        }
    }
}

@Composable
private fun FontSizeControl(
    fontSize: Int,
    textColor: Color,
    onFontSizeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Font Size: ${fontSize}px",
            style = MaterialTheme.typography.labelMedium,
            color = textColor.copy(alpha = ReaderDefaults.LabelAlpha)
        )

        Row {
            IconButton(
                onClick = { onFontSizeChange(fontSize - 1) },
                enabled = fontSize > ReaderDefaults.MIN_FONT_SIZE
            ) {
                Icon(
                    imageVector = Icons.Default.TextDecrease,
                    contentDescription = "Decrease font size",
                    tint = textColor
                )
            }

            IconButton(
                onClick = { onFontSizeChange(fontSize + 1) },
                enabled = fontSize < ReaderDefaults.MAX_FONT_SIZE
            ) {
                Icon(
                    imageVector = Icons.Default.TextIncrease,
                    contentDescription = "Increase font size",
                    tint = textColor
                )
            }
        }
    }

    Slider(
        value = fontSize.toFloat(),
        onValueChange = { onFontSizeChange(it.toInt()) },
        valueRange = ReaderDefaults.MIN_FONT_SIZE.toFloat()..ReaderDefaults.MAX_FONT_SIZE.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = Orange500,
            activeTrackColor = Orange500
        )
    )
}

@Composable
private fun FontFamilySelector(
    selectedFamily: com.emptycastle.novery.domain.model.FontFamily,
    onFontFamilyChange: (com.emptycastle.novery.domain.model.FontFamily) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.emptycastle.novery.domain.model.FontFamily.entries.forEach { family ->
            FilterChip(
                text = family.displayName(),
                selected = selectedFamily == family,
                onClick = { onFontFamilyChange(family) }
            )
        }
    }
}

// =============================================================================
// READER CONTENT
// =============================================================================

@Composable
private fun ReaderContent(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    onSegmentClick: (Int) -> Unit,
    onSentenceClick: (Int, Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit
) {
    val fontFamily = remember(uiState.settings.fontFamily) {
        when (uiState.settings.fontFamily) {
            com.emptycastle.novery.domain.model.FontFamily.SERIF -> FontFamily.Serif
            com.emptycastle.novery.domain.model.FontFamily.SANS -> FontFamily.SansSerif
            com.emptycastle.novery.domain.model.FontFamily.MONO -> FontFamily.Monospace
        }
    }

    val textAlign = remember(uiState.settings.textAlign) {
        when (uiState.settings.textAlign) {
            com.emptycastle.novery.domain.model.TextAlign.LEFT -> TextAlign.Start
            com.emptycastle.novery.domain.model.TextAlign.JUSTIFY -> TextAlign.Justify
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    val topPadding = if (uiState.showControls) 100.dp else statusBarPadding.calculateTopPadding() + 16.dp

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topPadding,
            bottom = navBarPadding.calculateBottomPadding() + 100.dp
        )
    ) {
        itemsIndexed(
            items = uiState.displayItems,
            key = { _, item -> item.itemId }
        ) { index, item ->
            when (item) {
                is ReaderDisplayItem.ChapterHeader -> {
                    ChapterHeaderItem(
                        item = item,
                        colors = colors,
                        fontFamily = fontFamily
                    )
                }

                is ReaderDisplayItem.Segment -> {
                    val isSegmentActive = index == uiState.currentSegmentIndex && uiState.isTTSActive

                    SegmentItem(
                        item = item,
                        displayIndex = index,
                        isSegmentActive = isSegmentActive,
                        currentSentenceHighlight = uiState.currentSentenceHighlight,
                        isTTSActive = uiState.isTTSActive,
                        highlightEnabled = uiState.ttsSettings.highlightSentence,
                        settings = uiState.settings,
                        fontFamily = fontFamily,
                        textAlign = textAlign,
                        textColor = colors.text,
                        highlightColor = colors.sentenceHighlight,
                        onClick = { onSegmentClick(index) },
                        onSentenceClick = { sentenceIndex ->
                            onSentenceClick(index, sentenceIndex)
                        }
                    )
                }

                is ReaderDisplayItem.ChapterDivider -> {
                    ChapterDividerItem(
                        item = item,
                        colors = colors,
                        infiniteScrollEnabled = uiState.infiniteScrollEnabled,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onBackToDetails = onBack
                    )
                }

                is ReaderDisplayItem.LoadingIndicator -> {
                    LoadingIndicatorItem(colors = colors)
                }

                is ReaderDisplayItem.ErrorIndicator -> {
                    ErrorIndicatorItem(
                        item = item,
                        colors = colors,
                        onRetry = { onRetryChapter(item.chapterIndex) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// DISPLAY ITEM COMPOSABLES
// =============================================================================

@Composable
private fun ChapterHeaderItem(
    item: ReaderDisplayItem.ChapterHeader,
    colors: ReaderColors,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding)
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colors.divider
            )
            Text(
                text = " ✦ ",
                color = colors.accent,
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colors.divider
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chapter ${item.chapterNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.chapterName,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = fontFamily
            ),
            fontWeight = FontWeight.Bold,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${item.chapterNumber} of ${item.totalChapters}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.text.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SegmentItem(
    item: ReaderDisplayItem.Segment,
    displayIndex: Int,
    isSegmentActive: Boolean,
    currentSentenceHighlight: SentenceHighlight?,
    isTTSActive: Boolean,
    highlightEnabled: Boolean,
    settings: ReaderSettings,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    textColor: Color,
    highlightColor: Color,
    onClick: () -> Unit,
    onSentenceClick: (Int) -> Unit
) {
    val segment = item.segment

    val hasSentenceHighlight = isTTSActive &&
            highlightEnabled &&
            currentSentenceHighlight != null &&
            currentSentenceHighlight.segmentDisplayIndex == displayIndex

    val annotatedText = remember(segment.text, hasSentenceHighlight, currentSentenceHighlight) {
        buildAnnotatedString {
            append(segment.text)

            if (hasSentenceHighlight && currentSentenceHighlight != null) {
                val sentence = currentSentenceHighlight.sentence
                val start = sentence.startIndex.coerceIn(0, segment.text.length)
                val end = sentence.endIndex.coerceIn(0, segment.text.length)

                if (start < end) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding)
            .padding(vertical = ReaderDefaults.SegmentSpacing / 2)
            .clip(RoundedCornerShape(ReaderDefaults.CornerRadius))
            .background(
                if (isSegmentActive && !hasSentenceHighlight) {
                    Orange500.copy(alpha = ReaderDefaults.ActiveSegmentBackgroundAlpha)
                } else {
                    Color.Transparent
                }
            )
            .then(
                if (isSegmentActive && !hasSentenceHighlight) {
                    Modifier.border(
                        width = ReaderDefaults.ActiveSegmentBorderWidth,
                        color = Orange500.copy(alpha = ReaderDefaults.ActiveSegmentBorderAlpha),
                        shape = RoundedCornerShape(ReaderDefaults.CornerRadius)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = settings.fontSize.sp,
                fontFamily = fontFamily,
                lineHeight = (settings.fontSize * settings.lineHeight).sp
            ),
            color = textColor,
            textAlign = textAlign
        )
    }
}

@Composable
private fun ChapterDividerItem(
    item: ReaderDisplayItem.ChapterDivider,
    colors: ReaderColors,
    infiniteScrollEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ReaderDefaults.ChapterDividerVerticalPadding)
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = colors.divider
        )

        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = colors.text.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "End of Chapter ${item.chapterNumber}",
            style = MaterialTheme.typography.titleSmall,
            color = colors.text.copy(alpha = 0.6f)
        )

        Text(
            text = item.chapterName,
            style = MaterialTheme.typography.bodySmall,
            color = colors.text.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!infiniteScrollEnabled) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChapterNavButton(
                    text = "Previous",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = item.chapterNumber > 1,
                    isPrimary = false,
                    colors = colors,
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                )

                ChapterNavButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = item.hasNextChapter,
                    isPrimary = true,
                    colors = colors,
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GhostButton(
                text = "Back to Novel",
                onClick = onBackToDetails
            )
        } else if (!item.hasNextChapter) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.accent.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "🎉 You've reached the end!",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GhostButton(
                text = "Back to Novel",
                onClick = onBackToDetails
            )
        }
    }
}

@Composable
private fun ChapterNavButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    isPrimary: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !enabled -> Zinc800.copy(alpha = 0.5f)
        isPrimary -> colors.accent
        else -> Zinc800
    }

    val contentColor = when {
        !enabled -> Zinc600
        isPrimary -> Color.White
        else -> colors.text
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isPrimary) Arrangement.End else Arrangement.Start
        ) {
            if (!isPrimary) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            if (isPrimary) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicatorItem(colors: ReaderColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colors.accent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading chapter...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorIndicatorItem(
    item: ReaderDisplayItem.ErrorIndicator,
    colors: ReaderColors,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Failed to load chapter",
                style = MaterialTheme.typography.titleSmall,
                color = colors.text
            )

            Text(
                text = item.error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.text.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}