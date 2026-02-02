// com/emptycastle/novery/ui/components/NovelCard.kt

package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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

private object NovelCardTokens {
    // Shapes
    val CardCornerRadius = 16.dp
    val CardShape = RoundedCornerShape(CardCornerRadius)
    val ImageShapeTop = RoundedCornerShape(topStart = CardCornerRadius, topEnd = CardCornerRadius)
    val BadgeShape = RoundedCornerShape(8.dp)
    val PillShape = RoundedCornerShape(50)

    // Sizing
    val AspectRatio = 2f / 3f
    val BadgeIconSize = 14.dp
    val StatusDotSize = 8.dp

    // Spacing
    object Padding {
        val Compact = 8.dp
        val Default = 10.dp
        val Comfortable = 12.dp
        val Badge = 8.dp
    }

    // Elevation
    object Elevation {
        val Resting = 2.dp
        val Pressed = 1.dp
        val Badge = 4.dp
    }

    // Animation
    object Animation {
        val PressScale = 0.97f
        const val PressDuration = 100
        const val ShimmerDuration = 1400
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Main Entry Point
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelCard(
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
    val semanticsLabel = buildString {
        append(novel.name)
        readingStatus?.let { append(", ${it.displayName()}") }
        if (newChapterCount > 0) append(", $newChapterCount new chapters")
        lastReadChapter?.let { append(", last read: $it") }
    }

    when (density) {
        UiDensity.COMFORTABLE -> ComfortableNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = modifier.semantics {
                contentDescription = semanticsLabel
                role = Role.Button
            },
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName,
            isSelected = isSelected
        )
        else -> CompactNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = modifier.semantics {
                contentDescription = semanticsLabel
                role = Role.Button
            },
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName,
            isCompact = density == UiDensity.COMPACT,
            isSelected = isSelected
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Comfortable Layout: Image on top, content below
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComfortableNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    newChapterCount: Int,
    readingStatus: ReadingStatus?,
    lastReadChapter: String?,
    showApiName: Boolean,
    isSelected: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovelCardTokens.Animation.PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> NovelCardTokens.Elevation.Pressed
            isSelected -> 6.dp
            else -> NovelCardTokens.Elevation.Resting
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_elevation"
    )

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = NovelCardTokens.CardShape
                    )
                } else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // We handle our own visual feedback
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column {
            // Cover Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(NovelCardTokens.AspectRatio)
                    .clip(NovelCardTokens.ImageShapeTop)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                NovelCoverImage(
                    url = novel.posterUrl,
                    title = novel.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle vignette overlay for badge contrast
                VignetteOverlay(
                    modifier = Modifier.fillMaxSize(),
                    topAlpha = 0.4f,
                    bottomAlpha = 0f
                )

                // Badges row
                BadgeRow(
                    readingStatus = readingStatus,
                    newChapterCount = newChapterCount,
                    compactMode = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NovelCardTokens.Padding.Badge)
                )
            }

            // Content Section
            Column(
                modifier = Modifier.padding(NovelCardTokens.Padding.Comfortable),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Progress indicator
                lastReadChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                    ChapterProgress(
                        chapterName = chapter,
                        style = ChapterProgressStyle.Comfortable
                    )
                }

                // Title
                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                // Source name
                if (showApiName && novel.apiName.isNotBlank()) {
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
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Compact Layout: Full bleed image with text overlay
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    newChapterCount: Int,
    readingStatus: ReadingStatus?,
    lastReadChapter: String?,
    showApiName: Boolean,
    isCompact: Boolean,
    isSelected: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovelCardTokens.Animation.PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) NovelCardTokens.Elevation.Pressed else NovelCardTokens.Elevation.Resting,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_elevation"
    )

    Card(
        modifier = modifier
            .aspectRatio(NovelCardTokens.AspectRatio)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = NovelCardTokens.CardShape
                    )
                } else Modifier
            )
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
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover image
            NovelCoverImage(
                url = novel.posterUrl,
                title = novel.name,
                modifier = Modifier.fillMaxSize()
            )

            // Cinematic gradient overlay
            CinematicOverlay(modifier = Modifier.fillMaxSize())

            // Badges
            BadgeRow(
                readingStatus = readingStatus,
                newChapterCount = newChapterCount,
                compactMode = isCompact,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NovelCardTokens.Padding.Badge)
            )

            // Content overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(if (isCompact) NovelCardTokens.Padding.Compact else NovelCardTokens.Padding.Default),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title with text shadow for readability
                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(0f, 1f),
                            blurRadius = 3f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (isCompact) 14.sp else 16.sp,
                    fontSize = if (isCompact) 11.sp else 13.sp
                )

                // Progress
                lastReadChapter?.takeIf { it.isNotBlank() }?.let { chapter ->
                    ChapterProgress(
                        chapterName = chapter,
                        style = ChapterProgressStyle.Overlay
                    )
                }

                // Source (only in default mode)
                if (showApiName && novel.apiName.isNotBlank() && !isCompact) {
                    Text(
                        text = novel.apiName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shared Components
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NovelCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null, // Handled by parent semantics
        modifier = modifier,
        contentScale = ContentScale.Crop
    ) {
        val state = painter.state

        when {
            state is coil.compose.AsyncImagePainter.State.Loading -> {
                CoverPlaceholder(title = title)
            }
            state is coil.compose.AsyncImagePainter.State.Error || url.isNullOrBlank() -> {
                CoverFallback(title = title)
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun CoverPlaceholder(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .shimmerEffect(),
        contentAlignment = Alignment.Center
    ) {
        // Faint icon hint
        Icon(
            imageVector = Icons.Rounded.AutoStories,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer { alpha = 0.2f },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoverFallback(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = title.take(20).ifEmpty { "Novel" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Overlays
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VignetteOverlay(
    modifier: Modifier = Modifier,
    topAlpha: Float = 0.3f,
    bottomAlpha: Float = 0.6f
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = topAlpha),
                0.3f to Color.Transparent,
                0.65f to Color.Transparent,
                1f to Color.Black.copy(alpha = bottomAlpha)
            )
        )
    )
}

@Composable
private fun CinematicOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            val gradient = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = 0.25f),
                    0.25f to Color.Transparent,
                    0.5f to Color.Transparent,
                    0.75f to Color.Black.copy(alpha = 0.5f),
                    1f to Color.Black.copy(alpha = 0.9f)
                )
            )
            onDrawBehind {
                drawRect(gradient)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Badges
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BadgeRow(
    readingStatus: ReadingStatus?,
    newChapterCount: Int,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Status badge - left
        AnimatedVisibility(
            visible = readingStatus != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            readingStatus?.let {
                StatusBadge(
                    status = it,
                    compactMode = compactMode
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // New chapters badge - right
        AnimatedVisibility(
            visible = newChapterCount > 0,
            enter = fadeIn() + scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ),
            exit = fadeOut() + scaleOut()
        ) {
            NewChaptersBadge(
                count = newChapterCount,
                compactMode = compactMode
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: ReadingStatus,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
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

    if (compactMode) {
        // Minimal dot indicator with subtle background
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.padding(6.dp),
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
                        .size(NovelCardTokens.StatusDotSize)
                        .shadow(2.dp, CircleShape)
                        .background(statusColor, CircleShape)
                )
            }
        }
    } else {
        // Full pill label
        Surface(
            modifier = modifier,
            shape = NovelCardTokens.BadgeShape,
            color = statusColor,
            shadowElevation = NovelCardTokens.Elevation.Badge,
            tonalElevation = 2.dp
        ) {
            Text(
                text = status.displayName(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun NewChaptersBadge(
    count: Int,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val displayText = remember(count) {
        when {
            count > 99 -> "99+"
            else -> "+$count"
        }
    }

    // Subtle pulse animation for attention
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier.graphicsLayer { alpha = pulseAlpha },
        shape = if (compactMode) CircleShape else NovelCardTokens.BadgeShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = NovelCardTokens.Elevation.Badge
    ) {
        if (compactMode) {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 9.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(NovelCardTokens.BadgeIconSize),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Chapter Progress
// ══════════════════════════════════════════════════════════════════════════════

private enum class ChapterProgressStyle { Comfortable, Overlay }

@Composable
private fun ChapterProgress(
    chapterName: String,
    style: ChapterProgressStyle,
    modifier: Modifier = Modifier
) {
    val textStyle = when (style) {
        ChapterProgressStyle.Comfortable -> MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        ChapterProgressStyle.Overlay -> MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.7f),
                offset = Offset(0f, 1f),
                blurRadius = 2f
            )
        )
    }

    Text(
        text = chapterName,
        style = textStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Skeleton / Loading State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun NovelCardSkeleton(
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT
) {
    when (density) {
        UiDensity.COMFORTABLE -> ComfortableSkeleton(modifier)
        else -> CompactSkeleton(modifier)
    }
}

@Composable
private fun ComfortableSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NovelCardTokens.Elevation.Resting)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(NovelCardTokens.AspectRatio)
                    .clip(NovelCardTokens.ImageShapeTop)
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier.padding(NovelCardTokens.Padding.Comfortable),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title line 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
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
                // Subtitle
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun CompactSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(NovelCardTokens.AspectRatio),
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NovelCardTokens.Elevation.Resting)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )

            // Fake content area at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(NovelCardTokens.Padding.Default),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Shimmer Effect
// ══════════════════════════════════════════════════════════════════════════════

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = NovelCardTokens.Animation.ShimmerDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.surfaceContainerHighest
    )

    this
        .onGloballyPositioned { size = it.size }
        .drawWithCache {
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            val shimmerWidth = width * 0.4f

            val startX = -shimmerWidth + (width + shimmerWidth * 2) * translateAnim

            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(startX, 0f),
                end = Offset(startX + shimmerWidth, height)
            )

            onDrawBehind {
                drawRect(brush)
            }
        }
}

// Extension for border support (if not already defined)
@Composable
private fun Modifier.border(
    width: Dp,
    color: Color,
    shape: RoundedCornerShape
): Modifier = this.then(
    Modifier.drawWithCache {
        onDrawBehind {
            drawRoundRect(
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    NovelCardTokens.CardCornerRadius.toPx()
                )
            )
        }
    }
)