package com.emptycastle.novery.ui.screens.reader.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.FontCategory
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.PageAnimation
import com.emptycastle.novery.domain.model.ProgressStyle
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.ReadingDirection
import com.emptycastle.novery.domain.model.ScreenOrientation
import com.emptycastle.novery.domain.model.ScrollMode
import com.emptycastle.novery.domain.model.TapAction
import com.emptycastle.novery.domain.model.TapZoneConfig
import com.emptycastle.novery.domain.model.ThemeCategory
import com.emptycastle.novery.domain.model.VolumeKeyDirection
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.domain.model.FontWeight as ReaderFontWeight
import com.emptycastle.novery.domain.model.TextAlign as ReaderTextAlign

// =============================================================================
// SETTINGS TABS
// =============================================================================

enum class SettingsTab(
    val title: String,
    val icon: ImageVector
) {
    APPEARANCE("Theme", Icons.Outlined.Palette),
    TYPOGRAPHY("Text", Icons.Outlined.TextFields),
    LAYOUT("Layout", Icons.Outlined.SpaceBar),
    READING("Reading", Icons.Outlined.Gesture),
    TTS("TTS", Icons.Outlined.RecordVoiceOver),
    ADVANCED("More", Icons.Outlined.Tune)
}

// =============================================================================
// MAIN SCREEN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsScreen(
    onBack: () -> Unit
) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.readerSettings.collectAsState()

    val colors = remember(settings.theme) {
        ReaderColors.fromTheme(settings.theme)
    }

    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Count modified settings
    val modifiedCount = remember(settings) {
        countModifiedSettings(settings, ReaderSettings.DEFAULT)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reader Settings",
                            color = colors.text
                        )
                        if (modifiedCount > 0) {
                            Text(
                                text = "$modifiedCount changes from default",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.icon
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            preferencesManager.updateReaderSettings(ReaderSettings.DEFAULT)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colors.accent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            color = colors.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    scrolledContainerColor = colors.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            SettingsTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                colors = colors
            )

            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))

            // Live Preview Card (collapsible)
            var showPreview by remember { mutableStateOf(true) }
            LivePreviewSection(
                settings = settings,
                colors = colors,
                isExpanded = showPreview,
                onToggle = { showPreview = !showPreview }
            )

            // Content
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "settings_content"
                ) { tab ->
                    when (tab) {
                        SettingsTab.APPEARANCE -> AppearanceSettings(
                            settings = settings,
                            colors = colors,
                            onSettingsChange = { preferencesManager.updateReaderSettings(it) }
                        )
                        SettingsTab.TYPOGRAPHY -> TypographySettings(
                            settings = settings,
                            colors = colors,
                            onSettingsChange = { preferencesManager.updateReaderSettings(it) }
                        )
                        SettingsTab.LAYOUT -> LayoutSettings(
                            settings = settings,
                            colors = colors,
                            onSettingsChange = { preferencesManager.updateReaderSettings(it) }
                        )
                        SettingsTab.READING -> ReadingSettings(
                            settings = settings,
                            colors = colors,
                            onSettingsChange = { preferencesManager.updateReaderSettings(it) }
                        )
                        SettingsTab.TTS -> TTSSettings(
                            colors = colors,
                            preferencesManager = preferencesManager
                        )
                        SettingsTab.ADVANCED -> AdvancedSettings(
                            settings = settings,
                            colors = colors,
                            onSettingsChange = { preferencesManager.updateReaderSettings(it) }
                        )
                    }
                }
            }
        }
    }
}

private fun countModifiedSettings(current: ReaderSettings, default: ReaderSettings): Int {
    var count = 0
    if (current.fontSize != default.fontSize) count++
    if (current.fontFamily != default.fontFamily) count++
    if (current.fontWeight != default.fontWeight) count++
    if (current.lineHeight != default.lineHeight) count++
    if (current.letterSpacing != default.letterSpacing) count++
    if (current.wordSpacing != default.wordSpacing) count++
    if (current.textAlign != default.textAlign) count++
    if (current.hyphenation != default.hyphenation) count++
    if (current.maxWidth != default.maxWidth) count++
    if (current.marginHorizontal != default.marginHorizontal) count++
    if (current.marginVertical != default.marginVertical) count++
    if (current.paragraphSpacing != default.paragraphSpacing) count++
    if (current.paragraphIndent != default.paragraphIndent) count++
    if (current.theme != default.theme) count++
    if (current.brightness != default.brightness) count++
    if (current.warmthFilter != default.warmthFilter) count++
    if (current.scrollMode != default.scrollMode) count++
    if (current.pageAnimation != default.pageAnimation) count++
    if (current.readingDirection != default.readingDirection) count++
    if (current.scrollSensitivity != default.scrollSensitivity) count++
    if (current.volumeKeyNavigation != default.volumeKeyNavigation) count++
    if (current.volumeKeyDirection != default.volumeKeyDirection) count++
    if (current.autoHideControlsDelay != default.autoHideControlsDelay) count++
    if (current.edgeGestures != default.edgeGestures) count++
    return count
}

// =============================================================================
// LIVE PREVIEW
// =============================================================================

@Composable
private fun LivePreviewSection(
    settings: ReaderSettings,
    colors: ReaderColors,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        Surface(
            onClick = onToggle,
            color = colors.surface.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.iconSecondary
                    )
                    Text(
                        text = "Live Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val previewColors = remember(settings.theme) {
                ReaderColors.fromTheme(settings.theme)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = previewColors.background,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .padding(
                            horizontal = settings.marginHorizontal.dp,
                            vertical = settings.marginVertical.dp.coerceAtMost(12.dp)
                        )
                ) {
                    Text(
                        text = buildString {
                            append("    ".repeat((settings.paragraphIndent).toInt()))
                            append("The quick brown fox jumps over the lazy dog. ")
                            append("This is how your text will appear with the current settings. ")
                            append("Adjust the options below to customize your reading experience.")
                        },
                        fontSize = settings.fontSize.sp,
                        lineHeight = (settings.fontSize * settings.lineHeight).sp,
                        letterSpacing = settings.letterSpacing.sp,
                        textAlign = when (settings.textAlign) {
                            ReaderTextAlign.LEFT -> TextAlign.Start
                            ReaderTextAlign.RIGHT -> TextAlign.End
                            ReaderTextAlign.CENTER -> TextAlign.Center
                            ReaderTextAlign.JUSTIFY -> TextAlign.Justify
                        },
                        color = previewColors.text
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB ROW
// =============================================================================

@Composable
private fun SettingsTabRow(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    colors: ReaderColors
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SettingsTab.entries) { tab ->
            val isSelected = tab == selectedTab
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.6f,
                label = "tab_alpha"
            )

            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    colors.accent.copy(alpha = 0.15f)
                else
                    Color.Transparent,
                border = if (isSelected)
                    BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                else
                    null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(animatedAlpha),
                        tint = if (isSelected) colors.accent else colors.textSecondary
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) colors.accent else colors.textSecondary,
                        modifier = Modifier.alpha(animatedAlpha)
                    )
                }
            }
        }
    }
}

// =============================================================================
// APPEARANCE SETTINGS
// =============================================================================

@Composable
private fun AppearanceSettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Quick presets
        SettingSection(
            title = "Quick Presets",
            icon = Icons.Default.Speed,
            colors = colors
        ) {
            PresetSelector(
                currentSettings = settings,
                colors = colors,
                onPresetSelected = onSettingsChange
            )
        }

        // Theme selection
        SettingSection(
            title = "Color Theme",
            icon = Icons.Default.Palette,
            colors = colors
        ) {
            ThemeSelector(
                selectedTheme = settings.theme,
                colors = colors,
                onThemeChange = { onSettingsChange(settings.copy(theme = it)) }
            )
        }

        // Brightness control
        SettingSection(
            title = "Brightness",
            icon = Icons.Default.Brightness6,
            colors = colors
        ) {
            BrightnessControl(
                brightness = settings.brightness,
                colors = colors,
                onBrightnessChange = { onSettingsChange(settings.copy(brightness = it)) }
            )
        }

        // Warmth filter
        SettingSection(
            title = "Night Warmth",
            subtitle = "Reduce blue light for comfortable night reading",
            icon = Icons.Default.WbSunny,
            colors = colors
        ) {
            WarmthControl(
                warmth = settings.warmthFilter,
                colors = colors,
                onWarmthChange = { onSettingsChange(settings.copy(warmthFilter = it)) }
            )
        }
    }
}

@Composable
private fun PresetSelector(
    currentSettings: ReaderSettings,
    colors: ReaderColors,
    onPresetSelected: (ReaderSettings) -> Unit
) {
    val presets = remember { ReaderSettings.getPresets() }
    var showAllPresets by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main presets row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayedPresets = if (showAllPresets) presets else presets.take(5)
            items(displayedPresets) { (name, preset) ->
                PresetChip(
                    name = name,
                    isSelected = currentSettings == preset,
                    colors = colors,
                    onClick = { onPresetSelected(preset) }
                )
            }

            if (!showAllPresets && presets.size > 5) {
                item {
                    Surface(
                        onClick = { showAllPresets = true },
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "+${presets.size - 5} more",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Preset descriptions
        AnimatedVisibility(visible = showAllPresets) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (name, preset) ->
                    PresetDetailCard(
                        name = name,
                        preset = preset,
                        isSelected = currentSettings == preset,
                        colors = colors,
                        onClick = { onPresetSelected(preset) }
                    )
                }

                TextButton(
                    onClick = { showAllPresets = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Show less", color = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun PresetDetailCard(
    name: String,
    preset: ReaderSettings,
    isSelected: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.accent.copy(alpha = 0.1f) else colors.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colors.accent else colors.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) colors.accent else colors.text
                )
                Text(
                    text = "${preset.fontSize}sp · ${preset.fontFamily.displayName} · ${preset.theme.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    name: String,
    isSelected: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colors.accent else colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) colors.accent else colors.border
        )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) colors.onAccent else colors.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: ReaderTheme,
    colors: ReaderColors,
    onThemeChange: (ReaderTheme) -> Unit
) {
    var expandedCategory by remember { mutableStateOf<ThemeCategory?>(null) }
    val groupedThemes = remember { ReaderTheme.getByCategory() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick access to common themes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ReaderTheme.LIGHT,
                ReaderTheme.SEPIA,
                ReaderTheme.DARK,
                ReaderTheme.AMOLED
            ).forEach { theme ->
                ThemeButton(
                    theme = theme,
                    isSelected = selectedTheme == theme,
                    colors = colors,
                    onClick = { onThemeChange(theme) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Expandable categories for all themes
        ThemeCategory.entries.forEach { category ->
            val themes = groupedThemes[category] ?: emptyList()
            if (themes.isNotEmpty()) {
                ThemeCategorySection(
                    category = category,
                    themes = themes,
                    selectedTheme = selectedTheme,
                    isExpanded = expandedCategory == category,
                    colors = colors,
                    onExpandChange = {
                        expandedCategory = if (expandedCategory == category) null else category
                    },
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

@Composable
private fun ThemeCategorySection(
    category: ThemeCategory,
    themes: List<ReaderTheme>,
    selectedTheme: ReaderTheme,
    isExpanded: Boolean,
    colors: ReaderColors,
    onExpandChange: () -> Unit,
    onThemeChange: (ReaderTheme) -> Unit
) {
    val hasSelectedTheme = themes.contains(selectedTheme)

    Column {
        Surface(
            onClick = onExpandChange,
            shape = RoundedCornerShape(8.dp),
            color = if (hasSelectedTheme)
                colors.accent.copy(alpha = 0.08f)
            else
                Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (hasSelectedTheme) colors.accent else colors.text
                    )

                    if (hasSelectedTheme) {
                        Badge(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.ExpandLess
                    else
                        Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.iconSecondary
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(themes) { theme ->
                    ThemeGridItem(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        colors = colors,
                        onClick = { onThemeChange(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeButton(
    theme: ReaderTheme,
    isSelected: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = remember(theme) { ReaderColors.fromTheme(theme) }
    val icon = remember(theme) {
        when {
            theme.isOled -> Icons.Default.Contrast
            theme == ReaderTheme.SEPIA || theme == ReaderTheme.PARCHMENT -> Icons.Default.Coffee
            theme.isDark -> Icons.Default.DarkMode
            theme.isHighContrast -> Icons.Default.Contrast
            else -> Icons.Default.LightMode
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.border,
        label = "theme_border"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = "theme_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = themeColors.background,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "${theme.displayName} theme",
                    modifier = Modifier.size(20.dp),
                    tint = themeColors.text
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeGridItem(
    theme: ReaderTheme,
    isSelected: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit
) {
    val themeColors = remember(theme) { ReaderColors.fromTheme(theme) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = themeColors.background,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(themeColors.background)
                    .border(1.dp, themeColors.text.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = themeColors.text
                )
            }

            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) colors.accent else colors.textSecondary
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(12.dp),
                    tint = colors.accent
                )
            }
        }
    }
}

@Composable
private fun BrightnessControl(
    brightness: Float,
    colors: ReaderColors,
    onBrightnessChange: (Float) -> Unit
) {
    val isSystemBrightness = brightness == ReaderSettings.BRIGHTNESS_SYSTEM

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSystemBrightness) "Using system brightness" else "${(brightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Surface(
                onClick = {
                    onBrightnessChange(
                        if (isSystemBrightness) 0.5f else ReaderSettings.BRIGHTNESS_SYSTEM
                    )
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isSystemBrightness)
                    colors.accent.copy(alpha = 0.15f)
                else
                    colors.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessAuto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSystemBrightness) colors.accent else colors.iconSecondary
                    )
                    Text(
                        text = "Auto",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSystemBrightness) colors.accent else colors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(visible = !isSystemBrightness) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Brightness4,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )

                Slider(
                    value = if (isSystemBrightness) 0.5f else brightness,
                    onValueChange = { onBrightnessChange(it) },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.progressTrack
                    )
                )

                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )
            }
        }
    }
}

@Composable
private fun WarmthControl(
    warmth: Float,
    colors: ReaderColors,
    onWarmthChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (warmth == 0f) "Off" else "${(warmth * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brightness4,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.iconSecondary
            )

            Slider(
                value = warmth,
                onValueChange = onWarmthChange,
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9800),
                    activeTrackColor = Color(0xFFFF9800),
                    inactiveTrackColor = colors.progressTrack
                )
            )

            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFFF9800)
            )
        }

        // Preview box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFF4E0),
                            Color(0xFFFFE4B5),
                            Color(0xFFFFD49A)
                        )
                    )
                )
        )
    }
}
// =============================================================================
// TYPOGRAPHY SETTINGS
// =============================================================================

@Composable
private fun TypographySettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Font size
        SettingSection(
            title = "Font Size",
            icon = Icons.Default.FormatSize,
            colors = colors,
            trailing = {
                Text(
                    text = "${settings.fontSize}sp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
            }
        ) {
            FontSizeControl(
                fontSize = settings.fontSize,
                colors = colors,
                onFontSizeChange = { onSettingsChange(settings.copy(fontSize = it)) }
            )
        }

        // Font family
        SettingSection(
            title = "Font Family",
            icon = Icons.Default.TextFields,
            colors = colors
        ) {
            FontFamilySelector(
                selectedFamily = settings.fontFamily,
                colors = colors,
                onFontFamilyChange = { onSettingsChange(settings.copy(fontFamily = it)) }
            )
        }

        // Font weight
        SettingSection(
            title = "Font Weight",
            icon = Icons.AutoMirrored.Filled.Sort,
            colors = colors
        ) {
            FontWeightSelector(
                selectedWeight = settings.fontWeight,
                supportedWeights = settings.fontFamily.supportsAllWeights,
                colors = colors,
                onWeightChange = { onSettingsChange(settings.copy(fontWeight = it)) }
            )
        }

        // Line height
        SettingSection(
            title = "Line Spacing",
            icon = Icons.Default.FormatLineSpacing,
            colors = colors,
            trailing = {
                Text(
                    text = "×${String.format("%.1f", settings.lineHeight)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent
                )
            }
        ) {
            LineHeightControl(
                lineHeight = settings.lineHeight,
                colors = colors,
                onLineHeightChange = { onSettingsChange(settings.copy(lineHeight = it)) }
            )
        }

        // Letter spacing
        SettingSection(
            title = "Letter Spacing",
            icon = Icons.Default.SpaceBar,
            colors = colors,
            trailing = {
                Text(
                    text = "${String.format("%.2f", settings.letterSpacing)}em",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        ) {
            LetterSpacingControl(
                letterSpacing = settings.letterSpacing,
                colors = colors,
                onLetterSpacingChange = { onSettingsChange(settings.copy(letterSpacing = it)) }
            )
        }

        // Word spacing (NEW)
        SettingSection(
            title = "Word Spacing",
            icon = Icons.Default.SpaceBar,
            colors = colors,
            trailing = {
                Text(
                    text = "×${String.format("%.1f", settings.wordSpacing)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        ) {
            WordSpacingControl(
                wordSpacing = settings.wordSpacing,
                colors = colors,
                onWordSpacingChange = { onSettingsChange(settings.copy(wordSpacing = it)) }
            )
        }

        // Text alignment
        SettingSection(
            title = "Text Alignment",
            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
            colors = colors
        ) {
            TextAlignSelector(
                selectedAlign = settings.textAlign,
                colors = colors,
                onAlignChange = { onSettingsChange(settings.copy(textAlign = it)) }
            )
        }

        // Hyphenation
        SettingSwitch(
            title = "Enable Hyphenation",
            subtitle = "Break long words at line ends (best with justified text)",
            checked = settings.hyphenation,
            colors = colors,
            onCheckedChange = { onSettingsChange(settings.copy(hyphenation = it)) }
        )
    }
}

@Composable
private fun FontSizeControl(
    fontSize: Int,
    colors: ReaderColors,
    onFontSizeChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onFontSizeChange((fontSize - 1).coerceAtLeast(ReaderSettings.MIN_FONT_SIZE)) },
                enabled = fontSize > ReaderSettings.MIN_FONT_SIZE,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colors.icon,
                    disabledContentColor = colors.icon.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.TextDecrease,
                    contentDescription = "Decrease font size"
                )
            }

            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                modifier = Modifier.weight(1f),
                valueRange = ReaderSettings.MIN_FONT_SIZE.toFloat()..ReaderSettings.MAX_FONT_SIZE.toFloat(),
                steps = ReaderSettings.MAX_FONT_SIZE - ReaderSettings.MIN_FONT_SIZE - 1,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.progressTrack
                )
            )

            IconButton(
                onClick = { onFontSizeChange((fontSize + 1).coerceAtMost(ReaderSettings.MAX_FONT_SIZE)) },
                enabled = fontSize < ReaderSettings.MAX_FONT_SIZE,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colors.icon,
                    disabledContentColor = colors.icon.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.TextIncrease,
                    contentDescription = "Increase font size"
                )
            }
        }

        // Quick size buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(14, 16, 18, 20, 24).forEach { size ->
                val isSelected = fontSize == size
                Surface(
                    onClick = { onFontSizeChange(size) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = "$size",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.text,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FontFamilySelector(
    selectedFamily: FontFamily,
    colors: ReaderColors,
    onFontFamilyChange: (FontFamily) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(selectedFamily.category) }
    val groupedFonts = remember { FontFamily.getByCategory() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FontCategory.entries.sortedBy { it.sortOrder }) { category ->
                val isSelected = category == selectedCategory

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                        selectedLabelColor = colors.accent
                    )
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val fonts = groupedFonts[selectedCategory] ?: emptyList()
            items(fonts) { font ->
                FontFamilyChip(
                    font = font,
                    isSelected = selectedFamily == font,
                    colors = colors,
                    onClick = { onFontFamilyChange(font) }
                )
            }
        }

        // Selected font description
        if (selectedFamily.description.isNotEmpty()) {
            Text(
                text = selectedFamily.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FontFamilyChip(
    font: FontFamily,
    isSelected: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.accent else colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) colors.accent else colors.border
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 100.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isSelected) colors.onAccent else colors.text
            )

            Text(
                text = font.displayName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) colors.onAccent else colors.textSecondary
            )
        }
    }
}

@Composable
private fun FontWeightSelector(
    selectedWeight: ReaderFontWeight,
    supportedWeights: Boolean,
    colors: ReaderColors,
    onWeightChange: (ReaderFontWeight) -> Unit
) {
    if (!supportedWeights) {
        Text(
            text = "This font only supports regular weight",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ReaderFontWeight.COMMON) { weight ->
            val isSelected = weight == selectedWeight

            Surface(
                onClick = { onWeightChange(weight) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.accent else colors.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) colors.accent else colors.border
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = when (weight) {
                            ReaderFontWeight.LIGHT -> FontWeight.Light
                            ReaderFontWeight.REGULAR -> FontWeight.Normal
                            ReaderFontWeight.MEDIUM -> FontWeight.Medium
                            ReaderFontWeight.BOLD -> FontWeight.Bold
                            else -> FontWeight.Normal
                        },
                        color = if (isSelected) colors.onAccent else colors.text
                    )
                    Text(
                        text = weight.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.onAccent else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LineHeightControl(
    lineHeight: Float,
    colors: ReaderColors,
    onLineHeightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Tight" to 1.2f,
                "Normal" to 1.5f,
                "Relaxed" to 1.8f,
                "Loose" to 2.2f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(lineHeight - value) < 0.1f

                Surface(
                    onClick = { onLineHeightChange(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) colors.accent else colors.border
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Slider(
            value = lineHeight,
            onValueChange = { onLineHeightChange((it * 10).toInt() / 10f) },
            valueRange = ReaderSettings.MIN_LINE_HEIGHT..ReaderSettings.MAX_LINE_HEIGHT,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

@Composable
private fun LetterSpacingControl(
    letterSpacing: Float,
    colors: ReaderColors,
    onLetterSpacingChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "Tight" to -0.03f,
            "Normal" to 0f,
            "Wide" to 0.05f,
            "Extra" to 0.1f
        ).forEach { (label, value) ->
            val isSelected = kotlin.math.abs(letterSpacing - value) < 0.01f

            Surface(
                onClick = { onLetterSpacingChange(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.accent else colors.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) colors.accent else colors.border
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ABC",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = value.sp,
                        color = if (isSelected) colors.onAccent else colors.text
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.onAccent else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun WordSpacingControl(
    wordSpacing: Float,
    colors: ReaderColors,
    onWordSpacingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Tight" to 0.8f,
                "Normal" to 1.0f,
                "Wide" to 1.3f,
                "Extra" to 1.6f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(wordSpacing - value) < 0.1f

                Surface(
                    onClick = { onWordSpacingChange(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) colors.accent else colors.border
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Slider(
            value = wordSpacing,
            onValueChange = { onWordSpacingChange((it * 10).toInt() / 10f) },
            valueRange = ReaderSettings.MIN_WORD_SPACING..ReaderSettings.MAX_WORD_SPACING,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextAlignSelector(
    selectedAlign: ReaderTextAlign,
    colors: ReaderColors,
    onAlignChange: (ReaderTextAlign) -> Unit
) {
    val alignments = listOf(
        ReaderTextAlign.LEFT to Icons.AutoMirrored.Filled.FormatAlignLeft,
        ReaderTextAlign.CENTER to Icons.Default.FormatAlignCenter,
        ReaderTextAlign.RIGHT to Icons.AutoMirrored.Filled.FormatAlignRight,
        ReaderTextAlign.JUSTIFY to Icons.Default.FormatAlignJustify
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        alignments.forEachIndexed { index, (align, icon) ->
            SegmentedButton(
                selected = selectedAlign == align,
                onClick = { onAlignChange(align) },
                shape = SegmentedButtonDefaults.itemShape(index, alignments.size),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = align.displayName,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.accent.copy(alpha = 0.15f),
                    activeContentColor = colors.accent
                )
            )
        }
    }
}

// =============================================================================
// LAYOUT SETTINGS
// =============================================================================

@Composable
private fun LayoutSettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SettingSection(
            title = "Content Width",
            icon = Icons.Default.ViewColumn,
            colors = colors
        ) {
            MaxWidthSelector(
                selectedWidth = settings.maxWidth,
                colors = colors,
                onWidthChange = { onSettingsChange(settings.copy(maxWidth = it)) }
            )
        }

        SettingSection(
            title = "Horizontal Margins",
            subtitle = "${settings.marginHorizontal}dp",
            icon = Icons.Default.SpaceBar,
            colors = colors
        ) {
            MarginSlider(
                value = settings.marginHorizontal,
                colors = colors,
                onValueChange = { onSettingsChange(settings.copy(marginHorizontal = it)) }
            )
        }

        SettingSection(
            title = "Vertical Margins",
            subtitle = "${settings.marginVertical}dp",
            icon = Icons.Default.FormatLineSpacing,
            colors = colors
        ) {
            MarginSlider(
                value = settings.marginVertical,
                colors = colors,
                onValueChange = { onSettingsChange(settings.copy(marginVertical = it)) }
            )
        }

        SettingSection(
            title = "Paragraph Spacing",
            subtitle = "×${String.format("%.1f", settings.paragraphSpacing)}",
            icon = Icons.Default.FormatLineSpacing,
            colors = colors
        ) {
            ParagraphSpacingControl(
                spacing = settings.paragraphSpacing,
                colors = colors,
                onSpacingChange = { onSettingsChange(settings.copy(paragraphSpacing = it)) }
            )
        }

        SettingSection(
            title = "First Line Indent",
            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
            colors = colors
        ) {
            ParagraphIndentControl(
                indent = settings.paragraphIndent,
                colors = colors,
                onIndentChange = { onSettingsChange(settings.copy(paragraphIndent = it)) }
            )
        }
    }
}

@Composable
private fun MaxWidthSelector(
    selectedWidth: MaxWidth,
    colors: ReaderColors,
    onWidthChange: (MaxWidth) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MaxWidth.entries) { width ->
                val isSelected = selectedWidth == width

                Surface(
                    onClick = { onWidthChange(width) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) colors.accent else colors.border
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(
                                    when (width) {
                                        MaxWidth.NARROW -> 24.dp
                                        MaxWidth.MEDIUM -> 32.dp
                                        MaxWidth.LARGE -> 40.dp
                                        MaxWidth.EXTRA_LARGE -> 48.dp
                                        MaxWidth.FULL -> 56.dp
                                    }
                                )
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) colors.onAccent else colors.textSecondary)
                        )

                        Text(
                            text = width.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colors.onAccent else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Show description of selected width
        Text(
            text = selectedWidth.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun MarginSlider(
    value: Int,
    colors: ReaderColors,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "0",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..48f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )

        Text(
            text = "48",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ParagraphSpacingControl(
    spacing: Float,
    colors: ReaderColors,
    onSpacingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Compact" to 0.5f,
                "Normal" to 1.0f,
                "Relaxed" to 1.5f,
                "Loose" to 2.0f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(spacing - value) < 0.1f

                Surface(
                    onClick = { onSpacingChange(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Slider(
            value = spacing,
            onValueChange = { onSpacingChange((it * 10).toInt() / 10f) },
            valueRange = ReaderSettings.MIN_PARAGRAPH_SPACING..ReaderSettings.MAX_PARAGRAPH_SPACING,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

@Composable
private fun ParagraphIndentControl(
    indent: Float,
    colors: ReaderColors,
    onIndentChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "None" to 0f,
            "Small" to 1f,
            "Medium" to 2f,
            "Large" to 3f
        ).forEach { (label, value) ->
            val isSelected = kotlin.math.abs(indent - value) < 0.1f

            Surface(
                onClick = { onIndentChange(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.accent else colors.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) colors.accent else colors.border
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) colors.onAccent else colors.textSecondary,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}
// =============================================================================
// READING SETTINGS
// =============================================================================

@Composable
private fun ReadingSettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Scroll Mode
        SettingSection(
            title = "Scroll Mode",
            icon = Icons.Default.Swipe,
            colors = colors
        ) {
            ScrollModeSelector(
                selectedMode = settings.scrollMode,
                colors = colors,
                onModeChange = { onSettingsChange(settings.copy(scrollMode = it)) }
            )
        }

        // Page Animation (only for paged modes)
        AnimatedVisibility(visible = settings.scrollMode != ScrollMode.CONTINUOUS) {
            SettingSection(
                title = "Page Animation",
                icon = Icons.Default.Animation,
                colors = colors
            ) {
                PageAnimationSelector(
                    selectedAnimation = settings.pageAnimation,
                    colors = colors,
                    onAnimationChange = { onSettingsChange(settings.copy(pageAnimation = it)) }
                )
            }
        }

        // Reading Direction
        SettingSection(
            title = "Reading Direction",
            icon = Icons.Default.ScreenRotation,
            colors = colors
        ) {
            ReadingDirectionSelector(
                selectedDirection = settings.readingDirection,
                colors = colors,
                onDirectionChange = { onSettingsChange(settings.copy(readingDirection = it)) }
            )
        }

        // Scroll Sensitivity
        SettingSection(
            title = "Scroll Sensitivity",
            subtitle = "×${String.format("%.1f", settings.scrollSensitivity)}",
            icon = Icons.Default.Speed,
            colors = colors
        ) {
            ScrollSensitivityControl(
                sensitivity = settings.scrollSensitivity,
                colors = colors,
                onSensitivityChange = { onSettingsChange(settings.copy(scrollSensitivity = it)) }
            )
        }

        // Tap Zones
        SettingSection(
            title = "Tap Zones",
            icon = Icons.Default.TouchApp,
            colors = colors
        ) {
            TapZoneSelector(
                tapZones = settings.tapZones,
                colors = colors,
                onTapZonesChange = { onSettingsChange(settings.copy(tapZones = it)) }
            )
        }

        // Volume Key Navigation
        SettingSection(
            title = "Volume Key Navigation",
            icon = Icons.Default.VolumeUp,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Use Volume Keys",
                    subtitle = "Navigate pages with volume buttons",
                    checked = settings.volumeKeyNavigation,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(volumeKeyNavigation = it)) }
                )

                AnimatedVisibility(visible = settings.volumeKeyNavigation) {
                    VolumeKeyDirectionSelector(
                        selectedDirection = settings.volumeKeyDirection,
                        colors = colors,
                        onDirectionChange = { onSettingsChange(settings.copy(volumeKeyDirection = it)) }
                    )
                }
            }
        }

        // Edge Gestures
        SettingSwitch(
            title = "Edge Gestures",
            subtitle = "Swipe from screen edges for quick actions",
            checked = settings.edgeGestures,
            colors = colors,
            onCheckedChange = { onSettingsChange(settings.copy(edgeGestures = it)) }
        )

        // Smooth Scroll
        SettingSwitch(
            title = "Smooth Scrolling",
            subtitle = "Enable smooth scroll animations",
            checked = settings.smoothScroll,
            colors = colors,
            onCheckedChange = { onSettingsChange(settings.copy(smoothScroll = it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrollModeSelector(
    selectedMode: ScrollMode,
    colors: ReaderColors,
    onModeChange: (ScrollMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ScrollMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ScrollMode.entries.size),
                label = {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.accent.copy(alpha = 0.15f),
                    activeContentColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun PageAnimationSelector(
    selectedAnimation: PageAnimation,
    colors: ReaderColors,
    onAnimationChange: (PageAnimation) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PageAnimation.entries) { animation ->
            val isSelected = selectedAnimation == animation

            Surface(
                onClick = { onAnimationChange(animation) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) colors.accent else colors.surface,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = animation.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) colors.onAccent else colors.text
                    )
                    Text(
                        text = animation.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.onAccent.copy(alpha = 0.7f) else colors.textSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingDirectionSelector(
    selectedDirection: ReadingDirection,
    colors: ReaderColors,
    onDirectionChange: (ReadingDirection) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ReadingDirection.entries.forEachIndexed { index, direction ->
            SegmentedButton(
                selected = selectedDirection == direction,
                onClick = { onDirectionChange(direction) },
                shape = SegmentedButtonDefaults.itemShape(index, ReadingDirection.entries.size),
                label = {
                    Text(
                        text = direction.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.accent.copy(alpha = 0.15f),
                    activeContentColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun ScrollSensitivityControl(
    sensitivity: Float,
    colors: ReaderColors,
    onSensitivityChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Slow" to 0.5f,
                "Normal" to 1.0f,
                "Fast" to 1.5f,
                "Very Fast" to 2.0f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(sensitivity - value) < 0.1f

                Surface(
                    onClick = { onSensitivityChange(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Slider(
            value = sensitivity,
            onValueChange = { onSensitivityChange((it * 10).toInt() / 10f) },
            valueRange = 0.5f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

@Composable
private fun TapZoneSelector(
    tapZones: TapZoneConfig,
    colors: ReaderColors,
    onTapZonesChange: (TapZoneConfig) -> Unit
) {
    var showCustomization by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preset tap zones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Default" to TapZoneConfig.DEFAULT,
                "Kindle" to TapZoneConfig.KINDLE_STYLE,
                "Simple" to TapZoneConfig.SIMPLE,
                "Inverted" to TapZoneConfig.INVERTED
            ).forEach { (label, config) ->
                val isSelected = tapZones == config

                Surface(
                    onClick = { onTapZonesChange(config) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        // Visual tap zone preview
        TapZonePreview(
            tapZones = tapZones,
            colors = colors
        )

        // Customize button
        TextButton(
            onClick = { showCustomization = !showCustomization },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (showCustomization) "Hide Customization" else "Customize Zones",
                color = colors.accent
            )
        }

        // Customization options
        AnimatedVisibility(visible = showCustomization) {
            TapZoneCustomization(
                tapZones = tapZones,
                colors = colors,
                onTapZonesChange = onTapZonesChange
            )
        }
    }
}

@Composable
private fun TapZonePreview(
    tapZones: TapZoneConfig,
    colors: ReaderColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
    ) {
        // Left zone
        Box(
            modifier = Modifier
                .fillMaxWidth(tapZones.horizontalZoneRatio)
                .fillMaxSize()
                .background(colors.accent.copy(alpha = 0.2f))
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tapZones.leftZoneAction.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Right zone
        Box(
            modifier = Modifier
                .fillMaxWidth(tapZones.horizontalZoneRatio)
                .fillMaxSize()
                .background(colors.accent.copy(alpha = 0.2f))
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tapZones.rightZoneAction.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Center zone
        Box(
            modifier = Modifier
                .fillMaxWidth(1f - (tapZones.horizontalZoneRatio * 2))
                .fillMaxSize()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tapZones.centerZoneAction.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TapZoneCustomization(
    tapZones: TapZoneConfig,
    colors: ReaderColors,
    onTapZonesChange: (TapZoneConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Zone size sliders
        Text(
            text = "Zone Sizes",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Horizontal",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.width(80.dp)
            )
            Slider(
                value = tapZones.horizontalZoneRatio,
                onValueChange = {
                    onTapZonesChange(tapZones.copy(horizontalZoneRatio = it))
                },
                valueRange = 0f..0.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.progressTrack
                )
            )
            Text(
                text = "${(tapZones.horizontalZoneRatio * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }

        HorizontalDivider(color = colors.divider)

        // Zone actions
        Text(
            text = "Zone Actions",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )

        TapActionDropdown(
            label = "Left Zone",
            selectedAction = tapZones.leftZoneAction,
            colors = colors,
            onActionChange = { onTapZonesChange(tapZones.copy(leftZoneAction = it)) }
        )

        TapActionDropdown(
            label = "Right Zone",
            selectedAction = tapZones.rightZoneAction,
            colors = colors,
            onActionChange = { onTapZonesChange(tapZones.copy(rightZoneAction = it)) }
        )

        TapActionDropdown(
            label = "Center Zone",
            selectedAction = tapZones.centerZoneAction,
            colors = colors,
            onActionChange = { onTapZonesChange(tapZones.copy(centerZoneAction = it)) }
        )

        TapActionDropdown(
            label = "Double Tap",
            selectedAction = tapZones.doubleTapAction,
            colors = colors,
            onActionChange = { onTapZonesChange(tapZones.copy(doubleTapAction = it)) }
        )
    }
}

@Composable
private fun TapActionDropdown(
    label: String,
    selectedAction: TapAction,
    colors: ReaderColors,
    onActionChange: (TapAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedAction.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.text
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.iconSecondary
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TapAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.displayName) },
                        onClick = {
                            onActionChange(action)
                            expanded = false
                        },
                        leadingIcon = if (action == selectedAction) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accent
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeKeyDirectionSelector(
    selectedDirection: VolumeKeyDirection,
    colors: ReaderColors,
    onDirectionChange: (VolumeKeyDirection) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Volume Key Direction",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            VolumeKeyDirection.entries.forEachIndexed { index, direction ->
                SegmentedButton(
                    selected = selectedDirection == direction,
                    onClick = { onDirectionChange(direction) },
                    shape = SegmentedButtonDefaults.itemShape(index, VolumeKeyDirection.entries.size),
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = direction.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.accent.copy(alpha = 0.15f),
                        activeContentColor = colors.accent
                    )
                )
            }
        }

        Text(
            text = selectedDirection.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}

// =============================================================================
// ADVANCED SETTINGS
// =============================================================================

@Composable
private fun AdvancedSettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Display Options
        SettingSection(
            title = "Display",
            icon = Icons.Default.Visibility,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Show Reading Progress",
                    checked = settings.showProgress,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showProgress = it)) }
                )

                SettingSwitch(
                    title = "Show Reading Time",
                    checked = settings.showReadingTime,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showReadingTime = it)) }
                )

                SettingSwitch(
                    title = "Show Chapter Title",
                    checked = settings.showChapterTitle,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(showChapterTitle = it)) }
                )

                SettingSwitch(
                    title = "Immersive Mode",
                    subtitle = "Hide status bar while reading",
                    checked = settings.immersiveMode,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(immersiveMode = it)) }
                )
            }
        }

        // Progress Style
        AnimatedVisibility(visible = settings.showProgress) {
            SettingSection(
                title = "Progress Style",
                icon = Icons.Default.Speed,
                colors = colors
            ) {
                ProgressStyleSelector(
                    selectedStyle = settings.progressStyle,
                    colors = colors,
                    onStyleChange = { onSettingsChange(settings.copy(progressStyle = it)) }
                )
            }
        }

        // Screen Orientation
        SettingSection(
            title = "Screen Orientation",
            icon = Icons.Default.ScreenRotation,
            colors = colors
        ) {
            ScreenOrientationSelector(
                selectedOrientation = settings.screenOrientation,
                colors = colors,
                onOrientationChange = { onSettingsChange(settings.copy(screenOrientation = it)) }
            )
        }

        // Auto-hide Controls
        SettingSection(
            title = "Auto-hide Controls",
            icon = Icons.Default.Timer,
            colors = colors
        ) {
            AutoHideDelaySelector(
                delay = settings.autoHideControlsDelay,
                colors = colors,
                onDelayChange = { onSettingsChange(settings.copy(autoHideControlsDelay = it)) }
            )
        }

        // Auto-scroll
        SettingSection(
            title = "Auto-Scroll",
            subtitle = "Hands-free reading",
            icon = Icons.Default.PlayArrow,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Enable Auto-Scroll",
                    subtitle = "Automatically scroll the page while reading",
                    checked = settings.autoScrollEnabled,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(autoScrollEnabled = it)) }
                )

                AnimatedVisibility(visible = settings.autoScrollEnabled) {
                    AutoScrollSpeedControl(
                        speed = settings.autoScrollSpeed,
                        colors = colors,
                        onSpeedChange = { onSettingsChange(settings.copy(autoScrollSpeed = it)) }
                    )
                }
            }
        }

        // Behavior
        SettingSection(
            title = "Behavior",
            icon = Icons.Default.Settings,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off while reading",
                    checked = settings.keepScreenOn,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(keepScreenOn = it)) }
                )

                SettingSwitch(
                    title = "Text Selection",
                    subtitle = "Long press to select and copy text",
                    checked = settings.longPressSelection,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(longPressSelection = it)) }
                )

                SettingSwitch(
                    title = "Auto-advance Chapter (TTS)",
                    subtitle = "Automatically start next chapter when TTS finishes",
                    checked = settings.ttsAutoAdvanceChapter,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(ttsAutoAdvanceChapter = it)) }
                )
            }
        }

        // Accessibility
        SettingSection(
            title = "Accessibility",
            icon = Icons.Default.AccessibilityNew,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Force High Contrast",
                    subtitle = "Increase text/background contrast",
                    checked = settings.forceHighContrast,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(forceHighContrast = it)) }
                )

                SettingSwitch(
                    title = "Reduce Motion",
                    subtitle = "Minimize animations",
                    checked = settings.reduceMotion,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(reduceMotion = it)) }
                )

                SettingSwitch(
                    title = "Larger Touch Targets",
                    subtitle = "Easier to tap buttons and controls",
                    checked = settings.largerTouchTargets,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(largerTouchTargets = it)) }
                )
            }
        }

        // About section
        SettingSection(
            title = "About These Settings",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            colors = colors
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "These settings are saved automatically and apply to all chapters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Use presets for quick configuration or customize individual settings for the perfect reading experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenOrientationSelector(
    selectedOrientation: ScreenOrientation,
    colors: ReaderColors,
    onOrientationChange: (ScreenOrientation) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ScreenOrientation.entries.forEachIndexed { index, orientation ->
            SegmentedButton(
                selected = selectedOrientation == orientation,
                onClick = { onOrientationChange(orientation) },
                shape = SegmentedButtonDefaults.itemShape(index, ScreenOrientation.entries.size),
                label = {
                    Text(
                        text = orientation.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.accent.copy(alpha = 0.15f),
                    activeContentColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun AutoScrollSpeedControl(
    speed: Float,
    colors: ReaderColors,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scroll Speed",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = "×${String.format("%.1f", speed)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Slow" to 0.5f,
                "Normal" to 1.0f,
                "Fast" to 2.0f,
                "Very Fast" to 3.0f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(speed - value) < 0.1f

                Surface(
                    onClick = { onSpeedChange(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Slider(
            value = speed,
            onValueChange = { onSpeedChange((it * 10).toInt() / 10f) },
            valueRange = ReaderSettings.MIN_AUTO_SCROLL_SPEED..ReaderSettings.MAX_AUTO_SCROLL_SPEED,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

@Composable
private fun ProgressStyleSelector(
    selectedStyle: ProgressStyle,
    colors: ReaderColors,
    onStyleChange: (ProgressStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ProgressStyle.entries.filter { it != ProgressStyle.NONE }) { style ->
                val isSelected = selectedStyle == style

                Surface(
                    onClick = { onStyleChange(style) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.onAccent else colors.text
                        )
                    }
                }
            }
        }

        Text(
            text = selectedStyle.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun AutoHideDelaySelector(
    delay: Long,
    colors: ReaderColors,
    onDelayChange: (Long) -> Unit
) {
    val options = listOf(
        "Never" to 0L,
        "1.5s" to 1500L,
        "3s" to 3000L,
        "5s" to 5000L,
        "10s" to 10000L
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = delay == value

            Surface(
                onClick = { onDelayChange(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.accent else colors.surface,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) colors.onAccent else colors.textSecondary,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}
// =============================================================================
// TTS SETTINGS
// =============================================================================

@Composable
private fun TTSSettings(
    colors: ReaderColors,
    preferencesManager: PreferencesManager
) {
    // TTS playback settings
    var ttsSpeed by remember { mutableFloatStateOf(preferencesManager.getTtsSpeed()) }
    var ttsPitch by remember { mutableFloatStateOf(preferencesManager.getTtsPitch()) }
    var ttsAutoScroll by remember { mutableStateOf(preferencesManager.getTtsAutoScroll()) }
    var ttsHighlightSentence by remember { mutableStateOf(preferencesManager.getTtsHighlightSentence()) }
    var ttsContinueOnChapterEnd by remember { mutableStateOf(preferencesManager.getTtsContinueOnChapterEnd()) }
    var ttsPauseOnCalls by remember { mutableStateOf(preferencesManager.getTtsPauseOnCalls()) }
    var ttsUseSystemVoice by remember { mutableStateOf(preferencesManager.getTtsUseSystemVoice()) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Playback Settings Section
        SettingSection(
            title = "Playback",
            icon = Icons.Default.PlayArrow,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Auto-Scroll with TTS",
                    subtitle = "Scroll to follow the current sentence",
                    checked = ttsAutoScroll,
                    colors = colors,
                    onCheckedChange = {
                        ttsAutoScroll = it
                        preferencesManager.setTtsAutoScroll(it)
                    }
                )

                SettingSwitch(
                    title = "Highlight Sentence",
                    subtitle = "Visually highlight the sentence being read",
                    checked = ttsHighlightSentence,
                    colors = colors,
                    onCheckedChange = {
                        ttsHighlightSentence = it
                        preferencesManager.setTtsHighlightSentence(it)
                    }
                )

                SettingSwitch(
                    title = "Auto-Advance Chapter",
                    subtitle = "Automatically start next chapter when finished",
                    checked = ttsContinueOnChapterEnd,
                    colors = colors,
                    onCheckedChange = {
                        ttsContinueOnChapterEnd = it
                        preferencesManager.setTtsContinueOnChapterEnd(it)
                    }
                )

                SettingSwitch(
                    title = "Pause on Phone Calls",
                    subtitle = "Automatically pause when receiving calls",
                    checked = ttsPauseOnCalls,
                    colors = colors,
                    onCheckedChange = {
                        ttsPauseOnCalls = it
                        preferencesManager.setTtsPauseOnCalls(it)
                    }
                )

                SettingSwitch(
                    title = "Use System Voice",
                    subtitle = "Use system default TTS voice instead of app selection",
                    checked = ttsUseSystemVoice,
                    colors = colors,
                    onCheckedChange = {
                        ttsUseSystemVoice = it
                        preferencesManager.setTtsUseSystemVoice(it)
                    }
                )
            }
        }

        // Voice Settings Section
        SettingSection(
            title = "Voice Settings",
            icon = Icons.Default.RecordVoiceOver,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Speed Control
                TTSSpeedControl(
                    speed = ttsSpeed,
                    colors = colors,
                    onSpeedChange = {
                        ttsSpeed = it
                        preferencesManager.setTtsSpeed(it)
                    }
                )

                HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))

                // Pitch Control
                TTSPitchControl(
                    pitch = ttsPitch,
                    colors = colors,
                    onPitchChange = {
                        ttsPitch = it
                        preferencesManager.setTtsPitch(it)
                    }
                )
            }
        }
    }
}

// =============================================================================
// TTS SPEED CONTROL
// =============================================================================

@Composable
private fun TTSSpeedControl(
    speed: Float,
    colors: ReaderColors,
    onSpeedChange: (Float) -> Unit
) {
    var localSpeed by remember(speed) { mutableFloatStateOf(speed) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text
                )
            }
            Text(
                text = "×${String.format("%.1f", localSpeed)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }

        // Quick preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "0.5×" to 0.5f,
                "0.75×" to 0.75f,
                "1×" to 1.0f,
                "1.25×" to 1.25f,
                "1.5×" to 1.5f,
                "2×" to 2.0f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(localSpeed - value) < 0.05f

                Surface(
                    onClick = {
                        localSpeed = value
                        onSpeedChange(value)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Fine-tune slider
        Slider(
            value = localSpeed,
            onValueChange = { localSpeed = it },
            onValueChangeFinished = { onSpeedChange(localSpeed) },
            valueRange = 0.5f..2.5f,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

// =============================================================================
// TTS PITCH CONTROL
// =============================================================================

@Composable
private fun TTSPitchControl(
    pitch: Float,
    colors: ReaderColors,
    onPitchChange: (Float) -> Unit
) {
    var localPitch by remember(pitch) { mutableFloatStateOf(pitch) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )
                Text(
                    text = "Pitch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text
                )
            }
            Text(
                text = "×${String.format("%.1f", localPitch)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        }

        // Quick preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Low" to 0.5f,
                "Normal" to 1.0f,
                "High" to 1.5f,
                "Higher" to 2.0f
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(localPitch - value) < 0.05f

                Surface(
                    onClick = {
                        localPitch = value
                        onPitchChange(value)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Fine-tune slider
        Slider(
            value = localPitch,
            onValueChange = { localPitch = it },
            onValueChangeFinished = { onPitchChange(localPitch) },
            valueRange = 0.5f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.progressTrack
            )
        )
    }
}

// =============================================================================
// COMMON COMPONENTS
// =============================================================================

@Composable
private fun SettingSection(
    title: String,
    icon: ImageVector,
    colors: ReaderColors,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.iconSecondary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            trailing?.invoke()
        }

        content()
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    colors: ReaderColors,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) colors.text else colors.text.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accent.copy(alpha = 0.3f)
            )
        )
    }
}