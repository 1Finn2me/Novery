package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Utility functions for scroll operations in the reader.
 */
object ScrollUtils {

    /**
     * Scrolls by approximately one page (85% of viewport).
     */
    fun scrollByPage(
        scope: CoroutineScope,
        listState: LazyListState,
        forward: Boolean,
        smoothScroll: Boolean,
        reduceMotion: Boolean,
        sensitivity: Float,
        totalItems: Int
    ) {
        scope.launch {
            val viewportHeight = listState.layoutInfo.viewportEndOffset -
                    listState.layoutInfo.viewportStartOffset

            // Adjust scroll amount based on sensitivity
            val baseScrollPercent = 0.85f
            val adjustedPercent = baseScrollPercent * sensitivity.coerceIn(0.5f, 1.5f)
            val scrollAmount = (viewportHeight * adjustedPercent)

            try {
                if (smoothScroll && !reduceMotion) {
                    listState.animateScrollBy(if (forward) scrollAmount else -scrollAmount)
                } else {
                    // For instant scroll, calculate target item
                    val itemsPerPage = calculateItemsPerPage(listState, scrollAmount)
                    val targetIndex = if (forward) {
                        (listState.firstVisibleItemIndex + itemsPerPage)
                            .coerceAtMost(maxOf(0, totalItems - 1))
                    } else {
                        (listState.firstVisibleItemIndex - itemsPerPage).coerceAtLeast(0)
                    }
                    listState.scrollToItem(targetIndex)
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Scrolls by a specific number of items.
     */
    fun scrollByItems(
        scope: CoroutineScope,
        listState: LazyListState,
        itemCount: Int,
        forward: Boolean,
        smoothScroll: Boolean,
        reduceMotion: Boolean,
        totalItems: Int
    ) {
        scope.launch {
            val targetIndex = if (forward) {
                (listState.firstVisibleItemIndex + itemCount)
                    .coerceAtMost(maxOf(0, totalItems - 1))
            } else {
                (listState.firstVisibleItemIndex - itemCount).coerceAtLeast(0)
            }

            try {
                if (smoothScroll && !reduceMotion) {
                    listState.animateScrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(targetIndex)
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Scrolls to a specific percentage of the content.
     */
    fun scrollToProgress(
        scope: CoroutineScope,
        listState: LazyListState,
        progress: Float,
        smoothScroll: Boolean,
        reduceMotion: Boolean,
        totalItems: Int
    ) {
        scope.launch {
            val targetIndex = (totalItems * progress.coerceIn(0f, 1f)).roundToInt()
                .coerceIn(0, maxOf(0, totalItems - 1))

            try {
                if (smoothScroll && !reduceMotion) {
                    listState.animateScrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(targetIndex)
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Estimates how many items fit in the given scroll amount.
     */
    private fun calculateItemsPerPage(listState: LazyListState, scrollAmount: Float): Int {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 5

        val averageItemHeight = visibleItems.map { it.size }.average()
        return if (averageItemHeight > 0) {
            (scrollAmount / averageItemHeight).roundToInt().coerceAtLeast(1)
        } else {
            5
        }
    }

    /**
     * Calculates the current scroll progress (0.0 - 1.0).
     */
    fun calculateProgress(listState: LazyListState): Float {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems == 0) return 0f

        val firstVisibleIndex = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset

        // Estimate progress based on visible item position
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val firstItemSize = visibleItems.firstOrNull()?.size ?: 1

        val itemProgress = if (firstItemSize > 0) {
            firstVisibleOffset.toFloat() / firstItemSize
        } else {
            0f
        }

        return ((firstVisibleIndex + itemProgress) / totalItems).coerceIn(0f, 1f)
    }
}

/**
 * Extension function to animate scroll by a pixel amount.
 */
suspend fun LazyListState.animateScrollBy(pixels: Float) {
    val targetOffset = firstVisibleItemScrollOffset + pixels.toInt()

    // Calculate approximate target item
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val averageItemHeight = visibleItems.map { it.size }.average().toInt()
    if (averageItemHeight <= 0) return

    var remainingPixels = pixels.toInt()
    var targetIndex = firstVisibleItemIndex
    var targetItemOffset = firstVisibleItemScrollOffset

    if (pixels > 0) {
        // Scrolling forward
        while (remainingPixels > 0 && targetIndex < layoutInfo.totalItemsCount - 1) {
            val itemHeight = visibleItems.find { it.index == targetIndex }?.size ?: averageItemHeight
            val remainingInCurrentItem = itemHeight - targetItemOffset

            if (remainingPixels >= remainingInCurrentItem) {
                remainingPixels -= remainingInCurrentItem
                targetIndex++
                targetItemOffset = 0
            } else {
                targetItemOffset += remainingPixels
                remainingPixels = 0
            }
        }
    } else {
        // Scrolling backward
        remainingPixels = -remainingPixels
        while (remainingPixels > 0 && targetIndex > 0) {
            if (remainingPixels <= targetItemOffset) {
                targetItemOffset -= remainingPixels
                remainingPixels = 0
            } else {
                remainingPixels -= targetItemOffset
                targetIndex--
                val itemHeight = visibleItems.find { it.index == targetIndex }?.size ?: averageItemHeight
                targetItemOffset = itemHeight
            }
        }
        if (targetIndex == 0 && remainingPixels > 0) {
            targetItemOffset = 0
        }
    }

    animateScrollToItem(targetIndex, targetItemOffset.coerceAtLeast(0))
}