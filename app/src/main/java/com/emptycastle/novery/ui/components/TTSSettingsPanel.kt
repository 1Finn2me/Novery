package com.emptycastle.novery.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.service.TTSServiceManager
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.tts.VoiceManager

// =============================================================================
// DESIGN TOKENS (matching ReaderBottomBar)
// =============================================================================

private object TTSTheme {
    val background = Color(0xFF0A0A0B)
    val surface = Color(0xFF141416)
    val surfaceVariant = Color(0xFF1C1C1F)
    val surfaceElevated = Color(0xFF232328)

    val primary = Color(0xFFFF6B35)
    val primaryMuted = Color(0xFFFF6B35).copy(alpha = 0.15f)
    val primarySubtle = Color(0xFFFF6B35).copy(alpha = 0.08f)

    val textPrimary = Color(0xFFFAFAFA)
    val textSecondary = Color(0xFFA1A1AA)
    val textMuted = Color(0xFF71717A)
    val textDisabled = Color(0xFF52525B)

    val divider = Color(0xFF27272A)
    val border = Color(0xFF3F3F46)

    // Accent colors for different sections
    val emerald = Color(0xFF10B981)
    val emeraldMuted = Color(0xFF10B981).copy(alpha = 0.15f)
    val violet = Color(0xFF8B5CF6)
    val violetMuted = Color(0xFF8B5CF6).copy(alpha = 0.15f)
    val amber = Color(0xFFF59E0B)
    val amberMuted = Color(0xFFF59E0B).copy(alpha = 0.15f)

    val cornerRadiusMedium = 20.dp
    val cornerRadiusSmall = 14.dp
}

private val SleepTimerOptions = listOf(5, 10, 15, 30, 45, 60)

// Settings tabs
private enum class TTSTab(val title: String, val icon: ImageVector) {
    VOICE("Voice", Icons.Default.RecordVoiceOver),
    PLAYBACK("Playback", Icons.Default.Tune),
    OPTIONS("Options", Icons.Default.Settings)
}

// =============================================================================
// MAIN TTS SETTINGS PANEL
// =============================================================================

@Composable
fun TTSSettingsPanel(
    speed: Float,
    pitch: Float,
    selectedVoiceId: String?,
    autoScroll: Boolean,
    highlightSentence: Boolean,
    useSystemVoice: Boolean,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onHighlightChange: (Boolean) -> Unit,
    onUseSystemVoiceChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableStateOf(TTSTab.VOICE) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }
    var showVoiceSelector by remember { mutableStateOf(false) }

    val selectedVoice: VoiceInfo? by VoiceManager.selectedVoice.collectAsState()
    val sleepTimerRemaining = TTSServiceManager.getSleepTimerRemaining()

    DisposableEffect(Unit) {
        onDispose {
            if (isPreviewPlaying) {
                VoiceManager.stopPreview()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp),
        shape = RoundedCornerShape(TTSTheme.cornerRadiusMedium),
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TTSTheme.surfaceElevated, TTSTheme.surface)
                    )
                )
                .border(
                    width = 1.dp,
                    color = TTSTheme.border.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(TTSTheme.cornerRadiusMedium)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TTSSettingsHeader(
                    sleepTimerRemaining = sleepTimerRemaining,
                    onDismiss = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                )

                // Tab Bar
                TTSTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = tab
                    }
                )

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        label = "ttsTabContent"
                    ) { tab ->
                        when (tab) {
                            TTSTab.VOICE -> VoiceTabContent(
                                selectedVoice = selectedVoice,
                                selectedVoiceId = selectedVoiceId,
                                useSystemVoice = useSystemVoice,
                                showVoiceSelector = showVoiceSelector,
                                isPreviewPlaying = isPreviewPlaying,
                                previewingVoiceId = previewingVoiceId,
                                onUseSystemVoiceChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUseSystemVoiceChange(enabled)
                                    if (enabled) showVoiceSelector = false
                                },
                                onToggleVoiceSelector = {
                                    showVoiceSelector = !showVoiceSelector
                                },
                                onVoiceSelected = { voice ->
                                    onVoiceSelected(voice)
                                    TTSServiceManager.setVoice(voice.id)
                                    showVoiceSelector = false
                                    isPreviewPlaying = false
                                    previewingVoiceId = null
                                },
                                onPreviewVoice = { voice ->
                                    isPreviewPlaying = true
                                    previewingVoiceId = voice.id
                                    VoiceManager.previewVoice(voice.id)
                                },
                                onStopPreview = {
                                    isPreviewPlaying = false
                                    previewingVoiceId = null
                                    VoiceManager.stopPreview()
                                }
                            )

                            TTSTab.PLAYBACK -> PlaybackTabContent(
                                speed = speed,
                                pitch = pitch,
                                onSpeedChange = {
                                    onSpeedChange(it)
                                    TTSServiceManager.setSpeechRate(it)
                                },
                                onPitchChange = {
                                    onPitchChange(it)
                                    TTSServiceManager.setPitch(it)
                                }
                            )

                            TTSTab.OPTIONS -> OptionsTabContent(
                                autoScroll = autoScroll,
                                highlightSentence = highlightSentence,
                                sleepTimerRemaining = sleepTimerRemaining,
                                useSystemVoice = useSystemVoice,
                                onAutoScrollChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAutoScrollChange(it)
                                },
                                onHighlightChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHighlightChange(it)
                                },
                                onSetSleepTimer = { minutes ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    TTSServiceManager.setSleepTimer(context, minutes)
                                },
                                onCancelSleepTimer = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    TTSServiceManager.setSleepTimer(context, 0)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun TTSSettingsHeader(
    sleepTimerRemaining: Int?,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = TTSTheme.primaryMuted,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = TTSTheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Audio Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TTSTheme.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize your listening",
                        style = MaterialTheme.typography.labelSmall,
                        color = TTSTheme.textMuted
                    )
                    // Sleep timer indicator
                    if (sleepTimerRemaining != null && sleepTimerRemaining > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TTSTheme.amberMuted
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TTSTheme.amber,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "${sleepTimerRemaining}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = TTSTheme.amber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = TTSTheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TTSTheme.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =============================================================================
// TAB BAR
// =============================================================================

@Composable
private fun TTSTabBar(
    selectedTab: TTSTab,
    onTabSelected: (TTSTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TTSTheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TTSTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) TTSTheme.primary else Color.Transparent,
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TTSTheme.textMuted,
                label = "tabText"
            )

            Surface(
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = bgColor
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

// =============================================================================
// TAB CONTENT - VOICE
// =============================================================================

@Composable
private fun VoiceTabContent(
    selectedVoice: VoiceInfo?,
    selectedVoiceId: String?,
    useSystemVoice: Boolean,
    showVoiceSelector: Boolean,
    isPreviewPlaying: Boolean,
    previewingVoiceId: String?,
    onUseSystemVoiceChange: (Boolean) -> Unit,
    onToggleVoiceSelector: () -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onPreviewVoice: (VoiceInfo) -> Unit,
    onStopPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // System Voice Toggle
        TTSSectionCard(title = "Voice Source") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TTSToggleSetting(
                    icon = Icons.Default.Settings,
                    title = "Use System Voice",
                    subtitle = "Use your device's default TTS voice",
                    checked = useSystemVoice,
                    accentColor = TTSTheme.emerald,
                    onCheckedChange = onUseSystemVoiceChange
                )

                // System TTS Settings button when using system voice
                AnimatedVisibility(
                    visible = useSystemVoice,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SystemTTSSettingsButton()
                }
            }
        }

        // Custom Voice Selection
        AnimatedVisibility(
            visible = !useSystemVoice,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TTSSectionCard(title = "Select Voice") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Current selection display
                    if (selectedVoice != null) {
                        CurrentVoiceCard(
                            voice = selectedVoice,
                            isPlaying = isPreviewPlaying && previewingVoiceId == selectedVoice.id,
                            onPreview = {
                                if (isPreviewPlaying && previewingVoiceId == selectedVoice.id) {
                                    onStopPreview()
                                } else {
                                    onPreviewVoice(selectedVoice)
                                }
                            }
                        )
                    }

                    // Voice list toggle button
                    Surface(
                        onClick = onToggleVoiceSelector,
                        shape = RoundedCornerShape(10.dp),
                        color = TTSTheme.surface,
                        border = BorderStroke(1.dp, TTSTheme.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showVoiceSelector) "Hide voice list" else "Browse available voices",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TTSTheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (showVoiceSelector) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = TTSTheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Voice selector - using existing VoiceSelector component
                    AnimatedVisibility(
                        visible = showVoiceSelector,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        VoiceSelector(
                            selectedVoiceId = selectedVoiceId,
                            onVoiceSelected = onVoiceSelected,
                            onPreviewVoice = onPreviewVoice,
                            onStopPreview = onStopPreview,
                            isPreviewPlaying = isPreviewPlaying,
                            previewingVoiceId = previewingVoiceId,
                            showSystemSettings = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentVoiceCard(
    voice: VoiceInfo,
    isPlaying: Boolean,
    onPreview: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TTSTheme.primarySubtle,
        border = BorderStroke(1.dp, TTSTheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = TTSTheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = voice.shortName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TTSTheme.textPrimary
                    )
                    Text(
                        text = voice.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TTSTheme.textMuted
                    )
                }
            }

            Surface(
                onClick = onPreview,
                shape = CircleShape,
                color = if (isPlaying) TTSTheme.primary else TTSTheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Preview",
                        tint = if (isPlaying) Color.White else TTSTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemTTSSettingsButton() {
    val context = LocalContext.current

    Surface(
        onClick = {
            try {
                val intent = Intent("com.android.settings.TTS_SETTINGS")
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) { }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = TTSTheme.emeraldMuted,
        border = BorderStroke(1.dp, TTSTheme.emerald.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = TTSTheme.emerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Open System TTS Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TTSTheme.emerald,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// TAB CONTENT - PLAYBACK
// =============================================================================

@Composable
private fun PlaybackTabContent(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Speed Control
        TTSSectionCard(title = "Speech Rate", trailing = "${String.format("%.1f", speed)}x") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Slow" to 0.75f,
                        "Normal" to 1.0f,
                        "Fast" to 1.5f,
                        "Faster" to 2.0f
                    ).forEach { (label, value) ->
                        val isSelected = kotlin.math.abs(speed - value) < 0.1f
                        TTSQuickChip(
                            text = label,
                            isSelected = isSelected,
                            onClick = { onSpeedChange(value) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TTSTheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = speed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..2.5f,
                        steps = 7,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = TTSTheme.primary,
                            activeTrackColor = TTSTheme.primary,
                            inactiveTrackColor = TTSTheme.surface
                        )
                    )
                }
            }
        }

        // Pitch Control
        TTSSectionCard(title = "Pitch", trailing = "${String.format("%.1f", pitch)}x") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Low" to 0.75f,
                        "Normal" to 1.0f,
                        "High" to 1.25f,
                        "Higher" to 1.5f
                    ).forEach { (label, value) ->
                        val isSelected = kotlin.math.abs(pitch - value) < 0.1f
                        TTSQuickChip(
                            text = label,
                            isSelected = isSelected,
                            accentColor = TTSTheme.violet,
                            onClick = { onPitchChange(value) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = TTSTheme.violet,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = pitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = TTSTheme.violet,
                            activeTrackColor = TTSTheme.violet,
                            inactiveTrackColor = TTSTheme.surface
                        )
                    )
                }
            }
        }

        // Info card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TTSTheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TTSTheme.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Adjustments apply immediately. Find your comfortable listening pace for extended reading sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TTSTheme.textMuted
                )
            }
        }
    }
}

// =============================================================================
// TAB CONTENT - OPTIONS
// =============================================================================

@Composable
private fun OptionsTabContent(
    autoScroll: Boolean,
    highlightSentence: Boolean,
    sleepTimerRemaining: Int?,
    useSystemVoice: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    onHighlightChange: (Boolean) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Reading Options
        TTSSectionCard(title = "Reading Behavior") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TTSToggleSetting(
                    icon = Icons.Default.SwapVert,
                    title = "Auto-scroll",
                    subtitle = "Follow along as you listen",
                    checked = autoScroll,
                    accentColor = TTSTheme.emerald,
                    onCheckedChange = onAutoScrollChange
                )

                TTSToggleSetting(
                    icon = Icons.Default.HighlightAlt,
                    title = "Highlight Text",
                    subtitle = "Show current sentence being read",
                    checked = highlightSentence,
                    accentColor = TTSTheme.primary,
                    onCheckedChange = onHighlightChange
                )
            }
        }

        // Sleep Timer
        TTSSectionCard(title = "Sleep Timer") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Active timer display
                AnimatedVisibility(
                    visible = sleepTimerRemaining != null && sleepTimerRemaining > 0,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TTSTheme.amberMuted,
                        border = BorderStroke(1.dp, TTSTheme.amber.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TTSTheme.amber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "$sleepTimerRemaining minutes remaining",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TTSTheme.amber
                                    )
                                    Text(
                                        text = "Playback will stop automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TTSTheme.textMuted
                                    )
                                }
                            }

                            Surface(
                                onClick = onCancelSleepTimer,
                                shape = RoundedCornerShape(8.dp),
                                color = TTSTheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TimerOff,
                                        contentDescription = null,
                                        tint = TTSTheme.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TTSTheme.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Timer options
                Text(
                    text = if (sleepTimerRemaining != null && sleepTimerRemaining > 0) "Change timer" else "Stop playback after",
                    style = MaterialTheme.typography.labelSmall,
                    color = TTSTheme.textMuted
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SleepTimerOptions) { minutes ->
                        val isSelected = sleepTimerRemaining == minutes
                        SleepTimerChip(
                            minutes = minutes,
                            isSelected = isSelected,
                            onClick = { onSetSleepTimer(minutes) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TTSTheme.amber else TTSTheme.surface,
        label = "chipBg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (!isSelected) BorderStroke(1.dp, TTSTheme.border) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TTSTheme.textSecondary
            )
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun TTSSectionCard(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TTSTheme.cornerRadiusSmall),
        color = TTSTheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TTSTheme.textSecondary
                )
                if (trailing != null) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TTSTheme.primary
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun TTSToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = if (checked) accentColor.copy(alpha = 0.1f) else TTSTheme.surface,
        border = BorderStroke(1.dp, if (checked) accentColor.copy(alpha = 0.3f) else TTSTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (checked) accentColor.copy(alpha = 0.15f) else TTSTheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (checked) accentColor else TTSTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (checked) TTSTheme.textPrimary else TTSTheme.textSecondary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TTSTheme.textMuted
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = TTSTheme.textMuted,
                    uncheckedTrackColor = TTSTheme.surface
                )
            )
        }
    }
}

@Composable
private fun TTSQuickChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = TTSTheme.primary,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else TTSTheme.surface,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TTSTheme.textMuted,
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = if (!isSelected) BorderStroke(1.dp, TTSTheme.border) else null
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = textColor
            )
        }
    }
}