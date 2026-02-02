// com/emptycastle/novery/ui/screens/details/components/NovelHeader.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.ui.screens.details.util.DetailsColors
import com.emptycastle.novery.ui.screens.details.util.StatusUtils
import kotlinx.coroutines.delay

// ================================================================
// MAIN NOVEL HEADER
// ================================================================

private val HeaderHeight = 300.dp
private val CoverWidth = 130.dp
private val CoverAspectRatio = 2f / 3f

@Composable
fun NovelHeader(
    details: NovelDetails,
    providerName: String,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    readProgress: Float,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStatusClick: () -> Unit,
    onCoverClick: () -> Unit,
    onShare: (() -> Unit)? = null,
    onOpenInWebView: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxWidth()) {
        HeaderBackground(
            posterUrl = details.posterUrl,
            statusBarHeight = statusBarHeight
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Top bar WITHOUT favorite button (moved to info column)
            HeaderTopBar(
                onBack = onBack,
                onShare = onShare,
                onOpenInWebView = onOpenInWebView
            )

            Spacer(modifier = Modifier.height(12.dp))

            HeaderContentRow(
                details = details,
                providerName = providerName,
                isFavorite = isFavorite,
                readingStatus = readingStatus,
                readProgress = readProgress,
                onCoverClick = onCoverClick,
                onToggleFavorite = onToggleFavorite,
                onStatusClick = onStatusClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ================================================================
// BACKGROUND
// ================================================================

@Composable
private fun HeaderBackground(
    posterUrl: String?,
    statusBarHeight: Dp
) {
    val totalHeight = HeaderHeight + statusBarHeight
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        if (!posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
                    .graphicsLayer { alpha = 0.6f }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.5f),
                            0.4f to Color.Black.copy(alpha = 0.6f),
                            0.7f to backgroundColor.copy(alpha = 0.9f),
                            1.0f to backgroundColor
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        ),
                        center = Offset(0.5f, 0.3f),
                        radius = 1200f
                    )
                )
        )
    }
}

// ================================================================
// TOP BAR - WITHOUT FAVORITE BUTTON
// ================================================================

@Composable
private fun HeaderTopBar(
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenInWebView: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            onClick = onBack,
            contentDescription = "Navigate back"
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onShare != null) {
                GlassIconButton(
                    onClick = onShare,
                    contentDescription = "Share novel"
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            if (onOpenInWebView != null) {
                GlassIconButton(
                    onClick = onOpenInWebView,
                    contentDescription = "Open in browser"
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

        }
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
            .size(44.dp)
            .scale(scale)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

// ================================================================
// CONTENT ROW
// ================================================================

@Composable
private fun HeaderContentRow(
    details: NovelDetails,
    providerName: String,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    readProgress: Float,
    onCoverClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStatusClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NovelCoverCard(
            posterUrl = details.posterUrl,
            name = details.name,
            chapterCount = details.chapters.size,
            readProgress = readProgress,
            isFavorite = isFavorite,
            onClick = onCoverClick
        )

        NovelInfoColumn(
            details = details,
            providerName = providerName,
            isFavorite = isFavorite,
            readingStatus = readingStatus,
            onToggleFavorite = onToggleFavorite,
            onStatusClick = onStatusClick,
            modifier = Modifier.fillMaxHeight()
        )
    }
}

// ================================================================
// COVER CARD
// ================================================================

@Composable
private fun NovelCoverCard(
    posterUrl: String?,
    name: String,
    chapterCount: Int,
    readProgress: Float,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cover_scale"
    )

    Card(
        modifier = Modifier
            .width(CoverWidth)
            .aspectRatio(CoverAspectRatio)
            .scale(scale)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "Cover for $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CoverPlaceholder()
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(
                    visible = isFavorite,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = DetailsColors.Pink.copy(alpha = 0.9f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = "In library",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                ZoomHintBadge()
            }

            if (chapterCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "$chapterCount ch",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (readProgress > 0f) {
                ReadProgressIndicator(
                    progress = readProgress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun CoverPlaceholder() {
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
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ZoomHintBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Rounded.ZoomOutMap,
                contentDescription = "Tap to view full cover",
                modifier = Modifier.size(12.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ReadProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clampedProgress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DetailsColors.Success,
                            DetailsColors.Success.copy(green = 0.85f)
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = size.height * 0.8f,
                        center = Offset(size.width - 2.dp.toPx(), size.height / 2)
                    )
                }
        )
    }
}

// ================================================================
// INFO COLUMN
// ================================================================

@Composable
private fun NovelInfoColumn(
    details: NovelDetails,
    providerName: String,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    onToggleFavorite: () -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CopiableTitle(
                title = details.name,
                maxLines = 2
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.author?.takeIf { it.isNotBlank() }?.let { author ->
                    CopiableAuthorChip(author = author)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (providerName.isNotBlank()) {
                        ProviderChip(providerName = providerName)
                    }

                    details.status?.takeIf { it.isNotBlank() }?.let { status ->
                        NovelStatusBadge(status = status)
                    }
                }
            }
        }

        // ✨ NEW: Unified Library + Status Button
        LibraryStatusButton(
            isFavorite = isFavorite,
            readingStatus = readingStatus,
            onToggleFavorite = onToggleFavorite,
            onStatusClick = onStatusClick
        )
    }
}

// ================================================================
// ✨ NEW: UNIFIED LIBRARY + STATUS BUTTON
// ================================================================

@Composable
private fun LibraryStatusButton(
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    onToggleFavorite: () -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Heart burst animation when added to library
    var showHeartBurst by remember { mutableStateOf(false) }
    var previousFavorite by remember { mutableStateOf(isFavorite) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !previousFavorite) {
            showHeartBurst = true
            delay(500)
            showHeartBurst = false
        }
        previousFavorite = isFavorite
    }

    val heartBurstScale by animateFloatAsState(
        targetValue = if (showHeartBurst) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heart_burst_scale"
    )

    // Colors based on state
    val containerColor by animateColorAsState(
        targetValue = if (isFavorite) {
            StatusUtils.getStatusColor(readingStatus).copy(alpha = 0.12f)
        } else {
            DetailsColors.Pink.copy(alpha = 0.12f)
        },
        animationSpec = tween(350),
        label = "container_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFavorite) {
            StatusUtils.getStatusColor(readingStatus).copy(alpha = 0.4f)
        } else {
            DetailsColors.Pink.copy(alpha = 0.4f)
        },
        animationSpec = tween(350),
        label = "border_color"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = isFavorite,
            transitionSpec = {
                if (targetState) {
                    // Adding to library - expand with scale
                    (fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.85f, animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )))
                        .togetherWith(fadeOut(animationSpec = tween(200)))
                } else {
                    // Removing from library - shrink
                    (fadeIn(animationSpec = tween(200)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(200)) +
                                    scaleOut(targetScale = 0.85f)
                        )
                }
            },
            contentAlignment = Alignment.Center,
            label = "library_state"
        ) { isInLibrary ->
            if (isInLibrary) {
                InLibraryContent(
                    readingStatus = readingStatus,
                    heartScale = heartBurstScale,
                    onHeartClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    },
                    onStatusClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStatusClick()
                    }
                )
            } else {
                AddToLibraryContent(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    }
                )
            }
        }
    }
}

@Composable
private fun InLibraryContent(
    readingStatus: ReadingStatus,
    heartScale: Float,
    onHeartClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    val statusColor = StatusUtils.getStatusColor(readingStatus)

    Row(
        modifier = Modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Heart button (to remove from library)
        val heartInteraction = remember { MutableInteractionSource() }
        val heartPressed by heartInteraction.collectIsPressedAsState()

        val heartButtonScale by animateFloatAsState(
            targetValue = when {
                heartPressed -> 0.85f
                else -> heartScale
            },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "heart_button_scale"
        )

        Surface(
            onClick = onHeartClick,
            interactionSource = heartInteraction,
            shape = RoundedCornerShape(8.dp),
            color = DetailsColors.Pink.copy(alpha = 0.15f),
            border = BorderStroke(0.5.dp, DetailsColors.Pink.copy(alpha = 0.3f)),
            modifier = Modifier
                .size(38.dp)
                .scale(heartButtonScale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Remove from library",
                    modifier = Modifier.size(20.dp),
                    tint = DetailsColors.Pink
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Status button
        val statusInteraction = remember { MutableInteractionSource() }
        val statusPressed by statusInteraction.collectIsPressedAsState()

        val statusScale by animateFloatAsState(
            targetValue = if (statusPressed) 0.97f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "status_scale"
        )

        Surface(
            onClick = onStatusClick,
            interactionSource = statusInteraction,
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.08f),
            modifier = Modifier
                .weight(1f)
                .scale(statusScale)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = StatusUtils.getStatusIcon(readingStatus),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = statusColor
                    )

                    Text(
                        text = readingStatus.displayName(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Change status",
                    modifier = Modifier.size(20.dp),
                    tint = statusColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AddToLibraryContent(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "add_scale"
    )

    val heartScale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heart_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .scale(heartScale),
                tint = DetailsColors.Pink
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Add to Library",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = DetailsColors.Pink
            )
        }
    }
}

// ================================================================
// COPIABLE TITLE
// ================================================================

@Composable
private fun CopiableTitle(
    title: String,
    maxLines: Int = 2
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showCopiedFeedback by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "title_scale"
    )

    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            delay(1500)
            showCopiedFeedback = false
        }
    }

    Box {
        Row(
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    clipboardManager.setText(AnnotatedString(title))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCopiedFeedback = true
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp,
                modifier = Modifier.weight(1f, fill = false)
            )

            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Tap to copy title",
                modifier = Modifier
                    .size(14.dp)
                    .padding(top = 4.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = showCopiedFeedback,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DetailsColors.Success.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "Title copied",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ================================================================
// COPIABLE AUTHOR CHIP
// ================================================================

@Composable
private fun CopiableAuthorChip(author: String) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showCopiedFeedback by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "author_scale"
    )

    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            delay(1500)
            showCopiedFeedback = false
        }
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    clipboardManager.setText(AnnotatedString(author))
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCopiedFeedback = true
                }
                .padding(vertical = 2.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(22.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Tap to copy author",
                modifier = Modifier.size(11.dp),
                tint = Color.White.copy(alpha = 0.35f)
            )
        }

        AnimatedVisibility(
            visible = showCopiedFeedback,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DetailsColors.Success.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "Copied",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(providerName: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
            Text(
                text = providerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NovelStatusBadge(status: String) {
    val (statusColor, statusLabel) = remember(status) {
        when (status.lowercase()) {
            "ongoing" -> DetailsColors.StatusReading to "Ongoing"
            "completed" -> DetailsColors.StatusCompleted to "Completed"
            "hiatus" -> DetailsColors.StatusOnHold to "Hiatus"
            "dropped", "cancelled", "canceled" -> DetailsColors.StatusDropped to "Dropped"
            else -> Color.White.copy(alpha = 0.7f) to status.replaceFirstChar { it.uppercase() }
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusColor.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Circle,
                contentDescription = null,
                modifier = Modifier.size(6.dp),
                tint = statusColor
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}