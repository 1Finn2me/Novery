// com/emptycastle/novery/ui/screens/details/components/ChapterItem.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.screens.details.util.DetailsColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun ChapterItem(
    chapter: Chapter,
    index: Int,
    isRead: Boolean,
    isDownloaded: Boolean,
    isLastRead: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeToRead: (() -> Unit)? = null,
    onSwipeToDownload: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Swipe state
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val maxSwipe = with(density) { 100.dp.toPx() }

    // Animation states
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipe_offset"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            isLastRead -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            isRead -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "bg_color"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primary
            isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "text_color"
    )

    val secondaryTextColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(200),
        label = "secondary_text_color"
    )

    val itemScale by animateFloatAsState(
        targetValue = if (isSelectionMode && isSelected) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "item_scale"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isSelectionMode) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkbox_scale"
    )

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selected_scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isSelectionMode && isSelected -> 4.dp
            isLastRead -> 2.dp
            else -> 0.dp
        },
        animationSpec = tween(200),
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        // Swipe action backgrounds
        if (!isSelectionMode && (onSwipeToRead != null || onSwipeToDownload != null)) {
            SwipeActionBackground(
                offsetX = animatedOffset,
                swipeThreshold = swipeThreshold,
                isRead = isRead,
                isDownloaded = isDownloaded
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .scale(itemScale)
                .pointerInput(isSelectionMode, onSwipeToRead, onSwipeToDownload) {
                    if (!isSelectionMode && (onSwipeToRead != null || onSwipeToDownload != null)) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > swipeThreshold && onSwipeToRead != null -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeToRead()
                                    }
                                    offsetX < -swipeThreshold && onSwipeToDownload != null -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeToDownload()
                                    }
                                }
                                offsetX = 0f
                            },
                            onDragCancel = { offsetX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = offsetX + dragAmount
                                offsetX = newOffset.coerceIn(-maxSwipe, maxSwipe)

                                if (offsetX.absoluteValue >= swipeThreshold * 0.9f &&
                                    (offsetX - dragAmount).absoluteValue < swipeThreshold * 0.9f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                }
                .pointerInput(isSelectionMode) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() }
                    )
                },
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            shadowElevation = elevation,
            border = when {
                isLastRead -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                isSelectionMode && isSelected -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                else -> null
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Selection checkbox with animation
                    SelectionCheckbox(
                        isVisible = checkboxScale > 0.01f,
                        isSelected = isSelected,
                        selectedScale = selectedScale
                    )

                    // Last read indicator
                    LastReadIndicator(
                        isVisible = isLastRead && !isSelectionMode
                    )

                    // Chapter info
                    ChapterInfo(
                        chapter = chapter,
                        isLastRead = isLastRead,
                        isSelectionMode = isSelectionMode,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side status icons
                ChapterStatusIcons(
                    isDownloaded = isDownloaded,
                    isRead = isRead,
                    isSelectionMode = isSelectionMode
                )
            }
        }
    }
}

@Composable
private fun SwipeActionBackground(
    offsetX: Float,
    swipeThreshold: Float,
    isRead: Boolean,
    isDownloaded: Boolean
) {
    val leftProgress = (offsetX / swipeThreshold).coerceIn(0f, 1f)
    val rightProgress = (-offsetX / swipeThreshold).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left action (mark as read/unread)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (isRead) DetailsColors.Warning.copy(alpha = leftProgress * 0.25f)
                    else DetailsColors.Success.copy(alpha = leftProgress * 0.25f)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .graphicsLayer {
                        alpha = leftProgress
                        scaleX = 0.6f + leftProgress * 0.4f
                        scaleY = 0.6f + leftProgress * 0.4f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isRead) Icons.Outlined.VisibilityOff else Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isRead) DetailsColors.Warning else DetailsColors.Success
                )
                Text(
                    text = if (isRead) "Unread" else "Read",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRead) DetailsColors.Warning else DetailsColors.Success,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right action (download/delete)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (isDownloaded) Color.Red.copy(alpha = rightProgress * 0.25f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = rightProgress * 0.25f)
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .graphicsLayer {
                        alpha = rightProgress
                        scaleX = 0.6f + rightProgress * 0.4f
                        scaleY = 0.6f + rightProgress * 0.4f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isDownloaded) "Delete" else "Download",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDownloaded) Color.Red else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isDownloaded) Icons.Filled.Delete else Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDownloaded) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckbox(
    isVisible: Boolean,
    isSelected: Boolean,
    selectedScale: Float
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .scale(selectedScale)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent
                    )
                    .then(
                        if (!isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LastReadIndicator(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(3.dp, 24.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun ChapterInfo(
    chapter: Chapter,
    isLastRead: Boolean,
    isSelectionMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Chapter name
        Text(
            text = chapter.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isLastRead) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Secondary info row
        val hasSecondaryInfo = chapter.dateOfRelease != null || (isLastRead && !isSelectionMode)

        AnimatedVisibility(
            visible = hasSecondaryInfo,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Release date with icon
                chapter.dateOfRelease?.let { date ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = secondaryTextColor
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }

                // Separator dot
                if (chapter.dateOfRelease != null && isLastRead && !isSelectionMode) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                }

                // Continue reading hint
                if (isLastRead && !isSelectionMode) {
                    Text(
                        text = "Continue reading",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterStatusIcons(
    isDownloaded: Boolean,
    isRead: Boolean,
    isSelectionMode: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Download status indicator
        AnimatedContent(
            targetState = isDownloaded,
            transitionSpec = {
                (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
            },
            label = "download_status"
        ) { downloaded ->
            if (downloaded) {
                Surface(
                    shape = CircleShape,
                    color = DetailsColors.Success.copy(alpha = if (isRead) 0.12f else 0.18f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp),
                        tint = DetailsColors.Success.copy(alpha = if (isRead) 0.6f else 1f)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = "Not downloaded",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isRead) 0.3f else 0.4f
                    )
                )
            }
        }

        // Read status (only when not in selection mode)
        AnimatedVisibility(
            visible = !isSelectionMode && isRead,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Read",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}