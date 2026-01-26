package com.emptycastle.novery.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.rounded.CloudDownload
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.domain.model.GridColumns
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.LibrarySortOrder
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.ThemeMode
import com.emptycastle.novery.domain.model.UiDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val preferencesManager = remember { RepositoryProvider.getPreferencesManager() }
    val appSettings by preferencesManager.appSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // APPEARANCE SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSection(title = "Appearance", icon = Icons.Default.Palette)
            }

            item {
                SettingsCard {
                    // Theme Mode with visual selector
                    SettingsLabel(title = "Theme")
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeModeSelector(
                        selectedMode = appSettings.themeMode,
                        onModeSelected = { preferencesManager.updateThemeMode(it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // AMOLED Black
                    SettingsSwitch(
                        title = "AMOLED Black",
                        subtitle = "Pure black background (Dark mode only)",
                        checked = appSettings.amoledBlack,
                        enabled = appSettings.themeMode != ThemeMode.LIGHT,
                        onCheckedChange = { preferencesManager.updateAmoledBlack(it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Dynamic Color
                    SettingsSwitch(
                        title = "Dynamic Color",
                        subtitle = "Use Material You colors from wallpaper",
                        checked = appSettings.useDynamicColor,
                        onCheckedChange = {
                            val current = appSettings
                            preferencesManager.updateAppSettings(current.copy(useDynamicColor = it))
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // LAYOUT SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSection(title = "Layout", icon = Icons.Default.ViewCompact)
            }

            item {
                SettingsCard {
                    // UI Density with visual chips
                    SettingsLabel(title = "UI Density")
                    Spacer(modifier = Modifier.height(8.dp))
                    DensitySelector(
                        selectedDensity = appSettings.uiDensity,
                        onDensitySelected = { preferencesManager.updateDensity(it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Grid Columns Settings
                    SettingsLabel(title = "Grid Columns")
                    Spacer(modifier = Modifier.height(8.dp))

                    GridColumnsRow(
                        label = "Library",
                        selected = appSettings.libraryGridColumns,
                        onSelected = { preferencesManager.updateLibraryGridColumns(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GridColumnsRow(
                        label = "Browse",
                        selected = appSettings.browseGridColumns,
                        onSelected = { preferencesManager.updateBrowseGridColumns(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GridColumnsRow(
                        label = "Search",
                        selected = appSettings.searchGridColumns,
                        onSelected = { preferencesManager.updateSearchGridColumns(it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Search Results Per Provider
                    SettingsSlider(
                        title = "Search Results Per Provider",
                        subtitle = "Number of results shown per provider before 'Show more'",
                        value = appSettings.searchResultsPerProvider.toFloat(),
                        valueRange = 4f..12f,
                        steps = 7,
                        valueLabel = "${appSettings.searchResultsPerProvider}",
                        onValueChange = {
                            val current = appSettings
                            preferencesManager.updateAppSettings(
                                current.copy(searchResultsPerProvider = it.toInt())
                            )
                        }
                    )

                    // Show Badges
                    SettingsSwitch(
                        title = "Show Badges",
                        subtitle = "Display download count and status badges",
                        checked = appSettings.showBadges,
                        onCheckedChange = {
                            val current = appSettings
                            preferencesManager.updateAppSettings(current.copy(showBadges = it))
                        }
                    )
                }
            }

            // Live Preview
            item {
                SettingsLabel(title = "Preview")
                Spacer(modifier = Modifier.height(8.dp))
                LivePreviewCard(
                    gridColumns = appSettings.libraryGridColumns,
                    showBadges = appSettings.showBadges,
                    density = appSettings.uiDensity
                )
            }

            // ═══════════════════════════════════════════════════════════
            // LIBRARY SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSection(title = "Library", icon = Icons.Default.LibraryBooks)
            }

            item {
                SettingsCard {
                    // Default Filter
                    SettingsDropdown(
                        title = "Default Filter",
                        subtitle = appSettings.defaultLibraryFilter.displayName(),
                        options = LibraryFilter.entries.map { it.displayName() },
                        selectedIndex = appSettings.defaultLibraryFilter.ordinal,
                        onSelect = { index ->
                            val current = appSettings
                            preferencesManager.updateAppSettings(
                                current.copy(defaultLibraryFilter = LibraryFilter.entries[index])
                            )
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Default Sort
                    SettingsDropdown(
                        title = "Default Sort",
                        subtitle = appSettings.defaultLibrarySort.displayName(),
                        options = LibrarySortOrder.entries.map { it.displayName() },
                        selectedIndex = appSettings.defaultLibrarySort.ordinal,
                        onSelect = { index ->
                            val current = appSettings
                            preferencesManager.updateAppSettings(
                                current.copy(defaultLibrarySort = LibrarySortOrder.entries[index])
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════════════════
            // AUTO-DOWNLOAD SECTION
            // ═══════════════════════════════════════════════════════════════════════════
            item {
                SettingsSection(title = "Auto-Download", icon = Icons.Rounded.CloudDownload)
            }

            item {
                SettingsCard {
                    // Enable Auto-Download
                    SettingsSwitch(
                        title = "Auto-Download New Chapters",
                        subtitle = "Automatically download new chapters when found",
                        checked = appSettings.autoDownloadEnabled,
                        onCheckedChange = { preferencesManager.updateAutoDownloadEnabled(it) }
                    )

                    AnimatedVisibility(
                        visible = appSettings.autoDownloadEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // WiFi Only
                            SettingsSwitch(
                                title = "WiFi Only",
                                subtitle = "Only auto-download when connected to WiFi",
                                checked = appSettings.autoDownloadOnWifiOnly,
                                onCheckedChange = { preferencesManager.updateAutoDownloadWifiOnly(it) }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Download Limit
                            SettingsSlider(
                                title = "Download Limit per Novel",
                                subtitle = if (appSettings.autoDownloadLimit == 0) "Unlimited" else "Max ${appSettings.autoDownloadLimit} chapters",
                                value = appSettings.autoDownloadLimit.toFloat(),
                                valueRange = 0f..50f,
                                steps = 9,
                                valueLabel = if (appSettings.autoDownloadLimit == 0) "∞" else appSettings.autoDownloadLimit.toString(),
                                onValueChange = { preferencesManager.updateAutoDownloadLimit(it.toInt()) }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Status filter
                            SettingsLabel(title = "Auto-download for")
                            Spacer(modifier = Modifier.height(8.dp))
                            AutoDownloadStatusSelector(
                                selectedStatuses = appSettings.autoDownloadForStatuses,
                                onStatusesChanged = { preferencesManager.updateAutoDownloadStatuses(it) }
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // READER SECTION
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSection(title = "Reader", icon = Icons.Default.MenuBook)
            }

            item {
                SettingsCard {
                    // Keep Screen On
                    SettingsSwitch(
                        title = "Keep Screen On",
                        subtitle = "Prevent screen from turning off while reading",
                        checked = appSettings.keepScreenOn,
                        onCheckedChange = {
                            val current = appSettings
                            preferencesManager.updateAppSettings(current.copy(keepScreenOn = it))
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Infinite Scroll
                    SettingsSwitch(
                        title = "Infinite Scroll",
                        subtitle = "Automatically load next chapters while reading",
                        checked = appSettings.infiniteScroll,
                        onCheckedChange = {
                            val current = appSettings
                            preferencesManager.updateAppSettings(current.copy(infiniteScroll = it))
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // PROVIDERS SECTION (Placeholder for future)
            // ═══════════════════════════════════════════════════════════
            item {
                SettingsSection(title = "Providers", icon = Icons.Default.Extension)
            }

            item {
                SettingsCard {
                    SettingsInfo(
                        title = "Provider Settings",
                        subtitle = "Provider management coming soon"
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CUSTOM SELECTORS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AutoDownloadStatusSelector(
    selectedStatuses: Set<ReadingStatus>,
    onStatusesChanged: (Set<ReadingStatus>) -> Unit
) {
    val statuses = listOf(
        ReadingStatus.READING,
        ReadingStatus.PLAN_TO_READ,
        ReadingStatus.ON_HOLD
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { status ->
            val isSelected = selectedStatuses.contains(status)
            FilterChip(
                selected = isSelected,
                onClick = {
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
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        mode.displayName(),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UiDensity.entries.forEach { density ->
            val isSelected = density == selectedDensity
            FilterChip(
                selected = isSelected,
                onClick = { onDensitySelected(density) },
                label = {
                    Text(
                        density.displayName(),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun GridColumnsRow(
    label: String,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (columns, displayText) ->
                val isSelected = when {
                    columns is GridColumns.Auto && selected is GridColumns.Auto -> true
                    columns is GridColumns.Fixed && selected is GridColumns.Fixed ->
                        columns.count == selected.count
                    else -> false
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelected(columns) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    density: UiDensity = UiDensity.DEFAULT
) {
    val columnCount = when (gridColumns) {
        is GridColumns.Auto -> 3
        is GridColumns.Fixed -> gridColumns.count
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${density.displayName()} Layout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (gridColumns is GridColumns.Auto) "Auto Columns" else "$columnCount Columns",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulate the grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    if(density == UiDensity.COMPACT) 8.dp else 12.dp
                )
            ) {
                // Show up to 3 cards for preview
                val previewCount = minOf(columnCount, 4)

                repeat(previewCount) { index ->
                    PreviewNovelCard(
                        modifier = Modifier.weight(1f),
                        showBadge = showBadges && index == 0,
                        density = density,
                        title = if (index == 0) "The Beginning After The End" else "Solo Leveling"
                    )
                }

                // Add spacers to keep size correct if columns > 3
                if (previewCount < columnCount) {
                    repeat(columnCount - previewCount) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
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

    Column(modifier = modifier) {
        // Image Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(if (density == UiDensity.COMPACT) 8.dp else 12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            // Mock Image Icon
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )

            // IF COMPACT/DEFAULT: Text is overlay
            if (!isComfortable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 0.5f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(if (density == UiDensity.COMPACT) 4.dp else 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            // Badge
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // IF COMFORTABLE: Text is below
        if (isComfortable) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ch. 125",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BASE COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsLabel(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsDropdown(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
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
                            color = if (index == selectedIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    leadingIcon = {
                        if (index == selectedIndex) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSlider(
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
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsInfo(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}