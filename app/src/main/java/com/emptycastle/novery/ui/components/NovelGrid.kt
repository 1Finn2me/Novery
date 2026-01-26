// com/emptycastle/novery/ui/components/NovelGrid.kt

package com.emptycastle.novery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.ui.theme.NoveryTheme

/**
 * Data class for novel grid items with additional display info
 */
data class NovelGridItem(
    val novel: Novel,
    val newChapterCount: Int = 0,
    val readingStatus: ReadingStatus? = null,
    val lastReadChapter: String? = null
)

/**
 * Grid of novel cards
 */
@Composable
fun NovelGrid(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    onNovelLongClick: ((Novel) -> Unit)? = null,
    showApiName: Boolean = false,
    columns: Int = 2
) {
    val dimensions = NoveryTheme.dimensions

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.gridPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
    ) {
        items(
            items = items,
            key = { it.novel.url }
        ) { item ->
            NovelCard(
                novel = item.novel,
                onClick = { onNovelClick(item.novel) },
                onLongClick = onNovelLongClick?.let { { it(item.novel) } },
                newChapterCount = item.newChapterCount,
                readingStatus = item.readingStatus,
                lastReadChapter = item.lastReadChapter,
                showApiName = showApiName
            )
        }
    }
}

/**
 * Grid with loading skeletons
 */
@Composable
fun NovelGridSkeleton(
    count: Int = 10,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    val dimensions = NoveryTheme.dimensions

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.gridPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
        userScrollEnabled = false
    ) {
        items(count) {
            NovelCardSkeleton()
        }
    }
}

/**
 * Paginated novel grid with load more
 */
@Composable
fun PaginatedNovelGrid(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    hasMore: Boolean = true,
    showApiName: Boolean = false,
    columns: Int = 2
) {
    val gridState = rememberLazyGridState()
    val dimensions = NoveryTheme.dimensions

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensions.gridPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
    ) {
        items(
            items = items,
            key = { it.novel.url }
        ) { item ->
            NovelCard(
                novel = item.novel,
                onClick = { onNovelClick(item.novel) },
                newChapterCount = item.newChapterCount,
                readingStatus = item.readingStatus,
                lastReadChapter = item.lastReadChapter,
                showApiName = showApiName
            )
        }

        // Loading indicator at bottom
        if (isLoading) {
            items(columns) {
                NovelCardSkeleton()
            }
        }

        // Load more trigger
        if (hasMore && !isLoading) {
            item(span = { GridItemSpan(columns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    SecondaryButton(
                        text = "Load More",
                        onClick = onLoadMore
                    )
                }
            }
        }
    }
}