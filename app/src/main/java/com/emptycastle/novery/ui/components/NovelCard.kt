// com/emptycastle/novery/ui/components/NovelCard.kt

package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FiberNew
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.theme.StatusCompleted
import com.emptycastle.novery.ui.theme.StatusDROPPED
import com.emptycastle.novery.ui.theme.StatusOnHold
import com.emptycastle.novery.ui.theme.StatusPlanToRead
import com.emptycastle.novery.ui.theme.StatusReading

private object NovelCardTokens {
    val CardShape = RoundedCornerShape(14.dp)
    val ImageShapeTop = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    val BadgeShape = RoundedCornerShape(999.dp)

    val CompactPadding = 8.dp
    val ComfortablePadding = 10.dp
}

/**
 * Main Novel Card Entry Point
 */
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
    showApiName: Boolean = false
) {
    if (density == UiDensity.COMFORTABLE) {
        ComfortableNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = modifier,
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName
        )
    } else {
        CompactNovelCard(
            novel = novel,
            onClick = onClick,
            modifier = modifier,
            onLongClick = onLongClick,
            newChapterCount = newChapterCount,
            readingStatus = readingStatus,
            lastReadChapter = lastReadChapter,
            showApiName = showApiName,
            isCompact = density == UiDensity.COMPACT
        )
    }
}

/**
 * Comfortable Style: Image on top, Text below.
 */
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
    showApiName: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                onLongClick?.let {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            }
        ),
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(NovelCardTokens.ImageShapeTop)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                NovelCoverImage(
                    url = novel.posterUrl,
                    title = novel.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Light top scrim for badge contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Status badge - top left
                if (readingStatus != null) {
                    StatusBadge(
                        status = readingStatus,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        compactMode = false
                    )
                }

                // NEW CHAPTERS BADGE - top right
                if (newChapterCount > 0) {
                    NewChaptersBadge(
                        count = newChapterCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(NovelCardTokens.ComfortablePadding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!lastReadChapter.isNullOrBlank()) {
                    Text(
                        text = lastReadChapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

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

/**
 * Compact Style: Image fills card, Text overlay with gradient.
 */
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
    isCompact: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick?.let {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        shape = NovelCardTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NovelCoverImage(
                url = novel.posterUrl,
                title = novel.name,
                modifier = Modifier.fillMaxSize()
            )

            // Balanced scrim: slight top + stronger bottom for title readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Status badge - top left
            if (readingStatus != null) {
                StatusBadge(
                    status = readingStatus,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    compactMode = isCompact
                )
            }

            // NEW CHAPTERS BADGE - top right
            if (newChapterCount > 0) {
                NewChaptersBadge(
                    count = newChapterCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    compactMode = isCompact
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(if (isCompact) 8.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    fontSize = if (isCompact) 11.sp else 12.sp
                )

                if (!lastReadChapter.isNullOrBlank()) {
                    Text(
                        text = lastReadChapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }

                if (showApiName && novel.apiName.isNotBlank() && !isCompact) {
                    Text(
                        text = novel.apiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// ---------------- Helper Components ----------------

@Composable
private fun NovelCoverImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        CoverFallback(title = title, modifier = Modifier.fillMaxSize())

        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CoverFallback(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
        )
    }
}

/**
 * New Chapters Badge - Shows count of new chapters since last view
 */
@Composable
private fun NewChaptersBadge(
    count: Int,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = NovelCardTokens.BadgeShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            if (compactMode) {
                // Just show the number in a small circle
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp
                    )
                }
            } else {
                // Show icon with count
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (count > 99) "+99" else "+$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Status badge: pill label in normal mode, dot indicator in compact mode.
 */
@Composable
private fun StatusBadge(
    status: ReadingStatus,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val statusColor = when (status) {
        ReadingStatus.READING -> StatusReading
        ReadingStatus.COMPLETED -> StatusCompleted
        ReadingStatus.ON_HOLD -> StatusOnHold
        ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
        ReadingStatus.DROPPED -> StatusDROPPED
    }

    val containerColor = if (compactMode) {
        Color.Black.copy(alpha = 0.55f)
    } else {
        statusColor.copy(alpha = 0.85f)
    }

    Surface(
        modifier = modifier,
        shape = NovelCardTokens.BadgeShape,
        color = containerColor,
        shadowElevation = if (compactMode) 0.dp else 2.dp
    ) {
        if (compactMode) {
            Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .shadow(1.dp, CircleShape)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        } else {
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
fun NovelCardSkeleton(
    modifier: Modifier = Modifier,
    density: UiDensity = UiDensity.DEFAULT
) {
    if (density == UiDensity.COMFORTABLE) {
        Card(
            modifier = modifier,
            shape = NovelCardTokens.CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(NovelCardTokens.ImageShapeTop)
                        .shimmerEffect()
                )
                Column(modifier = Modifier.padding(NovelCardTokens.ComfortablePadding)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier.aspectRatio(2f / 3f),
            shape = NovelCardTokens.CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
    )

    this
        .onGloballyPositioned { coordinates ->
            size = coordinates.size
        }
        .background(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(
                    x = startOffsetX * size.width,
                    y = 0f
                ),
                end = Offset(
                    x = (startOffsetX + 1f) * size.width,
                    y = size.height.toFloat()
                )
            )
        )
}