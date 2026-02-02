package com.emptycastle.novery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Two-stage swipe to delete states - PUBLIC so it can be used across files
 */
enum class SwipeDeleteState {
    Default,    // Normal state
    Primed,     // First swipe done, delete button visible
    Deleting    // Second swipe confirmed, about to delete
}

/**
 * Reusable two-stage swipe to delete container
 * - First swipe past threshold: reveals delete button, content stays offset
 * - Second swipe past threshold OR tap delete button: triggers deletion
 * - Tap on content when primed: resets to default state
 * - Swipe right when primed: resets to default state
 *
 * @param onDelete Called when deletion is confirmed (second swipe or tap delete button)
 * @param deleteButtonWidth Width of the delete button area
 * @param shape Shape for clipping
 * @param modifier Modifier for the container
 * @param content Content composable that receives current swipe state and a reset callback
 */
@Composable
fun TwoStageSwipeToDelete(
    onDelete: () -> Unit,
    deleteButtonWidth: Dp = 80.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    modifier: Modifier = Modifier,
    content: @Composable (swipeState: SwipeDeleteState, onResetSwipe: () -> Unit) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    val swipeThreshold = deleteButtonWidthPx * 0.6f

    var swipeState by remember { mutableStateOf(SwipeDeleteState.Default) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val baseOffset = when (swipeState) {
        SwipeDeleteState.Default -> 0f
        SwipeDeleteState.Primed -> -deleteButtonWidthPx
        SwipeDeleteState.Deleting -> -deleteButtonWidthPx * 2
    }

    val animatedOffset by animateFloatAsState(
        targetValue = baseOffset + dragOffset,
        animationSpec = tween(if (dragOffset != 0f) 0 else 200),
        label = "swipe_offset"
    )

    val visibleOffset = animatedOffset.coerceIn(-deleteButtonWidthPx * 2, 0f)

    val draggableState = rememberDraggableState { delta ->
        val newOffset = dragOffset + delta

        when (swipeState) {
            SwipeDeleteState.Default -> {
                dragOffset = newOffset.coerceIn(-deleteButtonWidthPx * 1.5f, 0f)

                if (dragOffset < -swipeThreshold && !hasTriggeredHaptic) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hasTriggeredHaptic = true
                } else if (dragOffset > -swipeThreshold) {
                    hasTriggeredHaptic = false
                }
            }
            SwipeDeleteState.Primed -> {
                dragOffset = newOffset.coerceIn(-deleteButtonWidthPx * 1.5f, deleteButtonWidthPx)

                if (dragOffset < -swipeThreshold && !hasTriggeredHaptic) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hasTriggeredHaptic = true
                } else if (dragOffset > -swipeThreshold) {
                    hasTriggeredHaptic = false
                }
            }
            SwipeDeleteState.Deleting -> { /* No dragging while deleting */ }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
    ) {
        // Delete button background
        DeleteButtonBackground(
            onClick = {
                if (swipeState == SwipeDeleteState.Primed) {
                    swipeState = SwipeDeleteState.Deleting
                    onDelete()
                }
            },
            revealProgress = (-visibleOffset / deleteButtonWidthPx).coerceIn(0f, 1f),
            isPrimed = swipeState == SwipeDeleteState.Primed,
            width = deleteButtonWidth,
            shape = shape,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(visibleOffset.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        when (swipeState) {
                            SwipeDeleteState.Default -> {
                                if (dragOffset < -swipeThreshold) {
                                    swipeState = SwipeDeleteState.Primed
                                }
                                dragOffset = 0f
                                hasTriggeredHaptic = false
                            }
                            SwipeDeleteState.Primed -> {
                                if (dragOffset < -swipeThreshold) {
                                    swipeState = SwipeDeleteState.Deleting
                                    onDelete()
                                } else if (dragOffset > swipeThreshold) {
                                    swipeState = SwipeDeleteState.Default
                                }
                                dragOffset = 0f
                                hasTriggeredHaptic = false
                            }
                            SwipeDeleteState.Deleting -> {
                                dragOffset = 0f
                            }
                        }
                    }
                )
        ) {
            content(swipeState) {
                swipeState = SwipeDeleteState.Default
                dragOffset = 0f
            }
        }
    }
}

@Composable
private fun DeleteButtonBackground(
    onClick: () -> Unit,
    revealProgress: Float,
    isPrimed: Boolean,
    width: Dp,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPrimed -> MaterialTheme.colorScheme.error
            revealProgress > 0.3f -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        },
        animationSpec = tween(150),
        label = "delete_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isPrimed -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(150),
        label = "delete_content"
    )

    val scale by animateFloatAsState(
        targetValue = if (revealProgress > 0.5f) 1f else 0.8f,
        animationSpec = tween(150),
        label = "delete_scale"
    )

    Surface(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clip(shape),
        color = backgroundColor,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = if (isPrimed) "Tap to delete" else "Delete",
                    modifier = Modifier.size((22 * scale).dp),
                    tint = contentColor.copy(alpha = revealProgress.coerceIn(0.5f, 1f))
                )
                Text(
                    text = if (isPrimed) "Tap to\nDelete" else "Delete",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isPrimed) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor.copy(alpha = revealProgress.coerceIn(0.5f, 1f)),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}