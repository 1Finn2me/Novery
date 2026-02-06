package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.PageAnimation
import com.emptycastle.novery.domain.model.ReadingDirection
import com.emptycastle.novery.domain.model.ScrollMode
import com.emptycastle.novery.ui.screens.reader.logic.AuthorNoteDisplayMode
import com.emptycastle.novery.ui.screens.reader.logic.BlockType
import com.emptycastle.novery.ui.screens.reader.logic.ReaderPage
import com.emptycastle.novery.ui.screens.reader.logic.rememberPaginatedContent
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.theme.FontProvider
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.emptycastle.novery.domain.model.TextAlign as ReaderTextAlign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagedReaderContainer(
    uiState: ReaderUiState,
    colors: ReaderColors,
    initialPage: Int = 0,
    onPageChanged: (pageIndex: Int, chapterIndex: Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onTapCenter: () -> Unit
) {
    val settings = uiState.settings
    val scope = rememberCoroutineScope()

    // State for image viewer dialog
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    // Layout direction
    val layoutDirection = remember(settings.readingDirection) {
        when (settings.readingDirection) {
            ReadingDirection.RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    }

    // Get font
    val fontFamily = remember(settings.fontFamily) {
        FontProvider.getFontFamily(settings.fontFamily)
    }

    val textAlign = remember(settings.textAlign, settings.readingDirection) {
        mapTextAlign(settings.textAlign, settings.readingDirection)
    }

    val fontWeight = remember(settings.fontWeight) {
        mapFontWeight(settings.fontWeight)
    }

    // High contrast
    val effectiveColors = remember(colors, settings.forceHighContrast) {
        if (settings.forceHighContrast) {
            colors.copy(
                text = if (colors.isDarkTheme) Color.White else Color.Black,
                textSecondary = if (colors.isDarkTheme)
                    Color.White.copy(alpha = 0.9f)
                else
                    Color.Black.copy(alpha = 0.9f)
            )
        } else {
            colors
        }
    }

    // Padding
    val horizontalPadding = remember(settings.marginHorizontal, settings.largerTouchTargets) {
        if (settings.largerTouchTargets) {
            (settings.marginHorizontal - 4).coerceAtLeast(8).dp
        } else {
            settings.marginHorizontal.dp
        }
    }

    val paragraphSpacing = remember(settings.paragraphSpacing, settings.fontSize) {
        (settings.fontSize * settings.paragraphSpacing * 0.5f).dp
    }

    // Max width
    val maxWidth = remember(settings.maxWidth) {
        when (settings.maxWidth) {
            MaxWidth.NARROW -> 480.dp
            MaxWidth.MEDIUM -> 600.dp
            MaxWidth.LARGE -> 720.dp
            MaxWidth.EXTRA_LARGE -> 900.dp
            MaxWidth.FULL -> Dp.Unspecified
        }
    }

    // System insets
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val viewportHeight = maxHeight
            val viewportWidth = if (maxWidth != Dp.Unspecified) {
                minOf(maxWidth, maxWidth)
            } else {
                maxWidth
            }

            // Paginate content
            val pages = rememberPaginatedContent(
                displayItems = uiState.displayItems,
                fontSize = settings.fontSize,
                lineHeight = settings.lineHeight,
                paragraphSpacing = settings.paragraphSpacing,
                marginVertical = settings.marginVertical,
                viewportHeight = viewportHeight,
                viewportWidth = viewportWidth
            )

            if (pages.isEmpty()) {
                // Show loading or empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        color = effectiveColors.textSecondary
                    )
                }
                return@BoxWithConstraints
            }

            // Pager state
            val pagerState = rememberPagerState(
                initialPage = initialPage.coerceIn(0, pages.lastIndex),
                pageCount = { pages.size }
            )

            // Track page changes
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { pageIndex ->
                        val page = pages.getOrNull(pageIndex)
                        if (page != null) {
                            onPageChanged(pageIndex, page.chapterIndex)
                        }
                    }
            }

            // Page content
            val isHorizontal = settings.scrollMode == ScrollMode.PAGED

            if (isHorizontal) {
                HorizontalPagedContent(
                    pages = pages,
                    pagerState = pagerState,
                    settings = settings,
                    colors = effectiveColors,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    horizontalPadding = horizontalPadding,
                    paragraphSpacing = paragraphSpacing,
                    maxWidth = maxWidth,
                    statusBarPadding = statusBarPadding,
                    navBarPadding = navBarPadding,
                    currentChapterUrl = uiState.currentChapterUrl,
                    onTapLeft = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            } else {
                                onPrevious()
                            }
                        }
                    },
                    onTapRight = {
                        scope.launch {
                            if (pagerState.currentPage < pages.lastIndex) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onNext()
                            }
                        }
                    },
                    onTapCenter = onTapCenter,
                    onRetryChapter = onRetryChapter,
                    onImageClick = { url -> fullScreenImageUrl = url }
                )
            } else {
                VerticalPagedContent(
                    pages = pages,
                    pagerState = pagerState,
                    settings = settings,
                    colors = effectiveColors,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    horizontalPadding = horizontalPadding,
                    paragraphSpacing = paragraphSpacing,
                    maxWidth = maxWidth,
                    statusBarPadding = statusBarPadding,
                    navBarPadding = navBarPadding,
                    currentChapterUrl = uiState.currentChapterUrl,
                    onTapTop = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            } else {
                                onPrevious()
                            }
                        }
                    },
                    onTapBottom = {
                        scope.launch {
                            if (pagerState.currentPage < pages.lastIndex) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onNext()
                            }
                        }
                    },
                    onTapCenter = onTapCenter,
                    onRetryChapter = onRetryChapter,
                    onImageClick = { url -> fullScreenImageUrl = url }
                )
            }

            // Page indicator
            if (!uiState.showControls) {
                PageIndicator(
                    currentPage = pagerState.currentPage,
                    totalPages = pages.size,
                    colors = effectiveColors,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navBarPadding.calculateBottomPadding() + 16.dp)
                )
            }
        }

        // Full-screen image viewer
        fullScreenImageUrl?.let { url ->
            ImageViewerDialog(
                imageUrl = url,
                onDismiss = { fullScreenImageUrl = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalPagedContent(
    pages: List<ReaderPage>,
    pagerState: PagerState,
    settings: com.emptycastle.novery.domain.model.ReaderSettings,
    colors: ReaderColors,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontWeight: FontWeight,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    maxWidth: Dp,
    statusBarPadding: PaddingValues,
    navBarPadding: PaddingValues,
    currentChapterUrl: String,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onTapCenter: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onImageClick: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 0.dp,
        beyondViewportPageCount = 1,
        key = { pages.getOrNull(it)?.pageIndex ?: it }
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager

        // Apply page animation effects
        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (settings.pageAnimation) {
                        PageAnimation.FADE -> Modifier.graphicsLayer {
                            alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        }
                        PageAnimation.SLIDE -> Modifier // Default pager behavior
                        PageAnimation.FLIP -> Modifier.graphicsLayer {
                            rotationY = pageOffset * 30f
                            cameraDistance = 8 * density
                        }
                        else -> Modifier
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val width = size.width
                        when {
                            offset.x < width * 0.3f -> onTapLeft()
                            offset.x > width * 0.7f -> onTapRight()
                            else -> onTapCenter()
                        }
                    }
                }
        ) {
            PageContent(
                page = page,
                settings = settings,
                colors = colors,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                textAlign = textAlign,
                horizontalPadding = horizontalPadding,
                paragraphSpacing = paragraphSpacing,
                maxWidth = maxWidth,
                statusBarPadding = statusBarPadding,
                navBarPadding = navBarPadding,
                currentChapterUrl = currentChapterUrl,
                onRetryChapter = onRetryChapter,
                onImageClick = onImageClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalPagedContent(
    pages: List<ReaderPage>,
    pagerState: PagerState,
    settings: com.emptycastle.novery.domain.model.ReaderSettings,
    colors: ReaderColors,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontWeight: FontWeight,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    maxWidth: Dp,
    statusBarPadding: PaddingValues,
    navBarPadding: PaddingValues,
    currentChapterUrl: String,
    onTapTop: () -> Unit,
    onTapBottom: () -> Unit,
    onTapCenter: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onImageClick: (String) -> Unit
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 0.dp,
        beyondViewportPageCount = 1,
        key = { pages.getOrNull(it)?.pageIndex ?: it }
    ) { pageIndex ->
        val page = pages.getOrNull(pageIndex) ?: return@VerticalPager

        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (settings.pageAnimation) {
                        PageAnimation.FADE -> Modifier.graphicsLayer {
                            alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        }
                        else -> Modifier
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val height = size.height
                        when {
                            offset.y < height * 0.3f -> onTapTop()
                            offset.y > height * 0.7f -> onTapBottom()
                            else -> onTapCenter()
                        }
                    }
                }
        ) {
            PageContent(
                page = page,
                settings = settings,
                colors = colors,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                textAlign = textAlign,
                horizontalPadding = horizontalPadding,
                paragraphSpacing = paragraphSpacing,
                maxWidth = maxWidth,
                statusBarPadding = statusBarPadding,
                navBarPadding = navBarPadding,
                currentChapterUrl = currentChapterUrl,
                onRetryChapter = onRetryChapter,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
private fun PageContent(
    page: ReaderPage,
    settings: com.emptycastle.novery.domain.model.ReaderSettings,
    colors: ReaderColors,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontWeight: FontWeight,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    maxWidth: Dp,
    statusBarPadding: PaddingValues,
    navBarPadding: PaddingValues,
    currentChapterUrl: String,
    onRetryChapter: (Int) -> Unit,
    onImageClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (maxWidth != Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier
                    }
                )
                .padding(
                    top = statusBarPadding.calculateTopPadding() + settings.marginVertical.dp,
                    bottom = navBarPadding.calculateBottomPadding() + settings.marginVertical.dp + 40.dp
                )
                .padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.Top
        ) {
            page.items.forEach { pageItem ->
                when (val item = pageItem.displayItem) {
                    is ReaderDisplayItem.ChapterHeader -> {
                        ChapterHeaderItem(
                            item = item,
                            colors = colors,
                            fontFamily = fontFamily,
                            horizontalPadding = 0.dp,
                            largerTouchTargets = settings.largerTouchTargets
                        )
                    }

                    is ReaderDisplayItem.Segment -> {
                        when (item.segment.blockType) {
                            BlockType.BLOCKQUOTE -> {
                                BlockquoteSegmentItem(
                                    item = item,
                                    settings = settings,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    textColor = colors.text,
                                    horizontalPadding = 0.dp,
                                    paragraphSpacing = paragraphSpacing,
                                    colors = colors
                                )
                            }
                            BlockType.CODE_BLOCK -> {
                                CodeBlockSegmentItem(
                                    item = item,
                                    displayIndex = pageItem.displayIndex,
                                    currentSentenceHighlight = null,
                                    isTTSActive = false,
                                    highlightEnabled = false,
                                    settings = settings,
                                    textColor = colors.text,
                                    highlightColor = colors.sentenceHighlight,
                                    horizontalPadding = 0.dp,
                                    paragraphSpacing = paragraphSpacing
                                )
                            }
                            BlockType.SYSTEM_MESSAGE -> {
                                SystemMessageSegmentItem(
                                    item = item,
                                    displayIndex = pageItem.displayIndex,
                                    currentSentenceHighlight = null,
                                    isTTSActive = false,
                                    highlightEnabled = false,
                                    settings = settings,
                                    textColor = colors.text,
                                    highlightColor = colors.sentenceHighlight,
                                    horizontalPadding = 0.dp,
                                    paragraphSpacing = paragraphSpacing
                                )
                            }
                            BlockType.NORMAL -> {
                                SegmentItem(
                                    item = item,
                                    displayIndex = pageItem.displayIndex,
                                    currentSentenceHighlight = null,
                                    isTTSActive = false,
                                    highlightEnabled = false,
                                    settings = settings,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    textAlign = textAlign,
                                    textColor = colors.text,
                                    highlightColor = colors.sentenceHighlight,
                                    horizontalPadding = 0.dp,
                                    paragraphSpacing = paragraphSpacing,
                                    linkColor = colors.linkColor,
                                    onLinkClick = null
                                )
                            }
                        }
                    }

                    is ReaderDisplayItem.AuthorNote -> {
                        // Get display mode from preferences
                        val authorNoteDisplayMode = remember {
                            // This should come from ViewModel/preferences
                            AuthorNoteDisplayMode.COLLAPSED
                        }

                        AuthorNoteItem(
                            item = item,
                            colors = colors,
                            displayMode = authorNoteDisplayMode,
                            fontFamily = fontFamily,
                            fontSize = settings.fontSize,
                            horizontalPadding = horizontalPadding,
                            paragraphSpacing = paragraphSpacing
                        )
                    }

                    is ReaderDisplayItem.Image -> {
                        ChapterImageItem(
                            item = item,
                            colors = colors,
                            horizontalPadding = 0.dp,
                            baseUrl = currentChapterUrl,
                            onImageClick = onImageClick
                        )
                    }

                    is ReaderDisplayItem.HorizontalRule -> {
                        HorizontalRuleItem(
                            item = item,
                            colors = colors,
                            horizontalPadding = 0.dp
                        )
                    }

                    is ReaderDisplayItem.SceneBreak -> {
                        SceneBreakItem(
                            item = item,
                            colors = colors,
                            horizontalPadding = 0.dp
                        )
                    }

                    is ReaderDisplayItem.ChapterDivider -> {
                        ChapterDividerItem(
                            item = item,
                            colors = colors,
                            infiniteScrollEnabled = false,
                            horizontalPadding = 0.dp,
                            largerTouchTargets = settings.largerTouchTargets,
                            onPrevious = { },
                            onNext = { },
                            onBackToDetails = { }
                        )
                    }

                    is ReaderDisplayItem.LoadingIndicator -> {
                        LoadingIndicatorItem(colors = colors)
                    }

                    is ReaderDisplayItem.ErrorIndicator -> {
                        ErrorIndicatorItem(
                            error = item.error,
                            colors = colors,
                            onRetry = { onRetryChapter(item.chapterIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    colors: ReaderColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${currentPage + 1} / $totalPages",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary.copy(alpha = 0.7f)
        )
    }
}

// Mapping functions (same as ReaderContainer)
private fun mapTextAlign(
    readerTextAlign: ReaderTextAlign,
    readingDirection: ReadingDirection
): androidx.compose.ui.text.style.TextAlign {
    return when (readerTextAlign) {
        ReaderTextAlign.LEFT -> {
            if (readingDirection == ReadingDirection.RTL)
                androidx.compose.ui.text.style.TextAlign.End
            else
                androidx.compose.ui.text.style.TextAlign.Start
        }
        ReaderTextAlign.RIGHT -> {
            if (readingDirection == ReadingDirection.RTL)
                androidx.compose.ui.text.style.TextAlign.Start
            else
                androidx.compose.ui.text.style.TextAlign.End
        }
        ReaderTextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        ReaderTextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
    }
}

private fun mapFontWeight(
    readerFontWeight: com.emptycastle.novery.domain.model.FontWeight
): FontWeight {
    return when (readerFontWeight) {
        com.emptycastle.novery.domain.model.FontWeight.THIN -> FontWeight.Thin
        com.emptycastle.novery.domain.model.FontWeight.EXTRA_LIGHT -> FontWeight.ExtraLight
        com.emptycastle.novery.domain.model.FontWeight.LIGHT -> FontWeight.Light
        com.emptycastle.novery.domain.model.FontWeight.REGULAR -> FontWeight.Normal
        com.emptycastle.novery.domain.model.FontWeight.MEDIUM -> FontWeight.Medium
        com.emptycastle.novery.domain.model.FontWeight.SEMI_BOLD -> FontWeight.SemiBold
        com.emptycastle.novery.domain.model.FontWeight.BOLD -> FontWeight.Bold
        com.emptycastle.novery.domain.model.FontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
        com.emptycastle.novery.domain.model.FontWeight.BLACK -> FontWeight.Black
    }
}