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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
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
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.FontCategory
import com.emptycastle.novery.domain.model.FontFamily
import com.emptycastle.novery.domain.model.MaxWidth
import com.emptycastle.novery.domain.model.ProgressStyle
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.domain.model.ReaderTheme
import com.emptycastle.novery.domain.model.ThemeCategory
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reader Settings",
                        color = colors.text
                    )
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

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { (name, preset) ->
            PresetChip(
                name = name,
                isSelected = currentSettings == preset,
                colors = colors,
                onClick = { onPresetSelected(preset) }
            )
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
                text = if (isSystemBrightness) "Using system brightness" else "Manual brightness",
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
            colors = colors
        ) {
            LetterSpacingControl(
                letterSpacing = settings.letterSpacing,
                colors = colors,
                onLetterSpacingChange = { onSettingsChange(settings.copy(letterSpacing = it)) }
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

        // Preview text
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = colors.surface
        ) {
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize.sp,
                color = colors.text,
                modifier = Modifier.padding(12.dp)
            )
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
// ADVANCED SETTINGS
// =============================================================================

@Composable
private fun AdvancedSettings(
    settings: ReaderSettings,
    colors: ReaderColors,
    onSettingsChange: (ReaderSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
            }
        }

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

        SettingSection(
            title = "Scrolling",
            icon = Icons.AutoMirrored.Filled.Sort,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Smooth Scrolling",
                    checked = settings.smoothScroll,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(smoothScroll = it)) }
                )

                SettingSwitch(
                    title = "Volume Key Navigation",
                    checked = settings.volumeKeyNavigation,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(volumeKeyNavigation = it)) }
                )
            }
        }

        SettingSection(
            title = "Behavior",
            icon = Icons.Default.Settings,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Keep Screen On",
                    checked = settings.keepScreenOn,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(keepScreenOn = it)) }
                )

                SettingSwitch(
                    title = "Text Selection on Long Press",
                    checked = settings.longPressSelection,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(longPressSelection = it)) }
                )

                SettingSwitch(
                    title = "Enable Hyphenation",
                    subtitle = "For justified text",
                    checked = settings.hyphenation,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(hyphenation = it)) }
                )
            }
        }

        SettingSection(
            title = "Accessibility",
            icon = Icons.Default.AccessibilityNew,
            colors = colors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingSwitch(
                    title = "Force High Contrast",
                    checked = settings.forceHighContrast,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(forceHighContrast = it)) }
                )

                SettingSwitch(
                    title = "Reduce Motion",
                    checked = settings.reduceMotion,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(reduceMotion = it)) }
                )

                SettingSwitch(
                    title = "Larger Touch Targets",
                    checked = settings.largerTouchTargets,
                    colors = colors,
                    onCheckedChange = { onSettingsChange(settings.copy(largerTouchTargets = it)) }
                )
            }
        }
    }
}

@Composable
private fun ProgressStyleSelector(
    selectedStyle: ProgressStyle,
    colors: ReaderColors,
    onStyleChange: (ProgressStyle) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ProgressStyle.entries.filter { it != ProgressStyle.NONE }) { style ->
            val isSelected = selectedStyle == style

            FilterChip(
                selected = isSelected,
                onClick = { onStyleChange(style) },
                label = { Text(style.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                    selectedLabelColor = colors.accent
                )
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    colors: ReaderColors,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
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
                color = colors.text
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accent.copy(alpha = 0.3f)
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