package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.ui.screens.details.util.DetailsColors
import com.emptycastle.novery.util.RatingUtils
import java.text.DecimalFormat

@Composable
fun StatsRow(
    chapterCount: Int,
    readCount: Int,
    downloadedCount: Int,
    rating: Int?,
    peopleVoted: Int?,
    views: Int? = null,
    providerName: String? = null
) {
    // Get rating format from preferences
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsState()
    val ratingFormat = appSettings.ratingFormat

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Outlined.MenuBook,
                value = chapterCount.toString(),
                label = "Chapters",
                color = MaterialTheme.colorScheme.primary
            )

            StatDivider()

            StatItem(
                icon = Icons.Outlined.Visibility,
                value = readCount.toString(),
                label = "Read",
                color = if (readCount > 0) DetailsColors.Success else MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatDivider()

            StatItem(
                icon = Icons.Default.DownloadDone,
                value = downloadedCount.toString(),
                label = "Saved",
                color = if (downloadedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show views if available
            if (views != null) {
                StatDivider()

                StatItem(
                    icon = Icons.Outlined.RemoveRedEye,
                    value = formatViewCount(views),
                    label = "Views",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (rating != null) {
                StatDivider()

                // Format rating using user's preferred format
                val formattedRating = RatingUtils.format(rating, ratingFormat, providerName)

                StatItem(
                    icon = Icons.Default.Star,
                    value = formattedRating,
                    label = if (peopleVoted != null) formatVoteCount(peopleVoted) else "Rating",
                    color = DetailsColors.Warning
                )
            }
        }
    }
}

/**
 * Format view count to human-readable format
 */
private fun formatViewCount(count: Int): String {
    return when {
        count >= 1_000_000_000 -> {
            val value = count / 1_000_000_000.0
            "${DecimalFormat("#.#").format(value)}B"
        }
        count >= 1_000_000 -> {
            val value = count / 1_000_000.0
            "${DecimalFormat("#.#").format(value)}M"
        }
        count >= 1_000 -> {
            val value = count / 1_000.0
            "${DecimalFormat("#.#").format(value)}K"
        }
        else -> count.toString()
    }
}

/**
 * Format vote count for the rating label
 */
private fun formatVoteCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            val value = count / 1_000_000.0
            "${DecimalFormat("#.#").format(value)}M votes"
        }
        count >= 1_000 -> {
            val value = count / 1_000.0
            "${DecimalFormat("#.#").format(value)}K votes"
        }
        else -> "$count votes"
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            }
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}