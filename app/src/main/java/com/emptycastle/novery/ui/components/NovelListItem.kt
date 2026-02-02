package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.theme.StatusCompleted
import com.emptycastle.novery.ui.theme.StatusDROPPED
import com.emptycastle.novery.ui.theme.StatusOnHold
import com.emptycastle.novery.ui.theme.StatusPlanToRead
import com.emptycastle.novery.ui.theme.StatusReading

// ══════════════════════════════════════════════════════════════════════════════
// Design Tokens
// ══════════════════════════════════════════════════════════════════════════════

private object ListItemTokens {
    val CardShape = RoundedCornerShape(16.dp)
    val ImageShape = RoundedCornerShape(12.dp)
    val BadgeShape = RoundedCornerShape(8.dp)
    val PillShape = RoundedCornerShape(50)

    object Height {
        val Compact = 100.dp
        val Default = 120.dp
        val Comfortable = 140.dp
    }

    object ImageWidth {
        val Compact = 70.dp
        val Default = 85.dp
        val Comfortable = 100.dp
    }

    object Padding {
        val Compact = 10.dp
        val Default = 12.dp
        val Comfortable = 14.dp
        val Badge = 6.dp
    }

    object Elevation {
        val Resting = 2.dp
        val Pressed = 1.dp
        val Selected = 4.dp
        val Badge = 4.dp
    }

    object Animation {
        const val PressScale = 0.98f
        const val ShimmerDuration = 1400
    }

    val StatusDotSize = 10.dp
    val BadgeIconSize = 12.dp
}

// ══════════════════════════════════════════════════════════════════════════════
// Main List Item Component
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelListItem(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT,
    onLongClick: (() -> Unit)? = null,
    newChapterCount: Int = 0,
    readingStatus: ReadingStatus? = null,
    lastReadChapter: String? = null,
    showApiName: Boolean = false,
    isSelected: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) ListItemTokens.Animation.PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "list_item_scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> ListItemTokens.Elevation.Pressed
            isSelected -> ListItemTokens.Elevation.Selected
            else -> ListItemTokens.Elevation.Resting
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "list_item_elevation"
    )

    val cardHeight = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Height.Compact
        UiDensity.DEFAULT -> ListItemTokens.Height.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Height.Comfortable
    }

    val imageWidth = when (density) {
        UiDensity.COMPACT -> ListItemTokens.ImageWidth.Compact
        UiDensity.DEFAULT -> ListItemTokens.ImageWidth.Default
        UiDensity.COMFORTABLE -> ListItemTokens.ImageWidth.Comfortable
    }

    val contentPadding = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Padding.Compact
        UiDensity.DEFAULT -> ListItemTokens.Padding.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Padding.Comfortable
    }

    val semanticsLabel = buildString {
        append(novel.name)
        readingStatus?.let { append(", ${it.displayName()}") }
        if (newChapterCount > 0) append(", $newChapterCount new chapters")
        lastReadChapter?.let { append(", last read: $it") }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isSelected) {
                    Modifier.listItemBorder(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        cornerRadius = 16.dp
                    )
                } else Modifier
            )
            .semantics {
                contentDescription = semanticsLabel
                role = Role.Button
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        shape = ListItemTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image with overlay
            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .fillMaxHeight()
                    .clip(ListItemTokens.ImageShape)
            ) {
                ListItemCoverImage(
                    url = novel.posterUrl,
                    title = novel.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle vignette for badge contrast
                ListItemVignette(modifier = Modifier.fillMaxSize())

                // New chapters badge on image
                // FIX: Use fully qualified name to avoid RowScope/BoxScope conflict
                androidx.compose.animation.AnimatedVisibility(
                    visible = newChapterCount > 0,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(ListItemTokens.Padding.Badge),
                    enter = fadeIn() + scaleIn(
                        initialScale = 0.5f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = fadeOut() + scaleOut()
                ) {
                    ListNewChaptersBadge(
                        count = newChapterCount,
                        compact = density == UiDensity.COMPACT
                    )
                }

                // Status indicator at bottom-left of image
                // FIX: Use fully qualified name to avoid RowScope/BoxScope conflict
                androidx.compose.animation.AnimatedVisibility(
                    visible = readingStatus != null && density == UiDensity.COMPACT,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(ListItemTokens.Padding.Badge),
                    enter = fadeIn() + slideInHorizontally { -it },
                    exit = fadeOut() + slideOutHorizontally { -it }
                ) {
                    readingStatus?.let {
                        ListStatusDot(status = it)
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title
                    Text(
                        text = novel.name,
                        style = when (density) {
                            UiDensity.COMPACT -> MaterialTheme.typography.bodyMedium
                            UiDensity.DEFAULT -> MaterialTheme.typography.titleSmall
                            UiDensity.COMFORTABLE -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = when (density) {
                            UiDensity.COMPACT -> 16.sp
                            UiDensity.DEFAULT -> 18.sp
                            UiDensity.COMFORTABLE -> 22.sp
                        }
                    )

                    // Source
                    if (showApiName && novel.apiName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                            Text(
                                text = novel.apiName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom row: Status + Last read chapter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge (not in compact mode - it's shown on image)
                    AnimatedVisibility(
                        visible = readingStatus != null && density != UiDensity.COMPACT,
                        enter = fadeIn() + slideInHorizontally { -it },
                        exit = fadeOut() + slideOutHorizontally { -it }
                    ) {
                        readingStatus?.let {
                            ListStatusBadge(
                                status = it,
                                compact = false
                            )
                        }
                    }

                    if (readingStatus == null || density == UiDensity.COMPACT) {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Last read chapter with styled indicator
                    lastReadChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                        ListChapterProgress(
                            chapterName = chapter,
                            compact = density == UiDensity.COMPACT,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // Animated chevron indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = if (isPressed) 2.dp.toPx() else 0f
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isPressed) 0.8f else 0.6f
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Cover Image Component
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListItemCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    ) {
        val state = painter.state

        when {
            state is coil.compose.AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { alpha = 0.2f },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            state is coil.compose.AsyncImagePainter.State.Error || url.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = title.take(8),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 8.sp
                        )
                    }
                }
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Overlays
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListItemVignette(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = 0.15f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.2f)
                )
            )
        )
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Status Badges
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListStatusBadge(
    status: ReadingStatus,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusColor = remember(status) {
        when (status) {
            ReadingStatus.READING -> StatusReading
            ReadingStatus.COMPLETED -> StatusCompleted
            ReadingStatus.ON_HOLD -> StatusOnHold
            ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
            ReadingStatus.DROPPED -> StatusDROPPED
        }
    }

    if (compact) {
        ListStatusDot(status = status, modifier = modifier)
    } else {
        Surface(
            modifier = modifier,
            shape = ListItemTokens.BadgeShape,
            color = statusColor.copy(alpha = 0.15f),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Small dot indicator
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = status.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ListStatusDot(
    status: ReadingStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = remember(status) {
        when (status) {
            ReadingStatus.READING -> StatusReading
            ReadingStatus.COMPLETED -> StatusCompleted
            ReadingStatus.ON_HOLD -> StatusOnHold
            ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
            ReadingStatus.DROPPED -> StatusDROPPED
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .blur(4.dp, BlurredEdgeTreatment.Unbounded)
                    .background(statusColor.copy(alpha = 0.5f), CircleShape)
            )
            // Solid dot
            Box(
                modifier = Modifier
                    .size(ListItemTokens.StatusDotSize)
                    .shadow(2.dp, CircleShape)
                    .background(statusColor, CircleShape)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// New Chapters Badge
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListNewChaptersBadge(
    count: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val displayText = remember(count) {
        when {
            count > 99 -> "99+"
            else -> "+$count"
        }
    }

    // Subtle pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier.graphicsLayer { alpha = pulseAlpha },
        shape = if (compact) CircleShape else RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = ListItemTokens.Elevation.Badge
    ) {
        if (compact) {
            Box(
                modifier = Modifier.padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 8.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(ListItemTokens.BadgeIconSize),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Chapter Progress
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ListChapterProgress(
    chapterName: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Reading progress indicator dot
        Box(
            modifier = Modifier
                .size(if (compact) 4.dp else 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = chapterName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (compact) 10.sp else 11.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Skeleton Loading State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelListItemSkeleton(
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT
) {
    val cardHeight = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Height.Compact
        UiDensity.DEFAULT -> ListItemTokens.Height.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Height.Comfortable
    }

    val imageWidth = when (density) {
        UiDensity.COMPACT -> ListItemTokens.ImageWidth.Compact
        UiDensity.DEFAULT -> ListItemTokens.ImageWidth.Default
        UiDensity.COMFORTABLE -> ListItemTokens.ImageWidth.Comfortable
    }

    val contentPadding = when (density) {
        UiDensity.COMPACT -> ListItemTokens.Padding.Compact
        UiDensity.DEFAULT -> ListItemTokens.Padding.Default
        UiDensity.COMFORTABLE -> ListItemTokens.Padding.Comfortable
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = ListItemTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ListItemTokens.Elevation.Resting)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .fillMaxHeight()
                    .clip(ListItemTokens.ImageShape)
                    .shimmerEffect()
            )

            // Content placeholders
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Title line 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    // Title line 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    // Source
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status placeholder
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                    // Chapter placeholder
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }

            // Chevron placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Utilities
// ══════════════════════════════════════════════════════════════════════════════

private fun Modifier.listItemBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp
): Modifier = this.then(
    Modifier.drawWithCache {
        onDrawBehind {
            drawRoundRect(
                color = color,
                style = Stroke(width.toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
)
