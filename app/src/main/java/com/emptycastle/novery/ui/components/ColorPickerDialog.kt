package com.emptycastle.novery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * A comprehensive color picker dialog with:
 * - Preset colors
 * - Hue slider
 * - Saturation/Brightness picker
 * - Hex input
 * - Live preview
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    title: String,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    // HSV state
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var hexInput by remember { mutableStateOf("") }

    // Initialize from current color
    LaunchedEffect(currentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexInput = colorToHex(currentColor)
    }

    val selectedColor = Color.hsv(hue, saturation, brightness)

    // Update hex when HSV changes
    LaunchedEffect(hue, saturation, brightness) {
        hexInput = colorToHex(selectedColor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = selectedColor
                )
                Text(title, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color Preview
                ColorPreview(
                    currentColor = currentColor,
                    newColor = selectedColor
                )

                // Saturation/Brightness Picker
                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onSaturationBrightnessChange = { s, b ->
                        saturation = s
                        brightness = b
                    }
                )

                // Hue Slider
                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it }
                )

                // Hex Input
                HexInput(
                    hexValue = hexInput,
                    onHexChange = { hex ->
                        hexInput = hex
                        parseHexColor(hex)?.let { color ->
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    }
                )

                // Preset Colors
                Text(
                    "Presets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { preset ->
                        PresetColorItem(
                            color = preset,
                            isSelected = colorToHex(selectedColor) == colorToHex(preset),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(preset.toArgb(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onColorSelected(selectedColor)
                }
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ColorPreview(
    currentColor: Color,
    newColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            // Current color
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(currentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Current",
                    style = MaterialTheme.typography.labelSmall,
                    color = getContrastColor(currentColor)
                )
            }

            // New color
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(newColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "New",
                    style = MaterialTheme.typography.labelSmall,
                    color = getContrastColor(newColor)
                )
            }
        }
    }
}

@Composable
private fun SaturationBrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onSaturationBrightnessChange: (Float, Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val b = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                    onSaturationBrightnessChange(s, b)
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val b = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    onSaturationBrightnessChange(s, b)
                }
            }
    ) {
        // Background gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            // White to Hue color (horizontal)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
                )
            )
            // Transparent to Black (vertical)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )
        }

        // Selection indicator - calculate positions INSIDE the Canvas scope
        Canvas(modifier = Modifier.fillMaxSize()) {
            val indicatorX = saturation * size.width
            val indicatorY = (1f - brightness) * size.height

            // Outer ring (white)
            drawCircle(
                color = Color.White,
                radius = 14.dp.toPx(),
                center = Offset(x = indicatorX, y = indicatorY),
                style = Stroke(width = 3.dp.toPx())
            )
            // Inner ring (black for contrast)
            drawCircle(
                color = Color.Black,
                radius = 11.dp.toPx(),
                center = Offset(x = indicatorX, y = indicatorY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Hue: ${hue.roundToInt()}°",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Rainbow gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradient = Brush.horizontalGradient(
                    colors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                )
                drawRect(brush = gradient)
            }

            // Slider
            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.hsv(hue, 1f, 1f),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun HexInput(
    hexValue: String,
    onHexChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = hexValue,
        onValueChange = { value ->
            // Only allow valid hex characters
            val filtered = value.uppercase().filter { it in "0123456789ABCDEF#" }
            if (filtered.length <= 7) {
                onHexChange(filtered)
            }
        },
        label = { Text("Hex Color") },
        placeholder = { Text("#RRGGBB") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        leadingIcon = {
            parseHexColor(hexValue)?.let { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PresetColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderWidth by animateFloatAsState(
        targetValue = if (isSelected) 3f else 0f,
        label = "border"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = borderWidth.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(20.dp),
                tint = getContrastColor(color)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}

private fun parseHexColor(hex: String): Color? {
    val cleanHex = hex.removePrefix("#")
    if (cleanHex.length != 6) return null

    return try {
        val colorInt = android.graphics.Color.parseColor("#$cleanHex")
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}

private fun getContrastColor(color: Color): Color {
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Preset colors for quick selection
 */
private val presetColors = listOf(
    // Reds
    Color(0xFFEF4444), // Red-500
    Color(0xFFF43F5E), // Rose-500
    Color(0xFFEC4899), // Pink-500

    // Oranges/Yellows
    Color(0xFFF97316), // Orange-500
    Color(0xFFF59E0B), // Amber-500
    Color(0xFFEAB308), // Yellow-500

    // Greens
    Color(0xFF84CC16), // Lime-500
    Color(0xFF22C55E), // Green-500
    Color(0xFF10B981), // Emerald-500
    Color(0xFF14B8A6), // Teal-500

    // Blues
    Color(0xFF06B6D4), // Cyan-500
    Color(0xFF0EA5E9), // Sky-500
    Color(0xFF3B82F6), // Blue-500
    Color(0xFF6366F1), // Indigo-500

    // Purples
    Color(0xFF8B5CF6), // Violet-500
    Color(0xFFA855F7), // Purple-500
    Color(0xFFD946EF), // Fuchsia-500

    // Neutrals
    Color(0xFF71717A), // Zinc-500
    Color(0xFF78716C), // Stone-500
    Color(0xFFA1A1AA), // Zinc-400
)