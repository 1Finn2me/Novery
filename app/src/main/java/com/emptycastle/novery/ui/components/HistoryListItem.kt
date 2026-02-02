package com.emptycastle.novery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.util.formatRelativeTime

private object HistoryItemTokens {
    val CardShape = RoundedCornerShape(14.dp)
    val CompactCardShape = RoundedCornerShape(10.dp)
    val ImageShape = RoundedCornerShape(10.dp)
    val CompactImageShape = RoundedCornerShape(8.dp)

    val CoverWidth = 56.dp
    val CoverHeight = 80.dp
    val CompactCoverWidth = 44.dp
    val CompactCoverHeight = 62.dp

    val DeleteButtonWidth = 80.dp
    val CompactDeleteButtonWidth = 70.dp
}

/**
 * Standard History List Item with two-stage swipe-to-delete confirmation
 */
@Composable
fun HistoryListItem(
    item: HistoryItem,
    onContinueClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TwoStageSwipeToDelete(
        onDelete = onRemoveClick,
        deleteButtonWidth = HistoryItemTokens.DeleteButtonWidth,
        shape = HistoryItemTokens.CardShape,
        modifier = modifier
    ) { swipeState: SwipeDeleteState, onResetSwipe: () -> Unit ->
        HistoryCardContent(
            item = item,
            onContinueClick = onContinueClick,
            onRemoveClick = onRemoveClick,
            onItemClick = {
                if (swipeState == SwipeDeleteState.Primed) {
                    onResetSwipe()
                } else {
                    onItemClick()
                }
            },
            onLongClick = onLongClick,
            isCompact = false,
            isDeletePrimed = swipeState == SwipeDeleteState.Primed
        )
    }
}

/**
 * Compact version with two-stage swipe-to-delete confirmation
 */
@Composable
fun HistoryListItemCompact(
    item: HistoryItem,
    onContinueClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TwoStageSwipeToDelete(
        onDelete = onRemoveClick,
        deleteButtonWidth = HistoryItemTokens.CompactDeleteButtonWidth,
        shape = HistoryItemTokens.CompactCardShape,
        modifier = modifier
    ) { swipeState: SwipeDeleteState, onResetSwipe: () -> Unit ->
        HistoryCardContent(
            item = item,
            onContinueClick = onContinueClick,
            onRemoveClick = onRemoveClick,
            onItemClick = {
                if (swipeState == SwipeDeleteState.Primed) {
                    onResetSwipe()
                } else {
                    onItemClick()
                }
            },
            onLongClick = onLongClick,
            isCompact = true,
            isDeletePrimed = swipeState == SwipeDeleteState.Primed
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCardContent(
    item: HistoryItem,
    onContinueClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    isCompact: Boolean,
    isDeletePrimed: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val relativeTime = remember(item.timestamp) { formatRelativeTime(item.timestamp) }

    val cardColor by animateColorAsState(
        targetValue = if (isDeletePrimed) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(200),
        label = "card_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isDeletePrimed) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "border_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = if (isCompact) HistoryItemTokens.CompactCardShape else HistoryItemTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isDeletePrimed) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover thumbnail
            CoverThumbnail(
                imageUrl = item.novel.posterUrl,
                contentDescription = item.novel.name,
                isCompact = isCompact
            )

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.dp)
            ) {
                Text(
                    text = item.novel.name,
                    style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (isCompact) 18.sp else 20.sp
                )

                Text(
                    text = item.chapterName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    Text(
                        text = item.novel.apiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            // Action buttons - hide when primed
            if (!isDeletePrimed) {
                if (isCompact) {
                    FilledIconButton(
                        onClick = onContinueClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Continue reading",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onContinueClick,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Continue reading",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onRemoveClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove from history",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "← Swipe or tap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun CoverThumbnail(
    imageUrl: String?,
    contentDescription: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val width = if (isCompact) HistoryItemTokens.CompactCoverWidth else HistoryItemTokens.CoverWidth
    val height = if (isCompact) HistoryItemTokens.CompactCoverHeight else HistoryItemTokens.CoverHeight
    val shape = if (isCompact) HistoryItemTokens.CompactImageShape else HistoryItemTokens.ImageShape

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(if (isCompact) 18.dp else 24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height / 3)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
        )
    }
}