package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.tts.LanguageGroup
import com.emptycastle.novery.tts.VoiceGender
import com.emptycastle.novery.tts.VoiceInfo
import com.emptycastle.novery.tts.VoiceManager
import com.emptycastle.novery.tts.VoiceQuality
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800

/**
 * Full voice selector with language groups and search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelector(
    selectedVoiceId: String?,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onPreviewVoice: (VoiceInfo) -> Unit,
    onStopPreview: () -> Unit,
    modifier: Modifier = Modifier,
    isPreviewPlaying: Boolean = false,
    previewingVoiceId: String? = null
) {
    val languageGroups by VoiceManager.languageGroups.collectAsState()
    val isLoading by VoiceManager.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var expandedLanguage by remember { mutableStateOf<String?>(null) }

    // Filter voices by search
    val filteredGroups = remember(languageGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            languageGroups
        } else {
            languageGroups.mapNotNull { group ->
                val filteredVoices = group.voices.filter { voice ->
                    voice.displayName.contains(searchQuery, ignoreCase = true) ||
                            voice.languageDisplayName.contains(searchQuery, ignoreCase = true)
                }
                if (filteredVoices.isNotEmpty()) {
                    group.copy(voices = filteredVoices)
                } else {
                    null
                }
            }
        }
    }

    // Stop preview when unmounted
    DisposableEffect(Unit) {
        onDispose { onStopPreview() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("Search voices...", color = Zinc500)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Zinc500
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange500,
                unfocusedBorderColor = Zinc700,
                focusedContainerColor = Zinc800,
                unfocusedContainerColor = Zinc800,
                cursorColor = Orange500
            ),
            singleLine = true
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Orange500)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading voices...",
                        color = Zinc400,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (filteredGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Zinc600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No voices found" else "No voices available",
                        color = Zinc400,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredGroups, key = { it.languageCode }) { group ->
                    LanguageGroupItem(
                        group = group,
                        isExpanded = expandedLanguage == group.languageCode,
                        selectedVoiceId = selectedVoiceId,
                        isPreviewPlaying = isPreviewPlaying,
                        previewingVoiceId = previewingVoiceId,
                        onExpandToggle = {
                            expandedLanguage = if (expandedLanguage == group.languageCode) {
                                null
                            } else {
                                group.languageCode
                            }
                        },
                        onVoiceSelected = onVoiceSelected,
                        onPreviewVoice = onPreviewVoice,
                        onStopPreview = onStopPreview
                    )
                }
            }
        }
    }
}

/**
 * Language group item with expandable voice list
 */
@Composable
private fun LanguageGroupItem(
    group: LanguageGroup,
    isExpanded: Boolean,
    selectedVoiceId: String?,
    isPreviewPlaying: Boolean,
    previewingVoiceId: String?,
    onExpandToggle: () -> Unit,
    onVoiceSelected: (VoiceInfo) -> Unit,
    onPreviewVoice: (VoiceInfo) -> Unit,
    onStopPreview: () -> Unit
) {
    val hasSelectedVoice = group.voices.any { it.id == selectedVoiceId }

    val backgroundColor by animateColorAsState(
        targetValue = if (hasSelectedVoice) Orange500.copy(alpha = 0.1f) else Zinc800,
        label = "groupBg"
    )

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag
                if (group.flag != null) {
                    Text(
                        text = group.flag,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Language name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasSelectedVoice) Orange400 else Color.White
                    )
                    Text(
                        text = "${group.voiceCount} voice${if (group.voiceCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Zinc400
                    )
                }

                // Quality indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (group.hasPremiumVoices) {
                        QualityBadge(quality = VoiceQuality.PREMIUM)
                    }
                    if (group.hasLocalVoices) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Local voices available",
                            tint = Zinc400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expand icon
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Zinc400,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            // Voice list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = Zinc700)

                    group.voices.forEach { voice ->
                        VoiceListItem(
                            voice = voice,
                            isSelected = voice.id == selectedVoiceId,
                            isPreviewPlaying = isPreviewPlaying && previewingVoiceId == voice.id,
                            onSelect = { onVoiceSelected(voice) },
                            onPreview = {
                                if (isPreviewPlaying && previewingVoiceId == voice.id) {
                                    onStopPreview()
                                } else {
                                    onPreviewVoice(voice)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual voice item
 */
@Composable
private fun VoiceListItem(
    voice: VoiceInfo,
    isSelected: Boolean,
    isPreviewPlaying: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Orange500.copy(alpha = 0.15f) else Color.Transparent,
        label = "voiceBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) Orange500 else Zinc700)
                .then(
                    if (isSelected) Modifier else Modifier.border(1.dp, Zinc600, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Voice info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = voice.shortName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Orange400 else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Gender badge
                if (voice.gender != VoiceGender.UNKNOWN) {
                    GenderBadge(gender = voice.gender)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quality
                QualityBadge(quality = voice.quality)

                // Network indicator
                if (voice.isNetworkRequired) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Requires network",
                        tint = Zinc500,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Country
                if (voice.countryDisplayName.isNotBlank()) {
                    Text(
                        text = "• ${voice.countryDisplayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Zinc500
                    )
                }
            }
        }

        // Preview button
        IconButton(
            onClick = onPreview,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPreviewPlaying) "Stop preview" else "Preview voice",
                tint = if (isPreviewPlaying) Orange500 else Zinc400
            )
        }
    }
}

/**
 * Voice quality badge
 */
@Composable
private fun QualityBadge(quality: VoiceQuality) {
    val (color, text) = when (quality) {
        VoiceQuality.PREMIUM -> Orange500 to "Premium"
        VoiceQuality.HIGH -> Color(0xFF4CAF50) to "HD"
        VoiceQuality.NORMAL -> Zinc500 to "Standard"
        VoiceQuality.LOW -> Zinc600 to "Basic"
        VoiceQuality.UNKNOWN -> Zinc600 to ""
    }

    if (text.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quality == VoiceQuality.PREMIUM) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Voice gender badge
 */
@Composable
private fun GenderBadge(gender: VoiceGender) {
    val text = when (gender) {
        VoiceGender.FEMALE -> "♀"
        VoiceGender.MALE -> "♂"
        VoiceGender.NEUTRAL -> "◎"
        VoiceGender.UNKNOWN -> ""
    }

    if (text.isNotBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Zinc400
        )
    }
}

/**
 * Compact voice selector for inline use
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactVoiceSelector(
    selectedVoice: VoiceInfo?,
    onOpenFullSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenFullSelector,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Zinc800,
        border = androidx.compose.foundation.BorderStroke(1.dp, Zinc700)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = Orange500,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
                Text(
                    text = selectedVoice?.shortName ?: "Select a voice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedVoice != null) Color.White else Zinc500,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Select voice",
                tint = Zinc400
            )
        }
    }
}

/**
 * Quick voice filter chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceFilterChips(
    selectedFilter: VoiceFilter,
    onFilterSelected: (VoiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(VoiceFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                leadingIcon = if (selectedFilter == filter) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Orange500,
                    selectedLabelColor = Color.White,
                    containerColor = Zinc800,
                    labelColor = Zinc300
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Zinc700,
                    selectedBorderColor = Orange500,
                    enabled = true,
                    selected = selectedFilter == filter
                )
            )
        }
    }
}

enum class VoiceFilter(val displayName: String) {
    ALL("All"),
    LOCAL("Local Only"),
    PREMIUM("Premium"),
    ENGLISH("English"),
    OTHER("Other Languages")
}