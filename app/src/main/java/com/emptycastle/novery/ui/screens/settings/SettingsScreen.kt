package com.emptycastle.novery.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewComfy
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material.icons.outlined.ViewCozy
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.CustomThemeColors
import com.emptycastle.novery.domain.model.DisplayMode
import com.emptycastle.novery.domain.model.GridColumns
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.RatingFormat
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.ThemeMode
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.components.ColorPickerDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val settings by preferencesManager.appSettings.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                preferencesManager.resetToDefaults()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // THEME (Color & Visual Theming)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Theme", Icons.Outlined.Palette) }
            item {
                SettingsCard {
                    SettingsLabel("Theme Mode", Icons.Outlined.DarkMode)
                    Spacer(Modifier.height(12.dp))
                    ThemeModeSelector(
                        selected = settings.themeMode,
                        onSelect = { preferencesManager.updateThemeMode(it) }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.Contrast,
                        title = "AMOLED Black",
                        subtitle = "Pure black for OLED screens",
                        checked = settings.amoledBlack,
                        enabled = settings.themeMode != ThemeMode.LIGHT,
                        onCheckedChange = { preferencesManager.updateAmoledBlack(it) }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Use Material You colors",
                        checked = settings.useDynamicColor,
                        enabled = !settings.useCustomTheme,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(useDynamicColor = it))
                        }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.Palette,
                        title = "Custom Theme",
                        subtitle = "Pick your own colors",
                        checked = settings.useCustomTheme,
                        highlight = true,
                        onCheckedChange = { preferencesManager.updateUseCustomTheme(it) }
                    )

                    AnimatedVisibility(
                        visible = settings.useCustomTheme,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            CustomThemeSection(
                                colors = settings.customThemeColors,
                                onColorsChange = { preferencesManager.updateCustomThemeColors(it) }
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // LAYOUT (UI Structure & Display)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Layout", Icons.Outlined.ViewCompact) }
            item {
                SettingsCard {
                    SettingsLabel("UI Density", Icons.Outlined.SpaceDashboard)
                    Spacer(Modifier.height(12.dp))
                    DensitySelector(
                        selected = settings.uiDensity,
                        onSelect = { preferencesManager.updateDensity(it) }
                    )
                    SettingsDivider()
                    SettingsLabel("Display Mode", Icons.Outlined.ViewModule)
                    Spacer(Modifier.height(8.dp))
                    DisplayModeRow("Library", settings.libraryDisplayMode) {
                        preferencesManager.updateLibraryDisplayMode(it)
                    }
                    DisplayModeRow("Browse", settings.browseDisplayMode) {
                        preferencesManager.updateBrowseDisplayMode(it)
                    }
                    DisplayModeRow("Search", settings.searchDisplayMode) {
                        preferencesManager.updateSearchDisplayMode(it)
                    }
                    SettingsDivider()
                    SettingsLabel("Grid Columns", Icons.Outlined.GridView)
                    Spacer(Modifier.height(8.dp))
                    GridColumnsRow("Library", settings.libraryGridColumns) {
                        preferencesManager.updateLibraryGridColumns(it)
                    }
                    GridColumnsRow("Browse", settings.browseGridColumns) {
                        preferencesManager.updateBrowseGridColumns(it)
                    }
                    GridColumnsRow("Search", settings.searchGridColumns) {
                        preferencesManager.updateSearchGridColumns(it)
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // LIBRARY (Library Behavior & Display)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Library", Icons.Outlined.LibraryBooks) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.Badge,
                        title = "Show Badges",
                        subtitle = "Display download and status indicators",
                        checked = settings.showBadges,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(showBadges = it))
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Outlined.FilterList,
                        title = "Default Filter",
                        selectedValue = settings.defaultLibraryFilter.displayName(),
                        options = LibraryFilter.values().map { it.displayName() },
                        selectedIndex = settings.defaultLibraryFilter.ordinal,
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(defaultLibraryFilter = LibraryFilter.values()[it])
                            )
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Outlined.Sort,
                        title = "Default Sort",
                        selectedValue = settings.defaultLibrarySort.displayName(),
                        options = LibrarySortOrder.values().map { it.displayName() },
                        selectedIndex = settings.defaultLibrarySort.ordinal,
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(defaultLibrarySort = LibrarySortOrder.values()[it])
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // BROWSE & SEARCH (Discovery Settings)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Browse & Search", Icons.Outlined.Search) }
            item {
                SettingsCard {
                    SliderItem(
                        icon = Icons.Outlined.Numbers,
                        title = "Search Results",
                        subtitle = "Results per provider: ${settings.searchResultsPerProvider}",
                        value = settings.searchResultsPerProvider.toFloat(),
                        valueRange = 4f..12f,
                        steps = 7,
                        valueLabel = settings.searchResultsPerProvider.toString(),
                        onValueChange = {
                            preferencesManager.updateAppSettings(
                                settings.copy(searchResultsPerProvider = it.toInt())
                            )
                        }
                    )
                    SettingsDivider()
                    DropdownItem(
                        icon = Icons.Outlined.Star,
                        title = "Rating Format",
                        selectedValue = settings.ratingFormat.shortDisplayName(),
                        options = RatingFormat.values().map { it.displayName() },
                        selectedIndex = settings.ratingFormat.ordinal,
                        onSelect = {
                            preferencesManager.updateAppSettings(
                                settings.copy(ratingFormat = RatingFormat.values()[it])
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // AUTO-DOWNLOAD (Download Automation)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Auto-Download", Icons.Rounded.CloudDownload) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.DownloadForOffline,
                        title = "Auto-Download Chapters",
                        subtitle = "Download new chapters automatically",
                        checked = settings.autoDownloadEnabled,
                        highlight = true,
                        onCheckedChange = { preferencesManager.updateAutoDownloadEnabled(it) }
                    )

                    AnimatedVisibility(
                        visible = settings.autoDownloadEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            SettingsDivider()
                            ToggleItem(
                                icon = Icons.Outlined.Wifi,
                                title = "WiFi Only",
                                subtitle = "Only download on WiFi",
                                checked = settings.autoDownloadOnWifiOnly,
                                onCheckedChange = { preferencesManager.updateAutoDownloadWifiOnly(it) }
                            )
                            SettingsDivider()
                            SliderItem(
                                icon = Icons.Outlined.Numbers,
                                title = "Download Limit",
                                subtitle = if (settings.autoDownloadLimit == 0) "Unlimited"
                                else "Max ${settings.autoDownloadLimit} per novel",
                                value = settings.autoDownloadLimit.toFloat(),
                                valueRange = 0f..50f,
                                steps = 9,
                                valueLabel = if (settings.autoDownloadLimit == 0) "∞"
                                else settings.autoDownloadLimit.toString(),
                                onValueChange = { preferencesManager.updateAutoDownloadLimit(it.toInt()) }
                            )
                            SettingsDivider()
                            SettingsLabel("Download for Status", Icons.Outlined.Bookmark)
                            Spacer(Modifier.height(8.dp))
                            StatusChips(
                                selected = settings.autoDownloadForStatuses,
                                onChange = { preferencesManager.updateAutoDownloadStatuses(it) }
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // READER (Reading Experience)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Reader", Icons.Outlined.MenuBook) }
            item {
                SettingsCard {
                    ToggleItem(
                        icon = Icons.Outlined.LightMode,
                        title = "Keep Screen On",
                        subtitle = "Prevent timeout while reading",
                        checked = settings.keepScreenOn,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(keepScreenOn = it))
                        }
                    )
                    SettingsDivider()
                    ToggleItem(
                        icon = Icons.Outlined.AllInclusive,
                        title = "Infinite Scroll",
                        subtitle = "Auto-load next chapters",
                        checked = settings.infiniteScroll,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(settings.copy(infiniteScroll = it))
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // PROVIDERS (Source Management)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Providers", Icons.Outlined.Extension) }
            item {
                ProviderCard(
                    settings = settings,
                    onOrderChange = { preferencesManager.updateProviderOrder(it) },
                    onEnabledChange = { name, enabled ->
                        preferencesManager.setProviderEnabled(name, enabled)
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════
            // STORAGE & BACKUP (Data Management)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("Data", Icons.Outlined.Storage) }
            item {
                SettingsCard {
                    NavigationItem(
                        icon = Icons.Outlined.Storage,
                        title = "Storage & Backup",
                        subtitle = "Manage downloads, cache, and backups",
                        onClick = onNavigateToStorage
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // ABOUT (App Information)
            // ═══════════════════════════════════════════════════════════
            item { SectionHeader("About", Icons.Outlined.Info) }
            item {
                SettingsCard {
                    InfoItem(Icons.Outlined.Apartment, "Version", "1.0.0")
                    SettingsDivider()
                    ClickableItem(
                        icon = Icons.Outlined.RestartAlt,
                        title = "Reset to Defaults",
                        subtitle = "Restore all settings",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsLabel(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.values().forEach { mode ->
            val isSelected = mode == selected
            val icon = when (mode) {
                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                ThemeMode.DARK -> Icons.Outlined.DarkMode
                ThemeMode.SYSTEM -> Icons.Outlined.SettingsSuggest
            }
            SelectableChip(
                selected = isSelected,
                label = mode.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(mode)
                }
            )
        }
    }
}

@Composable
private fun DensitySelector(selected: UiDensity, onSelect: (UiDensity) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UiDensity.values().forEach { density ->
            val isSelected = density == selected
            val icon = when (density) {
                UiDensity.COMPACT -> Icons.Outlined.ViewCompact
                UiDensity.DEFAULT -> Icons.Outlined.ViewComfy
                UiDensity.COMFORTABLE -> Icons.Outlined.ViewCozy
            }
            SelectableChip(
                selected = isSelected,
                label = density.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(density)
                }
            )
        }
    }
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), content)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content
            )
        }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    highlight: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null, Modifier.size(24.dp),
            if (checked && enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
            enabled = enabled
        )
    }
}

@Composable
private fun DropdownItem(
    icon: ImageVector,
    title: String,
    selectedValue: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                selectedValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (i == selectedIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(i)
                        expanded = false
                    },
                    leadingIcon = if (i == selectedIndex) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    valueLabel,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(start = 36.dp),
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun InfoItem(icon: ImageVector, title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tint.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisplayModeRow(
    label: String,
    selected: DisplayMode,
    onSelect: (DisplayMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        DisplayMode.values().forEach { mode ->
            val isSelected = mode == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "bg"
            )
            Surface(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(mode)
                    },
                color = bg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        mode.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GridColumnsRow(
    label: String,
    selected: GridColumns,
    onSelect: (GridColumns) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(
        GridColumns.Auto to "Auto",
        GridColumns.Fixed(2) to "2",
        GridColumns.Fixed(3) to "3",
        GridColumns.Fixed(4) to "4",
        GridColumns.Fixed(5) to "5"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        options.forEach { (cols, text) ->
            val isSelected = when {
                cols is GridColumns.Auto && selected is GridColumns.Auto -> true
                cols is GridColumns.Fixed && selected is GridColumns.Fixed -> cols.count == selected.count
                else -> false
            }
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "bg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(cols)
                    },
                Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusChips(
    selected: Set<ReadingStatus>,
    onChange: (Set<ReadingStatus>) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ReadingStatus.READING,
            ReadingStatus.PLAN_TO_READ,
            ReadingStatus.ON_HOLD
        ).forEach { status ->
            val isSelected = status in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChange(if (isSelected) selected - status else selected + status)
                },
                label = {
                    Text(
                        status.displayName(),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isSelected) Icons.Default.Check else Icons.Outlined.Bookmark,
                        null,
                        Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CUSTOM THEME SECTION
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThemeSection(
    colors: CustomThemeColors,
    onColorsChange: (CustomThemeColors) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showColorPicker by remember { mutableStateOf<ColorPickerTarget?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Preset Themes
        Text(
            "Presets",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomThemeColors.PRESETS.forEach { (name, preset) ->
                ThemePresetCard(
                    name = name,
                    colors = preset,
                    isSelected = colors == preset,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onColorsChange(preset)
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Individual Color Pickers
        Text(
            "Custom Colors",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Color picker rows
        ColorPickerRow(
            label = "Primary",
            color = Color(colors.primaryColor),
            description = "Main accent color",
            onClick = { showColorPicker = ColorPickerTarget.PRIMARY }
        )

        ColorPickerRow(
            label = "Secondary",
            color = Color(colors.secondaryColor),
            description = "Supporting accent color",
            onClick = { showColorPicker = ColorPickerTarget.SECONDARY }
        )

        ColorPickerRow(
            label = "Background",
            color = Color(colors.backgroundColor),
            description = "App background",
            onClick = { showColorPicker = ColorPickerTarget.BACKGROUND }
        )

        ColorPickerRow(
            label = "Surface",
            color = Color(colors.surfaceColor),
            description = "Cards and dialogs",
            onClick = { showColorPicker = ColorPickerTarget.SURFACE }
        )

        // Live Preview
        Spacer(Modifier.height(8.dp))
        ThemePreviewCard(colors = colors)
    }

    // Color Picker Dialog
    showColorPicker?.let { target ->
        val currentColor = when (target) {
            ColorPickerTarget.PRIMARY -> Color(colors.primaryColor)
            ColorPickerTarget.SECONDARY -> Color(colors.secondaryColor)
            ColorPickerTarget.BACKGROUND -> Color(colors.backgroundColor)
            ColorPickerTarget.SURFACE -> Color(colors.surfaceColor)
        }

        ColorPickerDialog(
            currentColor = currentColor,
            title = "Pick ${target.displayName} Color",
            onColorSelected = { newColor ->
                val colorLong = newColor.toArgb().toLong() and 0xFFFFFFFFL or 0xFF000000L.toLong()
                val newColors = when (target) {
                    ColorPickerTarget.PRIMARY -> colors.copy(primaryColor = colorLong)
                    ColorPickerTarget.SECONDARY -> colors.copy(secondaryColor = colorLong)
                    ColorPickerTarget.BACKGROUND -> colors.copy(backgroundColor = colorLong)
                    ColorPickerTarget.SURFACE -> colors.copy(surfaceColor = colorLong)
                }
                onColorsChange(newColors)
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }
}

private enum class ColorPickerTarget(val displayName: String) {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    BACKGROUND("Background"),
    SURFACE("Surface")
}

@Composable
private fun ThemePresetCard(
    name: String,
    colors: CustomThemeColors,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    borderColor,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(colors.backgroundColor)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Color preview dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.primaryColor))
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.secondaryColor))
                )
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(colors.surfaceColor))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
            }

            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(colors.primaryColor)
                else Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = Color(colors.primaryColor)
                )
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Color,
    description: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color swatch
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
        )

        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hex value
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                colorToHex(color),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemePreviewCard(colors: CustomThemeColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(colors.backgroundColor)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            // Mock card
            Surface(
                color = Color(colors.surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mock image
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(colors.primaryColor).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Color(colors.primaryColor)
                        )
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            "Sample Novel Title",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Chapter 42 • Author Name",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Mock button
                    Surface(
                        color = Color(colors.primaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Read",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = getContrastColor(Color(colors.primaryColor))
                        )
                    }
                }
            }

            // Mock chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(colors.primaryColor).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Fantasy",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(colors.primaryColor)
                    )
                }
                Surface(
                    color = Color(colors.secondaryColor).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Adventure",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(colors.secondaryColor)
                    )
                }
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}

private fun getContrastColor(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// PROVIDER MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProviderCard(
    settings: AppSettings,
    onOrderChange: (List<String>) -> Unit,
    onEnabledChange: (String, Boolean) -> Unit
) {
    val allProviders = remember { com.emptycastle.novery.provider.MainProvider.getProviders() }
    var order by remember(settings.providerOrder) {
        mutableStateOf(settings.providerOrder.ifEmpty { allProviders.map { it.name } })
    }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        order = order.toMutableList().apply { add(to.index, removeAt(from.index)) }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging) {
            val current = settings.providerOrder.ifEmpty { allProviders.map { it.name } }
            if (order != current) onOrderChange(order)
        }
    }

    SettingsCard {
        SettingsLabel("Provider Order", Icons.Outlined.SwapVert)
        Text(
            "Long press to reorder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height((56.dp + 4.dp) * minOf(order.size, 6)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(order, key = { _, item -> item }) { _, name ->
                ReorderableItem(reorderState, key = name) { isDragging ->
                    ProviderItem(
                        name = name,
                        enabled = name !in settings.disabledProviders,
                        isDragging = isDragging,
                        onEnabledChange = { onEnabledChange(name, it) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderItem(
    name: String,
    enabled: Boolean,
    isDragging: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val elevation by animateDpAsState(
        if (isDragging) 8.dp else 0.dp,
        spring(stiffness = Spring.StiffnessHigh),
        label = "elev"
    )
    val scale by animateFloatAsState(
        if (isDragging) 1.02f else 1f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    val bg by animateColorAsState(
        when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer
            enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        label = "bg"
    )

    Surface(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .zIndex(if (isDragging) 1f else 0f),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = elevation
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                "Drag",
                modifier.size(24.dp),
                if (isDragging) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 0.6f else 0.3f
                )
            )
            Text(
                name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isDragging) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Switch(enabled, onEnabledChange)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// RESET DIALOG
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResetConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Warning,
                null,
                Modifier.size(32.dp),
                MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Reset All Settings?", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "This will restore all settings to defaults. Your library and downloads will not be affected.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}