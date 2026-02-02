// com/emptycastle/novery/ui/screens/details/components/ChapterListHeader.kt
// Restored original with minor visual improvements
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.screens.details.ChapterDisplayMode
import com.emptycastle.novery.ui.screens.details.ChapterFilter
import com.emptycastle.novery.ui.screens.details.ChaptersPerPage
import com.emptycastle.novery.ui.screens.details.PaginationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListHeader(
    chapterCount: Int,
    filteredCount: Int,
    isDescending: Boolean,
    isSelectionMode: Boolean,
    currentFilter: ChapterFilter,
    isSearchActive: Boolean,
    searchQuery: String,
    unreadCount: Int,
    downloadedCount: Int,
    notDownloadedCount: Int,
    displayMode: ChapterDisplayMode,
    paginationState: PaginationState,
    onToggleSort: () -> Unit,
    onFilterChange: (ChapterFilter) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEnableSelection: () -> Unit,
    onDisplayModeChange: (ChapterDisplayMode) -> Unit,
    onChaptersPerPageChange: (ChaptersPerPage) -> Unit,
    onJumpToFirstUnread: () -> Unit,
    onJumpToLastRead: () -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Title row with actions
            ChapterHeaderTitleRow(
                filteredCount = filteredCount,
                chapterCount = chapterCount,
                isSearchActive = isSearchActive,
                showFilters = showFilters,
                showDisplayOptions = showDisplayOptions,
                currentFilter = currentFilter,
                isDescending = isDescending,
                isSelectionMode = isSelectionMode,
                displayMode = displayMode,
                onToggleSearch = onToggleSearch,
                onToggleFilters = { showFilters = !showFilters },
                onToggleDisplayOptions = { showDisplayOptions = !showDisplayOptions },
                onToggleSort = onToggleSort,
                onEnableSelection = onEnableSelection
            )

            // Search field
            ChapterSearchField(
                isVisible = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )

            // Filter chips
            ChapterFilterChips(
                isVisible = showFilters || currentFilter != ChapterFilter.ALL,
                currentFilter = currentFilter,
                chapterCount = chapterCount,
                unreadCount = unreadCount,
                downloadedCount = downloadedCount,
                notDownloadedCount = notDownloadedCount,
                onFilterChange = onFilterChange
            )

            // Display options section
            DisplayOptionsSection(
                isVisible = showDisplayOptions,
                displayMode = displayMode,
                paginationState = paginationState,
                onDisplayModeChange = onDisplayModeChange,
                onChaptersPerPageChange = onChaptersPerPageChange,
                onJumpToFirstUnread = onJumpToFirstUnread,
                onJumpToLastRead = onJumpToLastRead
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterHeaderTitleRow(
    filteredCount: Int,
    chapterCount: Int,
    isSearchActive: Boolean,
    showFilters: Boolean,
    showDisplayOptions: Boolean,
    currentFilter: ChapterFilter,
    isDescending: Boolean,
    isSelectionMode: Boolean,
    displayMode: ChapterDisplayMode,
    onToggleSearch: () -> Unit,
    onToggleFilters: () -> Unit,
    onToggleDisplayOptions: () -> Unit,
    onToggleSort: () -> Unit,
    onEnableSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter count with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AnimatedContent(
                    targetState = Pair(filteredCount, chapterCount),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chapter_count"
                ) { (filtered, total) ->
                    Text(
                        text = if (filtered != total) "$filtered of $total" else "$total total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Search button
            HeaderActionButton(
                onClick = onToggleSearch,
                isActive = isSearchActive,
                icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
                contentDescription = "Search"
            )

            // Filter button
            FilterActionButton(
                onClick = onToggleFilters,
                isActive = showFilters,
                hasActiveFilter = currentFilter != ChapterFilter.ALL
            )

            // Display options button
            HeaderActionButton(
                onClick = onToggleDisplayOptions,
                isActive = showDisplayOptions,
                icon = if (displayMode == ChapterDisplayMode.PAGINATED)
                    Icons.Outlined.AutoStories
                else
                    Icons.Outlined.TableRows,
                contentDescription = "Display options"
            )

            // Sort button
            IconButton(
                onClick = onToggleSort,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = if (isDescending) "Newest first" else "Oldest first",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Selection mode button
            if (!isSelectionMode) {
                IconButton(
                    onClick = onEnableSelection,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckBox,
                        contentDescription = "Select chapters",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayOptionsSection(
    isVisible: Boolean,
    displayMode: ChapterDisplayMode,
    paginationState: PaginationState,
    onDisplayModeChange: (ChapterDisplayMode) -> Unit,
    onChaptersPerPageChange: (ChaptersPerPage) -> Unit,
    onJumpToFirstUnread: () -> Unit,
    onJumpToLastRead: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Display mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Display Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = displayMode == ChapterDisplayMode.SCROLL,
                        onClick = { onDisplayModeChange(ChapterDisplayMode.SCROLL) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {}
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TableRows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Scroll", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    SegmentedButton(
                        selected = displayMode == ChapterDisplayMode.PAGINATED,
                        onClick = { onDisplayModeChange(ChapterDisplayMode.PAGINATED) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {}
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Pages", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Chapters per page (only for paginated mode)
            AnimatedVisibility(
                visible = displayMode == ChapterDisplayMode.PAGINATED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ChaptersPerPageSelector(
                    currentValue = paginationState.chaptersPerPage,
                    onValueChange = onChaptersPerPageChange
                )
            }

            // Quick navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickNavigationButton(
                    text = "First Unread",
                    onClick = onJumpToFirstUnread,
                    modifier = Modifier.weight(1f)
                )
                QuickNavigationButton(
                    text = "Last Read",
                    onClick = onJumpToLastRead,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChaptersPerPageSelector(
    currentValue: ChaptersPerPage,
    onValueChange: (ChaptersPerPage) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chapters per page",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChaptersPerPage.entries.forEach { option ->
                item {
                    FilterChip(
                        selected = currentValue == option,
                        onClick = { onValueChange(option) },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickNavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun HeaderActionButton(
    onClick: () -> Unit,
    isActive: Boolean,
    icon: ImageVector,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (isActive)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterActionButton(
    onClick: () -> Unit,
    isActive: Boolean,
    hasActiveFilter: Boolean
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        BadgedBox(
            badge = {
                if (hasActiveFilter) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                modifier = Modifier.size(20.dp),
                tint = if (isActive)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChapterSearchField(
    isVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            placeholder = {
                Text(
                    "Search chapters...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterFilterChips(
    isVisible: Boolean,
    currentFilter: ChapterFilter,
    chapterCount: Int,
    unreadCount: Int,
    downloadedCount: Int,
    notDownloadedCount: Int,
    onFilterChange: (ChapterFilter) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ChapterFilterChip(
                    selected = currentFilter == ChapterFilter.ALL,
                    onClick = { onFilterChange(ChapterFilter.ALL) },
                    label = "All",
                    count = chapterCount
                )
            }
            item {
                ChapterFilterChip(
                    selected = currentFilter == ChapterFilter.UNREAD,
                    onClick = { onFilterChange(ChapterFilter.UNREAD) },
                    label = "Unread",
                    count = unreadCount,
                    icon = Icons.Outlined.VisibilityOff
                )
            }
            item {
                ChapterFilterChip(
                    selected = currentFilter == ChapterFilter.DOWNLOADED,
                    onClick = { onFilterChange(ChapterFilter.DOWNLOADED) },
                    label = "Downloaded",
                    count = downloadedCount,
                    icon = Icons.Default.DownloadDone
                )
            }
            item {
                ChapterFilterChip(
                    selected = currentFilter == ChapterFilter.NOT_DOWNLOADED,
                    onClick = { onFilterChange(ChapterFilter.NOT_DOWNLOADED) },
                    label = "Not Downloaded",
                    count = notDownloadedCount,
                    icon = Icons.Outlined.CloudDownload
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        leadingIcon = if (icon != null && selected) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    )
}