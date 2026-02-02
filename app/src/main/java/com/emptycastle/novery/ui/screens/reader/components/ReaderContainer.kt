package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.ReadingDirection
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.theme.FontProvider
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.domain.model.TextAlign as ReaderTextAlign

@Composable
fun ReaderContainer(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit
) {
    val settings = uiState.settings

    // State for full-screen image viewer
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    // Determine layout direction based on reading direction setting
    val layoutDirection = remember(settings.readingDirection) {
        when (settings.readingDirection) {
            ReadingDirection.RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    }

    // Get font from FontProvider
    val fontFamily = remember(settings.fontFamily) {
        FontProvider.getFontFamily(settings.fontFamily)
    }

    val textAlign = remember(settings.textAlign, settings.readingDirection) {
        mapTextAlign(settings.textAlign, settings.readingDirection)
    }

    val fontWeight = remember(settings.fontWeight) {
        mapFontWeight(settings.fontWeight)
    }

    // Apply high contrast adjustments
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

    // Calculate spacing and padding from settings
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

    // Calculate max width
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

    // Larger touch targets adjustments
    val touchTargetPadding = if (settings.largerTouchTargets) 8.dp else 0.dp

    // Top padding adjusts based on whether controls are visible
    val topPadding = if (uiState.showControls) {
        100.dp + touchTargetPadding
    } else {
        statusBarPadding.calculateTopPadding() + settings.marginVertical.dp
    }

    // Bottom padding includes nav bar + margin
    val bottomPadding = navBarPadding.calculateBottomPadding() + 100.dp +
            settings.marginVertical.dp + touchTargetPadding

    // Create custom fling behavior based on scroll sensitivity
    val flingBehavior = rememberCustomFlingBehavior(
        sensitivity = settings.scrollSensitivity,
        smoothScroll = settings.smoothScroll,
        reduceMotion = settings.reduceMotion
    )

    // Apply layout direction
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Conditionally wrap in SelectionContainer based on settings
            val content: @Composable () -> Unit = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (maxWidth != Dp.Unspecified) {
                                Modifier.widthIn(max = maxWidth)
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(
                        top = topPadding,
                        bottom = bottomPadding
                    ),
                    flingBehavior = flingBehavior
                ) {
                    itemsIndexed(
                        items = uiState.displayItems,
                        key = { _, item -> item.itemId }
                    ) { index, item ->
                        when (item) {
                            is ReaderDisplayItem.ChapterHeader -> {
                                ChapterHeaderItem(
                                    item = item,
                                    colors = effectiveColors,
                                    fontFamily = fontFamily,
                                    horizontalPadding = horizontalPadding,
                                    largerTouchTargets = settings.largerTouchTargets
                                )
                            }

                            is ReaderDisplayItem.Segment -> {
                                SegmentItem(
                                    item = item,
                                    displayIndex = index,
                                    currentSentenceHighlight = uiState.currentSentenceHighlight,
                                    isTTSActive = uiState.isTTSActive,
                                    highlightEnabled = uiState.ttsSettings.highlightSentence,
                                    settings = settings,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    textAlign = textAlign,
                                    textColor = effectiveColors.text,
                                    highlightColor = effectiveColors.sentenceHighlight,
                                    horizontalPadding = horizontalPadding,
                                    paragraphSpacing = paragraphSpacing,
                                    linkColor = effectiveColors.linkColor, // Pass the link color
                                    onLinkClick = { url ->
                                        // Optional: custom link handling
                                        // You could navigate to an in-app browser, show a dialog, etc.
                                    }
                                )
                            }


                            is ReaderDisplayItem.Image -> {
                                ChapterImageItem(
                                    item = item,
                                    colors = effectiveColors,
                                    horizontalPadding = horizontalPadding,
                                    baseUrl = uiState.currentChapterUrl,
                                    onImageClick = { url ->
                                        fullScreenImageUrl = url
                                    }
                                )
                            }

                            is ReaderDisplayItem.ChapterDivider -> {
                                ChapterDividerItem(
                                    item = item,
                                    colors = effectiveColors,
                                    infiniteScrollEnabled = uiState.infiniteScrollEnabled,
                                    horizontalPadding = horizontalPadding,
                                    largerTouchTargets = settings.largerTouchTargets,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onBackToDetails = onBack
                                )
                            }

                            is ReaderDisplayItem.LoadingIndicator -> {
                                LoadingIndicatorItem(colors = effectiveColors)
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

            // Wrap in SelectionContainer if long press selection is enabled
            if (settings.longPressSelection) {
                SelectionContainer {
                    content()
                }
            } else {
                content()
            }

            // Full-screen image viewer dialog
            fullScreenImageUrl?.let { url ->
                ImageViewerDialog(
                    imageUrl = url,
                    onDismiss = { fullScreenImageUrl = null }
                )
            }
        }
    }
}

// =============================================================================
// MAPPING FUNCTIONS
// =============================================================================

private fun mapTextAlign(
    readerTextAlign: ReaderTextAlign,
    readingDirection: ReadingDirection
): TextAlign {
    return when (readerTextAlign) {
        ReaderTextAlign.LEFT -> {
            if (readingDirection == ReadingDirection.RTL) TextAlign.End else TextAlign.Start
        }
        ReaderTextAlign.RIGHT -> {
            if (readingDirection == ReadingDirection.RTL) TextAlign.Start else TextAlign.End
        }
        ReaderTextAlign.CENTER -> TextAlign.Center
        ReaderTextAlign.JUSTIFY -> TextAlign.Justify
    }
}

private fun mapFontWeight(readerFontWeight: com.emptycastle.novery.domain.model.FontWeight): FontWeight {
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