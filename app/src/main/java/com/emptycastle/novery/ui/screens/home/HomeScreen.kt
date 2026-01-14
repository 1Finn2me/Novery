package com.emptycastle.novery.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.components.EmptyHistory
import com.emptycastle.novery.ui.components.EmptyLibrary
import com.emptycastle.novery.ui.components.EmptySearchResults
import com.emptycastle.novery.ui.components.FilterChip
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.components.HistoryListItem
import com.emptycastle.novery.ui.components.HistoryListItemCompact
import com.emptycastle.novery.ui.components.LoadingIndicator
import com.emptycastle.novery.ui.components.NovelActionSheet
import com.emptycastle.novery.ui.components.NovelCard
import com.emptycastle.novery.ui.components.NovelGridSkeleton
import com.emptycastle.novery.ui.components.NoveryPullToRefreshBox
import com.emptycastle.novery.ui.components.NoverySearchBar
import com.emptycastle.novery.ui.screens.MainScaffold
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.calculateGridColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialTab: String = "library",
    appSettings: AppSettings = AppSettings(),
    onNovelClick: (Novel, String) -> Unit,
    onSettingsClick: () -> Unit,
    onChapterClick: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSheetState = rememberModalBottomSheetState()

    LaunchedEffect(initialTab) {
        val tab = when (initialTab) {
            "library" -> HomeTab.LIBRARY
            "browse" -> HomeTab.BROWSE
            "search" -> HomeTab.SEARCH
            "history" -> HomeTab.HISTORY
            else -> HomeTab.LIBRARY
        }
        if (uiState.currentTab != tab) {
            viewModel.selectTab(tab)
        }
    }

    val currentRoute = when (uiState.currentTab) {
        HomeTab.LIBRARY -> "library"
        HomeTab.BROWSE -> "browse"
        HomeTab.SEARCH -> "search"
        HomeTab.HISTORY -> "history"
    }

    // Unified Action Sheet
    if (uiState.showActionSheet && uiState.actionSheetData != null) {
        val data = uiState.actionSheetData!!
        val source = uiState.actionSheetSource

        NovelActionSheet(
            data = data,
            sheetState = actionSheetState,
            onDismiss = { viewModel.hideActionSheet() },
            onViewDetails = {
                onNovelClick(data.novel, data.novel.apiName)
            },
            onContinueReading = {
                val libraryPosition = viewModel.getReadingPosition(data.novel.url)
                val historyChapter = viewModel.getHistoryChapter(data.novel.url)

                when {
                    libraryPosition != null -> {
                        onChapterClick(libraryPosition.chapterUrl, data.novel.url, data.novel.apiName)
                    }
                    historyChapter != null -> {
                        onChapterClick(historyChapter.first, data.novel.url, data.novel.apiName)
                    }
                    else -> {
                        onNovelClick(data.novel, data.novel.apiName)
                    }
                }
            },
            onAddToLibrary = {
                viewModel.addToLibrary(data.novel)
            },
            onRemoveFromLibrary = {
                viewModel.removeFromLibrary(data.novel.url)
            },
            onRemoveFromHistory = if (source == ActionSheetSource.HISTORY) {
                { viewModel.removeFromHistory(data.novel.url) }
            } else null
        )
    }

    MainScaffold(
        currentTab = currentRoute,
        onTabChange = { route ->
            val tab = when (route) {
                "library" -> HomeTab.LIBRARY
                "browse" -> HomeTab.BROWSE
                "search" -> HomeTab.SEARCH
                "history" -> HomeTab.HISTORY
                else -> HomeTab.LIBRARY
            }
            viewModel.selectTab(tab)
        },
        onSettingsClick = onSettingsClick,
        appSettings = appSettings
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.currentTab) {
                HomeTab.LIBRARY -> LibraryTab(
                    items = viewModel.getFilteredLibrary(),
                    downloadCounts = uiState.downloadCounts,
                    selectedFilter = uiState.libraryFilter,
                    searchQuery = uiState.librarySearchQuery,
                    onSearchQueryChange = { viewModel.setLibrarySearchQuery(it) },
                    onFilterChange = { viewModel.setLibraryFilter(it) },
                    onNovelClick = { item ->
                        val position = item.lastReadPosition
                        if (position != null) {
                            onChapterClick(position.chapterUrl, item.novel.url, item.novel.apiName)
                        } else {
                            onNovelClick(item.novel, item.novel.apiName)
                        }
                    },
                    onNovelLongClick = { item ->
                        viewModel.showLibraryActionSheet(item)
                    },
                    appSettings = appSettings
                )

                HomeTab.BROWSE -> BrowseTab(
                    uiState = uiState,
                    onProviderChange = { viewModel.setActiveProvider(it) },
                    onTagChange = { viewModel.setSelectedTag(it) },
                    onSortChange = { viewModel.setSelectedSort(it) },
                    onNovelClick = { novel ->
                        onNovelClick(novel, uiState.activeProvider?.name ?: "")
                    },
                    onNovelLongClick = { novel ->
                        viewModel.showBrowseActionSheet(novel)
                    },
                    onNextPage = { viewModel.nextPage() },
                    onPreviousPage = { viewModel.previousPage() },
                    onRetry = { viewModel.loadBrowsePage() },
                    appSettings = appSettings
                )

                HomeTab.SEARCH -> SearchTab(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { viewModel.performSearch() },
                    isSearching = uiState.isSearching,
                    results = uiState.searchResults,
                    providers = uiState.providers,
                    onNovelClick = { novel -> onNovelClick(novel, novel.apiName) },
                    onNovelLongClick = { novel ->
                        viewModel.showSearchActionSheet(novel)
                    },
                    appSettings = appSettings
                )

                HomeTab.HISTORY -> HistoryTab(
                    items = uiState.historyItems,
                    onContinueReading = { item ->
                        onChapterClick(item.chapterUrl, item.novel.url, item.novel.apiName)
                    },
                    onRemoveFromHistory = { novelUrl ->
                        viewModel.removeFromHistory(novelUrl)
                    },
                    onNovelClick = { item ->
                        onNovelClick(item.novel, item.novel.apiName)
                    },
                    onNovelLongClick = { item ->
                        viewModel.showHistoryActionSheet(item)
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    appSettings = appSettings
                )
            }
        }
    }
}

// ============================================================================
// LIBRARY TAB
// ============================================================================

@Composable
private fun LibraryTab(
    items: List<LibraryItem>,
    downloadCounts: Map<String, Int>,
    selectedFilter: LibraryFilter,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onNovelClick: (LibraryItem) -> Unit,
    onNovelLongClick: (LibraryItem) -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    val gridColumns = calculateGridColumns(appSettings.libraryGridColumns)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar at very top with status bar padding
            LibrarySearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding.calculateTopPadding() + dimensions.spacingSm)
                    .padding(horizontal = dimensions.gridPadding)
            )

            Spacer(modifier = Modifier.height(dimensions.spacingSm))

            // Content area
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (searchQuery.isNotBlank()) {
                        EmptySearchResults(query = searchQuery)
                    } else {
                        EmptyLibrary(onBrowse = {})
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = dimensions.gridPadding,
                        end = dimensions.gridPadding,
                        top = dimensions.spacingSm,
                        bottom = 60.dp // Space for filter chips
                    ),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                    verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
                ) {
                    items(items, key = { it.novel.url }) { item ->
                        NovelCard(
                            novel = item.novel,
                            onClick = { onNovelClick(item) },
                            onLongClick = { onNovelLongClick(item) },
                            downloadCount = if (appSettings.showBadges) downloadCounts[item.novel.url] ?: 0 else 0,
                            readingStatus = if (appSettings.showBadges) item.readingStatus else null,
                            lastReadChapter = item.lastReadPosition?.chapterName,
                            density = appSettings.uiDensity
                        )
                    }
                }
            }
        }

        // Filter chips at bottom (above the navigation bar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = dimensions.gridPadding, vertical = dimensions.spacingSm),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSm)
            ) {
                LibraryFilter.entries.forEach { filter ->
                    FilterChip(
                        text = filter.displayName(),
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = {
            Text(
                "Search library...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

// ============================================================================
// BROWSE TAB - Improved Design
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BrowseTab(
    uiState: HomeUiState,
    onProviderChange: (MainProvider) -> Unit,
    onTagChange: (String?) -> Unit,
    onSortChange: (String?) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onRetry: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    var showFilters by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val gridColumns = calculateGridColumns(appSettings.browseGridColumns)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    val hasActiveFilters = remember(uiState.selectedSort, uiState.selectedTag, uiState.activeProvider) {
        val provider = uiState.activeProvider
        if (provider == null) false
        else {
            val defaultSort = provider.orderBys.firstOrNull()?.value
            val defaultTag = provider.tags.firstOrNull()?.value
            uiState.selectedSort != defaultSort || uiState.selectedTag != defaultTag
        }
    }

    LaunchedEffect(uiState.isBrowseLoading) {
        if (!uiState.isBrowseLoading) {
            isRefreshing = false
        }
    }

    NoveryPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRetry()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.browseError != null -> {
                    BrowseErrorState(
                        message = uiState.browseError,
                        onRetry = onRetry
                    )
                }

                uiState.isBrowseLoading && !isRefreshing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding.calculateTopPadding())
                    ) {
                        // Show header even during loading
                        if (uiState.providers.isNotEmpty()) {
                            BrowseHeader(
                                providers = uiState.providers,
                                selectedProvider = uiState.activeProvider,
                                onProviderSelected = onProviderChange,
                                showFilters = showFilters,
                                hasActiveFilters = hasActiveFilters,
                                onToggleFilters = { showFilters = !showFilters }
                            )
                        }
                        NovelGridSkeleton(columns = gridColumns)
                    }
                }

                uiState.browseNovels.isEmpty() && !uiState.isBrowseLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding.calculateTopPadding())
                    ) {
                        if (uiState.providers.isNotEmpty()) {
                            BrowseHeader(
                                providers = uiState.providers,
                                selectedProvider = uiState.activeProvider,
                                onProviderSelected = onProviderChange,
                                showFilters = showFilters,
                                hasActiveFilters = hasActiveFilters,
                                onToggleFilters = { showFilters = !showFilters }
                            )
                        }
                        BrowseEmptyState()
                    }
                }

                else -> {
                    // Main scrollable grid with embedded header
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = statusBarPadding.calculateTopPadding(),
                            bottom = 80.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
                    ) {
                        // Provider chips - spans full width
                        if (uiState.providers.isNotEmpty()) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "header"
                            ) {
                                BrowseHeader(
                                    providers = uiState.providers,
                                    selectedProvider = uiState.activeProvider,
                                    onProviderSelected = onProviderChange,
                                    showFilters = showFilters,
                                    hasActiveFilters = hasActiveFilters,
                                    onToggleFilters = { showFilters = !showFilters }
                                )
                            }
                        }

                        // Collapsible filters panel - spans full width
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "filters"
                        ) {
                            AnimatedVisibility(
                                visible = showFilters && uiState.activeProvider != null,
                                enter = expandVertically(
                                    animationSpec = tween(durationMillis = 250)
                                ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                                exit = shrinkVertically(
                                    animationSpec = tween(durationMillis = 200)
                                ) + fadeOut(animationSpec = tween(durationMillis = 200))
                            ) {
                                ModernFiltersPanel(
                                    provider = uiState.activeProvider!!,
                                    selectedSort = uiState.selectedSort,
                                    selectedTag = uiState.selectedTag,
                                    onSortChange = onSortChange,
                                    onTagChange = onTagChange,
                                    onClearFilters = {
                                        onSortChange(uiState.activeProvider.orderBys.firstOrNull()?.value)
                                        onTagChange(uiState.activeProvider.tags.firstOrNull()?.value)
                                    }
                                )
                            }
                        }

                        // Spacer item for padding before novels
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "spacer"
                        ) {
                            Spacer(modifier = Modifier.height(dimensions.spacingSm))
                        }

                        // Novel cards
                        items(
                            items = uiState.browseNovels,
                            key = { it.url }
                        ) { novel ->
                            NovelCard(
                                novel = novel,
                                onClick = { onNovelClick(novel) },
                                onLongClick = { onNovelLongClick(novel) },
                                density = appSettings.uiDensity,
                                modifier = Modifier.padding(horizontal = dimensions.gridPadding / 2)
                            )
                        }
                    }
                }
            }

            // Floating Pagination Bar
            if (uiState.browseNovels.isNotEmpty()) {
                ModernPaginationBar(
                    currentPage = uiState.browsePage,
                    onPrevious = onPreviousPage,
                    onNext = onNextPage,
                    hasPrevious = uiState.browsePage > 1,
                    isLoading = uiState.isBrowseLoading,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BrowseHeader(
    providers: List<MainProvider>,
    selectedProvider: MainProvider?,
    onProviderSelected: (MainProvider) -> Unit,
    showFilters: Boolean,
    hasActiveFilters: Boolean,
    onToggleFilters: () -> Unit
) {
    val dimensions = NoveryTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Provider Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = dimensions.gridPadding, vertical = dimensions.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            providers.forEach { provider ->
                val isSelected = provider.name == selectedProvider?.name

                ProviderChip(
                    name = provider.name,
                    isSelected = isSelected,
                    onClick = { onProviderSelected(provider) }
                )
            }

            // Spacer to ensure last item is visible
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding)
                .padding(bottom = dimensions.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current provider info
            if (selectedProvider != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Source,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedProvider.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Filter Toggle Button
            FilterToggleButton(
                isOpen = showFilters,
                hasActiveFilters = hasActiveFilters,
                onClick = onToggleFilters
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "chip_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterToggleButton(
    isOpen: Boolean,
    hasActiveFilters: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "filter_rotation"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isOpen -> MaterialTheme.colorScheme.primaryContainer
            hasActiveFilters -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = when {
                    isOpen -> MaterialTheme.colorScheme.primary
                    hasActiveFilters -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = "Filters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isOpen -> MaterialTheme.colorScheme.primary
                    hasActiveFilters -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Active indicator badge
            if (hasActiveFilters && !isOpen) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary,
                            CircleShape
                        )
                )
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = when {
                    isOpen -> MaterialTheme.colorScheme.primary
                    hasActiveFilters -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModernFiltersPanel(
    provider: MainProvider,
    selectedSort: String?,
    selectedTag: String?,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    val dimensions = NoveryTheme.dimensions

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.gridPadding)
            .padding(bottom = dimensions.spacingSm),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Reset button
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
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Filter Options",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Reset button
                Surface(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Sort By Section
            FilterSection(
                title = "Sort By",
                icon = Icons.Rounded.Sort
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    provider.orderBys.forEach { option ->
                        ModernFilterChip(
                            text = option.label,
                            selected = selectedSort == option.value,
                            onClick = { onSortChange(option.value) }
                        )
                    }
                }
            }

            // Genre Section
            if (provider.tags.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                FilterSection(
                    title = "Genre",
                    icon = Icons.Rounded.Category
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        provider.tags.forEach { tag ->
                            ModernFilterChip(
                                text = tag.label,
                                selected = selectedTag == tag.value,
                                onClick = { onTagChange(tag.value) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(150),
        label = "filter_chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(150),
        label = "filter_chip_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernPaginationBar(
    currentPage: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            PaginationButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                onClick = onPrevious,
                enabled = hasPrevious && !isLoading,
                contentDescription = "Previous page"
            )

            // Page Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Page $currentPage",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Next Button
            PaginationButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onNext,
                enabled = !isLoading,
                contentDescription = "Next page"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaginationButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(150),
        label = "pagination_btn_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        },
        animationSpec = tween(150),
        label = "pagination_btn_content"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        modifier = Modifier.size(44.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
    }
}

@Composable
private fun BrowseErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Failed to Load",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BrowseEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "No Novels Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Try adjusting your filters or selecting a different source",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// SEARCH TAB
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    results: Map<String, List<Novel>>,
    providers: List<MainProvider>,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    val gridColumns = calculateGridColumns(appSettings.searchGridColumns)
    val resultsPerProvider = appSettings.searchResultsPerProvider
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    var expandedProvider by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        NoverySearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            isLoading = isSearching,
            autoFocus = true,
            modifier = Modifier.padding(dimensions.gridPadding)
        )

        when {
            isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = "Searching...")
                }
            }

            results.isEmpty() && query.isNotBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptySearchResults(query = query)
                }
            }

            results.isNotEmpty() -> {
                if (expandedProvider != null && results.containsKey(expandedProvider)) {
                    ExpandedProviderResults(
                        providerName = expandedProvider!!,
                        novels = results[expandedProvider!!] ?: emptyList(),
                        gridColumns = gridColumns,
                        onNovelClick = onNovelClick,
                        onNovelLongClick = onNovelLongClick,
                        onBack = { expandedProvider = null },
                        appSettings = appSettings
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = dimensions.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingLg)
                    ) {
                        results.forEach { (providerName, novels) ->
                            item(key = providerName) {
                                ProviderSearchResults(
                                    providerName = providerName,
                                    novels = novels,
                                    maxResults = resultsPerProvider,
                                    onNovelClick = onNovelClick,
                                    onNovelLongClick = onNovelLongClick,
                                    onShowMore = { expandedProvider = providerName },
                                    appSettings = appSettings
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSearchResults(
    providerName: String,
    novels: List<Novel>,
    maxResults: Int,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    onShowMore: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    val displayNovels = novels.take(maxResults)
    val hasMore = novels.size > maxResults

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${novels.size}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (hasMore) {
                Surface(
                    onClick = onShowMore,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensions.spacingSm))

        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensions.gridPadding),
            horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
        ) {
            items(displayNovels, key = { it.url }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onLongClick = { onNovelLongClick(novel) },
                    density = appSettings.uiDensity,
                    modifier = Modifier.width(100.dp)
                )
            }

            if (hasMore) {
                item {
                    ShowMoreCard(
                        remainingCount = novels.size - maxResults,
                        onClick = onShowMore
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowMoreCard(
    remainingCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(150.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+$remainingCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ExpandedProviderResults(
    providerName: String,
    novels: List<Novel>,
    gridColumns: Int,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding, vertical = dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = providerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${novels.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(dimensions.gridPadding),
            horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
            verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
        ) {
            items(novels, key = { it.url }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onLongClick = { onNovelLongClick(novel) },
                    showApiName = false,
                    density = appSettings.uiDensity
                )
            }
        }
    }
}

// ============================================================================
// HISTORY TAB
// ============================================================================

@Composable
private fun HistoryTab(
    items: List<HistoryItem>,
    onContinueReading: (HistoryItem) -> Unit,
    onRemoveFromHistory: (String) -> Unit,
    onNovelClick: (HistoryItem) -> Unit,
    onNovelLongClick: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    val useCompactLayout = appSettings.uiDensity == UiDensity.COMPACT
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        if (items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.gridPadding, vertical = dimensions.spacingSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${items.size} ${if (items.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GhostButton(
                    text = "Clear",
                    onClick = onClearHistory,
                    icon = Icons.Default.Delete,
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyHistory()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = dimensions.gridPadding,
                    vertical = dimensions.spacingSm
                ),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingSm)
            ) {
                items(
                    items = items,
                    key = { "${it.novel.url}_${it.timestamp}" }
                ) { item ->
                    if (useCompactLayout) {
                        HistoryListItemCompact(
                            item = item,
                            onContinueClick = { onContinueReading(item) },
                            onRemoveClick = { onRemoveFromHistory(item.novel.url) },
                            onItemClick = { onNovelClick(item) },
                            onLongClick = { onNovelLongClick(item) }
                        )
                    } else {
                        HistoryListItem(
                            item = item,
                            onContinueClick = { onContinueReading(item) },
                            onRemoveClick = { onRemoveFromHistory(item.novel.url) },
                            onItemClick = { onNovelClick(item) },
                            onLongClick = { onNovelLongClick(item) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}