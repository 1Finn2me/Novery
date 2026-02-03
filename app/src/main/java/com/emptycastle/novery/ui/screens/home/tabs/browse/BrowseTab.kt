// BrowseTab.kt
package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.remote.CloudflareManager
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.components.NovelActionSheet
import com.emptycastle.novery.ui.components.NovelCard
import com.emptycastle.novery.ui.components.NoverySearchBar
import com.emptycastle.novery.ui.components.SearchSuggestionsDropdown
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.calculateGridColumns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// Shimmer Effect Extension
// ============================================================================

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
    )

    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnimation - 500f, 0f),
                end = Offset(translateAnimation, 0f)
            ),
            blendMode = BlendMode.SrcAtop
        )
    }
}

// ============================================================================
// Provider Colors
// ============================================================================

private object ProviderColors {
    private val colorPalette = listOf(
        Color(0xFF6366F1) to Color(0xFF818CF8),
        Color(0xFF8B5CF6) to Color(0xFFA78BFA),
        Color(0xFFEC4899) to Color(0xFFF472B6),
        Color(0xFFF43F5E) to Color(0xFFFB7185),
        Color(0xFFF97316) to Color(0xFFFB923C),
        Color(0xFF10B981) to Color(0xFF34D399),
        Color(0xFF14B8A6) to Color(0xFF2DD4BF),
        Color(0xFF06B6D4) to Color(0xFF22D3EE),
        Color(0xFF3B82F6) to Color(0xFF60A5FA),
        Color(0xFF8B5CF6) to Color(0xFFC084FC),
    )

    fun getColors(providerName: String): Pair<Color, Color> {
        val index = kotlin.math.abs(providerName.hashCode()) % colorPalette.size
        return colorPalette[index]
    }

    fun getColor(providerName: String): Color = getColors(providerName).first
}

// ============================================================================
// Main Browse Tab
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseTab(
    appSettings: AppSettings,
    onNavigateToProvider: (providerName: String) -> Unit,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSheetState by viewModel.actionSheetState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dimensions = NoveryTheme.dimensions
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val gridColumns = calculateGridColumns(appSettings.searchGridColumns)
    val resultsPerProvider = appSettings.searchResultsPerProvider

    // Action Sheet
    if (actionSheetState.isVisible && actionSheetState.data != null) {
        val data = actionSheetState.data!!

        NovelActionSheet(
            data = data,
            sheetState = sheetState,
            onDismiss = { viewModel.hideActionSheet() },
            onViewDetails = {
                viewModel.hideActionSheet()
                onNavigateToDetails(data.novel.url, data.novel.apiName)
            },
            onContinueReading = {
                viewModel.hideActionSheet()
                scope.launch {
                    val chapter = viewModel.getContinueReadingChapter(data.novel.url)
                    if (chapter != null) {
                        val (chapterUrl, novelUrl, providerName) = chapter
                        onNavigateToReader(chapterUrl, novelUrl, providerName)
                    } else {
                        onNavigateToDetails(data.novel.url, data.novel.apiName)
                    }
                }
            },
            onAddToLibrary = if (!data.isInLibrary) {
                { viewModel.addToLibrary(data.novel) }
            } else null,
            onRemoveFromLibrary = if (data.isInLibrary) {
                { viewModel.removeFromLibrary(data.novel.url) }
            } else null,
            onStatusChange = { status -> viewModel.updateReadingStatus(status) },
            onRemoveFromHistory = null
        )
    }

    // Filter Bottom Sheet
    if (uiState.showFilters) {
        SearchFiltersSheet(
            filters = uiState.filters,
            availableProviders = uiState.searchResults.keys.toList(),
            onDismiss = { viewModel.toggleFiltersPanel() },
            onFiltersChanged = { viewModel.updateFilters(it) },
            onClearFilters = { viewModel.clearFilters() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Search Bar
        NoverySearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onSearch = viewModel::search,
            isLoading = uiState.isSearching,
            placeholder = "Search across all sources...",
            modifier = Modifier.padding(dimensions.gridPadding),
            showBackButton = uiState.isInSearchMode || uiState.expandedProvider != null,
            onBackClick = {
                when {
                    uiState.expandedProvider != null -> viewModel.expandProvider(null)
                    uiState.isInSearchMode -> viewModel.clearSearch()
                }
            },
            onFocusChanged = { focused ->
                if (focused) viewModel.onSearchBarFocused()
                else viewModel.onSearchBarUnfocused()
            },
            showFilterButton = uiState.isInSearchMode && uiState.searchResults.isNotEmpty(),
            hasActiveFilters = uiState.filters.selectedProviders.isNotEmpty() ||
                    uiState.filters.sortOrder != SearchSortOrder.RELEVANCE,
            onFilterClick = { viewModel.toggleFiltersPanel() }
        )

        // Content area with search suggestions overlay
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            AnimatedContent(
                targetState = Triple(
                    uiState.isInSearchMode,
                    uiState.expandedProvider,
                    uiState.isSearching
                ),
                transitionSpec = {
                    when {
                        targetState.second != null && initialState.second == null -> {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        }
                        targetState.second == null && initialState.second != null -> {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                        else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "browse_content"
            ) { (isSearchMode, expandedProvider, isSearching) ->
                when {
                    isSearching -> {
                        SearchLoadingState()
                    }

                    uiState.isSearchEmpty -> {
                        EmptySearchState(
                            query = uiState.searchQuery,
                            suggestions = uiState.trendingSearches.take(3),
                            onSuggestionClick = { viewModel.search(it) }
                        )
                    }

                    expandedProvider != null && uiState.filteredSearchResults.containsKey(expandedProvider) -> {
                        ExpandedSearchResults(
                            providerName = expandedProvider,
                            novels = uiState.filteredSearchResults[expandedProvider] ?: emptyList(),
                            gridColumns = gridColumns,
                            onNovelClick = { novel ->
                                onNavigateToDetails(novel.url, novel.apiName)
                            },
                            onNovelLongClick = { novel ->
                                viewModel.showActionSheet(novel)
                            },
                            onBack = { viewModel.expandProvider(null) },
                            appSettings = appSettings
                        )
                    }

                    isSearchMode -> {
                        SearchResultsContent(
                            results = uiState.filteredSearchResults,
                            resultsPerProvider = resultsPerProvider,
                            filters = uiState.filters,
                            onNovelClick = { novel ->
                                onNavigateToDetails(novel.url, novel.apiName)
                            },
                            onNovelLongClick = { novel ->
                                viewModel.showActionSheet(novel)
                            },
                            onShowMore = { providerName ->
                                viewModel.expandProvider(providerName)
                            },
                            onClearSearch = { viewModel.clearSearch() },
                            appSettings = appSettings
                        )
                    }

                    else -> {
                        when {
                            uiState.isLoadingProviders -> ProviderGridSkeleton()
                            uiState.providerError != null -> ProviderErrorState(
                                message = uiState.providerError!!,
                                onRetry = { viewModel.retryLoadProviders() }
                            )
                            uiState.providers.isEmpty() -> ProviderEmptyState()
                            else -> ProviderGrid(
                                providers = uiState.providers,
                                favoriteProviders = uiState.favoriteProviders,
                                onProviderClick = onNavigateToProvider,
                                onToggleFavorite = { viewModel.toggleFavoriteProvider(it) },
                                onRefresh = { viewModel.retryLoadProviders() }
                            )
                        }
                    }
                }
            }

            // Search suggestions dropdown overlay
            // Use the standalone AnimatedVisibility (not ColumnScope version)
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.showSearchHistory &&
                        (uiState.searchHistory.isNotEmpty() || uiState.trendingSearches.isNotEmpty()),
                enter = fadeIn() + slideInVertically { -it / 4 },
                exit = fadeOut() + slideOutVertically { -it / 4 },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SearchSuggestionsDropdown(
                    searchHistory = uiState.searchHistory,
                    trendingSearches = uiState.trendingSearches,
                    onHistoryItemClick = { query ->
                        viewModel.search(query)
                    },
                    onRemoveHistoryItem = { query ->
                        viewModel.removeFromSearchHistory(query)
                    },
                    onClearHistory = { viewModel.clearSearchHistory() },
                    onTrendingClick = { query ->
                        viewModel.search(query)
                    },
                    modifier = Modifier
                        .padding(horizontal = dimensions.gridPadding)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// Search Loading State
// ============================================================================

@Composable
private fun SearchLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "search_loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Searching all sources...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This may take a moment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// Empty Search State with Suggestions
// ============================================================================

@Composable
private fun EmptySearchState(
    query: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(100.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "We couldn't find any novels matching \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (suggestions.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Try searching for:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { onSuggestionClick(suggestion) },
                                label = {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Search Filters Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFiltersSheet(
    filters: SearchFilters,
    availableProviders: List<String>,
    onDismiss: () -> Unit,
    onFiltersChanged: (SearchFilters) -> Unit,
    onClearFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onClearFilters) {
                    Text("Reset")
                }
            }

            // Sort Order
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchSortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = filters.sortOrder == order,
                            onClick = {
                                onFiltersChanged(filters.copy(sortOrder = order))
                            },
                            label = { Text(order.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Provider Filter
            if (availableProviders.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sources",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (filters.selectedProviders.isNotEmpty()) {
                            Text(
                                text = "${filters.selectedProviders.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableProviders.forEach { provider ->
                            val isSelected = provider in filters.selectedProviders
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newSelection = if (isSelected) {
                                        filters.selectedProviders - provider
                                    } else {
                                        filters.selectedProviders + provider
                                    }
                                    onFiltersChanged(filters.copy(selectedProviders = newSelection))
                                },
                                label = { Text(provider) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Apply button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================================
// Search Results Content
// ============================================================================

@Composable
private fun SearchResultsContent(
    results: Map<String, List<Novel>>,
    resultsPerProvider: Int,
    filters: SearchFilters,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    onShowMore: (String) -> Unit,
    onClearSearch: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = dimensions.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXl)
    ) {
        item(key = "search_header") {
            SearchResultsHeader(
                totalResults = results.values.sumOf { it.size },
                providerCount = results.size,
                hasFilters = filters.selectedProviders.isNotEmpty() ||
                        filters.sortOrder != SearchSortOrder.RELEVANCE,
                onClear = onClearSearch
            )
        }

        results.forEach { (providerName, novels) ->
            item(key = "provider_$providerName") {
                ProviderSearchResultsSection(
                    providerName = providerName,
                    novels = novels,
                    maxResults = resultsPerProvider,
                    onNovelClick = onNovelClick,
                    onNovelLongClick = onNovelLongClick,
                    onShowMore = { onShowMore(providerName) },
                    appSettings = appSettings
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsHeader(
    totalResults: Int,
    providerCount: Int,
    hasFilters: Boolean,
    onClear: () -> Unit
) {
    val dimensions = NoveryTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.gridPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (hasFilters) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Filtered",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "$totalResults novels from $providerCount sources",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            onClick = onClear,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSearchResultsSection(
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
    val providerColor = remember(providerName) { ProviderColors.getColor(providerName) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = if (hasMore) onShowMore else ({}),
            enabled = hasMore,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.gridPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(providerColor, CircleShape)
                    )

                    Text(
                        text = providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "${novels.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasMore) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                    modifier = Modifier.width(110.dp)
                )
            }

            if (hasMore) {
                item {
                    ViewMoreCard(
                        remainingCount = novels.size - maxResults,
                        color = providerColor,
                        onClick = onShowMore
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewMoreCard(
    remainingCount: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(110.dp)
            .height(165.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = color
                        )
                    }
                }

                Text(
                    text = "+$remainingCount more",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
}

// ============================================================================
// Expanded Search Results
// ============================================================================

@Composable
private fun ExpandedSearchResults(
    providerName: String,
    novels: List<Novel>,
    gridColumns: Int,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    onBack: () -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions
    val providerColor = remember(providerName) { ProviderColors.getColor(providerName) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.gridPadding,
                    vertical = dimensions.spacingMd
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(providerColor, CircleShape)
            )

            Text(
                text = providerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = providerColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, providerColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "${novels.size} results",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = providerColor
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensions.gridPadding,
                end = dimensions.gridPadding,
                bottom = 80.dp
            ),
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
// Provider Grid
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderGrid(
    providers: List<MainProvider>,
    favoriteProviders: Set<String>,
    onProviderClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationStarted = true
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                onRefresh()
                delay(1000)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseHeader(
                    providerCount = providers.size,
                    favoriteCount = favoriteProviders.size
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Source,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All Sources",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            itemsIndexed(
                items = providers,
                key = { _, provider -> provider.name }
            ) { index, provider ->
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (animationStarted) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = index * 40,
                        easing = EaseOutCubic
                    ),
                    label = "card_alpha"
                )

                val animatedOffset by animateFloatAsState(
                    targetValue = if (animationStarted) 0f else 24f,
                    animationSpec = tween(
                        durationMillis = 400,
                        delayMillis = index * 40,
                        easing = EaseOutCubic
                    ),
                    label = "card_offset"
                )

                ProviderCard(
                    provider = provider,
                    isFavorite = provider.name in favoriteProviders,
                    onClick = { onProviderClick(provider.name) },
                    onFavoriteClick = { onToggleFavorite(provider.name) },
                    modifier = Modifier.graphicsLayer {
                        alpha = animatedAlpha
                        translationY = animatedOffset
                    }
                )
            }
        }
    }
}

@Composable
private fun BrowseHeader(
    providerCount: Int,
    favoriteCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Browse",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Discover novels from your sources",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    value = "$providerCount",
                    label = "sources",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.secondary
                )

                if (favoriteCount > 0) {
                    StatBadge(
                        value = "$favoriteCount",
                        label = "favorites",
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    value: String,
    label: String,
    color: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderCard(
    provider: MainProvider,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryColor, secondaryColor) = remember(provider.name) {
        ProviderColors.getColors(provider.name)
    }

    val haptic = LocalHapticFeedback.current

    val cookieStateVersion by CloudflareManager.cookieStateChanged.collectAsState()
    val cookieStatus = remember(cookieStateVersion, provider.mainUrl) {
        CloudflareManager.getCookieStatus(provider.mainUrl)
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.12f),
                                secondaryColor.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 70.dp, y = (-25).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ProviderIcon(
                        name = provider.name,
                        color = primaryColor,
                        iconRes = provider.iconRes
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onFavoriteClick()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                modifier = Modifier.size(18.dp),
                                tint = if (isFavorite) {
                                    Color(0xFFE91E63)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }

                        if (cookieStatus != CloudflareManager.CookieStatus.NONE) {
                            CookieStatusBadge(status = cookieStatus)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProviderStatChip(
                            icon = Icons.Rounded.Category,
                            text = "${provider.tags.size}",
                            label = "genres",
                            color = primaryColor
                        )

                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open",
                                    modifier = Modifier.size(16.dp),
                                    tint = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookieStatusBadge(status: CloudflareManager.CookieStatus) {
    val (color, icon, label) = when (status) {
        CloudflareManager.CookieStatus.VALID -> Triple(
            Color(0xFF10B981),
            Icons.Rounded.VerifiedUser,
            "Active"
        )
        CloudflareManager.CookieStatus.EXPIRED -> Triple(
            Color(0xFFF59E0B),
            Icons.Rounded.Warning,
            "Expired"
        )
        CloudflareManager.CookieStatus.NONE -> return
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                color = color
            )
        }
    }
}

@Composable
private fun ProviderIcon(
    name: String,
    color: Color,
    @DrawableRes iconRes: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (iconRes != null) MaterialTheme.colorScheme.surfaceContainerHigh else color,
        modifier = Modifier.size(52.dp),
        shadowElevation = if (iconRes != null) 1.dp else 3.dp,
        border = if (iconRes != null) {
            BorderStroke(1.dp, color.copy(alpha = 0.2f))
        } else null
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "$name icon",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            val initials = remember(name) {
                name.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifEmpty { name.take(2).uppercase() }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ProviderStatChip(
    icon: ImageVector,
    text: String,
    label: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = color
                )
            }
        }
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 9.sp,
                lineHeight = 10.sp
            )
        }
    }
}

// ============================================================================
// Skeleton and State Composables
// ============================================================================

@Composable
private fun ProviderGridSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmerEffect()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        items(4) { ProviderCardSkeleton() }
    }
}

@Composable
private fun ProviderCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .shimmerEffect()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .shimmerEffect()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try Again",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Source,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No Sources Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "There are no novel sources configured yet. Check back later for updates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}