package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.service.TTSServiceManager
import com.emptycastle.novery.tts.TTSEngine
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Orange600
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900

// =============================================================================
// CONSTANTS
// =============================================================================

private object TTSPlayerDefaults {
    val PlayerCornerRadius = 24.dp
    val MiniPlayerHeight = 72.dp
    val ExpandedPlayerMaxHeight = 400.dp
    val ControlButtonSize = 56.dp
    val SmallButtonSize = 40.dp
    val WaveformBarCount = 5
    val BackgroundAlpha = 0.98f

    val SpeedPresets = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val SleepTimerOptions = listOf(5, 10, 15, 30, 45, 60)
}

// =============================================================================
// MAIN TTS PLAYER
// =============================================================================

/**
 * Modern TTS Player with expandable controls
 */
@Composable
fun TTSPlayer(
    currentSegmentIndex: Int,
    totalSegments: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    novelName: String = "",
    chapterName: String = ""
) {
    val playbackState by TTSServiceManager.playbackState.collectAsState()
    val serviceState by TTSServiceManager.serviceState.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var showSleepTimerPanel by remember { mutableStateOf(false) }
    var currentRate by remember { mutableFloatStateOf(TTSServiceManager.getSpeechRate()) }

    // Calculate progress
    val progress by remember(currentSegmentIndex, totalSegments) {
        derivedStateOf {
            if (totalSegments > 0) {
                (currentSegmentIndex.toFloat() / totalSegments).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Estimated time remaining (rough estimate: 3 seconds per segment at 1x speed)
    val estimatedTimeRemaining by remember(currentSegmentIndex, totalSegments, currentRate) {
        derivedStateOf {
            val remainingSegments = totalSegments - currentSegmentIndex
            val secondsPerSegment = 3f / currentRate
            val totalSeconds = (remainingSegments * secondsPerSegment).toInt()
            formatTime(totalSeconds)
        }
    }

    // Sleep timer state
    val sleepTimerRemaining = TTSServiceManager.getSleepTimerRemaining()

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = navBarPadding.calculateBottomPadding() + 8.dp)
    ) {
        // Settings panels (appear above player)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speed Panel
            AnimatedVisibility(
                visible = showSpeedPanel && isExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SpeedControlPanel(
                    currentRate = currentRate,
                    onRateChange = { rate ->
                        currentRate = rate
                        TTSServiceManager.setSpeechRate(rate)
                    },
                    onDismiss = { showSpeedPanel = false }
                )
            }

            // Sleep Timer Panel
            AnimatedVisibility(
                visible = showSleepTimerPanel && isExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SleepTimerPanel(
                    currentMinutes = sleepTimerRemaining,
                    onSetTimer = { minutes ->
                        // You'll need to pass context for this
                        // TTSServiceManager.setSleepTimer(context, minutes)
                        showSleepTimerPanel = false
                    },
                    onCancelTimer = {
                        // TTSServiceManager.setSleepTimer(context, 0)
                        showSleepTimerPanel = false
                    },
                    onDismiss = { showSleepTimerPanel = false }
                )
            }

            // Main Player Card
            PlayerCard(
                isExpanded = isExpanded,
                isPlaying = playbackState.isPlaying,
                isPaused = playbackState.isPaused,
                currentSegment = currentSegmentIndex,
                totalSegments = totalSegments,
                progress = progress,
                currentRate = currentRate,
                estimatedTimeRemaining = estimatedTimeRemaining,
                novelName = novelName.ifBlank { playbackState.novelName },
                chapterName = chapterName.ifBlank { playbackState.chapterName },
                sleepTimerRemaining = sleepTimerRemaining,
                onPlayPause = {
                    if (playbackState.isPlaying) {
                        TTSServiceManager.pause()
                    } else {
                        TTSServiceManager.resume()
                    }
                },
                onNext = onNext,
                onPrevious = onPrevious,
                onClose = {
                    TTSServiceManager.stop()
                    onClose()
                },
                onExpand = { isExpanded = !isExpanded },
                onSpeedClick = {
                    showSpeedPanel = !showSpeedPanel
                    showSleepTimerPanel = false
                },
                onSleepTimerClick = {
                    showSleepTimerPanel = !showSleepTimerPanel
                    showSpeedPanel = false
                },
                onSeek = { segment ->
                    TTSServiceManager.seekToSegment(segment)
                }
            )
        }
    }
}

// =============================================================================
// PLAYER CARD
// =============================================================================

@Composable
private fun PlayerCard(
    isExpanded: Boolean,
    isPlaying: Boolean,
    isPaused: Boolean,
    currentSegment: Int,
    totalSegments: Int,
    progress: Float,
    currentRate: Float,
    estimatedTimeRemaining: String,
    novelName: String,
    chapterName: String,
    sleepTimerRemaining: Int?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSeek: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TTSPlayerDefaults.PlayerCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc900.copy(alpha = TTSPlayerDefaults.BackgroundAlpha)
        ),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Progress bar at top
            ProgressBar(
                progress = progress,
                isPlaying = isPlaying,
                onSeek = { seekProgress ->
                    val targetSegment = (seekProgress * totalSegments).toInt()
                        .coerceIn(0, totalSegments - 1)
                    onSeek(targetSegment)
                }
            )

            // Mini player content (always visible)
            MiniPlayerContent(
                isPlaying = isPlaying,
                currentSegment = currentSegment,
                totalSegments = totalSegments,
                currentRate = currentRate,
                novelName = novelName,
                chapterName = chapterName,
                sleepTimerRemaining = sleepTimerRemaining,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onExpand = onExpand,
                onClose = onClose,
                isExpanded = isExpanded
            )

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                ExpandedPlayerContent(
                    currentSegment = currentSegment,
                    totalSegments = totalSegments,
                    estimatedTimeRemaining = estimatedTimeRemaining,
                    currentRate = currentRate,
                    sleepTimerRemaining = sleepTimerRemaining,
                    onSpeedClick = onSpeedClick,
                    onSleepTimerClick = onSleepTimerClick,
                    onSeek = onSeek
                )
            }
        }
    }
}

// =============================================================================
// PROGRESS BAR
// =============================================================================

@Composable
private fun ProgressBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    val displayProgress = if (isDragging) dragProgress else progress

    val progressColor by animateColorAsState(
        targetValue = if (isPlaying) Orange500 else Orange400.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "progressColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onTap = { offset ->
                        val seekProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(seekProgress)
                        isDragging = false
                    }
                )
            }
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = TTSPlayerDefaults.PlayerCornerRadius, topEnd = TTSPlayerDefaults.PlayerCornerRadius))
                .background(Zinc800)
        )

        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = TTSPlayerDefaults.PlayerCornerRadius))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Orange600, progressColor)
                    )
                )
        )
    }
}

// =============================================================================
// MINI PLAYER CONTENT
// =============================================================================

@Composable
private fun MiniPlayerContent(
    isPlaying: Boolean,
    currentSegment: Int,
    totalSegments: Int,
    currentRate: Float,
    novelName: String,
    chapterName: String,
    sleepTimerRemaining: Int?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    isExpanded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Waveform animation
        AudioWaveform(
            isPlaying = isPlaying,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info section
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Chapter name
            Text(
                text = chapterName.ifBlank { "Now Playing" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress and indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${currentSegment + 1}/$totalSegments",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )

                // Speed badge
                if (currentRate != 1.0f) {
                    SpeedBadge(rate = currentRate)
                }

                // Sleep timer badge
                sleepTimerRemaining?.let { minutes ->
                    if (minutes > 0) {
                        SleepTimerBadge(minutes = minutes)
                    }
                }
            }
        }

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Previous
            IconButton(
                onClick = onPrevious,
                enabled = currentSegment > 0,
                modifier = Modifier.size(TTSPlayerDefaults.SmallButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (currentSegment > 0) Zinc300 else Zinc600,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play/Pause button with animation
            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = onPlayPause,
                modifier = Modifier.size(TTSPlayerDefaults.ControlButtonSize)
            )

            // Next
            IconButton(
                onClick = onNext,
                enabled = currentSegment < totalSegments - 1,
                modifier = Modifier.size(TTSPlayerDefaults.SmallButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = if (currentSegment < totalSegments - 1) Zinc300 else Zinc600,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expand/Collapse
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(TTSPlayerDefaults.SmallButtonSize)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Zinc400,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Close
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(TTSPlayerDefaults.SmallButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Zinc400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// =============================================================================
// EXPANDED PLAYER CONTENT
// =============================================================================

@Composable
private fun ExpandedPlayerContent(
    currentSegment: Int,
    totalSegments: Int,
    estimatedTimeRemaining: String,
    currentRate: Float,
    sleepTimerRemaining: Int?,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSeek: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        HorizontalDivider(color = Zinc800, modifier = Modifier.padding(bottom = 12.dp))

        // Segment slider
        SegmentSlider(
            currentSegment = currentSegment,
            totalSegments = totalSegments,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Time info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Segment ${currentSegment + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = Zinc400
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Zinc500,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$estimatedTimeRemaining remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = Zinc400
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speed control button
            QuickActionButton(
                icon = Icons.Default.Speed,
                label = "${formatRate(currentRate)}x",
                isActive = currentRate != 1.0f,
                onClick = onSpeedClick,
                modifier = Modifier.weight(1f)
            )

            // Sleep timer button
            QuickActionButton(
                icon = if (sleepTimerRemaining != null) Icons.Default.Bedtime else Icons.Default.Timer,
                label = sleepTimerRemaining?.let { "${it}m" } ?: "Sleep",
                isActive = sleepTimerRemaining != null,
                onClick = onSleepTimerClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// SEGMENT SLIDER
// =============================================================================

@Composable
private fun SegmentSlider(
    currentSegment: Int,
    totalSegments: Int,
    onSeek: (Int) -> Unit
) {
    var sliderPosition by remember(currentSegment) {
        mutableFloatStateOf(currentSegment.toFloat())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                onSeek(sliderPosition.toInt().coerceIn(0, totalSegments - 1))
            },
            valueRange = 0f..(totalSegments - 1).toFloat().coerceAtLeast(0f),
            steps = (totalSegments - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Orange500,
                activeTrackColor = Orange500,
                inactiveTrackColor = Zinc700,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =============================================================================
// PLAY PAUSE BUTTON
// =============================================================================

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = tween(150),
        label = "playPauseScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = CircleShape,
        color = Orange600,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(TTSPlayerDefaults.ControlButtonSize)
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(initialScale = 0.8f) + fadeIn())
                        .togetherWith(scaleOut(targetScale = 0.8f) + fadeOut())
                },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// =============================================================================
// AUDIO WAVEFORM ANIMATION
// =============================================================================

@Composable
private fun AudioWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(TTSPlayerDefaults.WaveformBarCount) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 100),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waveformBar$index"
            )

            val height = if (isPlaying) animatedHeight else 0.4f

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((24.dp * height))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) Orange500 else Zinc600
                    )
            )
        }
    }
}

// =============================================================================
// BADGES
// =============================================================================

@Composable
private fun SpeedBadge(rate: Float) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Orange500.copy(alpha = 0.15f)
    ) {
        Text(
            text = "${formatRate(rate)}x",
            style = MaterialTheme.typography.labelSmall,
            color = Orange400,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SleepTimerBadge(minutes: Int) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Zinc700
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = Zinc400,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = Zinc400,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =============================================================================
// QUICK ACTION BUTTON
// =============================================================================

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Orange500.copy(alpha = 0.15f) else Zinc800,
        animationSpec = tween(200),
        label = "quickActionBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Orange400 else Zinc400,
        animationSpec = tween(200),
        label = "quickActionContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =============================================================================
// SPEED CONTROL PANEL
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedControlPanel(
    currentRate: Float,
    onRateChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Zinc900.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Orange400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Zinc500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TTSPlayerDefaults.SpeedPresets) { rate ->
                    SpeedPresetChip(
                        rate = rate,
                        isSelected = kotlin.math.abs(currentRate - rate) < 0.01f,
                        onClick = { onRateChange(rate) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fine-tune slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fine-tune",
                    style = MaterialTheme.typography.labelMedium,
                    color = Zinc400
                )
                Text(
                    text = "${formatRate(currentRate)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = Orange400,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentRate,
                onValueChange = onRateChange,
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Orange500,
                    activeTrackColor = Orange500,
                    inactiveTrackColor = Zinc700
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0.5x", style = MaterialTheme.typography.labelSmall, color = Zinc600)
                Text(text = "1.5x", style = MaterialTheme.typography.labelSmall, color = Zinc600)
                Text(text = "2.5x", style = MaterialTheme.typography.labelSmall, color = Zinc600)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPresetChip(
    rate: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = "${formatRate(rate)}x",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Orange500,
            selectedLabelColor = Color.White,
            containerColor = Zinc800,
            labelColor = Zinc300
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) Orange500 else Zinc700,
            selectedBorderColor = Orange500,
            enabled = true,
            selected = isSelected
        )
    )
}

// =============================================================================
// SLEEP TIMER PANEL
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerPanel(
    currentMinutes: Int?,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Zinc900.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = Orange400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Zinc500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current timer status
            if (currentMinutes != null && currentMinutes > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Orange500.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timer active: $currentMinutes minutes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Orange400
                        )

                        IconButton(
                            onClick = onCancelTimer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TimerOff,
                                contentDescription = "Cancel timer",
                                tint = Orange400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Timer options
            Text(
                text = "Stop playback after:",
                style = MaterialTheme.typography.labelMedium,
                color = Zinc400
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TTSPlayerDefaults.SleepTimerOptions) { minutes ->
                    FilterChip(
                        selected = currentMinutes == minutes,
                        onClick = { onSetTimer(minutes) },
                        label = {
                            Text(
                                text = "$minutes min",
                                fontWeight = if (currentMinutes == minutes) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Orange500,
                            selectedLabelColor = Color.White,
                            containerColor = Zinc800,
                            labelColor = Zinc300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (currentMinutes == minutes) Orange500 else Zinc700,
                            selectedBorderColor = Orange500,
                            enabled = true,
                            selected = currentMinutes == minutes
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

private fun formatRate(rate: Float): String {
    return if (rate == rate.toInt().toFloat()) {
        rate.toInt().toString()
    } else {
        String.format("%.2f", rate).trimEnd('0').trimEnd('.')
    }
}

private fun formatTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

// =============================================================================
// LEGACY COMPATIBILITY
// =============================================================================

/**
 * Legacy overload for backwards compatibility
 */
@Composable
fun TTSPlayer(
    engine: TTSEngine,
    currentSegmentIndex: Int,
    totalSegments: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TTSPlayer(
        currentSegmentIndex = currentSegmentIndex,
        totalSegments = totalSegments,
        onNext = onNext,
        onPrevious = onPrevious,
        onClose = onClose,
        modifier = modifier
    )
}