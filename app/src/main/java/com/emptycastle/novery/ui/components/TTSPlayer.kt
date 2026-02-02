package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// =============================================================================
// DESIGN TOKENS
// =============================================================================

private object PlayerColors {
    val surface = Color(0xFF18181B)
    val surfaceVariant = Color(0xFF27272A)

    val accent = Color(0xFFFF6B35)
    val accentGlow = Color(0xFFFF6B35).copy(alpha = 0.2f)

    val iconPrimary = Color(0xFFFAFAFA)
    val iconSecondary = Color(0xFFA1A1AA)
    val iconDisabled = Color(0xFF52525B)

    val textSecondary = Color(0xFFA1A1AA)
    val textMuted = Color(0xFF71717A)

    val progressTrack = Color(0xFF3F3F46)
    val progressActive = Color(0xFFFF6B35)
}

private object PlayerSizes {
    val cornerRadius = 24.dp
    val playButtonSize = 64.dp
    val navButtonSize = 48.dp
    val actionButtonSize = 40.dp
    val progressHeight = 4.dp
}

// =============================================================================
// MAIN TTS PLAYER
// =============================================================================

@Composable
fun TTSPlayer(
    isPlaying: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    currentSentenceInChapter: Int,
    totalSentencesInChapter: Int,
    chapterNumber: Int,
    totalChapters: Int,
    speechRate: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Progress within current chapter based on TTS position
    val chapterTTSProgress by remember(currentSentenceInChapter, totalSentencesInChapter) {
        derivedStateOf {
            if (totalSentencesInChapter > 0) {
                (currentSentenceInChapter.toFloat() / totalSentencesInChapter).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Estimate time remaining for current chapter
    val estimatedTimeLeft by remember(currentSentenceInChapter, totalSentencesInChapter, speechRate) {
        derivedStateOf {
            val remaining = (totalSentencesInChapter - currentSentenceInChapter).coerceAtLeast(0)
            val secondsPerSentence = 2.5f / speechRate
            val totalSeconds = (remaining * secondsPerSentence).toInt()
            formatTime(totalSeconds)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = navBarPadding.calculateBottomPadding() + 12.dp)
    ) {
        // Glow effect
        PlayingGlow(
            isPlaying = isPlaying,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
        )

        // Player card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(PlayerSizes.cornerRadius),
            color = PlayerColors.surface,
            shadowElevation = 24.dp
        ) {
            Column {
                // Progress section
                ChapterTTSProgressSection(
                    progress = chapterTTSProgress,
                    currentSentence = currentSentenceInChapter,
                    totalSentences = totalSentencesInChapter,
                    chapterNumber = chapterNumber,
                    totalChapters = totalChapters,
                    timeRemaining = estimatedTimeLeft,
                    isPlaying = isPlaying
                )

                // Controls
                PlayerControls(
                    isPlaying = isPlaying,
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    onPlayPause = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause()
                    },
                    onPrevious = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    },
                    onSettings = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSettings()
                    },
                    onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                    }
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    return when {
        seconds <= 0 -> "< 1m"
        seconds < 60 -> "< 1m"
        seconds < 3600 -> "${seconds / 60}m"
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }
    }
}

// =============================================================================
// GLOW EFFECT
// =============================================================================

@Composable
private fun PlayingGlow(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(600),
        label = "glow"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier
                .graphicsLayer { this.alpha = alpha }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PlayerColors.accentGlow, Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.8f),
                            radius = size.width * 0.5f
                        )
                    )
                }
        )
    }
}

// =============================================================================
// PROGRESS SECTION
// =============================================================================

@Composable
private fun ChapterTTSProgressSection(
    progress: Float,
    currentSentence: Int,
    totalSentences: Int,
    chapterNumber: Int,
    totalChapters: Int,
    timeRemaining: String,
    isPlaying: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Playing indicator + position
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayingIndicator(isPlaying = isPlaying)

                Text(
                    text = "$currentSentence / $totalSentences",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = PlayerColors.textSecondary
                )

                // Chapter badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PlayerColors.surfaceVariant
                ) {
                    Text(
                        text = "Ch. $chapterNumber/$totalChapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlayerColors.textMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Right: Time + percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = PlayerColors.textMuted
                )
                Text(
                    text = timeRemaining,
                    style = MaterialTheme.typography.labelSmall,
                    color = PlayerColors.textMuted
                )

                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(PlayerColors.textMuted, CircleShape)
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = PlayerColors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        ProgressBar(progress = progress, isPlaying = isPlaying)
    }
}

// =============================================================================
// PLAYING INDICATOR
// =============================================================================

@Composable
private fun PlayingIndicator(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "bars")

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val height by transition.animateFloat(
                initialValue = 4f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + i * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 80)
                ),
                label = "bar$i"
            )

            val h = if (isPlaying) height else 6f
            val color by animateColorAsState(
                targetValue = if (isPlaying) PlayerColors.accent else PlayerColors.textMuted,
                label = "color"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

// =============================================================================
// PROGRESS BAR
// =============================================================================

@Composable
private fun ProgressBar(progress: Float, isPlaying: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmer.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerSizes.progressHeight)
            .clip(RoundedCornerShape(PlayerSizes.progressHeight / 2))
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.progressTrack)
        )

        // Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceAtLeast(0.001f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(PlayerSizes.progressHeight / 2))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            PlayerColors.progressActive.copy(alpha = 0.8f),
                            PlayerColors.progressActive
                        )
                    )
                )
                .then(
                    if (isPlaying) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    startX = size.width * (shimmerOffset - 0.2f),
                                    endX = size.width * (shimmerOffset + 0.2f)
                                )
                            )
                        }
                    } else Modifier
                )
        )

        // Thumb
        if (animatedProgress > 0.01f) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(PlayerSizes.progressHeight + 4.dp)
                        .offset(x = (PlayerSizes.progressHeight + 4.dp) / 2)
                        .shadow(if (isPlaying) 4.dp else 2.dp, CircleShape)
                        .background(PlayerColors.progressActive, CircleShape)
                )
            }
        }
    }
}

// =============================================================================
// CONTROLS
// =============================================================================

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(Icons.Rounded.Tune, onSettings, "Settings")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavButton(Icons.Rounded.SkipPrevious, canGoPrevious, onPrevious, "Previous")
            PlayPauseButton(isPlaying, onPlayPause)
            NavButton(Icons.Rounded.SkipNext, canGoNext, onNext, "Next")
        }

        ActionButton(Icons.Rounded.Close, onStop, "Stop")
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) pulseScale else 1f,
        label = "actualScale"
    )

    val shadow by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 8.dp,
        label = "shadow"
    )

    Box(modifier = Modifier.scale(scale), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(PlayerSizes.playButtonSize + 16.dp)
                    .blur(16.dp)
                    .background(PlayerColors.accentGlow, CircleShape)
            )
        }

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = PlayerColors.accent,
            shadowElevation = shadow,
            modifier = Modifier.size(PlayerSizes.playButtonSize)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.75f, animationSpec = spring(dampingRatio = 0.5f)) + fadeIn(tween(100)))
                            .togetherWith(scaleOut(targetScale = 0.75f) + fadeOut(tween(80)))
                    },
                    label = "icon"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit, desc: String) {
    val tint by animateColorAsState(
        targetValue = if (enabled) PlayerColors.iconPrimary else PlayerColors.iconDisabled,
        label = "tint"
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        label = "scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(PlayerSizes.navButtonSize).scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, desc, Modifier.size(28.dp), tint)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, onClick: () -> Unit, desc: String) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = PlayerColors.surfaceVariant,
        modifier = Modifier.size(PlayerSizes.actionButtonSize)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, desc, Modifier.size(20.dp), PlayerColors.iconSecondary)
        }
    }
}