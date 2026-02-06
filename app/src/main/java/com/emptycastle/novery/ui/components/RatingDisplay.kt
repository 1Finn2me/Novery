package com.emptycastle.novery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.util.RatingColor
import com.emptycastle.novery.util.RatingDisplayStyle
import com.emptycastle.novery.util.RatingUtils

private val RatingExcellent = Color(0xFF22C55E)  // Green
private val RatingGood = Color(0xFF84CC16)       // Lime
private val RatingAverage = Color(0xFFF59E0B)    // Amber
private val RatingPoor = Color(0xFFF97316)       // Orange
private val RatingVeryPoor = Color(0xFFEF4444)   // Red

/**
 * Display rating as stars with optional value text
 */
@Composable
fun StarRating(
    rating: Int?,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    showValue: Boolean = true,
    size: RatingSize = RatingSize.MEDIUM,
    providerName: String? = null
) {
    if (rating == null || rating <= 0) return

    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsState()
    val ratingFormat = appSettings.ratingFormat

    val starValue = RatingUtils.to5Stars(rating)
    val fullStars = starValue.toInt()
    val hasHalfStar = (starValue - fullStars) >= 0.5f
    val emptyStars = maxStars - fullStars - (if (hasHalfStar) 1 else 0)

    val iconSize = when (size) {
        RatingSize.SMALL -> 12.dp
        RatingSize.MEDIUM -> 16.dp
        RatingSize.LARGE -> 20.dp
    }

    val textStyle = when (size) {
        RatingSize.SMALL -> MaterialTheme.typography.labelSmall
        RatingSize.MEDIUM -> MaterialTheme.typography.labelMedium
        RatingSize.LARGE -> MaterialTheme.typography.labelLarge
    }

    val color = getRatingDisplayColor(RatingUtils.getRatingColor(rating))

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Full stars
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = color
            )
        }

        // Half star
        if (hasHalfStar) {
            Icon(
                imageVector = Icons.Filled.StarHalf,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = color
            )
        }

        // Empty stars
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = color.copy(alpha = 0.3f)
            )
        }

        if (showValue) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = RatingUtils.format(rating, ratingFormat, providerName),
                style = textStyle,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

/**
 * Compact rating display with number and votes
 */
@Composable
fun CompactRating(
    rating: Int?,
    votes: Int? = null,
    modifier: Modifier = Modifier,
    providerName: String? = null,
    style: RatingDisplayStyle? = null // Optional override
) {
    if (rating == null || rating <= 0) return

    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsState()

    val displayStyle = style ?: RatingUtils.getDisplayStyle(appSettings.ratingFormat, providerName)
    val color = getRatingDisplayColor(RatingUtils.getRatingColor(rating))
    val formattedRating = RatingUtils.format(rating, displayStyle)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )

        Text(
            text = formattedRating,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )

        if (votes != null && votes > 0) {
            Text(
                text = "(${formatVotes(votes)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Rating chip for displaying in cards
 */
@Composable
fun RatingChip(
    rating: Int?,
    modifier: Modifier = Modifier,
    providerName: String? = null
) {
    if (rating == null || rating <= 0) return

    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsState()

    val color = getRatingDisplayColor(RatingUtils.getRatingColor(rating))
    val formattedRating = RatingUtils.format(rating, appSettings.ratingFormat, providerName)

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Text(
                text = formattedRating,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Formatted rating text without icon
 * Useful for inline rating display
 */
@Composable
fun RatingText(
    rating: Int?,
    modifier: Modifier = Modifier,
    providerName: String? = null,
    showColor: Boolean = true
) {
    if (rating == null || rating <= 0) return

    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsState()

    val color = if (showColor) {
        getRatingDisplayColor(RatingUtils.getRatingColor(rating))
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val formattedRating = RatingUtils.format(rating, appSettings.ratingFormat, providerName)

    Text(
        text = formattedRating,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

enum class RatingSize {
    SMALL, MEDIUM, LARGE
}

@Composable
private fun getRatingDisplayColor(ratingColor: RatingColor): Color {
    return when (ratingColor) {
        RatingColor.EXCELLENT -> RatingExcellent
        RatingColor.GOOD -> RatingGood
        RatingColor.AVERAGE -> RatingAverage
        RatingColor.POOR -> RatingPoor
        RatingColor.VERY_POOR -> RatingVeryPoor
        RatingColor.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatVotes(votes: Int): String {
    return when {
        votes >= 1_000_000 -> String.format("%.1fM", votes / 1_000_000f)
        votes >= 1_000 -> String.format("%.1fK", votes / 1_000f)
        else -> votes.toString()
    }
}