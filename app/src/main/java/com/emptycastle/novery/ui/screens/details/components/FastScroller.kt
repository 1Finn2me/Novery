// com/emptycastle/novery/ui/screens/details/components/FastScroller.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScrollerContainer(
    listState: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var showScrollbar by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }
    var scrollbarTopOffset by remember { mutableFloatStateOf(0f) }

    // Calculate thumb position from list state
    val thumbPosition by remember {
        derivedStateOf {
            if (totalItems == 0) return@derivedStateOf 0f
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val scrollProgress = firstVisibleIndex.toFloat() / totalItems.coerceAtLeast(1)
            scrollProgress.coerceIn(0f, 1f)
        }
    }

    // Calculate current visible index
    val currentIndex by remember {
        derivedStateOf {
            if (isDragging) {
                (dragOffset / containerHeight * totalItems).toInt().coerceIn(0, totalItems - 1)
            } else {
                listState.firstVisibleItemIndex
            }
        }
    }

    // Trigger haptic feedback on significant index changes while dragging
    LaunchedEffect(currentIndex, isDragging) {
        if (isDragging && currentIndex != lastHapticIndex) {
            if (currentIndex % 10 == 0 || currentIndex == 0 || currentIndex == totalItems - 1) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastHapticIndex = currentIndex
        }
    }

    // Animate visibility
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (isDragging || showScrollbar) 1f else 0f,
        animationSpec = tween(200),
        label = "scrollbar_alpha"
    )

    // Show scrollbar when scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            showScrollbar = true
        } else {
            delay(1500)
            if (!isDragging) {
                showScrollbar = false
            }
        }
    }

    // Calculate thumb Y position
    val thumbY = if (isDragging) {
        dragOffset
    } else {
        thumbPosition * containerHeight
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        content()

        // Fast scroller
        if (totalItems > 20) {
            // Position indicator bubble (horizontal) - placed outside the constrained scrollbar box
            AnimatedVisibility(
                visible = isDragging,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            (-36).dp.roundToPx(),
                            (scrollbarTopOffset + thumbY - with(density) { 14.dp.roundToPx() }).roundToInt()
                        )
                    }
                    .wrapContentSize(unbounded = true, align = Alignment.CenterEnd)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ch. ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${currentIndex + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 100.dp, horizontal = 2.dp)
                    .width(20.dp)
                    .graphicsLayer { alpha = scrollbarAlpha }
                    .onGloballyPositioned { coordinates ->
                        containerHeight = coordinates.size.height.toFloat()
                        scrollbarTopOffset = coordinates.localToRoot(androidx.compose.ui.geometry.Offset.Zero).y
                    }
            ) {
                // Track
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val progress = (offset.y / containerHeight).coerceIn(0f, 1f)
                                val targetIndex = (progress * totalItems).toInt().coerceIn(0, totalItems - 1)
                                scope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        }
                )

                // Thumb
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbY.roundToInt() - with(density) { 20.dp.roundToPx() }) }
                        .size(20.dp, 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDragging)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    showScrollbar = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    lastHapticIndex = -1
                                    scope.launch {
                                        delay(1500)
                                        if (!listState.isScrollInProgress) {
                                            showScrollbar = false
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    lastHapticIndex = -1
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset = (dragOffset + dragAmount.y).coerceIn(0f, containerHeight)
                                    val progress = dragOffset / containerHeight
                                    val targetIndex = (progress * totalItems).toInt().coerceIn(0, totalItems - 1)
                                    scope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}