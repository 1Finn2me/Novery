package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs
import kotlin.math.sign

/**
 * Snap modes for scroll behavior
 */
enum class SnapMode {
    /** No snapping - free scroll */
    NONE,
    /** Snap to the start of items */
    SNAP_TO_ITEM,
    /** Snap to page boundaries */
    SNAP_TO_PAGE
}

/**
 * Creates a fling behavior that snaps to items or pages.
 */
@Composable
fun rememberSnapFlingBehavior(
    listState: LazyListState,
    snapMode: SnapMode = SnapMode.NONE,
    sensitivity: Float = 1.0f
): FlingBehavior {
    return remember(listState, snapMode, sensitivity) {
        when (snapMode) {
            SnapMode.NONE -> CustomSensitivityFlingBehaviorSimple(sensitivity)
            SnapMode.SNAP_TO_ITEM -> SnapToItemFlingBehavior(listState, sensitivity)
            SnapMode.SNAP_TO_PAGE -> SnapToPageFlingBehavior(listState, sensitivity)
        }
    }
}

/**
 * Simple sensitivity-adjusted fling behavior without snapping.
 */
private class CustomSensitivityFlingBehaviorSimple(
    private val sensitivity: Float
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val adjustedVelocity = initialVelocity * sensitivity

        if (abs(adjustedVelocity) < 50f) {
            return adjustedVelocity
        }

        var velocity = adjustedVelocity
        val friction = 0.015f / sensitivity.coerceAtLeast(0.5f)

        while (abs(velocity) > 50f) {
            val delta = velocity * 0.016f // ~60fps
            val consumed = scrollBy(delta)

            if (abs(delta - consumed) > 0.5f) {
                break
            }

            velocity *= (1f - friction)
        }

        return 0f
    }
}

/**
 * Fling behavior that snaps to item boundaries.
 */
private class SnapToItemFlingBehavior(
    private val listState: LazyListState,
    private val sensitivity: Float
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val adjustedVelocity = initialVelocity * sensitivity

        // First, do a regular fling
        var velocity = adjustedVelocity
        val friction = 0.02f / sensitivity.coerceAtLeast(0.5f)

        while (abs(velocity) > 100f) {
            val delta = velocity * 0.016f
            val consumed = scrollBy(delta)

            if (abs(delta - consumed) > 0.5f) {
                break
            }

            velocity *= (1f - friction)
        }

        // Then snap to nearest item
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isNotEmpty()) {
            val firstItem = visibleItems.first()
            val snapOffset = if (firstItem.offset > -firstItem.size / 2) {
                // Snap back to current item
                -firstItem.offset.toFloat()
            } else {
                // Snap to next item
                (firstItem.size + firstItem.offset).toFloat()
            }

            if (abs(snapOffset) > 1f) {
                // Animate snap
                val steps = 10
                val stepAmount = snapOffset / steps
                repeat(steps) {
                    scrollBy(stepAmount)
                }
            }
        }

        return 0f
    }
}

/**
 * Fling behavior that snaps to page boundaries.
 */
private class SnapToPageFlingBehavior(
    private val listState: LazyListState,
    private val sensitivity: Float
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val viewportHeight = listState.layoutInfo.viewportEndOffset -
                listState.layoutInfo.viewportStartOffset

        val adjustedVelocity = initialVelocity * sensitivity

        // Determine scroll direction and target
        val direction = sign(adjustedVelocity)
        val currentOffset = listState.firstVisibleItemScrollOffset

        // Calculate target based on velocity
        val pagesToScroll = when {
            abs(adjustedVelocity) > 2000f -> 2
            abs(adjustedVelocity) > 500f -> 1
            else -> 0
        }

        if (pagesToScroll == 0) {
            // Just snap to current position
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val firstItem = visibleItems.first()
                scrollBy(-firstItem.offset.toFloat())
            }
            return 0f
        }

        // Scroll by pages
        val targetScroll = direction * viewportHeight * pagesToScroll * 0.85f

        // Animate the page scroll
        val steps = 20
        val stepAmount = targetScroll / steps
        repeat(steps) {
            scrollBy(stepAmount)
        }

        // Final snap
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isNotEmpty()) {
            val firstItem = visibleItems.first()
            if (abs(firstItem.offset) > 10) {
                scrollBy(-firstItem.offset.toFloat())
            }
        }

        return 0f
    }
}