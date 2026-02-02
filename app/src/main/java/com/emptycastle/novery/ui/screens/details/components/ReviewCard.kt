// com/emptycastle/novery/ui/screens/details/components/ReviewCard.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.emptycastle.novery.domain.model.ReviewScore
import com.emptycastle.novery.domain.model.UserReview
import java.util.Locale

/**
 * Review style for different UI treatments
 */
enum class ReviewStyle {
    DETAILED,  // Has ratings, advanced scores - Royal Road/Webnovel
    SIMPLE     // Simple comments - NovelFire
}

@Composable
fun ReviewCard(
    review: UserReview,
    style: ReviewStyle = ReviewStyle.DETAILED,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isSpoilerRevealed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Pinned indicator
            if (review.isPinned) {
                PinnedBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Header row: Avatar + User info + Rating (if detailed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                UserAvatar(
                    avatarUrl = review.avatarUrl,
                    username = review.username ?: "Anonymous"
                )

                // User info column
                Column(modifier = Modifier.weight(1f)) {
                    // Username row with badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = review.username ?: "Anonymous",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Level badge
                        review.userLevel?.let { level ->
                            LevelBadge(level = level)
                        }

                        // Author/Mod badge
                        if (review.isAuthor) {
                            RoleBadge(text = "Author", color = MaterialTheme.colorScheme.primary)
                        } else if (review.isModerator) {
                            RoleBadge(text = "Mod", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    // Time and edited indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        review.time?.takeIf { it.isNotBlank() }?.let { time ->
                            Text(
                                text = formatTimeDisplay(time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        if (review.isEdited) {
                            Text(
                                text = "• Edited",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Rating badge (for detailed style)
                if (style == ReviewStyle.DETAILED && review.overallScore != null) {
                    RatingBadge(score = review.overallScore)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reply indicator (inline, not nested)
            if (review.parentUsername != null) {
                ReplyIndicator(
                    parentUsername = review.parentUsername,
                    parentContent = review.parentContentPreview
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title (for detailed reviews only)
            if (style == ReviewStyle.DETAILED && !review.title.isNullOrBlank() &&
                !review.title.startsWith("Read") && !review.title.startsWith("Reply to")) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Advanced scores (for detailed reviews)
            if (style == ReviewStyle.DETAILED && review.advancedScores.isNotEmpty()) {
                AdvancedScoresRow(scores = review.advancedScores)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content
            ReviewContent(
                content = review.content,
                isSpoiler = review.isSpoiler,
                isSpoilerRevealed = isSpoilerRevealed,
                onRevealSpoiler = { isSpoilerRevealed = true },
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Engagement stats
            EngagementRow(
                likeCount = review.likeCount,
                dislikeCount = review.dislikeCount,
                replyCount = review.replyCount,
                isLikedByAuthor = review.isLikedByAuthor,
                showDislike = style == ReviewStyle.SIMPLE
            )
        }
    }
}

@Composable
private fun PinnedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Pinned",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun UserAvatar(
    avatarUrl: String?,
    username: String,
    modifier: Modifier = Modifier
) {
    val initial = username.firstOrNull()?.uppercase() ?: "?"

    val gradientColors = remember(username) {
        val hash = username.hashCode()
        val hue1 = (hash and 0xFF) / 255f * 360f
        val hue2 = ((hash shr 8) and 0xFF) / 255f * 360f
        listOf(
            Color.hsl(hue1, 0.5f, 0.6f),
            Color.hsl(hue2, 0.4f, 0.5f)
        )
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LevelBadge(level: Int) {
    val color = when {
        level >= 15 -> Color(0xFFFFD700)
        level >= 10 -> Color(0xFFC0C0C0)
        level >= 5 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "Lv.$level",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun RoleBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RatingBadge(score: Int) {
    val starValue = score / 200f
    val ratingColor = getRatingColor(starValue)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ratingColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ratingColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = ratingColor
            )
            Text(
                text = String.format(Locale.US, "%.1f", starValue),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ratingColor
            )
        }
    }
}

@Composable
private fun ReplyIndicator(
    parentUsername: String,
    parentContent: String?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = "Replying to @$parentUsername",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!parentContent.isNullOrBlank()) {
                    Text(
                        text = parentContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedScoresRow(scores: List<ReviewScore>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        scores.take(5).forEach { score ->
            ScoreChip(
                category = score.category,
                score = score.score
            )
        }
    }
}

@Composable
private fun ScoreChip(category: String, score: Int) {
    val starValue = score / 200f
    val scoreColor = getRatingColor(starValue)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "%.1f", starValue),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}

@Composable
private fun ReviewContent(
    content: String,
    isSpoiler: Boolean,
    isSpoilerRevealed: Boolean,
    onRevealSpoiler: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val cleanContent = remember(content) { cleanHtmlContent(content) }
    val isLongContent = cleanContent.length > 280 || cleanContent.count { it == '\n' } > 3
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow

    Column {
        Box {
            // Spoiler blur
            val contentModifier = if (isSpoiler && !isSpoilerRevealed) {
                Modifier.blur(6.dp)
            } else {
                Modifier
            }

            Text(
                text = cleanContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = contentModifier
            )

            // Spoiler overlay
            if (isSpoiler && !isSpoilerRevealed) {
                SpoilerOverlay(onReveal = onRevealSpoiler)
            }

            // Gradient fade for long content
            if (!isExpanded && isLongContent && (!isSpoiler || isSpoilerRevealed)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = 0.8f),
                                    surfaceColor
                                )
                            )
                        )
                )
            }
        }

        // Expand/collapse button
        AnimatedVisibility(
            visible = isLongContent && (!isSpoiler || isSpoilerRevealed),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TextButton(
                onClick = onToggleExpand,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SpoilerOverlay(onReveal: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onReveal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Spoiler • Tap to reveal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EngagementRow(
    likeCount: Int,
    dislikeCount: Int,
    replyCount: Int,
    isLikedByAuthor: Boolean,
    showDislike: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Likes
        if (likeCount > 0 || isLikedByAuthor) {
            EngagementItem(
                icon = Icons.Filled.ThumbUp,
                count = likeCount,
                color = if (isLikedByAuthor)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                isHighlighted = isLikedByAuthor
            )
        }

        // Dislikes (for simple style)
        if (showDislike && dislikeCount > 0) {
            EngagementItem(
                icon = Icons.Filled.ThumbDown,
                count = dislikeCount,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Reply count
        if (replyCount > 0) {
            EngagementItem(
                icon = Icons.AutoMirrored.Filled.Reply,
                count = replyCount,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EngagementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    color: Color,
    isHighlighted: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

// Utility functions
private fun cleanHtmlContent(content: String): String {
    return content
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<p>"), "")
        .replace(Regex("</p>"), "\n\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("\n{3,}"), "\n\n")
        .replace(Regex(" {2,}"), " ")
        .trim()
}

private fun formatTimeDisplay(time: String): String {
    return when {
        time.matches(Regex("^\\d+[hdwmy]$")) -> "${time.dropLast(1)}${time.last()} ago"
        time.matches(Regex("^\\d+mth$")) -> "${time.removeSuffix("mth")}mo ago"
        time.contains("ago") -> time
        else -> time
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
    count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
    else -> count.toString()
}

private fun getRatingColor(starValue: Float): Color = when {
    starValue >= 4.5f -> Color(0xFF2E7D32)
    starValue >= 4.0f -> Color(0xFF43A047)
    starValue >= 3.5f -> Color(0xFF7CB342)
    starValue >= 3.0f -> Color(0xFFFBC02D)
    starValue >= 2.5f -> Color(0xFFFFA000)
    starValue >= 2.0f -> Color(0xFFFF7043)
    else -> Color(0xFFE53935)
}