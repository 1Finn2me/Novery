package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.tts.VoiceManager
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900

/**
 * Full TTS settings panel with voice selection, speed, pitch controls
 */
@Composable
fun TTSSettingsPanel(
    speed: Float,
    pitch: Float,
    selectedVoiceId: String?,
    autoScroll: Boolean,
    highlightSentence: Boolean,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onHighlightChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceSelector by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }

    val selectedVoice by VoiceManager.selectedVoice.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Zinc900.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TTS Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Zinc400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voice selector
            CompactVoiceSelector(
                selectedVoice = selectedVoice,
                onOpenFullSelector = { showVoiceSelector = !showVoiceSelector }
            )

            // Expanded voice selector
            AnimatedVisibility(
                visible = showVoiceSelector,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    VoiceSelector(
                        selectedVoiceId = selectedVoiceId,
                        onVoiceSelected = { voice ->
                            onVoiceSelected(voice)
                            showVoiceSelector = false
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
                        },
                        isPreviewPlaying = isPreviewPlaying,
                        previewingVoiceId = previewingVoiceId
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Zinc700)
            Spacer(modifier = Modifier.height(20.dp))

            // Speed control
            SliderSetting(
                icon = Icons.Default.Speed,
                label = "Speed",
                value = speed,
                valueRange = 0.5f..2.5f,
                steps = 7,
                valueDisplay = { "${String.format("%.2f", it).trimEnd('0').trimEnd('.')}x" },
                onValueChange = onSpeedChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pitch control
            SliderSetting(
                icon = Icons.Default.GraphicEq,
                label = "Pitch",
                value = pitch,
                valueRange = 0.5f..2.0f,
                steps = 5,
                valueDisplay = { "${String.format("%.1f", it).trimEnd('0').trimEnd('.')}x" },
                onValueChange = onPitchChange
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Zinc700)
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle settings
            ToggleSetting(
                label = "Auto-scroll",
                description = "Scroll to current sentence",
                isChecked = autoScroll,
                onCheckedChange = onAutoScrollChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleSetting(
                label = "Highlight sentence",
                description = "Highlight the current sentence",
                isChecked = highlightSentence,
                onCheckedChange = onHighlightChange
            )
        }
    }
}

@Composable
private fun SliderSetting(
    icon: ImageVector,
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Orange500,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Zinc800
            ) {
                Text(
                    text = valueDisplay(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = Orange400,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Orange500,
                activeTrackColor = Orange500,
                inactiveTrackColor = Zinc700,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Min/Max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = valueDisplay(valueRange.start),
                style = MaterialTheme.typography.labelSmall,
                color = Zinc600
            )
            Text(
                text = valueDisplay(valueRange.endInclusive),
                style = MaterialTheme.typography.labelSmall,
                color = Zinc600
            )
        }
    }
}

@Composable
private fun ToggleSetting(
    label: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Zinc500
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Orange500,
                uncheckedThumbColor = Zinc400,
                uncheckedTrackColor = Zinc700
            )
        )
    }
}

/**
 * Compact TTS settings for quick access
 */
@Composable
fun QuickTTSSettings(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onOpenFullSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedPresets = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speedPresets.forEach { preset ->
            val isSelected = kotlin.math.abs(speed - preset) < 0.01f

            Surface(
                onClick = { onSpeedChange(preset) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Orange500 else Zinc800
            ) {
                Text(
                    text = "${preset}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else Zinc300,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}