// com/emptycastle/novery/ui/screens/details/components/ChaptersTabContent.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.screens.details.ChapterDisplayMode
import com.emptycastle.novery.ui.screens.details.ChapterFilter
import com.emptycastle.novery.ui.screens.details.ChaptersPerPage
import com.emptycastle.novery.ui.screens.details.DetailsUiState
import com.emptycastle.novery.ui.screens.details.PaginationState
import kotlinx.coroutines.launch

@Composable
fun ChaptersTabContent(
    listState: LazyListState,
    uiState: DetailsUiState,
    filteredChapters: List<Chapter>,
    novelUrl: String,
    providerName: String,
    displayMode: ChapterDisplayMode,
    paginationState: PaginationState,
    onChapterClick: (String, String, String) -> Unit,
    onHapticFeedback: (HapticFeedbackType) -> Unit,
    onToggleSort: () -> Unit,
    onFilterChange: (ChapterFilter) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEnableSelection: () -> Unit,
    onToggleChapterSelection: (Int, String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onEnableSelectionWithChapter: (String) -> Unit,
    onDisplayModeChange: (ChapterDisplayMode) -> Unit,
    onChaptersPerPageChange: (ChaptersPerPage) -> Unit,
    onPageChange: (Int) -> Unit,
    onJumpToFirstUnread: () -> Unit,
    onJumpToLastRead: () -> Unit,
    onSwipeToRead: ((String, Boolean) -> Unit)? = null,
    onSwipeToDownload: ((String, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Calculate displayed chapters based on display mode
    val displayedChapters = remember(filteredChapters, displayMode, paginationState) {
        when (displayMode) {
            ChapterDisplayMode.SCROLL -> filteredChapters
            ChapterDisplayMode.PAGINATED -> {
                val range = paginationState.getPageRange(filteredChapters.size)
                if (range.isEmpty()) {
                    emptyList()
                } else {
                    filteredChapters.subList(range.first, range.last.coerceAtMost(filteredChapters.size))
                }
            }
        }
    }

    // Show scroll to top FAB
    val showScrollToTop by remember {
        derivedStateOf {
            displayMode == ChapterDisplayMode.SCROLL &&
                    listState.firstVisibleItemIndex > 10
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (displayMode) {
            ChapterDisplayMode.SCROLL -> {
                // Scroll mode with fast scroller
                FastScrollerContainer(
                    listState = listState,
                    totalItems = filteredChapters.size
                ) {
                    ChapterListContent(
                        listState = listState,
                        uiState = uiState,
                        displayedChapters = displayedChapters,
                        filteredChapters = filteredChapters,
                        novelUrl = novelUrl,
                        providerName = providerName,
                        displayMode = displayMode,
                        paginationState = paginationState,
                        onChapterClick = onChapterClick,
                        onHapticFeedback = onHapticFeedback,
                        onToggleSort = onToggleSort,
                        onFilterChange = onFilterChange,
                        onToggleSearch = onToggleSearch,
                        onSearchQueryChange = onSearchQueryChange,
                        onEnableSelection = onEnableSelection,
                        onToggleChapterSelection = onToggleChapterSelection,
                        onSelectRange = onSelectRange,
                        onEnableSelectionWithChapter = onEnableSelectionWithChapter,
                        onDisplayModeChange = onDisplayModeChange,
                        onChaptersPerPageChange = onChaptersPerPageChange,
                        onPageChange = onPageChange,
                        onJumpToFirstUnread = onJumpToFirstUnread,
                        onJumpToLastRead = onJumpToLastRead,
                        onSwipeToRead = onSwipeToRead,
                        onSwipeToDownload = onSwipeToDownload
                    )
                }
            }

            ChapterDisplayMode.PAGINATED -> {
                ChapterListContent(
                    listState = listState,
                    uiState = uiState,
                    displayedChapters = displayedChapters,
                    filteredChapters = filteredChapters,
                    novelUrl = novelUrl,
                    providerName = providerName,
                    displayMode = displayMode,
                    paginationState = paginationState,
                    onChapterClick = onChapterClick,
                    onHapticFeedback = onHapticFeedback,
                    onToggleSort = onToggleSort,
                    onFilterChange = onFilterChange,
                    onToggleSearch = onToggleSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onEnableSelection = onEnableSelection,
                    onToggleChapterSelection = onToggleChapterSelection,
                    onSelectRange = onSelectRange,
                    onEnableSelectionWithChapter = onEnableSelectionWithChapter,
                    onDisplayModeChange = onDisplayModeChange,
                    onChaptersPerPageChange = onChaptersPerPageChange,
                    onPageChange = onPageChange,
                    onJumpToFirstUnread = onJumpToFirstUnread,
                    onJumpToLastRead = onJumpToLastRead,
                    onSwipeToRead = onSwipeToRead,
                    onSwipeToDownload = onSwipeToDownload
                )
            }
        }

        // Scroll to top FAB
        AnimatedVisibility(
            visible = showScrollToTop && !uiState.isSelectionMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}

@Composable
private fun ChapterListContent(
    listState: LazyListState,
    uiState: DetailsUiState,
    displayedChapters: List<Chapter>,
    filteredChapters: List<Chapter>,
    novelUrl: String,
    providerName: String,
    displayMode: ChapterDisplayMode,
    paginationState: PaginationState,
    onChapterClick: (String, String, String) -> Unit,
    onHapticFeedback: (HapticFeedbackType) -> Unit,
    onToggleSort: () -> Unit,
    onFilterChange: (ChapterFilter) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEnableSelection: () -> Unit,
    onToggleChapterSelection: (Int, String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onEnableSelectionWithChapter: (String) -> Unit,
    onDisplayModeChange: (ChapterDisplayMode) -> Unit,
    onChaptersPerPageChange: (ChaptersPerPage) -> Unit,
    onPageChange: (Int) -> Unit,
    onJumpToFirstUnread: () -> Unit,
    onJumpToLastRead: () -> Unit,
    onSwipeToRead: ((String, Boolean) -> Unit)?,
    onSwipeToDownload: ((String, Boolean) -> Unit)?
) {
    // Reset scroll when page changes in paginated mode
    LaunchedEffect(paginationState.currentPage, displayMode) {
        if (displayMode == ChapterDisplayMode.PAGINATED) {
            listState.scrollToItem(0)
        }
    }

    val totalCount = uiState.novelDetails?.chapters?.size ?: 0

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = if (uiState.isSelectionMode) 200.dp else 16.dp
        )
    ) {
        // Chapter list header with filters
        item(key = "chapter_header") {
            ChapterListHeader(
                chapterCount = totalCount,
                filteredCount = filteredChapters.size,
                isDescending = uiState.isChapterSortDescending,
                isSelectionMode = uiState.isSelectionMode,
                currentFilter = uiState.chapterFilter,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.chapterSearchQuery,
                unreadCount = uiState.unreadCount,
                downloadedCount = uiState.downloadedCount,
                notDownloadedCount = uiState.notDownloadedCount,
                displayMode = displayMode,
                paginationState = paginationState,
                onToggleSort = onToggleSort,
                onFilterChange = onFilterChange,
                onToggleSearch = onToggleSearch,
                onSearchQueryChange = onSearchQueryChange,
                onEnableSelection = onEnableSelection,
                onDisplayModeChange = onDisplayModeChange,
                onChaptersPerPageChange = onChaptersPerPageChange,
                onJumpToFirstUnread = onJumpToFirstUnread,
                onJumpToLastRead = onJumpToLastRead
            )
        }

        // Pagination controls (top) for paginated mode
        if (displayMode == ChapterDisplayMode.PAGINATED && filteredChapters.isNotEmpty()) {
            item(key = "pagination_top") {
                PaginationControls(
                    paginationState = paginationState,
                    totalChapters = filteredChapters.size,
                    onPageChange = onPageChange
                )
            }
        }

        // Chapter list or empty message
        if (displayedChapters.isEmpty()) {
            item(key = "empty_chapters") {
                EmptyChaptersMessage(
                    filter = uiState.chapterFilter,
                    hasSearch = uiState.chapterSearchQuery.isNotBlank()
                )
            }
        } else {
            itemsIndexed(
                items = displayedChapters,
                key = { _, chapter -> chapter.url }
            ) { displayIndex, chapter ->
                // Calculate the actual index in the full list
                val actualIndex = when (displayMode) {
                    ChapterDisplayMode.SCROLL -> displayIndex
                    ChapterDisplayMode.PAGINATED -> {
                        paginationState.getPageRange(filteredChapters.size).first + displayIndex
                    }
                }

                val isRead = uiState.readChapters.contains(chapter.url)
                val isDownloaded = uiState.downloadedChapters.contains(chapter.url)

                ChapterItem(
                    chapter = chapter,
                    index = actualIndex,
                    isRead = isRead,
                    isDownloaded = isDownloaded,
                    isLastRead = chapter.url == uiState.lastReadChapterUrl,
                    isSelectionMode = uiState.isSelectionMode,
                    isSelected = uiState.selectedChapters.contains(chapter.url),
                    onTap = {
                        if (uiState.isSelectionMode) {
                            onHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleChapterSelection(actualIndex, chapter.url)
                        } else {
                            onChapterClick(chapter.url, novelUrl, providerName)
                        }
                    },
                    onLongPress = {
                        onHapticFeedback(HapticFeedbackType.LongPress)
                        if (uiState.isSelectionMode) {
                            onSelectRange(actualIndex)
                        } else {
                            onEnableSelectionWithChapter(chapter.url)
                        }
                    },
                    onSwipeToRead = if (!uiState.isSelectionMode && onSwipeToRead != null) {
                        { onSwipeToRead(chapter.url, isRead) }
                    } else null,
                    onSwipeToDownload = if (!uiState.isSelectionMode && onSwipeToDownload != null) {
                        { onSwipeToDownload(chapter.url, isDownloaded) }
                    } else null
                )
            }
        }

        // Pagination controls (bottom) for paginated mode
        if (displayMode == ChapterDisplayMode.PAGINATED && filteredChapters.isNotEmpty()) {
            item(key = "pagination_bottom") {
                PaginationControls(
                    paginationState = paginationState,
                    totalChapters = filteredChapters.size,
                    onPageChange = onPageChange
                )
            }
        }

        // Bottom padding
        item(key = "bottom_spacer") {
            Spacer(
                modifier = Modifier
                    .height(100.dp)
                    .navigationBarsPadding()
            )
        }
    }
}