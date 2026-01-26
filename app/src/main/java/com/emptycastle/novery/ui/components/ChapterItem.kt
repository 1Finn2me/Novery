// com/emptycastle/novery/ui/components/ChapterItem.kt
package com.emptycastle.novery.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.theme.Orange200
import com.emptycastle.novery.ui.theme.Orange300
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Orange600
import com.emptycastle.novery.ui.theme.Success
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900

/**
 * Chapter list item component with support for:
 * - Read/unread status
 * - Downloaded status
 * - Selection mode
 * - Last read indicator
 * - Long press for selection
 * - Release date display
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterItem(
    chapter: Chapter,
    index: Int,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    isDownloaded: Boolean = false,
    isLastRead: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val backgroundColor = when {
        isLastRead -> Orange600.copy(alpha = 0.15f)
        isSelectionMode && isSelected -> Orange600.copy(alpha = 0.2f)
        isRead -> Zinc900.copy(alpha = 0.1f)
        else -> Zinc900.copy(alpha = 0.3f)
    }

    val borderColor = when {
        isLastRead -> Orange500
        isSelectionMode && isSelected -> Orange500
        isRead -> Zinc800.copy(alpha = 0.2f)
        else -> Zinc800.copy(alpha = 0.5f)
    }

    val textColor = when {
        isLastRead -> Orange200
        isSelectionMode && isSelected -> Orange200
        isRead -> Zinc600
        else -> Zinc300
    }

    val subtextColor = when {
        isLastRead -> Orange300.copy(alpha = 0.7f)
        isSelectionMode && isSelected -> Orange300.copy(alpha = 0.7f)
        isRead -> Zinc700
        else -> Zinc500
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last read indicator
        if (isLastRead && !isSelectionMode) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Last read",
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
                tint = Orange500
            )
        }

        // Chapter info column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Chapter name
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLastRead) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Release date
            chapter.dateOfRelease?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                // Selection checkbox
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) Orange500 else Zinc600
                )
            } else {
                // Download/Read status
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        modifier = Modifier.size(16.dp),
                        tint = if (isRead) Success.copy(alpha = 0.5f) else Success
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Online only",
                        modifier = Modifier.size(16.dp),
                        tint = if (isRead) Zinc700 else Zinc500
                    )
                }
            }
        }
    }
}

/**
 * Simplified chapter item without long press support
 * Use for contexts where selection mode is not needed
 */
@Composable
fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    isDownloaded: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    ChapterItem(
        chapter = chapter,
        index = 0,
        onTap = onClick,
        onLongPress = {},
        modifier = modifier,
        isRead = isRead,
        isDownloaded = isDownloaded,
        isLastRead = false,
        isSelectionMode = isSelectionMode,
        isSelected = isSelected
    )
}

/**
 * Chapter item skeleton for loading state
 */
@Composable
fun ChapterItemSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .shimmerEffect()
    )
}