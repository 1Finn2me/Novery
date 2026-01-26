// ProviderBrowseScreen.kt - Fixed Design Flow
package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sort
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.components.NovelActionSheet
import com.emptycastle.novery.ui.components.NovelCard
import com.emptycastle.novery.ui.components.NovelGridSkeleton
import com.emptycastle.novery.ui.components.NoveryPullToRefreshBox
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.calculateGridColumns
import kotlinx.coroutines.launch

// ============================================================================
// Design Constants
// ============================================================================

private object BrowseDesign {
    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusXl = 20.dp

    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 16.dp
    val spacingXl = 20.dp
    val spacingXxl = 24.dp

    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val iconXl = 40.dp

    val buttonHeight = 44.dp
    val chipHeight = 36.dp
    val searchBarHeight = 48.dp
    val paginationButtonSize = 44.dp
}

// ============================================================================
// Main Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderBrowseScreen(
    providerName: String,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToWebView: (providerName: String, initialUrl: String?) -> Unit,
    viewModel: ProviderBrowseViewModel = viewModel(
        factory = ProviderBrowseViewModel.Factory(providerName)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSheetState by viewModel.actionSheetState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val dimensions = NoveryTheme.dimensions
    var showFilters by remember { mutableStateOf(false) }
    val gridColumns = calculateGridColumns(appSettings.browseGridColumns)

    // Auto-expand filters when empty with active filters (help user find the issue)
    LaunchedEffect(uiState.isEmpty, uiState.hasActiveFilters) {
        if (uiState.isEmpty && uiState.hasActiveFilters && !uiState.isSearchMode) {
            showFilters = true
        }
    }

    // Collapse filters when entering search mode
    LaunchedEffect(uiState.isSearchMode) {
        if (uiState.isSearchMode) showFilters = false
    }

    // Action Sheet
    if (actionSheetState.isVisible && actionSheetState.data != null) {
        val data = actionSheetState.data!!

        NovelActionSheet(
            data = data,
            sheetState = sheetState,
            onDismiss = { viewModel.hideActionSheet() },
            onViewDetails = {
                viewModel.hideActionSheet()
                onNavigateToDetails(data.novel.url, providerName)
            },
            onContinueReading = {
                viewModel.hideActionSheet()
                val position = viewModel.getReadingPosition(data.novel.url)
                if (position != null) {
                    onNavigateToReader(position.chapterUrl, data.novel.url, providerName)
                } else {
                    scope.launch {
                        val history = viewModel.getHistoryChapter(data.novel.url)
                        if (history != null) {
                            onNavigateToReader(history.first, data.novel.url, providerName)
                        } else {
                            onNavigateToDetails(data.novel.url, providerName)
                        }
                    }
                }
            },
            onAddToLibrary = { viewModel.addToLibrary(data.novel) }.takeIf { !data.isInLibrary },
            onRemoveFromLibrary = { viewModel.removeFromLibrary(data.novel.url) }.takeIf { data.isInLibrary },
            onRemoveFromHistory = null
        )
    }

    Scaffold(
        topBar = {
            ProviderTopBar(
                provider = uiState.provider,
                searchQuery = uiState.searchQuery,
                isSearchMode = uiState.isSearchMode,
                isSearching = uiState.isSearching,
                onBack = onBack,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::performSearch,
                onClearSearch = viewModel::clearSearch,
                onOpenWebView = {
                    onNavigateToWebView(providerName, uiState.providerUrl)
                }
            )
        }
    ) { padding ->
        NoveryPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Error state (not in search mode)
                    uiState.displayError != null && !uiState.isSearchMode -> {
                        ErrorState(
                            uiState = uiState,
                            showFilters = showFilters,
                            onToggleFilters = { showFilters = !showFilters },
                            onSortChange = viewModel::setSelectedSort,
                            onTagChange = viewModel::setSelectedTag,
                            onClearFilters = viewModel::clearFilters,
                            onRetry = viewModel::loadPage,
                            onOpenWebView = {
                                onNavigateToWebView(providerName, uiState.providerUrl)
                            }
                        )
                    }

                    // Loading state (initial load, not refresh)
                    uiState.isDisplayLoading -> {
                        LoadingContent(
                            uiState = uiState,
                            showFilters = showFilters,
                            onToggleFilters = { showFilters = !showFilters },
                            onSortChange = viewModel::setSelectedSort,
                            onTagChange = viewModel::setSelectedTag,
                            onClearFilters = viewModel::clearFilters,
                            gridColumns = gridColumns
                        )
                    }

                    // Empty state
                    uiState.isEmpty -> {
                        EmptyContent(
                            uiState = uiState,
                            showFilters = showFilters,
                            onToggleFilters = { showFilters = !showFilters },
                            onSortChange = viewModel::setSelectedSort,
                            onTagChange = viewModel::setSelectedTag,
                            onClearFilters = viewModel::clearFilters,
                            onRetry = viewModel::loadPage,
                            onOpenWebView = {
                                onNavigateToWebView(providerName, uiState.providerUrl)
                            }
                        )
                    }

                    // Content loaded
                    else -> {
                        MainContent(
                            uiState = uiState,
                            gridColumns = gridColumns,
                            showFilters = showFilters,
                            onToggleFilters = { showFilters = !showFilters },
                            onSortChange = viewModel::setSelectedSort,
                            onTagChange = viewModel::setSelectedTag,
                            onClearFilters = viewModel::clearFilters,
                            onNovelClick = { novel ->
                                onNavigateToDetails(novel.url, providerName)
                            },
                            onNovelLongClick = { novel ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.showActionSheet(novel)
                            },
                            appSettings = appSettings
                        )
                    }
                }

                // Floating Pagination Bar
                AnimatedVisibility(
                    visible = uiState.showPagination,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BrowseDesign.spacingXl),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    PaginationBar(
                        currentPage = uiState.currentPage,
                        onPrevious = viewModel::previousPage,
                        onNext = viewModel::nextPage,
                        hasPrevious = uiState.hasPreviousPage,
                        isLoading = uiState.isLoading
                    )
                }

                // Floating Search Results Indicator
                AnimatedVisibility(
                    visible = uiState.showSearchIndicator,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BrowseDesign.spacingXl),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    SearchResultsIndicator(
                        resultCount = uiState.searchResults.size,
                        query = uiState.searchQuery,
                        onClear = viewModel::clearSearch
                    )
                }
            }
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@Composable
private fun ProviderTopBar(
    provider: MainProvider?,
    searchQuery: String,
    isSearchMode: Boolean,
    isSearching: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onOpenWebView: () -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchMode) {
        if (!isSearchMode) isSearchExpanded = false
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSearchExpanded) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = BrowseDesign.spacingSm, vertical = BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
        ) {
            IconButton(
                onClick = {
                    if (isSearchExpanded) {
                        onClearSearch()
                        isSearchExpanded = false
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription = if (isSearchExpanded) "Close search" else "Go back"
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }

            AnimatedContent(
                targetState = isSearchExpanded,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.95f))
                },
                modifier = Modifier.weight(1f),
                label = "top_bar_content"
            ) { expanded ->
                if (expanded) {
                    SearchField(
                        query = searchQuery,
                        isLoading = isSearching,
                        onQueryChange = onSearchQueryChange,
                        onSearch = {
                            onSearch()
                            keyboardController?.hide()
                        },
                        onClear = { onSearchQueryChange("") },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                } else {
                    ProviderTitle(
                        provider = provider,
                        isSearchMode = isSearchMode
                    )
                }
            }

            if (!isSearchExpanded) {
                IconButton(
                    onClick = { isSearchExpanded = true },
                    modifier = Modifier.semantics {
                        contentDescription = "Search novels"
                    }
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = if (isSearchMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (isSearchMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenWebView,
                    modifier = Modifier.semantics {
                        contentDescription = "Open in browser"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderTitle(
    provider: MainProvider?,
    isSearchMode: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        provider?.iconRes?.let { iconRes ->
            Surface(
                shape = RoundedCornerShape(BrowseDesign.radiusMd),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(36.dp)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Column {
            Text(
                text = provider?.name ?: "Browse",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(visible = isSearchMode) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BrowseDesign.radiusLg),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowseDesign.searchBarHeight)
                .padding(horizontal = BrowseDesign.spacingLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "search_icon"
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(BrowseDesign.iconMd),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconMd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search novels...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// Filter Bar & Panel - Reusable Component
// ============================================================================

@Composable
private fun FilterBarWithPanel(
    uiState: ProviderBrowseUiState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions

    Column(modifier = modifier) {
        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding, vertical = BrowseDesign.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active filters summary
            AnimatedVisibility(
                visible = uiState.hasActiveFilters && !showFilters,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ActiveFiltersSummary(
                    provider = uiState.provider,
                    selectedSort = uiState.selectedSort,
                    selectedTag = uiState.selectedTag
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            FilterToggleButton(
                isOpen = showFilters,
                hasActiveFilters = uiState.hasActiveFilters,
                onClick = onToggleFilters
            )
        }

        // Expandable Filters Panel
        AnimatedVisibility(
            visible = showFilters && uiState.provider != null,
            enter = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            FiltersPanel(
                provider = uiState.provider!!,
                selectedSort = uiState.selectedSort,
                selectedTag = uiState.selectedTag,
                onSortChange = onSortChange,
                onTagChange = onTagChange,
                onClearFilters = onClearFilters
            )
        }
    }
}

@Composable
private fun ActiveFiltersSummary(
    provider: MainProvider?,
    selectedSort: String?,
    selectedTag: String?
) {
    val sortLabel = provider?.orderBys?.find { it.value == selectedSort }?.label
    val tagLabel = provider?.tags?.find { it.value == selectedTag }?.label

    val defaultSort = provider?.orderBys?.firstOrNull()?.value
    val defaultTag = provider?.tags?.firstOrNull()?.value

    Row(
        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.FilterList,
            contentDescription = null,
            modifier = Modifier.size(BrowseDesign.iconSm),
            tint = MaterialTheme.colorScheme.primary
        )

        if (sortLabel != null && selectedSort != defaultSort) {
            ActiveFilterTag(text = sortLabel)
        }
        if (tagLabel != null && selectedTag != defaultTag) {
            ActiveFilterTag(text = tagLabel)
        }
    }
}

@Composable
private fun ActiveFilterTag(text: String) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusSm),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingSm, vertical = BrowseDesign.spacingXs)
        )
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filter_rotation"
    )

    val color = when {
        isOpen -> MaterialTheme.colorScheme.primary
        hasActiveFilters -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bgColor = when {
        isOpen -> MaterialTheme.colorScheme.primaryContainer
        hasActiveFilters -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = bgColor,
        modifier = Modifier.height(BrowseDesign.buttonHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(BrowseDesign.iconMd),
                tint = color
            )

            Text(
                text = "Filters",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )

            if (hasActiveFilters && !isOpen) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                )
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(BrowseDesign.iconMd)
                    .graphicsLayer { rotationZ = rotation },
                tint = color
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersPanel(
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
            .padding(bottom = BrowseDesign.spacingMd),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(BrowseDesign.radiusXl),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(BrowseDesign.spacingLg),
            verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
        ) {
            FilterPanelHeader(onClearFilters = onClearFilters)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sort Section
            FilterSection(
                title = "Sort By",
                icon = Icons.Rounded.Sort,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                ) {
                    provider.orderBys.forEach { option ->
                        FilterChip(
                            text = option.label,
                            selected = selectedSort == option.value,
                            onClick = { onSortChange(option.value) }
                        )
                    }
                }
            }

            if (provider.tags.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                FilterSection(
                    title = "Genre",
                    icon = Icons.Rounded.Category,
                    iconTint = MaterialTheme.colorScheme.tertiary
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                    ) {
                        provider.tags.forEach { tag ->
                            FilterChip(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanelHeader(onClearFilters: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(BrowseDesign.radiusMd),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconMd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize your browse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            onClick = onClearFilters,
            shape = RoundedCornerShape(BrowseDesign.radiusMd),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = BrowseDesign.spacingMd, vertical = BrowseDesign.spacingSm),
                horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(BrowseDesign.iconSm),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(BrowseDesign.iconSm),
                tint = iconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(150),
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "chip_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = backgroundColor,
        modifier = Modifier.height(BrowseDesign.chipHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(BrowseDesign.iconSm),
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

// ============================================================================
// Search Results Indicator
// ============================================================================

@Composable
private fun SearchResultsIndicator(
    resultCount: Int,
    query: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusXl),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = BrowseDesign.spacingLg,
                end = BrowseDesign.spacingSm,
                top = BrowseDesign.spacingMd,
                bottom = BrowseDesign.spacingMd
            ),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(BrowseDesign.iconMd),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "$resultCount result${if (resultCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "\"$query\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    modifier = Modifier.size(BrowseDesign.iconSm),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ============================================================================
// Pagination Bar
// ============================================================================

@Composable
private fun PaginationBar(
    currentPage: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(BrowseDesign.radiusXl),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(BrowseDesign.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaginationButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                onClick = onPrevious,
                enabled = hasPrevious && !isLoading,
                contentDescription = "Previous page"
            )

            PageIndicator(currentPage = currentPage, isLoading = isLoading)

            PaginationButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onNext,
                enabled = !isLoading,
                contentDescription = "Next page"
            )
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, isLoading: Boolean) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusLg),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(horizontal = BrowseDesign.spacingXs)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingLg, vertical = BrowseDesign.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "page_loading"
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Page $currentPage",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(150),
        label = "button_alpha"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(BrowseDesign.paginationButtonSize)
            .graphicsLayer { this.alpha = alpha }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(BrowseDesign.iconLg),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// Content States - ALL with Filter Support
// ============================================================================

@Composable
private fun LoadingContent(
    uiState: ProviderBrowseUiState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    gridColumns: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Show filters even during loading (not in search mode)
        if (uiState.provider != null && !uiState.isSearchMode) {
            FilterBarWithPanel(
                uiState = uiState,
                showFilters = showFilters,
                onToggleFilters = onToggleFilters,
                onSortChange = onSortChange,
                onTagChange = onTagChange,
                onClearFilters = onClearFilters
            )
        }

        if (uiState.isSearching) {
            SearchingHeader(query = uiState.searchQuery)
        }

        NovelGridSkeleton(columns = gridColumns)
    }
}

@Composable
private fun SearchingHeader(query: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BrowseDesign.spacingLg, vertical = BrowseDesign.spacingMd),
        shape = RoundedCornerShape(BrowseDesign.radiusLg),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(BrowseDesign.spacingLg),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(BrowseDesign.iconLg),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = "Searching...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Looking for \"$query\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyContent(
    uiState: ProviderBrowseUiState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit
) {
    // Make the entire column scrollable so pull-to-refresh works
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ALWAYS show filters when not in search mode - this is the key fix!
        if (uiState.provider != null && !uiState.isSearchMode) {
            FilterBarWithPanel(
                uiState = uiState,
                showFilters = showFilters,
                onToggleFilters = onToggleFilters,
                onSortChange = onSortChange,
                onTagChange = onTagChange,
                onClearFilters = onClearFilters
            )
        }

        // Empty state card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(BrowseDesign.spacingXxl),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                isSearchMode = uiState.isSearchMode,
                hasActiveFilters = uiState.hasActiveFilters,
                selectedSortLabel = uiState.selectedSortLabel,
                selectedTagLabel = uiState.selectedTagLabel,
                onClearFilters = onClearFilters,
                onRetry = onRetry,
                onOpenWebView = onOpenWebView
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    isSearchMode: Boolean,
    hasActiveFilters: Boolean,
    selectedSortLabel: String?,
    selectedTagLabel: String?,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(BrowseDesign.radiusXl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(BrowseDesign.spacingXxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = if (hasActiveFilters && !isSearchMode)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = when {
                            isSearchMode -> Icons.Rounded.SearchOff
                            hasActiveFilters -> Icons.Rounded.FilterListOff
                            else -> Icons.Rounded.AutoStories
                        },
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconXl),
                        tint = if (hasActiveFilters && !isSearchMode)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title and description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
            ) {
                Text(
                    text = when {
                        isSearchMode -> "No Results Found"
                        hasActiveFilters -> "No Novels Match Filters"
                        else -> "No Novels Found"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        isSearchMode -> "Try different search terms or browse available content"
                        hasActiveFilters -> "Your current filters didn't return any results. Try adjusting them or clear all filters."
                        else -> "This source may be temporarily unavailable"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Show active filters when empty due to filters
            if (hasActiveFilters && !isSearchMode) {
                ActiveFiltersInfo(
                    selectedSortLabel = selectedSortLabel,
                    selectedTagLabel = selectedTagLabel
                )
            }

            // Action buttons
            when {
                isSearchMode -> {
                    // No additional buttons for search - user can use the floating indicator
                }

                hasActiveFilters -> {
                    // Primary action: Clear filters
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
                    ) {
                        Button(
                            onClick = onClearFilters,
                            shape = RoundedCornerShape(BrowseDesign.radiusMd),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(
                                horizontal = BrowseDesign.spacingXxl,
                                vertical = BrowseDesign.spacingMd
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterListOff,
                                contentDescription = null,
                                modifier = Modifier.size(BrowseDesign.iconMd)
                            )
                            Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                            Text("Clear All Filters", fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
                        ) {
                            TextButton(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconSm)
                                )
                                Spacer(modifier = Modifier.width(BrowseDesign.spacingXs))
                                Text("Retry")
                            }

                            TextButton(onClick = onOpenWebView) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconSm)
                                )
                                Spacer(modifier = Modifier.width(BrowseDesign.spacingXs))
                                Text("WebView")
                            }
                        }
                    }
                }

                else -> {
                    // No active filters - show retry and webview options
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(BrowseDesign.radiusMd),
                            contentPadding = PaddingValues(
                                horizontal = BrowseDesign.spacingLg,
                                vertical = BrowseDesign.spacingMd
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(BrowseDesign.iconSm)
                            )
                            Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                            Text("Retry")
                        }

                        Button(
                            onClick = onOpenWebView,
                            shape = RoundedCornerShape(BrowseDesign.radiusMd),
                            contentPadding = PaddingValues(
                                horizontal = BrowseDesign.spacingLg,
                                vertical = BrowseDesign.spacingMd
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                modifier = Modifier.size(BrowseDesign.iconSm)
                            )
                            Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                            Text("WebView")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFiltersInfo(
    selectedSortLabel: String?,
    selectedTagLabel: String?
) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(BrowseDesign.spacingMd),
            verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(BrowseDesign.iconSm),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Active Filters:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                modifier = Modifier.padding(start = BrowseDesign.spacingXl)
            ) {
                selectedSortLabel?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(BrowseDesign.radiusSm),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(
                                horizontal = BrowseDesign.spacingSm,
                                vertical = BrowseDesign.spacingXs
                            )
                        )
                    }
                }
                selectedTagLabel?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(BrowseDesign.radiusSm),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(
                                horizontal = BrowseDesign.spacingSm,
                                vertical = BrowseDesign.spacingXs
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    uiState: ProviderBrowseUiState,
    gridColumns: Int,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
    ) {
        if (!uiState.isSearchMode) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "filter_bar_panel") {
                FilterBarWithPanel(
                    uiState = uiState,
                    showFilters = showFilters,
                    onToggleFilters = onToggleFilters,
                    onSortChange = onSortChange,
                    onTagChange = onTagChange,
                    onClearFilters = onClearFilters
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "spacer") {
            Spacer(modifier = Modifier.height(BrowseDesign.spacingSm))
        }

        items(items = uiState.displayNovels, key = { it.url }) { novel ->
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

// ============================================================================
// Error State - Also with Filter Support
// ============================================================================

@Composable
private fun ErrorState(
    uiState: ProviderBrowseUiState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Show filters even in error state
        if (uiState.provider != null && !uiState.isSearchMode) {
            FilterBarWithPanel(
                uiState = uiState,
                showFilters = showFilters,
                onToggleFilters = onToggleFilters,
                onSortChange = onSortChange,
                onTagChange = onTagChange,
                onClearFilters = onClearFilters
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(BrowseDesign.spacingXxl),
            contentAlignment = Alignment.Center
        ) {
            ErrorStateCard(
                message = uiState.displayError ?: "Unknown error",
                isCloudflareError = uiState.isCloudflareError,
                onRetry = onRetry,
                onOpenWebView = onOpenWebView
            )
        }
    }
}

@Composable
private fun ErrorStateCard(
    message: String,
    isCloudflareError: Boolean,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(BrowseDesign.radiusXl),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(BrowseDesign.spacingXxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingLg)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCloudflareError)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isCloudflareError)
                            Icons.Rounded.Security
                        else
                            Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconXl),
                        tint = if (isCloudflareError)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
            ) {
                Text(
                    text = if (isCloudflareError) "Verification Required" else "Connection Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            if (isCloudflareError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
                ) {
                    Button(
                        onClick = onOpenWebView,
                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                        contentPadding = PaddingValues(
                            horizontal = BrowseDesign.spacingXxl,
                            vertical = BrowseDesign.spacingMd
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconMd)
                        )
                        Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                        Text("Verify in Browser", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(onClick = onRetry) {
                        Text("Try Again", fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
                    OutlinedButton(
                        onClick = onOpenWebView,
                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                        contentPadding = PaddingValues(
                            horizontal = BrowseDesign.spacingLg,
                            vertical = BrowseDesign.spacingMd
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconSm)
                        )
                        Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                        Text("WebView")
                    }

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                        contentPadding = PaddingValues(
                            horizontal = BrowseDesign.spacingLg,
                            vertical = BrowseDesign.spacingMd
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconSm)
                        )
                        Spacer(modifier = Modifier.width(BrowseDesign.spacingSm))
                        Text("Retry")
                    }
                }
            }
        }
    }
}