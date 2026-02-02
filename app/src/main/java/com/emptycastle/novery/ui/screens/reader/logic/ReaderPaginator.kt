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
                // Chapter headers are typically ~150-200dp
                160.dp
            }

            is ReaderDisplayItem.Segment -> {
                val text = item.segment.text
                val charCount = text.length

                // Estimate characters per line based on viewport width and font size
                val charsPerLine = estimateCharsPerLine(config.viewportWidth, config.fontSize)
                val lineCount = (charCount / charsPerLine).coerceAtLeast(1)

                // Calculate height: lines * lineHeight + paragraph spacing
                val lineHeightDp = (config.fontSize * config.lineHeight).dp
                val paragraphSpacing = (config.fontSize * config.paragraphSpacing * 0.5f).dp

                (lineHeightDp * lineCount) + paragraphSpacing
            }

            is ReaderDisplayItem.Image -> {
                // Images have variable height but we estimate based on typical display
                // The actual height is constrained between 100dp and 400dp in ChapterImageItem
                250.dp
            }

            is ReaderDisplayItem.ChapterDivider -> {
                // Chapter dividers are typically ~250-300dp
                280.dp
            }

            is ReaderDisplayItem.LoadingIndicator -> 200.dp
            is ReaderDisplayItem.ErrorIndicator -> 200.dp
        }
    }

    /**
     * Estimates characters per line based on viewport width and font size.
     */
    private fun estimateCharsPerLine(viewportWidth: Dp, fontSize: Int): Int {
        // Rough estimate: average character width is ~0.5 * fontSize
        val avgCharWidth = fontSize * 0.5f
        val usableWidth = viewportWidth.value - 48 // Account for margins
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

        // Available height for content (minus top/bottom margins)
        val availableHeight = config.viewportHeight - (config.marginVertical * 2).dp - 100.dp

        displayItems.forEachIndexed { index, item ->
            val itemHeight = estimateItemHeight(item, config)

            // Track chapter changes
            val itemChapterIndex = when (item) {
                is ReaderDisplayItem.ChapterHeader -> item.chapterIndex
                is ReaderDisplayItem.Segment -> item.chapterIndex
                is ReaderDisplayItem.Image -> item.chapterIndex
                is ReaderDisplayItem.ChapterDivider -> item.chapterIndex
                is ReaderDisplayItem.LoadingIndicator -> item.chapterIndex
                is ReaderDisplayItem.ErrorIndicator -> item.chapterIndex
            }

            // Check if this starts a new chapter
            if (itemChapterIndex != currentChapterIndex) {
                // If we have content, save current page first
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

            // Check if item fits on current page
            if (currentHeight + itemHeight > availableHeight && currentPageItems.isNotEmpty()) {
                // Save current page and start new one
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

            // Add item to current page
            currentPageItems.add(PageItem(displayItem = item, displayIndex = index))
            currentHeight += itemHeight
        }

        // Add final page
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