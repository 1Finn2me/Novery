package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.ReaderSettings

private object TransTheme {
    val background = Color(0xFF0A0A0B)
    val surface = Color(0xFF141416)
    val surfaceVariant = Color(0xFF1C1C1F)
    val surfaceElevated = Color(0xFF232328)

    val primary = Color(0xFFFF6B35)
    val primaryMuted = Color(0xFFFF6B35).copy(alpha = 0.15f)

    val textPrimary = Color(0xFFFAFAFA)
    val textSecondary = Color(0xFFA1A1AA)
    val textMuted = Color(0xFF71717A)

    val border = Color(0xFF3F3F46)

    val blue = Color(0xFF3B82F6)
    val blueMuted = Color(0xFF3B82F6).copy(alpha = 0.15f)

    val cornerRadiusMedium = 20.dp
    val cornerRadiusSmall = 14.dp
}

private val LANGUAGES = listOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "zh" to "Chinese",
    "ja" to "Japanese",
    "ko" to "Korean"
)

@Composable
fun TranslationPanel(
    settings: ReaderSettings,
    translationStatus: String?,
    onToggleTranslation: () -> Unit,
    onTargetLangChange: (String) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        shape = RoundedCornerShape(TransTheme.cornerRadiusMedium),
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TransTheme.surfaceElevated, TransTheme.surface)
                    )
                )
                .border(
                    width = 1.dp,
                    color = TransTheme.border.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(TransTheme.cornerRadiusMedium)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TranslationHeader(
                    onDismiss = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Main Toggle
                    TransToggleSetting(
                        icon = Icons.Default.Translate,
                        title = "Enable Translation",
                        subtitle = "Translate content automatically",
                        checked = settings.translationEnabled,
                        onCheckedChange = { onToggleTranslation() }
                    )

                    AnimatedVisibility(
                        visible = settings.translationEnabled || translationStatus != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Mode Selection
                            TransSectionCard(title = "Translation Mode") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ModeChip(
                                        icon = Icons.Default.CloudOff,
                                        label = "Offline",
                                        isSelected = !settings.useOnlineTranslation,
                                        onClick = { onModeChange(false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ModeChip(
                                        icon = Icons.Default.Cloud,
                                        label = "Online",
                                        isSelected = settings.useOnlineTranslation,
                                        onClick = { onModeChange(true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Language Selection
                            TransSectionCard(title = "Target Language") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(LANGUAGES) { (code, name) ->
                                            val isSelected = settings.targetLang == code
                                            LanguageChip(
                                                name = name,
                                                isSelected = isSelected,
                                                onClick = { onTargetLangChange(code) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Indicator (Embedded in Panel)
                            if (translationStatus != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TransTheme.blueMuted,
                                    border = BorderStroke(1.dp, TransTheme.blue.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (translationStatus.contains("download", ignoreCase = true) || 
                                            translationStatus.contains("Preparing", ignoreCase = true)) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = TransTheme.blue
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = TransTheme.blue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = translationStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TransTheme.blue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Disclaimer/Info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TransTheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TransTheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Translation replaces the original text in-place. Per-novel settings are saved automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TransTheme.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = TransTheme.primaryMuted,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = TransTheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "Translation",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TransTheme.textPrimary
            )
        }

        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = TransTheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TransTheme.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = if (checked) TransTheme.primaryMuted else TransTheme.surface,
        border = BorderStroke(1.dp, if (checked) TransTheme.primary.copy(alpha = 0.3f) else TransTheme.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) TransTheme.primary else TransTheme.textMuted,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal),
                        color = if (checked) TransTheme.textPrimary else TransTheme.textSecondary
                    )
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TransTheme.textMuted)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TransTheme.primary
                )
            )
        }
    }
}

@Composable
private fun TransSectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TransTheme.cornerRadiusSmall),
        color = TransTheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TransTheme.textSecondary
            )
            content()
        }
    }
}

@Composable
private fun ModeChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TransTheme.primary else TransTheme.surface,
        border = if (!isSelected) BorderStroke(1.dp, TransTheme.border) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TransTheme.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (isSelected) Color.White else TransTheme.textMuted
            )
        }
    }
}

@Composable
private fun LanguageChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TransTheme.primary else TransTheme.surface,
        border = if (!isSelected) BorderStroke(1.dp, TransTheme.border) else null
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) Color.White else TransTheme.textMuted,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
