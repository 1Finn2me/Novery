package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
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
// LINE HEIGHT OPTIONS
// =============================================================================

private data class LineHeightOption(
    val label: String,
    val value: Float
)

private val lineHeightOptions = listOf(
    LineHeightOption("Tight", 1.3f),
    LineHeightOption("Normal", 1.6f),
    LineHeightOption("Relaxed", 2.0f),
    LineHeightOption("Loose", 2.4f)
)

// =============================================================================
// MAIN BOTTOM BAR
// =============================================================================

@Composable
fun ReaderBottomBar(
    // TTS State
    isTTSActive: Boolean,
    isTTSPlaying: Boolean,
    currentSentenceIndex: Int,
    totalSentences: Int,
    chapterName: String,
    speechRate: Float,

    // Quick Settings State
    fontSize: Int,
    currentTheme: ReaderTheme,
    lineHeight: Float,

    // Callbacks - Normal Mode
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineHeightChange: (Float) -> Unit,  // Changed from toggle to value callback
    onOpenChapterList: () -> Unit,
    onOpenSettings: () -> Unit,

    // Callbacks - TTS
    onStartTTS: () -> Unit,
    onPauseTTS: () -> Unit,
    onResumeTTS: () -> Unit,
    onStopTTS: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit,
    onOpenTTSSettings: () -> Unit,

    modifier: Modifier = Modifier
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    var showInlineSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = navBarPadding.calculateBottomPadding() + 8.dp)
    ) {
        // Inline Settings Panel (expandable above the main bar)
        AnimatedVisibility(
            visible = showInlineSettings && !isTTSActive,
            enter = expandVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Bottom
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                shrinkTowards = Alignment.Bottom
            ) + fadeOut()
        ) {
            Column {
                InlineSettingsPanel(
                    fontSize = fontSize,
                    currentTheme = currentTheme,
                    lineHeight = lineHeight,
                    onFontSizeDecrease = onFontSizeDecrease,
                    onFontSizeIncrease = onFontSizeIncrease,
                    onThemeChange = onThemeChange,
                    onLineHeightChange = onLineHeightChange,
                    onMoreSettings = {
                        showInlineSettings = false
                        onOpenSettings()
                    },
                    onDismiss = { showInlineSettings = false }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Main Bottom Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Zinc900.copy(alpha = 0.98f),
            tonalElevation = 12.dp,
            shadowElevation = 16.dp
        ) {
            AnimatedContent(
                targetState = isTTSActive,
                transitionSpec = {
                    (slideInVertically(initialOffsetY = { it / 2 }) + fadeIn())
                        .togetherWith(slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut())
                },
                label = "bottomBarMode"
            ) { ttsActive ->
                if (ttsActive) {
                    TTSModeContent(
                        isPlaying = isTTSPlaying,
                        currentSentenceIndex = currentSentenceIndex,
                        totalSentences = totalSentences,
                        chapterName = chapterName,
                        speechRate = speechRate,
                        onPlayPause = { if (isTTSPlaying) onPauseTTS() else onResumeTTS() },
                        onNext = onNextSentence,
                        onPrevious = onPreviousSentence,
                        onStop = onStopTTS,
                        onOpenSettings = onOpenTTSSettings
                    )
                } else {
                    MainBarContent(
                        isSettingsOpen = showInlineSettings,
                        onOpenChapterList = onOpenChapterList,
                        onStartTTS = onStartTTS,
                        onToggleSettings = { showInlineSettings = !showInlineSettings }
                    )
                }
            }
        }
    }
}

// =============================================================================
// INLINE SETTINGS PANEL
// =============================================================================

@Composable
private fun InlineSettingsPanel(
    fontSize: Int,
    currentTheme: ReaderTheme,
    lineHeight: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onMoreSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Zinc900.copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Orange500.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Orange400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Quick Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Zinc800,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Zinc400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Font Size Section
            SettingRow(
                icon = Icons.Default.FormatSize,
                title = "Text Size"
            ) {
                FontSizeControl(
                    fontSize = fontSize,
                    onDecrease = onFontSizeDecrease,
                    onIncrease = onFontSizeIncrease
                )
            }

            StyledDivider()

            // Theme Section
            SettingRow(
                icon = Icons.Default.Palette,
                title = "Theme"
            ) {
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
            }

            StyledDivider()

            // Line Height Section - Now with options
            SettingSection(
                icon = Icons.Default.FormatLineSpacing,
                title = "Line Spacing"
            ) {
                LineHeightSelector(
                    currentLineHeight = lineHeight,
                    onLineHeightChange = onLineHeightChange
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // More Settings Button
            MoreSettingsButton(onClick = onMoreSettings)
        }
    }
}

@Composable
private fun FontSizeControl(
    fontSize: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Zinc800.copy(alpha = 0.7f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            // Decrease button
            Surface(
                onClick = onDecrease,
                shape = CircleShape,
                color = Zinc700,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease font size",
                        tint = Zinc300,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Font size value
            Text(
                text = "$fontSize",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            // Increase button
            Surface(
                onClick = onIncrease,
                shape = CircleShape,
                color = Zinc700,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase font size",
                        tint = Zinc300,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: ReaderTheme,
    onThemeChange: (ReaderTheme) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ReaderTheme.LIGHT,
            ReaderTheme.SEPIA,
            ReaderTheme.DARK,
            ReaderTheme.AMOLED
        ).forEach { theme ->
            ThemeOption(
                theme = theme,
                isSelected = currentTheme == theme,
                onClick = { onThemeChange(theme) }
            )
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = remember(theme) { ReaderColors.fromTheme(theme) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "themeScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Orange500 else Zinc600,
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .scale(scale),
        shape = CircleShape,
        color = themeColors.background,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.5.dp,
            color = borderColor
        ),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 0.5f) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Orange500)
                )
            }
        }
    }
}

@Composable
private fun LineHeightSelector(
    currentLineHeight: Float,
    onLineHeightChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lineHeightOptions.forEach { option ->
            val isSelected = kotlin.math.abs(currentLineHeight - option.value) < 0.15f

            LineHeightOptionChip(
                label = option.label,
                isSelected = isSelected,
                onClick = { onLineHeightChange(option.value) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LineHeightOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Orange500 else Zinc800,
        animationSpec = tween(durationMillis = 200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Zinc400,
        animationSpec = tween(durationMillis = 200),
        label = "chipText"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (!isSelected) {
            BorderStroke(1.dp, Zinc700)
        } else null,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun MoreSettingsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Orange500.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Orange500.copy(alpha = 0.08f),
                            Orange500.copy(alpha = 0.15f),
                            Orange500.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange400
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = Orange400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Zinc400,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Zinc300
            )
        }
        content()
    }
}

@Composable
private fun SettingSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Zinc400,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Zinc300
            )
        }
        content()
    }
}

@Composable
private fun StyledDivider() {
    HorizontalDivider(
        color = Zinc700.copy(alpha = 0.5f),
        thickness = 1.dp
    )
}

// =============================================================================
// MAIN BAR CONTENT
// =============================================================================

@Composable
private fun MainBarContent(
    isSettingsOpen: Boolean,
    onOpenChapterList: () -> Unit,
    onStartTTS: () -> Unit,
    onToggleSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapters Button
        BottomBarButton(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = "Chapters",
            onClick = onOpenChapterList
        )

        // Center Listen Button (highlighted)
        ListenButton(onClick = onStartTTS)

        // Settings Button
        BottomBarButton(
            icon = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Tune,
            label = "Settings",
            isActive = isSettingsOpen,
            onClick = onToggleSettings
        )
    }
}

@Composable
private fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    val iconColor by animateColorAsState(
        targetValue = if (isActive) Orange400 else Zinc400,
        label = "iconColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Orange500.copy(alpha = 0.12f) else Color.Transparent,
        label = "bgColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        modifier = Modifier.scale(scale)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) Orange400 else Zinc500,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ListenButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Orange500,
        shadowElevation = 4.dp,
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Listen",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// =============================================================================
// TTS MODE CONTENT
// =============================================================================

@Composable
private fun TTSModeContent(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        // Progress bar at top
        TTSProgressBar(progress = progress, isPlaying = isPlaying)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Info with animated waveform
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioWaveformIndicator(isPlaying = isPlaying)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chapterName.ifBlank { "Now Playing" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${currentSentenceIndex + 1} of $totalSentences",
                            style = MaterialTheme.typography.labelSmall,
                            color = Zinc400
                        )
                        if (speechRate != 1.0f) {
                            SpeedBadge(rate = speechRate)
                        }
                    }
                }
            }

            // Right: Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Previous button
                TTSControlButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    enabled = currentSentenceIndex > 0,
                    onClick = onPrevious
                )

                // Play/Pause button
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause
                )

                // Next button
                TTSControlButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    enabled = currentSentenceIndex < totalSentences - 1,
                    onClick = onNext
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Settings button
                Surface(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    color = Zinc800,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "TTS Settings",
                            tint = Zinc400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Stop button
                Surface(
                    onClick = onStop,
                    shape = CircleShape,
                    color = Zinc800,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = Zinc400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TTSControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Zinc300 else Zinc700,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun TTSProgressBar(
    progress: Float,
    isPlaying: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Zinc800)
        )

        // Progress with animated gradient
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(topStart = 28.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = if (isPlaying) {
                            listOf(Orange600, Orange500, Orange400)
                        } else {
                            listOf(Orange600.copy(alpha = 0.7f), Orange500.copy(alpha = 0.7f))
                        }
                    )
                )
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "playPauseScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .scale(scale),
        shape = CircleShape,
        color = Orange500,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(initialScale = 0.7f) + fadeIn())
                        .togetherWith(scaleOut(targetScale = 0.7f) + fadeOut())
                },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioWaveformIndicator(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) Orange500.copy(alpha = 0.15f) else Zinc800,
        modifier = Modifier.size(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val animatedHeight by infiniteTransition.animateFloat(
                    initialValue = 0.25f,
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

                val height = if (isPlaying) animatedHeight else 0.35f

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((24.dp * height))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isPlaying) Orange500 else Zinc600)
                )
            }
        }
    }
}

@Composable
private fun SpeedBadge(rate: Float) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Orange500.copy(alpha = 0.15f)
    ) {
        Text(
            text = "${formatRate(rate)}×",
            style = MaterialTheme.typography.labelSmall,
            color = Orange400,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun formatRate(rate: Float): String {
    return if (rate == rate.toInt().toFloat()) {
        rate.toInt().toString()
    } else {
        String.format("%.1f", rate).trimEnd('0').trimEnd('.')
    }
}