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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.screens.details.util.DetailsColors

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
    onLongPress: () -> Unit
) {
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .scale(itemScale)
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
                .padding(horizontal = 14.dp, vertical = 14.dp),
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
                    index = index,
                    isLastRead = isLastRead,
                    isRead = isRead,
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
                .padding(end = 14.dp)
                .scale(selectedScale)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
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
                        modifier = Modifier.size(16.dp),
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
                    .size(4.dp, 28.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun ChapterInfo(
    chapter: Chapter,
    index: Int,
    isLastRead: Boolean,
    isRead: Boolean,
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

        // Secondary info row - release date and continue reading hint
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

                // Separator dot when both date and continue reading are shown
                if (chapter.dateOfRelease != null && isLastRead && !isSelectionMode) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                }

                // "Continue reading" hint for last read
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Surface(
                shape = CircleShape,
                color = if (downloaded)
                    DetailsColors.Success.copy(alpha = if (isRead) 0.15f else 0.2f)
                else
                    Color.Transparent
            ) {
                Icon(
                    imageVector = if (downloaded) Icons.Default.DownloadDone else Icons.Outlined.CloudDownload,
                    contentDescription = if (downloaded) "Downloaded" else "Not downloaded",
                    modifier = Modifier
                        .padding(if (downloaded) 4.dp else 0.dp)
                        .size(if (downloaded) 16.dp else 18.dp),
                    tint = if (downloaded)
                        DetailsColors.Success.copy(alpha = if (isRead) 0.6f else 1f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
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
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}