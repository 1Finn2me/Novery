package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Orange600
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900

/**
 * Transforming bottom bar for the reader
 * - Shows reader controls when TTS is inactive
 * - Transforms into TTS player when TTS is active
 */
@Composable
fun ReaderBottomBar(
    // TTS State
    isTTSActive: Boolean,
    isTTSPlaying: Boolean,
    currentSentenceIndex: Int,
    totalSentences: Int,
    chapterName: String,
    speechRate: Float,

    // Callbacks
    onPlayTTS: () -> Unit,
    onPauseTTS: () -> Unit,
    onResumeTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit,
    onOpenChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTTSSettings: () -> Unit,

    modifier: Modifier = Modifier
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = navBarPadding.calculateBottomPadding() + 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = Zinc900.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
    ) {
        AnimatedContent(
            targetState = isTTSActive,
            transitionSpec = {
                (slideInVertically(initialOffsetY = { it }) + fadeIn())
                    .togetherWith(slideOutVertically(targetOffsetY = { it }) + fadeOut())
            },
            label = "bottomBarTransition"
        ) { ttsActive ->
            if (ttsActive) {
                TTSPlayer(
                    isPlaying = isTTSPlaying,
                    currentSentenceIndex = currentSentenceIndex,
                    totalSentences = totalSentences,
                    chapterName = chapterName,
                    speechRate = speechRate,
                    onPlayPause = {
                        if (isTTSPlaying) onPauseTTS() else onResumeTTS()
                    },
                    onNext = onNextSentence,
                    onPrevious = onPreviousSentence,
                    onStop = onStopTTS,
                    onOpenSettings = onOpenTTSSettings
                )
            } else {
                ReaderControls(
                    onPlayTTS = onPlayTTS,
                    onOpenChapterList = onOpenChapterList,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun ReaderControls(
    onPlayTTS: () -> Unit,
    onOpenChapterList: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarButton(
            icon = Icons.AutoMirrored.Filled.List,
            label = "Chapters",
            onClick = onOpenChapterList
        )

        BottomBarButton(
            icon = Icons.Default.Headphones,
            label = "Listen",
            onClick = onPlayTTS
        )

        BottomBarButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun TTSPlayer(
    isPlaying: Boolean,
    currentSentenceIndex: Int,
    totalSentences: Int,
    chapterName: String,
    speechRate: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val progress = if (totalSentences > 0) {
        (currentSentenceIndex.toFloat() / totalSentences).coerceIn(0f, 1f)
    } else 0f

    Column {
        TTSProgressBar(
            progress = progress,
            isPlaying = isPlaying
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioWaveformMini(isPlaying = isPlaying)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chapterName.ifBlank { "Now Playing" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${currentSentenceIndex + 1}/$totalSentences",
                            style = MaterialTheme.typography.labelSmall,
                            color = Zinc400
                        )
                        if (speechRate != 1.0f) {
                            SpeedBadgeMini(rate = speechRate)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = currentSentenceIndex > 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (currentSentenceIndex > 0) Zinc300 else Zinc600,
                        modifier = Modifier.size(22.dp)
                    )
                }

                PlayPauseButtonMini(
                    isPlaying = isPlaying,
                    onClick = onPlayPause
                )

                IconButton(
                    onClick = onNext,
                    enabled = currentSentenceIndex < totalSentences - 1,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (currentSentenceIndex < totalSentences - 1) Zinc300 else Zinc600,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "TTS Settings",
                        tint = Zinc400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = Zinc400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TTSProgressBar(
    progress: Float,
    isPlaying: Boolean
) {
    val displayProgress = progress

    val progressColor by animateColorAsState(
        targetValue = if (isPlaying) Orange500 else Orange400.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "progressColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Zinc800)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(3.dp)
                .clip(RoundedCornerShape(topStart = 20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Orange600, progressColor)
                    )
                )
        )
    }
}

@Composable
private fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Zinc300,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Zinc400
            )
        }
    }
}

@Composable
private fun PlayPauseButtonMini(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = tween(150),
        label = "playPauseScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .scale(scale),
        shape = CircleShape,
        color = Orange500,
        contentColor = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioWaveformMini(
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = Modifier.size(28.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 350 + (index * 80),
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
                    .height((20.dp * height))
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isPlaying) Orange500 else Zinc600)
            )
        }
    }
}

@Composable
private fun SpeedBadgeMini(rate: Float) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Orange500.copy(alpha = 0.15f)
    ) {
        Text(
            text = "${formatRate(rate)}x",
            style = MaterialTheme.typography.labelSmall,
            color = Orange400,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

private fun formatRate(rate: Float): String {
    return if (rate == rate.toInt().toFloat()) {
        rate.toInt().toString()
    } else {
        String.format("%.2f", rate).trimEnd('0').trimEnd('.')
    }
}