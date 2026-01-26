// com/emptycastle/novery/ui/screens/details/components/ChaptersTabContent.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.screens.details.ChapterFilter
import com.emptycastle.novery.ui.screens.details.DetailsUiState

@Composable
fun ChaptersTabContent(
    listState: LazyListState,
    uiState: DetailsUiState,
    filteredChapters: List<Chapter>,
    novelUrl: String,
    providerName: String,
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = if (uiState.isSelectionMode) 200.dp else 0.dp
        )
    ) {
        // Chapter list header with filters
        item(key = "chapter_header") {
            ChapterListHeader(
                chapterCount = uiState.novelDetails?.chapters?.size ?: 0,
                filteredCount = filteredChapters.size,
                isDescending = uiState.isChapterSortDescending,
                isSelectionMode = uiState.isSelectionMode,
                currentFilter = uiState.chapterFilter,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.chapterSearchQuery,
                unreadCount = uiState.unreadCount,
                downloadedCount = uiState.downloadedCount,
                notDownloadedCount = uiState.notDownloadedCount,
                onToggleSort = onToggleSort,
                onFilterChange = onFilterChange,
                onToggleSearch = onToggleSearch,
                onSearchQueryChange = onSearchQueryChange,
                onEnableSelection = onEnableSelection
            )
        }

        // Chapter list or empty message
        if (filteredChapters.isEmpty()) {
            item(key = "empty_chapters") {
                EmptyChaptersMessage(
                    filter = uiState.chapterFilter,
                    hasSearch = uiState.chapterSearchQuery.isNotBlank()
                )
            }
        } else {
            itemsIndexed(
                items = filteredChapters,
                key = { _, chapter -> chapter.url }
            ) { index, chapter ->
                ChapterItem(
                    chapter = chapter,
                    index = index,
                    isRead = uiState.readChapters.contains(chapter.url),
                    isDownloaded = uiState.downloadedChapters.contains(chapter.url),
                    isLastRead = chapter.url == uiState.lastReadChapterUrl,
                    isSelectionMode = uiState.isSelectionMode,
                    isSelected = uiState.selectedChapters.contains(chapter.url),
                    onTap = {
                        if (uiState.isSelectionMode) {
                            onHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleChapterSelection(index, chapter.url)
                        } else {
                            onChapterClick(chapter.url, novelUrl, providerName)
                        }
                    },
                    onLongPress = {
                        onHapticFeedback(HapticFeedbackType.LongPress)
                        if (uiState.isSelectionMode) {
                            onSelectRange(index)
                        } else {
                            onEnableSelectionWithChapter(chapter.url)
                        }
                    }
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