// com/emptycastle/novery/ui/screens/home/tabs/recommendation/components/TagFilterSheet.kt

package com.emptycastle.novery.ui.screens.home.tabs.recommendation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.data.local.entity.TagFilterType
import com.emptycastle.novery.recommendation.TagNormalizer
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.TagNormalizer.TagGroup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterSheet(
    tagFilters: Map<TagCategory, TagFilterType>,
    onSetFilter: (TagCategory, TagFilterType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val groupedTags = remember { TagNormalizer.getAllTagsByGroup() }

    // Build a flat list with headers for position tracking
    val flatListItems = remember(groupedTags) {
        buildList {
            groupedTags.forEach { (group, tags) ->
                if (tags.isNotEmpty()) {
                    add(TagListItem.Header(group))
                    tags.forEach { tag ->
                        add(TagListItem.Tag(tag, group))
                    }
                }
            }
        }
    }

    // Map group to its position in the flat list
    val groupPositions = remember(flatListItems) {
        flatListItems.mapIndexedNotNull { index, item ->
            if (item is TagListItem.Header) item.group to index else null
        }.toMap()
    }

    // Filter items based on search
    val filteredItems by remember(searchQuery, flatListItems) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                flatListItems
            } else {
                flatListItems.filter { item ->
                    when (item) {
                        is TagListItem.Header -> false // Headers are added back if they have matching tags
                        is TagListItem.Tag -> TagNormalizer.getDisplayName(item.tag)
                            .contains(searchQuery, ignoreCase = true)
                    }
                }.let { filteredTags ->
                    // Re-add headers for groups that have visible tags
                    val visibleGroups = filteredTags.filterIsInstance<TagListItem.Tag>()
                        .map { it.group }
                        .toSet()

                    buildList {
                        var lastGroup: TagGroup? = null
                        filteredTags.forEach { item ->
                            if (item is TagListItem.Tag && item.group != lastGroup) {
                                add(TagListItem.Header(item.group))
                                lastGroup = item.group
                            }
                            add(item)
                        }
                    }
                }
            }
        }
    }

    val blockedCount = tagFilters.count { it.value == TagFilterType.BLOCKED }
    val boostedCount = tagFilters.count { it.value == TagFilterType.BOOSTED }
    val reducedCount = tagFilters.count { it.value == TagFilterType.REDUCED }

    // Quick filter options with their target groups
    val quickFilters = remember {
        listOf(
            QuickFilter("🔞 Mature", TagGroup.CONTENT_WARNINGS),
            QuickFilter("🏳️‍🌈 LGBTQ+", TagGroup.LGBTQ),
            QuickFilter("💕 Romance", TagGroup.ROMANCE_TYPES),
            QuickFilter("🥋 Eastern", TagGroup.EASTERN),
            QuickFilter("🎮 GameLit", TagGroup.LITRPG_GAMELIT),
            QuickFilter("⚔️ Action", TagGroup.MAIN_GENRES),
            QuickFilter("🔮 Magic", TagGroup.MAGIC_SUPERNATURAL)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter by Tags",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Active filters summary
                    if (blockedCount > 0 || boostedCount > 0 || reducedCount > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (blockedCount > 0) {
                                FilterCountChip(
                                    count = blockedCount,
                                    label = "blocked",
                                    color = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            }
                            if (reducedCount > 0) {
                                FilterCountChip(
                                    count = reducedCount,
                                    label = "reduced",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                            if (boostedCount > 0) {
                                FilterCountChip(
                                    count = boostedCount,
                                    label = "boosted",
                                    color = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }
                    }
                }

                if (blockedCount > 0 || boostedCount > 0 || reducedCount > 0) {
                    TextButton(onClick = {
                        tagFilters.keys.forEach { tag ->
                            onSetFilter(tag, TagFilterType.NEUTRAL)
                        }
                    }) {
                        Text("Clear All", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search tags...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick filter chips - now scroll to section
            Text(
                text = "Jump to section",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(quickFilters) { filter ->
                    QuickFilterChip(
                        text = filter.label,
                        onClick = {
                            // Clear search and scroll to section
                            searchQuery = ""
                            groupPositions[filter.targetGroup]?.let { position ->
                                scope.launch {
                                    listState.animateScrollToItem(position)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.Rounded.Block,
                        label = "Hide"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Rounded.KeyboardArrowDown,
                        label = "Show less"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.TrendingUp,
                        label = "Boost"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { item ->
                        when (item) {
                            is TagListItem.Header -> "header_${item.group.name}"
                            is TagListItem.Tag -> "tag_${item.tag.name}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is TagListItem.Header -> {
                            TagGroupHeader(
                                group = item.group,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        is TagListItem.Tag -> {
                            TagFilterRow(
                                tag = item.tag,
                                currentFilter = tagFilters[item.tag] ?: TagFilterType.NEUTRAL,
                                onFilterChange = { newFilter -> onSetFilter(item.tag, newFilter) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

private sealed class TagListItem {
    data class Header(val group: TagGroup) : TagListItem()
    data class Tag(val tag: TagCategory, val group: TagGroup) : TagListItem()
}

private data class QuickFilter(
    val label: String,
    val targetGroup: TagGroup
)

@Composable
private fun FilterCountChip(
    count: Int,
    label: String,
    color: Color,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun QuickFilterChip(
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = null,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun TagGroupHeader(
    group: TagGroup,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = group.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun TagFilterRow(
    tag: TagCategory,
    currentFilter: TagFilterType,
    onFilterChange: (TagFilterType) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (currentFilter) {
            TagFilterType.BLOCKED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            TagFilterType.REDUCED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            TagFilterType.BOOSTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            TagFilterType.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(200),
        label = "bg_color"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TagNormalizer.getDisplayName(tag),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (currentFilter != TagFilterType.NEUTRAL) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Block button
                FilterToggleButton(
                    isSelected = currentFilter == TagFilterType.BLOCKED,
                    icon = Icons.Rounded.Block,
                    selectedColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        onFilterChange(
                            if (currentFilter == TagFilterType.BLOCKED) TagFilterType.NEUTRAL else TagFilterType.BLOCKED
                        )
                    },
                    contentDescription = "Block ${TagNormalizer.getDisplayName(tag)}"
                )

                // Reduce button
                FilterToggleButton(
                    isSelected = currentFilter == TagFilterType.REDUCED,
                    icon = Icons.Rounded.KeyboardArrowDown,
                    selectedColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        onFilterChange(
                            if (currentFilter == TagFilterType.REDUCED) TagFilterType.NEUTRAL else TagFilterType.REDUCED
                        )
                    },
                    contentDescription = "Reduce ${TagNormalizer.getDisplayName(tag)}"
                )

                // Boost button
                FilterToggleButton(
                    isSelected = currentFilter == TagFilterType.BOOSTED,
                    icon = Icons.Rounded.TrendingUp,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onFilterChange(
                            if (currentFilter == TagFilterType.BOOSTED) TagFilterType.NEUTRAL else TagFilterType.BOOSTED
                        )
                    },
                    contentDescription = "Boost ${TagNormalizer.getDisplayName(tag)}"
                )
            }
        }
    }
}

@Composable
private fun FilterToggleButton(
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedColor: Color,
    onClick: () -> Unit,
    contentDescription: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(200),
        label = "btn_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "btn_icon"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}