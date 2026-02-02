package com.emptycastle.novery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.DisplayMode
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.theme.NoveryTheme

/**
 * Data class for novel grid/list items with additional display info
 */
data class NovelGridItem(
    val novel: Novel,
    val newChapterCount: Int = 0,
    val readingStatus: ReadingStatus? = null,
    val lastReadChapter: String? = null
)

/**
 * Universal novel display component that supports both grid and list modes
 */
@Composable
fun NovelDisplay(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: DisplayMode = DisplayMode.GRID,
    density: UiDensity = UiDensity.DEFAULT,
    onNovelLongClick: ((Novel) -> Unit)? = null,
    showApiName: Boolean = false,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    when (displayMode) {
        DisplayMode.GRID -> {
            NovelGrid(
                items = items,
                onNovelClick = onNovelClick,
                modifier = modifier,
                density = density,
                onNovelLongClick = onNovelLongClick,
                showApiName = showApiName,
                columns = columns,
                contentPadding = contentPadding
            )
        }
        DisplayMode.LIST -> {
            NovelList(
                items = items,
                onNovelClick = onNovelClick,
                modifier = modifier,
                density = density,
                onNovelLongClick = onNovelLongClick,
                showApiName = showApiName,
                contentPadding = contentPadding
            )
        }
    }
}

/**
 * Grid of novel cards
 */
@Composable
fun NovelGrid(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT,
    onNovelLongClick: ((Novel) -> Unit)? = null,
    showApiName: Boolean = false,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val dimensions = NoveryTheme.dimensions

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
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
                showApiName = showApiName,
                density = density
            )
        }
    }
}

/**
 * List of novel items
 */
@Composable
fun NovelList(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT,
    onNovelLongClick: ((Novel) -> Unit)? = null,
    showApiName: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val dimensions = NoveryTheme.dimensions
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
    ) {
        items(
            items = items,
            key = { it.novel.url }
        ) { item ->
            NovelListItem(
                novel = item.novel,
                onClick = { onNovelClick(item.novel) },
                onLongClick = onNovelLongClick?.let { { it(item.novel) } },
                newChapterCount = item.newChapterCount,
                readingStatus = item.readingStatus,
                lastReadChapter = item.lastReadChapter,
                showApiName = showApiName,
                density = density
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
    displayMode: DisplayMode = DisplayMode.GRID,
    density: UiDensity = UiDensity.DEFAULT,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val dimensions = NoveryTheme.dimensions

    when (displayMode) {
        DisplayMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                userScrollEnabled = false
            ) {
                items(count) {
                    NovelCardSkeleton(density = density)
                }
            }
        }
        DisplayMode.LIST -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                userScrollEnabled = false
            ) {
                items(count) {
                    NovelListItemSkeleton(density = density)
                }
            }
        }
    }
}

/**
 * Paginated novel display with load more
 */
@Composable
fun PaginatedNovelDisplay(
    items: List<NovelGridItem>,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    displayMode: DisplayMode = DisplayMode.GRID,
    density: UiDensity = UiDensity.DEFAULT,
    isLoading: Boolean = false,
    hasMore: Boolean = true,
    showApiName: Boolean = false,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val dimensions = NoveryTheme.dimensions

    when (displayMode) {
        DisplayMode.GRID -> {
            val gridState = rememberLazyGridState()

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
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
                        showApiName = showApiName,
                        density = density
                    )
                }

                if (isLoading) {
                    items(columns) {
                        NovelCardSkeleton(density = density)
                    }
                }

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
        DisplayMode.LIST -> {
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
            ) {
                items(
                    items = items,
                    key = { it.novel.url }
                ) { item ->
                    NovelListItem(
                        novel = item.novel,
                        onClick = { onNovelClick(item.novel) },
                        newChapterCount = item.newChapterCount,
                        readingStatus = item.readingStatus,
                        lastReadChapter = item.lastReadChapter,
                        showApiName = showApiName,
                        density = density
                    )
                }

                if (isLoading) {
                    items(3) {
                        NovelListItemSkeleton(density = density)
                    }
                }

                if (hasMore && !isLoading) {
                    item {
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
    }
}