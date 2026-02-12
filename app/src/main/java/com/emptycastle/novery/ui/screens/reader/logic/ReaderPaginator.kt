package com.emptycastle.novery.ui.screens.reader.logic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem

/**
 * Represents a single page of content.
 */
data class ReaderPage(
    val pageIndex: Int,
    val items: List<PageItem>,
    val chapterIndex: Int,
    val isChapterStart: Boolean = false,
    val isChapterEnd: Boolean = false
)

/**
 * An item within a page, with its original display item reference.
 */
data class PageItem(
    val displayItem: ReaderDisplayItem,
    val displayIndex: Int
)

/**
 * Configuration for pagination.
 */
data class PaginationConfig(
    val fontSize: Int,
    val lineHeight: Float,
    val paragraphSpacing: Float,
    val marginVertical: Int,
    val viewportHeight: Dp,
    val viewportWidth: Dp
)

/**
 * Handles splitting display items into pages.
 */
object ReaderPaginator {

    /**
     * Estimates the height of a display item in dp.
     */
    fun estimateItemHeight(
        item: ReaderDisplayItem,
        config: PaginationConfig
    ): Dp {
        return when (item) {
            is ReaderDisplayItem.ChapterHeader -> {
                160.dp
            }

            is ReaderDisplayItem.Segment -> {
                val text = item.segment.text
                val charCount = text.length

                val charsPerLine = estimateCharsPerLine(config.viewportWidth, config.fontSize)
                val lineCount = (charCount / charsPerLine).coerceAtLeast(1)

                val lineHeightDp = (config.fontSize * config.lineHeight).dp
                val paragraphSpacing = (config.fontSize * config.paragraphSpacing * 0.5f).dp

                (lineHeightDp * lineCount) + paragraphSpacing
            }

            is ReaderDisplayItem.Image -> {
                250.dp
            }

            is ReaderDisplayItem.HorizontalRule -> {
                50.dp
            }

            is ReaderDisplayItem.SceneBreak -> {
                80.dp
            }

            is ReaderDisplayItem.AuthorNote -> {
                // Author notes have header + content, estimate based on text length
                val text = item.authorNote.plainText
                val charCount = text.length
                val charsPerLine = estimateCharsPerLine(config.viewportWidth, config.fontSize - 1)
                val lineCount = (charCount / charsPerLine).coerceAtLeast(1)

                // Header height (~40dp) + content lines + padding
                val headerHeight = 44.dp
                val lineHeightDp = ((config.fontSize - 1) * config.lineHeight).dp
                val padding = 24.dp // top + bottom padding

                // For collapsed state, show ~2 lines preview
                // For expanded state, show full content
                // Estimate average as collapsed + some buffer
                (headerHeight + (lineHeightDp * 2.coerceAtMost(lineCount)) + padding)
            }

            is ReaderDisplayItem.Table -> {
                // Estimate table height based on number of rows
                val rowCount = item.table.rows.size
                val rowHeight = (config.fontSize * config.lineHeight + 16).dp // line + padding
                val headerHeight = if (item.table.caption != null) 30.dp else 0.dp
                val borderPadding = 24.dp

                headerHeight + (rowHeight * rowCount) + borderPadding
            }

            is ReaderDisplayItem.List -> {
                // Estimate list height based on number of items
                val itemCount = countListItems(item.list.list)
                val itemHeight = (config.fontSize * config.lineHeight + 8).dp // line + spacing
                val padding = 16.dp

                (itemHeight * itemCount) + padding
            }

            is ReaderDisplayItem.ChapterDivider -> {
                280.dp
            }

            is ReaderDisplayItem.LoadingIndicator -> 200.dp
            is ReaderDisplayItem.ErrorIndicator -> 200.dp
        }
    }

    /**
     * Count total items in a list including nested items
     */
    private fun countListItems(list: ParsedList): Int {
        var count = 0
        for (item in list.items) {
            when (item) {
                is ListItemContent.TextContent -> count++
                is ListItemContent.NestedList -> count += countListItems(item.list)
            }
        }
        return count.coerceAtLeast(1)
    }

    /**
     * Estimates characters per line based on viewport width and font size.
     */
    private fun estimateCharsPerLine(viewportWidth: Dp, fontSize: Int): Int {
        val avgCharWidth = fontSize * 0.5f
        val usableWidth = viewportWidth.value - 48
        return (usableWidth / avgCharWidth).toInt().coerceAtLeast(20)
    }

    /**
     * Paginates display items into pages.
     */
    fun paginate(
        displayItems: List<ReaderDisplayItem>,
        config: PaginationConfig
    ): List<ReaderPage> {
        if (displayItems.isEmpty()) return emptyList()

        val pages = mutableListOf<ReaderPage>()
        var currentPageItems = mutableListOf<PageItem>()
        var currentHeight = 0.dp
        var currentChapterIndex = -1
        var isChapterStart = false

        val availableHeight = config.viewportHeight - (config.marginVertical * 2).dp - 100.dp

        displayItems.forEachIndexed { index, item ->
            val itemHeight = estimateItemHeight(item, config)

            val itemChapterIndex = getChapterIndex(item)

            if (itemChapterIndex != currentChapterIndex) {
                if (currentPageItems.isNotEmpty()) {
                    pages.add(
                        ReaderPage(
                            pageIndex = pages.size,
                            items = currentPageItems.toList(),
                            chapterIndex = currentChapterIndex,
                            isChapterStart = isChapterStart,
                            isChapterEnd = true
                        )
                    )
                    currentPageItems = mutableListOf()
                    currentHeight = 0.dp
                }
                currentChapterIndex = itemChapterIndex
                isChapterStart = true
            }

            if (currentHeight + itemHeight > availableHeight && currentPageItems.isNotEmpty()) {
                pages.add(
                    ReaderPage(
                        pageIndex = pages.size,
                        items = currentPageItems.toList(),
                        chapterIndex = currentChapterIndex,
                        isChapterStart = isChapterStart,
                        isChapterEnd = false
                    )
                )
                currentPageItems = mutableListOf()
                currentHeight = 0.dp
                isChapterStart = false
            }

            currentPageItems.add(PageItem(displayItem = item, displayIndex = index))
            currentHeight += itemHeight
        }

        if (currentPageItems.isNotEmpty()) {
            pages.add(
                ReaderPage(
                    pageIndex = pages.size,
                    items = currentPageItems.toList(),
                    chapterIndex = currentChapterIndex,
                    isChapterStart = isChapterStart,
                    isChapterEnd = true
                )
            )
        }

        return pages
    }

    /**
     * Extract chapter index from any display item type
     */
    private fun getChapterIndex(item: ReaderDisplayItem): Int {
        return when (item) {
            is ReaderDisplayItem.ChapterHeader -> item.chapterIndex
            is ReaderDisplayItem.Segment -> item.chapterIndex
            is ReaderDisplayItem.Image -> item.chapterIndex
            is ReaderDisplayItem.HorizontalRule -> item.chapterIndex
            is ReaderDisplayItem.SceneBreak -> item.chapterIndex
            is ReaderDisplayItem.AuthorNote -> item.chapterIndex
            is ReaderDisplayItem.Table -> item.chapterIndex
            is ReaderDisplayItem.List -> item.chapterIndex
            is ReaderDisplayItem.ChapterDivider -> item.chapterIndex
            is ReaderDisplayItem.LoadingIndicator -> item.chapterIndex
            is ReaderDisplayItem.ErrorIndicator -> item.chapterIndex
        }
    }

    /**
     * Finds the page index for a given display item index.
     */
    fun findPageForDisplayIndex(
        pages: List<ReaderPage>,
        displayIndex: Int
    ): Int {
        return pages.indexOfFirst { page ->
            page.items.any { it.displayIndex == displayIndex }
        }.coerceAtLeast(0)
    }

    /**
     * Finds the page index for a given chapter index.
     */
    fun findPageForChapter(
        pages: List<ReaderPage>,
        chapterIndex: Int
    ): Int {
        return pages.indexOfFirst { page ->
            page.chapterIndex == chapterIndex && page.isChapterStart
        }.coerceAtLeast(0)
    }
}

/**
 * Composable to remember paginated content.
 */
@Composable
fun rememberPaginatedContent(
    displayItems: List<ReaderDisplayItem>,
    fontSize: Int,
    lineHeight: Float,
    paragraphSpacing: Float,
    marginVertical: Int,
    viewportHeight: Dp,
    viewportWidth: Dp
): List<ReaderPage> {
    val config = remember(
        fontSize, lineHeight, paragraphSpacing,
        marginVertical, viewportHeight, viewportWidth
    ) {
        PaginationConfig(
            fontSize = fontSize,
            lineHeight = lineHeight,
            paragraphSpacing = paragraphSpacing,
            marginVertical = marginVertical,
            viewportHeight = viewportHeight,
            viewportWidth = viewportWidth
        )
    }

    return remember(displayItems, config) {
        ReaderPaginator.paginate(displayItems, config)
    }
}