package com.emptycastle.novery.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewComfy
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material.icons.outlined.ViewCozy
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.DisplayMode
import com.emptycastle.novery.domain.model.GridColumns
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.ThemeMode
import com.emptycastle.novery.domain.model.UiDensity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    // State for the reset confirmation dialog
    var showResetDialog by remember { mutableStateOf(false) }

    // Reset Confirmation Dialog
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
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // APPEARANCE SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Appearance",
                    icon = Icons.Outlined.Palette,
                    description = "Theme, colors, and visual style"
                )
            }

            item {
                SettingsCard {
                    SettingsItemLabel(
                        title = "Theme Mode",
                        icon = Icons.Outlined.DarkMode
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ThemeModeSelector(
                        selectedMode = appSettings.themeMode,
                        onModeSelected = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            preferencesManager.updateThemeMode(it)
                        }
                    )

                    SettingsDivider()

                    SettingsToggleItem(
                        icon = Icons.Outlined.Contrast,
                        title = "AMOLED Black",
                        subtitle = "Pure black background for OLED screens",
                        checked = appSettings.amoledBlack,
                        enabled = appSettings.themeMode != ThemeMode.LIGHT,
                        onCheckedChange = { preferencesManager.updateAmoledBlack(it) }
                    )

                    SettingsDivider()

                    SettingsToggleItem(
                        icon = Icons.Outlined.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Use Material You colors from your wallpaper",
                        checked = appSettings.useDynamicColor,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(
                                appSettings.copy(useDynamicColor = it)
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // LAYOUT SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Layout",
                    icon = Icons.Outlined.ViewCompact,
                    description = "Grid, spacing, and display options"
                )
            }


            item {
                SettingsCard {

                    SettingsItemLabel(
                        title = "Display Mode",
                        icon = Icons.Outlined.ViewModule
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DisplayModeSection(
                        libraryMode = appSettings.libraryDisplayMode,
                        browseMode = appSettings.browseDisplayMode,
                        searchMode = appSettings.searchDisplayMode,
                        onLibraryModeChange = { preferencesManager.updateLibraryDisplayMode(it) },
                        onBrowseModeChange = { preferencesManager.updateBrowseDisplayMode(it) },
                        onSearchModeChange = { preferencesManager.updateSearchDisplayMode(it) }
                    )

                    SettingsDivider()

                    SettingsItemLabel(
                        title = "UI Density",
                        icon = Icons.Outlined.SpaceDashboard
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DensitySelector(
                        selectedDensity = appSettings.uiDensity,
                        onDensitySelected = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            preferencesManager.updateDensity(it)
                        }
                    )

                    SettingsDivider()

                    SettingsItemLabel(
                        title = "Grid Columns",
                        icon = Icons.Outlined.GridView
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    GridColumnsSection(
                        libraryColumns = appSettings.libraryGridColumns,
                        browseColumns = appSettings.browseGridColumns,
                        searchColumns = appSettings.searchGridColumns,
                        onLibraryColumnsChange = { preferencesManager.updateLibraryGridColumns(it) },
                        onBrowseColumnsChange = { preferencesManager.updateBrowseGridColumns(it) },
                        onSearchColumnsChange = { preferencesManager.updateSearchGridColumns(it) }
                    )

                    SettingsDivider()

                    SettingsSliderItem(
                        icon = Icons.Outlined.Search,
                        title = "Search Results",
                        subtitle = "Results per provider before \"Show more\"",
                        value = appSettings.searchResultsPerProvider.toFloat(),
                        valueRange = 4f..12f,
                        steps = 7,
                        valueLabel = "${appSettings.searchResultsPerProvider}",
                        onValueChange = {
                            preferencesManager.updateAppSettings(
                                appSettings.copy(searchResultsPerProvider = it.toInt())
                            )
                        }
                    )

                    SettingsDivider()

                    SettingsToggleItem(
                        icon = Icons.Outlined.Badge,
                        title = "Show Badges",
                        subtitle = "Display download and status indicators",
                        checked = appSettings.showBadges,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(
                                appSettings.copy(showBadges = it)
                            )
                        }
                    )
                }
            }

            // Live Preview Card
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LivePreviewCard(
                        gridColumns = appSettings.libraryGridColumns,
                        showBadges = appSettings.showBadges,
                        density = appSettings.uiDensity,
                        displayMode = appSettings.libraryDisplayMode
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // LIBRARY SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Library",
                    icon = Icons.Outlined.LibraryBooks,
                    description = "Default filters and sorting"
                )
            }

            item {
                SettingsCard {
                    SettingsDropdownItem(
                        icon = Icons.Outlined.FilterList,
                        title = "Default Filter",
                        selectedValue = appSettings.defaultLibraryFilter.displayName(),
                        options = LibraryFilter.entries.map { it.displayName() },
                        selectedIndex = appSettings.defaultLibraryFilter.ordinal,
                        onSelect = { index ->
                            preferencesManager.updateAppSettings(
                                appSettings.copy(
                                    defaultLibraryFilter = LibraryFilter.entries[index]
                                )
                            )
                        }
                    )

                    SettingsDivider()

                    SettingsDropdownItem(
                        icon = Icons.Outlined.Sort,
                        title = "Default Sort",
                        selectedValue = appSettings.defaultLibrarySort.displayName(),
                        options = LibrarySortOrder.entries.map { it.displayName() },
                        selectedIndex = appSettings.defaultLibrarySort.ordinal,
                        onSelect = { index ->
                            preferencesManager.updateAppSettings(
                                appSettings.copy(
                                    defaultLibrarySort = LibrarySortOrder.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // AUTO-DOWNLOAD SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Auto-Download",
                    icon = Icons.Rounded.CloudDownload,
                    description = "Automatic chapter downloads"
                )
            }

            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Outlined.DownloadForOffline,
                        title = "Auto-Download Chapters",
                        subtitle = "Download new chapters automatically",
                        checked = appSettings.autoDownloadEnabled,
                        onCheckedChange = { preferencesManager.updateAutoDownloadEnabled(it) },
                        highlight = true
                    )

                    AnimatedVisibility(
                        visible = appSettings.autoDownloadEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            SettingsDivider()

                            SettingsToggleItem(
                                icon = Icons.Outlined.Wifi,
                                title = "WiFi Only",
                                subtitle = "Only download on WiFi networks",
                                checked = appSettings.autoDownloadOnWifiOnly,
                                onCheckedChange = {
                                    preferencesManager.updateAutoDownloadWifiOnly(it)
                                }
                            )

                            SettingsDivider()

                            SettingsSliderItem(
                                icon = Icons.Outlined.Numbers,
                                title = "Download Limit",
                                subtitle = if (appSettings.autoDownloadLimit == 0)
                                    "Unlimited chapters per novel"
                                else
                                    "Max ${appSettings.autoDownloadLimit} chapters per novel",
                                value = appSettings.autoDownloadLimit.toFloat(),
                                valueRange = 0f..50f,
                                steps = 9,
                                valueLabel = if (appSettings.autoDownloadLimit == 0) "∞"
                                else appSettings.autoDownloadLimit.toString(),
                                onValueChange = {
                                    preferencesManager.updateAutoDownloadLimit(it.toInt())
                                }
                            )

                            SettingsDivider()

                            SettingsItemLabel(
                                title = "Download for Status",
                                icon = Icons.Outlined.Bookmark
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            AutoDownloadStatusSelector(
                                selectedStatuses = appSettings.autoDownloadForStatuses,
                                onStatusesChanged = {
                                    preferencesManager.updateAutoDownloadStatuses(it)
                                }
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // READER SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Reader",
                    icon = Icons.Outlined.MenuBook,
                    description = "Reading experience settings"
                )
            }

            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Outlined.LightMode,
                        title = "Keep Screen On",
                        subtitle = "Prevent screen timeout while reading",
                        checked = appSettings.keepScreenOn,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(
                                appSettings.copy(keepScreenOn = it)
                            )
                        }
                    )

                    SettingsDivider()

                    SettingsToggleItem(
                        icon = Icons.Outlined.AllInclusive,
                        title = "Infinite Scroll",
                        subtitle = "Automatically load next chapters",
                        checked = appSettings.infiniteScroll,
                        onCheckedChange = {
                            preferencesManager.updateAppSettings(
                                appSettings.copy(infiniteScroll = it)
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // PROVIDERS SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Providers",
                    icon = Icons.Outlined.Extension,
                    description = "Manage content sources"
                )
            }

            item {
                ProviderManagementCard(
                    appSettings = appSettings,
                    onProviderOrderChange = { preferencesManager.updateProviderOrder(it) },
                    onProviderEnabledChange = { name, enabled ->
                        preferencesManager.setProviderEnabled(name, enabled)
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════
            // ABOUT / INFO SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "About",
                    icon = Icons.Outlined.Info,
                    description = "App information"
                )
            }

            item {
                SettingsCard {
                    SettingsInfoItem(
                        icon = Icons.Outlined.Apartment,
                        title = "Version",
                        value = "1.0.0"
                    )

                    SettingsDivider()

                    SettingsClickableItem(
                        icon = Icons.Outlined.RestartAlt,
                        title = "Reset to Defaults",
                        subtitle = "Restore all settings to defaults",
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetDialog = true
                        },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Bottom spacing for navigation bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DisplayModeSelector(
    selectedMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DisplayMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val icon = when (mode) {
                DisplayMode.GRID -> Icons.Outlined.GridView
                DisplayMode.LIST -> Icons.Outlined.ViewList
            }

            SelectableChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = mode.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DisplayModeSection(
    libraryMode: DisplayMode,
    browseMode: DisplayMode,
    searchMode: DisplayMode,
    onLibraryModeChange: (DisplayMode) -> Unit,
    onBrowseModeChange: (DisplayMode) -> Unit,
    onSearchModeChange: (DisplayMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DisplayModeRow(
            label = "Library",
            icon = Icons.Outlined.LibraryBooks,
            selected = libraryMode,
            onSelected = onLibraryModeChange
        )
        DisplayModeRow(
            label = "Browse",
            icon = Icons.Outlined.Explore,
            selected = browseMode,
            onSelected = onBrowseModeChange
        )
        DisplayModeRow(
            label = "Search",
            icon = Icons.Outlined.Search,
            selected = searchMode,
            onSelected = onSearchModeChange
        )
    }
}

@Composable
private fun DisplayModeRow(
    label: String,
    icon: ImageVector,
    selected: DisplayMode,
    onSelected: (DisplayMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.width(80.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            DisplayMode.entries.forEach { mode ->
                val isSelected = mode == selected
                val modeIcon = when (mode) {
                    DisplayMode.GRID -> Icons.Outlined.GridView
                    DisplayMode.LIST -> Icons.Outlined.ViewList
                }

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(200),
                    label = "modeBg"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelected(mode)
                        },
                    color = backgroundColor,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = mode.displayName(),
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.displayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// RESET CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Reset All Settings?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This will reset all settings to their default values, including:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ResetDialogBulletPoint("Theme and appearance settings")
                    ResetDialogBulletPoint("Layout and grid settings")
                    ResetDialogBulletPoint("Reader preferences")
                    ResetDialogBulletPoint("Auto-download settings")
                    ResetDialogBulletPoint("Provider order and status")
                    ResetDialogBulletPoint("TTS settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your library and downloaded chapters will not be affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Reset",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ResetDialogBulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    description: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS ITEMS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsItemLabel(
    title: String,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    highlight: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val backgroundColor by animateColorAsState(
        targetValue = if (highlight && checked)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "background"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (checked && enabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
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
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsDropdownItem(
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontWeight = if (index == selectedIndex)
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal,
                            color = if (index == selectedIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(index)
                        expanded = false
                    },
                    leadingIcon = if (index == selectedIndex) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SettingsSliderItem(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(start = 36.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint.copy(alpha = 0.7f)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = tint.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CUSTOM SELECTORS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val icon = when (mode) {
                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                ThemeMode.DARK -> Icons.Outlined.DarkMode
                ThemeMode.SYSTEM -> Icons.Outlined.SettingsSuggest
            }

            SelectableChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = mode.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DensitySelector(
    selectedDensity: UiDensity,
    onDensitySelected: (UiDensity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UiDensity.entries.forEach { density ->
            val isSelected = density == selectedDensity
            val icon = when (density) {
                UiDensity.COMPACT -> Icons.Outlined.ViewCompact
                UiDensity.DEFAULT -> Icons.Outlined.ViewComfy
                UiDensity.COMFORTABLE -> Icons.Outlined.ViewCozy
            }

            SelectableChip(
                selected = isSelected,
                onClick = { onDensitySelected(density) },
                label = density.displayName(),
                icon = icon,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "chipBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chipContent"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun AutoDownloadStatusSelector(
    selectedStatuses: Set<ReadingStatus>,
    onStatusesChanged: (Set<ReadingStatus>) -> Unit
) {
    val statuses = listOf(
        ReadingStatus.READING to Icons.Outlined.AutoStories,
        ReadingStatus.PLAN_TO_READ to Icons.Outlined.BookmarkAdd,
        ReadingStatus.ON_HOLD to Icons.Outlined.Pause
    )
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { (status, icon) ->
            val isSelected = selectedStatuses.contains(status)

            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newSet = if (isSelected) {
                        selectedStatuses - status
                    } else {
                        selectedStatuses + status
                    }
                    onStatusesChanged(newSet)
                },
                label = {
                    Text(
                        status.displayName(),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GRID COLUMNS SECTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GridColumnsSection(
    libraryColumns: GridColumns,
    browseColumns: GridColumns,
    searchColumns: GridColumns,
    onLibraryColumnsChange: (GridColumns) -> Unit,
    onBrowseColumnsChange: (GridColumns) -> Unit,
    onSearchColumnsChange: (GridColumns) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GridColumnsRow(
            label = "Library",
            icon = Icons.Outlined.LibraryBooks,
            selected = libraryColumns,
            onSelected = onLibraryColumnsChange
        )
        GridColumnsRow(
            label = "Browse",
            icon = Icons.Outlined.Explore,
            selected = browseColumns,
            onSelected = onBrowseColumnsChange
        )
        GridColumnsRow(
            label = "Search",
            icon = Icons.Outlined.Search,
            selected = searchColumns,
            onSelected = onSearchColumnsChange
        )
    }
}

@Composable
private fun GridColumnsRow(
    label: String,
    icon: ImageVector,
    selected: GridColumns,
    onSelected: (GridColumns) -> Unit
) {
    val options = listOf(
        GridColumns.Auto to "Auto",
        GridColumns.Fixed(2) to "2",
        GridColumns.Fixed(3) to "3",
        GridColumns.Fixed(4) to "4",
        GridColumns.Fixed(5) to "5"
    )
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.width(80.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            options.forEach { (columns, displayText) ->
                val isSelected = when {
                    columns is GridColumns.Auto && selected is GridColumns.Auto -> true
                    columns is GridColumns.Fixed && selected is GridColumns.Fixed ->
                        columns.count == selected.count
                    else -> false
                }

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(200),
                    label = "gridBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelected(columns)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PROVIDER MANAGEMENT - DRAGGABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProviderManagementCard(
    appSettings: AppSettings,
    onProviderOrderChange: (List<String>) -> Unit,
    onProviderEnabledChange: (String, Boolean) -> Unit
) {
    val allProviders = remember {
        com.emptycastle.novery.provider.MainProvider.getProviders()
    }

    // Local mutable state for the order during drag operations
    var order by remember(appSettings.providerOrder) {
        mutableStateOf(
            if (appSettings.providerOrder.isEmpty())
                allProviders.map { it.name }
            else
                appSettings.providerOrder
        )
    }

    val haptics = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        order = order.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Save the new order when drag ends
    LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            // Only update if order actually changed
            val currentOrder = if (appSettings.providerOrder.isEmpty())
                allProviders.map { it.name }
            else
                appSettings.providerOrder

            if (order != currentOrder) {
                onProviderOrderChange(order)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsItemLabel(
                title = "Provider Order & Status",
                icon = Icons.Outlined.SwapVert
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Long press and drag to reorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calculate height based on number of items (each item ~56dp + 4dp spacing)
            val itemHeight = 56.dp
            val spacing = 4.dp
            val maxVisibleItems = 6
            val totalItems = order.size
            val listHeight = if (totalItems <= maxVisibleItems) {
                (itemHeight + spacing) * totalItems - spacing
            } else {
                (itemHeight + spacing) * maxVisibleItems
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(listHeight),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(order, key = { _, item -> item }) { _, providerName ->
                    ReorderableItem(
                        reorderableLazyListState,
                        key = providerName
                    ) { isDragging ->
                        val isEnabled = !appSettings.disabledProviders.contains(providerName)

                        DraggableProviderItem(
                            name = providerName,
                            isEnabled = isEnabled,
                            isDragging = isDragging,
                            onEnabledChange = { enabled ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onProviderEnabledChange(providerName, enabled)
                            },
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableProviderItem(
    name: String,
    isEnabled: Boolean,
    isDragging: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer
            isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "providerBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 1f else 0f),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = elevation,
        tonalElevation = if (isDragging) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drag Handle
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (isDragging)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isEnabled) 0.6f else 0.3f
                    ),
                modifier = modifier.size(24.dp)
            )

            // Provider Name
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isDragging) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
                    isEnabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Enable/Disable Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIVE PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LivePreviewCard(
    gridColumns: GridColumns,
    showBadges: Boolean,
    density: UiDensity,
    displayMode: DisplayMode
) {
    val columnCount = when (gridColumns) {
        is GridColumns.Auto -> 3
        is GridColumns.Fixed -> gridColumns.count
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = Icons.Outlined.Preview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${displayMode.displayName()} • ${density.displayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (displayMode) {
                DisplayMode.GRID -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            when (density) {
                                UiDensity.COMPACT -> 8.dp
                                UiDensity.DEFAULT -> 12.dp
                                UiDensity.COMFORTABLE -> 16.dp
                            }
                        )
                    ) {
                        val previewCount = minOf(columnCount, 4)

                        repeat(previewCount) { index ->
                            PreviewNovelCard(
                                modifier = Modifier.weight(1f),
                                showBadge = showBadges && index == 0,
                                density = density,
                                title = listOf(
                                    "The Beginning After The End",
                                    "Solo Leveling",
                                    "Omniscient Reader",
                                    "Tower of God"
                                ).getOrElse(index) { "Novel $index" }
                            )
                        }

                        if (previewCount < columnCount) {
                            repeat(columnCount - previewCount) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                DisplayMode.LIST -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(2) { index ->
                            PreviewNovelListItem(
                                showBadge = showBadges && index == 0,
                                density = density,
                                title = if (index == 0) "The Beginning After The End" else "Solo Leveling"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewNovelListItem(
    showBadge: Boolean = false,
    density: UiDensity,
    title: String
) {
    val itemHeight = when (density) {
        UiDensity.COMPACT -> 60.dp
        UiDensity.DEFAULT -> 72.dp
        UiDensity.COMFORTABLE -> 84.dp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover placeholder
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(16.dp)
                )

                if (showBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "3",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status placeholder
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                    // Chapter placeholder
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun PreviewNovelCard(
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    density: UiDensity,
    title: String
) {
    val isComfortable = density == UiDensity.COMFORTABLE
    val cornerRadius = when (density) {
        UiDensity.COMPACT -> 8.dp
        UiDensity.DEFAULT -> 10.dp
        UiDensity.COMFORTABLE -> 12.dp
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // Placeholder Image
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )

            if (!isComfortable) {
                // Gradient overlay for compact/default
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                ),
                                startY = 100f
                            )
                        )
                )

                // Title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            when (density) {
                                UiDensity.COMPACT -> 6.dp
                                else -> 8.dp
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }

            // Badge
            if (showBadge) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "3",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Title below card for comfortable mode
        if (isComfortable) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ch. 125",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
            )
        }
    }
}