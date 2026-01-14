package com.emptycastle.novery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.theme.*

/**
 * Chapter list item component
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
    val backgroundColor = when {
        isSelectionMode && isSelected -> Orange600.copy(alpha = 0.2f)
        isRead -> Zinc900.copy(alpha = 0.1f)
        else -> Zinc900.copy(alpha = 0.3f)
    }

    val borderColor = when {
        isSelectionMode && isSelected -> Orange500
        isRead -> Zinc800.copy(alpha = 0.2f)
        else -> Zinc800.copy(alpha = 0.5f)
    }

    val textColor = when {
        isSelectionMode && isSelected -> Orange200
        isRead -> Zinc600
        else -> Zinc300
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter name
        Text(
            text = chapter.name,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

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