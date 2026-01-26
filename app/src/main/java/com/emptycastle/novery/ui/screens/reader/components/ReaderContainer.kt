package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.FontCategory
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.domain.model.FontFamily as ReaderFontFamily
import com.emptycastle.novery.domain.model.TextAlign as ReaderTextAlign

@Composable
fun ReaderContainer(
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
        mapFontFamily(uiState.settings.fontFamily)
    }

    val textAlign = remember(uiState.settings.textAlign) {
        mapTextAlign(uiState.settings.textAlign)
    }

    val fontWeight = remember(uiState.settings.fontWeight) {
        mapFontWeight(uiState.settings.fontWeight)
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    val topPadding = if (uiState.showControls) 100.dp else statusBarPadding.calculateTopPadding() + 16.dp

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
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
                        fontWeight = fontWeight,
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
                        error = item.error,
                        colors = colors,
                        onRetry = { onRetryChapter(item.chapterIndex) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// FONT MAPPING FUNCTIONS
// =============================================================================

/**
 * Maps the domain FontFamily to Compose FontFamily.
 * This handles both system fonts and custom fonts.
 */
private fun mapFontFamily(readerFontFamily: ReaderFontFamily): FontFamily {
    return when (readerFontFamily) {
        // System fonts
        ReaderFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
        ReaderFontFamily.SYSTEM_SERIF -> FontFamily.Serif
        ReaderFontFamily.SYSTEM_SANS -> FontFamily.SansSerif
        ReaderFontFamily.SYSTEM_MONO -> FontFamily.Monospace

        // For custom fonts, we fall back to a category-appropriate system font
        // In a full implementation, you would load actual font resources here
        else -> when (readerFontFamily.category) {
            FontCategory.SYSTEM -> FontFamily.Default
            FontCategory.SERIF -> FontFamily.Serif
            FontCategory.SANS_SERIF -> FontFamily.SansSerif
            FontCategory.MONOSPACE -> FontFamily.Monospace
            FontCategory.HANDWRITING -> FontFamily.Cursive
            FontCategory.ACCESSIBILITY -> FontFamily.SansSerif
            FontCategory.SPECIALTY -> FontFamily.Serif
        }
    }
}

/**
 * Maps the domain TextAlign to Compose TextAlign.
 */
private fun mapTextAlign(readerTextAlign: ReaderTextAlign): TextAlign {
    return when (readerTextAlign) {
        ReaderTextAlign.LEFT -> TextAlign.Start
        ReaderTextAlign.RIGHT -> TextAlign.End
        ReaderTextAlign.CENTER -> TextAlign.Center
        ReaderTextAlign.JUSTIFY -> TextAlign.Justify
    }
}

/**
 * Maps the domain FontWeight to Compose FontWeight.
 */
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