package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A NestedScrollConnection that constrains scrolling to keep the TTS-highlighted SENTENCE visible.
 *
 * Unlike the previous implementation that bounded by entire paragraphs, this version:
 * - Tracks the sentence position WITHIN the paragraph
 * - Allows more scrolling since sentences are smaller than paragraphs
 * - Only constrains when the specific sentence would go off-screen
 */
class TTSBoundedScrollConnection(
    private val listState: LazyListState
) : NestedScrollConnection {

    private val stateFlow = MutableStateFlow(ConnectionState())

    private data class ConnectionState(
        val highlightedIndex: Int = -1,
        val isActive: Boolean = false,
        val topPadding: Int = 0,
        val bottomPadding: Int = 0,
        // Sentence bounds relative to segment item top
        val sentenceTopOffset: Float = 0f,
        val sentenceBottomOffset: Float = 0f,
        // Extra margin to keep sentence visible (not right at edge)
        val visibilityMargin: Int = 50
    )

    fun updateState(
        highlightedIndex: Int,
        isActive: Boolean,
        topPadding: Int = 0,
        bottomPadding: Int = 0,
        sentenceTopOffset: Float = 0f,
        sentenceBottomOffset: Float = 0f,
        visibilityMargin: Int = 50
    ) {
        stateFlow.update {
            it.copy(
                highlightedIndex = highlightedIndex,
                isActive = isActive,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                sentenceTopOffset = sentenceTopOffset,
                sentenceBottomOffset = sentenceBottomOffset,
                visibilityMargin = visibilityMargin
            )
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val state = stateFlow.value

        if (!state.isActive || state.highlightedIndex < 0) {
            return Offset.Zero // Allow all scrolling when not active
        }

        val layoutInfo = listState.layoutInfo
        val highlightedItem = layoutInfo.visibleItemsInfo.find {
            it.index == state.highlightedIndex
        }

        // If highlighted item is not currently visible, allow scroll
        // The auto-scroll will bring it back into view
        if (highlightedItem == null) {
            return Offset.Zero
        }

        // Calculate effective viewport bounds (accounting for overlays/padding)
        val viewportTop = layoutInfo.viewportStartOffset + state.topPadding + state.visibilityMargin
        val viewportBottom = layoutInfo.viewportEndOffset - state.bottomPadding - state.visibilityMargin

        // Calculate SENTENCE position in viewport coordinates
        // Item offset is the item's top relative to viewport
        val sentenceTopInViewport = highlightedItem.offset + state.sentenceTopOffset.toInt()
        val sentenceBottomInViewport = highlightedItem.offset + state.sentenceBottomOffset.toInt()

        // If we don't have valid sentence bounds, fall back to item bounds (but with some buffer)
        val effectiveTop = if (state.sentenceBottomOffset > state.sentenceTopOffset) {
            sentenceTopInViewport
        } else {
            // Fallback: use item top with some buffer
            highlightedItem.offset + (highlightedItem.size * 0.1f).toInt()
        }

        val effectiveBottom = if (state.sentenceBottomOffset > state.sentenceTopOffset) {
            sentenceBottomInViewport
        } else {
            // Fallback: use reduced item bottom
            highlightedItem.offset + (highlightedItem.size * 0.3f).toInt()
        }

        // Calculate scroll bounds to keep SENTENCE visible
        // Positive available.y = user swiping down = content moves down
        // Negative available.y = user swiping up = content moves up

        // Max we can scroll content DOWN (positive delta) before sentence bottom goes below viewport bottom
        val maxScrollDown = (viewportBottom - effectiveBottom).toFloat().coerceAtLeast(0f)

        // Max we can scroll content UP (negative delta) before sentence top goes above viewport top
        val maxScrollUp = (viewportTop - effectiveTop).toFloat().coerceAtMost(0f)

        val scrollDelta = available.y
        val clampedDelta = scrollDelta.coerceIn(maxScrollUp, maxScrollDown)
        val consumed = scrollDelta - clampedDelta

        return Offset(0f, consumed)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return Offset.Zero
    }
}

/**
 * Remember and configure a TTSBoundedScrollConnection with sentence-level tracking
 */
@Composable
fun rememberTTSBoundedScrollConnection(
    listState: LazyListState,
    highlightedIndex: Int,
    isActive: Boolean,
    topPadding: Int = 0,
    bottomPadding: Int = 0,
    sentenceTopOffset: Float = 0f,
    sentenceBottomOffset: Float = 0f
): NestedScrollConnection {
    val connection = remember(listState) {
        TTSBoundedScrollConnection(listState)
    }

    LaunchedEffect(highlightedIndex, isActive, topPadding, bottomPadding, sentenceTopOffset, sentenceBottomOffset) {
        connection.updateState(
            highlightedIndex = highlightedIndex,
            isActive = isActive,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            sentenceTopOffset = sentenceTopOffset,
            sentenceBottomOffset = sentenceBottomOffset
        )
    }

    return connection
}