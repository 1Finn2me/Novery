package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emptycastle.novery.domain.model.ScrollMode
import com.emptycastle.novery.ui.screens.reader.model.ReaderUiState
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors

/**
 * Container that switches between continuous scroll and paged reading modes.
 */
@Composable
fun ReaderModeContainer(
    uiState: ReaderUiState,
    colors: ReaderColors,
    listState: LazyListState,
    onPageChanged: (chapterIndex: Int, chapterUrl: String, chapterName: String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetryChapter: (Int) -> Unit,
    onToggleControls: () -> Unit
) {
    val scrollMode = uiState.settings.scrollMode

    // Track initial page for paged mode based on current scroll position
    var initialPageIndex by remember { mutableIntStateOf(0) }

    // When switching to paged mode, calculate the initial page
    LaunchedEffect(scrollMode, uiState.displayItems) {
        if (scrollMode != ScrollMode.CONTINUOUS && uiState.displayItems.isNotEmpty()) {
            // Use current scroll position to determine starting page
            // This is approximate since pagination hasn't happened yet
            initialPageIndex = listState.firstVisibleItemIndex / 3 // Rough estimate
        }
    }

    when (scrollMode) {
        ScrollMode.CONTINUOUS -> {
            // Standard continuous scroll
            ReaderContainer(
                uiState = uiState,
                colors = colors,
                listState = listState,
                onPrevious = onPrevious,
                onNext = onNext,
                onBack = onBack,
                onRetryChapter = onRetryChapter
            )
        }

        ScrollMode.PAGED, ScrollMode.PAGED_VERTICAL -> {
            // Paged reading mode
            PagedReaderContainer(
                uiState = uiState,
                colors = colors,
                initialPage = initialPageIndex,
                onPageChanged = { pageIndex, chapterIndex ->
                    val chapter = uiState.allChapters.getOrNull(chapterIndex)
                    if (chapter != null) {
                        onPageChanged(chapterIndex, chapter.url, chapter.name)
                    }
                },
                onPrevious = onPrevious,
                onNext = onNext,
                onBack = onBack,
                onRetryChapter = onRetryChapter,
                onTapCenter = onToggleControls
            )
        }
    }
}