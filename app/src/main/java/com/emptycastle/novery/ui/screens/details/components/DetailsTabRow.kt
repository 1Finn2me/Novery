// com/emptycastle/novery/ui/screens/details/components/DetailsTabRow.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.screens.details.DetailsTab

@Composable
fun DetailsTabRow(
    selectedTab: DetailsTab,
    onTabSelected: (DetailsTab) -> Unit,
    chapterCount: Int,
    relatedCount: Int,
    reviewCount: Int,
    hasReviewsSupport: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chapters Tab
            TabItem(
                title = "Chapters",
                icon = Icons.AutoMirrored.Filled.List,
                badge = if (chapterCount > 0) formatBadge(chapterCount) else null,
                isSelected = selectedTab == DetailsTab.CHAPTERS,
                onClick = { onTabSelected(DetailsTab.CHAPTERS) },
                modifier = Modifier.weight(1f)
            )

            // Related Tab
            TabItem(
                title = "Related",
                icon = Icons.Filled.AutoStories,
                badge = if (relatedCount > 0) formatBadge(relatedCount) else null,
                isSelected = selectedTab == DetailsTab.RELATED,
                onClick = { onTabSelected(DetailsTab.RELATED) },
                modifier = Modifier.weight(1f)
            )

            // Reviews Tab (only if supported)
            if (hasReviewsSupport) {
                TabItem(
                    title = "Reviews",
                    icon = Icons.Filled.RateReview,
                    badge = if (reviewCount > 0) formatBadge(reviewCount) else null,
                    isSelected = selectedTab == DetailsTab.REVIEWS,
                    onClick = { onTabSelected(DetailsTab.REVIEWS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatBadge(count: Int): String {
    return when {
        count > 999 -> "999+"
        count > 99 -> "${count / 100 * 100}+"
        else -> count.toString()
    }
}

@Composable
private fun TabItem(
    title: String,
    icon: ImageVector,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "tab_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "tab_content"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                // Badge
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-6).dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                shape = CircleShape
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}