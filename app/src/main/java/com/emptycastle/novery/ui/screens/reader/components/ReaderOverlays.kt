package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.ProgressStyle
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.screens.reader.theme.ReaderDefaults

// =============================================================================
// TOP BAR
// =============================================================================

@Composable
fun ReaderTopBar(
    chapterTitle: String,
    chapterNumber: Int,
    totalChapters: Int,
    isBookmarked: Boolean,
    chapterProgress: Float,
    estimatedTimeLeft: String?,
    colors: ReaderColors,
    progressStyle: ProgressStyle = ProgressStyle.BAR,
    largerTouchTargets: Boolean = false,
    onBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val buttonSize = if (largerTouchTargets) 56.dp else 48.dp

    // Clean up estimated time - treat empty string as null
    val displayTimeLeft = estimatedTimeLeft?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.controlsBackground,
        shadowElevation = ReaderDefaults.TopBarElevation
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding.calculateTopPadding())
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = colors.icon
                    )
                }

                // Title and info - centered
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = colors.text
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgressIndicator(
                            progress = chapterProgress,
                            chapterNumber = chapterNumber,
                            totalChapters = totalChapters,
                            estimatedTimeLeft = displayTimeLeft,
                            style = progressStyle,
                            colors = colors
                        )
                    }
                }

                // Bookmark button
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = if (isBookmarked)
                            Icons.Default.Bookmark
                        else
                            Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) colors.accent else colors.icon
                    )
                }
            }

            // Progress bar at bottom (only if style is BAR)
            if (progressStyle == ProgressStyle.BAR) {
                LinearProgressIndicator(
                    progress = { chapterProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = colors.accent,
                    trackColor = colors.progressTrack.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicator(
    progress: Float,
    chapterNumber: Int,
    totalChapters: Int,
    estimatedTimeLeft: String?,
    style: ProgressStyle,
    colors: ReaderColors
) {
    when (style) {
        ProgressStyle.NONE -> {
            // Show nothing
        }

        ProgressStyle.BAR -> {
            // Bar is shown separately, show chapter info and optional time
            Text(
                text = "$chapterNumber / $totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            // Chapter progress percentage
            Text(
                text = " • ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            // Only show time if we have a valid value
            if (estimatedTimeLeft != null) {
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = estimatedTimeLeft,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        ProgressStyle.PERCENTAGE -> {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )

            Text(
                text = " • Ch. $chapterNumber/$totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            if (estimatedTimeLeft != null) {
                Text(
                    text = " • $estimatedTimeLeft",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        ProgressStyle.PAGES -> {
            val estimatedPages = (progress * 100).toInt()
            Text(
                text = "~$estimatedPages%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            Text(
                text = " • Ch. $chapterNumber/$totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            if (estimatedTimeLeft != null) {
                Text(
                    text = " • $estimatedTimeLeft left",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        ProgressStyle.DOTS -> {
            DotsProgressIndicator(
                progress = progress,
                colors = colors
            )

            if (estimatedTimeLeft != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = estimatedTimeLeft,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        ProgressStyle.TIME_LEFT -> {
            if (estimatedTimeLeft != null) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = estimatedTimeLeft,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
            } else {
                // Fallback to percentage if no time available
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
            }

            Text(
                text = " • Ch. $chapterNumber",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun DotsProgressIndicator(
    progress: Float,
    colors: ReaderColors,
    dotCount: Int = 5
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val dotProgress = (index + 1).toFloat() / dotCount
            val isActive = progress >= dotProgress
            val isPartial = !isActive && progress > (index.toFloat() / dotCount)

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> colors.accent
                            isPartial -> colors.accent.copy(alpha = 0.4f)
                            else -> colors.progressTrack
                        }
                    )
            )
        }
    }
}

// =============================================================================
// TTS ACTIVE INDICATOR
// =============================================================================

@Composable
fun TTSActiveIndicator(
    colors: ReaderColors,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.accent.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AudioWaveIndicator(color = colors.accent)
            Text(
                text = "Playing",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun AudioWaveIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedHeight by animateFloatAsState(
                targetValue = when ((System.currentTimeMillis() / 200 + index) % 3) {
                    0L -> 8f
                    1L -> 12f
                    else -> 6f
                },
                animationSpec = tween(150),
                label = "wave_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}