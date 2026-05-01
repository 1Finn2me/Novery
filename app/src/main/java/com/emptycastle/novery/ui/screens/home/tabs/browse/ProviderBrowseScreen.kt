package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.emptycastle.novery.domain.model.ReadingStatus
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
    val radiusXxl = 28.dp

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
    val fabSize = 56.dp
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

    val gridColumns = calculateGridColumns(appSettings.browseGridColumns)

    // Filter overlay state
    var isFilterOverlayOpen by remember { mutableStateOf(false) }

    // Auto-open filter overlay when empty with active filters
    LaunchedEffect(uiState.isEmpty, uiState.hasActiveFilters) {
        if (uiState.isEmpty && uiState.hasActiveFilters && !uiState.isSearchMode) {
            isFilterOverlayOpen = true
        }
    }

    // Close filter overlay when entering search mode
    LaunchedEffect(uiState.isSearchMode) {
        if (uiState.isSearchMode) isFilterOverlayOpen = false
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
            onAddToLibrary = { status: ReadingStatus -> viewModel.addToLibrary(data.novel, status) }
                .takeIf { !data.isInLibrary },
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
                onOpenWebView = { onNavigateToWebView(providerName, uiState.providerUrl) }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        NoveryPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // ── Main Content ──────────────────────────────────────────
                when {
                    uiState.displayError != null && !uiState.isSearchMode -> {
                        ErrorState(
                            uiState = uiState,
                            onRetry = viewModel::loadPage,
                            onOpenWebView = { onNavigateToWebView(providerName, uiState.providerUrl) }
                        )
                    }

                    uiState.isDisplayLoading -> {
                        LoadingContent(uiState = uiState, gridColumns = gridColumns)
                    }

                    uiState.isEmpty -> {
                        EmptyContent(
                            uiState = uiState,
                            onRetry = viewModel::loadPage,
                            onClearFilters = viewModel::clearFilters,
                            onOpenWebView = { onNavigateToWebView(providerName, uiState.providerUrl) }
                        )
                    }

                    else -> {
                        MainContent(
                            uiState = uiState,
                            gridColumns = gridColumns,
                            onNovelClick = { novel -> onNavigateToDetails(novel.url, providerName) },
                            onNovelLongClick = { novel ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.showActionSheet(novel)
                            },
                            appSettings = appSettings
                        )
                    }
                }

                // ── Active Filters Chips (top, below content header) ──────
                AnimatedVisibility(
                    visible = uiState.hasActiveFilters && !uiState.isSearchMode && !isFilterOverlayOpen,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ActiveFiltersChipRow(
                        uiState = uiState,
                        onSortClear = { viewModel.setSelectedSort(uiState.provider?.orderBys?.firstOrNull()?.value) },
                        onTagClear = { viewModel.setSelectedTag(uiState.provider?.tags?.firstOrNull()?.value) },
                        onClearAll = viewModel::clearFilters
                    )
                }

                // ── Floating Pagination Bar ───────────────────────────────
                AnimatedVisibility(
                    visible = uiState.showPagination,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
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

                // ── Floating Search Results Indicator ─────────────────────
                AnimatedVisibility(
                    visible = uiState.showSearchIndicator,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
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

                // ── Filter FAB ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.provider != null && !uiState.isSearchMode,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = BrowseDesign.spacingXl,
                            bottom = if (uiState.showPagination) 84.dp else BrowseDesign.spacingXl
                        ),
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FilterFab(
                        isOpen = isFilterOverlayOpen,
                        hasActiveFilters = uiState.hasActiveFilters,
                        activeFilterCount = uiState.activeFilterCount,
                        onClick = { isFilterOverlayOpen = !isFilterOverlayOpen }
                    )
                }

                // ── Filter Overlay ────────────────────────────────────────
                FilterOverlay(
                    visible = isFilterOverlayOpen,
                    uiState = uiState,
                    onDismiss = { isFilterOverlayOpen = false },
                    onSortChange = { sort ->
                        viewModel.setSelectedSort(sort)
                    },
                    onExtraFilterChange = viewModel::setExtraFilter,
                    onTagChange = { tag ->
                        viewModel.setSelectedTag(tag)
                    },
                    onClearFilters = {
                        viewModel.clearFilters()
                        isFilterOverlayOpen = false
                    },
                    onApply = { isFilterOverlayOpen = false }
                )
            }
        }
    }
}

// ============================================================================
// Filter FAB
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterFab(
    isOpen: Boolean,
    hasActiveFilters: Boolean,
    activeFilterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_rotation"
    )

    val fabColor by animateColorAsState(
        targetValue = when {
            isOpen -> MaterialTheme.colorScheme.primary
            hasActiveFilters -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "fab_color"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isOpen -> MaterialTheme.colorScheme.onPrimary
            hasActiveFilters -> MaterialTheme.colorScheme.onTertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "fab_icon_color"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(BrowseDesign.radiusLg),
            color = fabColor,
            shadowElevation = if (isOpen) 12.dp else 6.dp,
            modifier = Modifier
                .size(BrowseDesign.fabSize)
                .semantics {
                    contentDescription = if (isOpen) "Close filters" else "Open filters"
                    role = Role.Button
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isOpen) Icons.Rounded.Close else Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(BrowseDesign.iconLg)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }

        // Badge
        AnimatedVisibility(
            visible = activeFilterCount > 0 && !isOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = activeFilterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// Filter Overlay (Bottom Sheet Modal)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterOverlay(
    visible: Boolean,
    uiState: ProviderBrowseUiState,
    onDismiss: () -> Unit,
    onSortChange: (String?) -> Unit,
    onTagChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit,
    onExtraFilterChange: (key: String, value: String?) -> Unit = { _, _ -> }
) {
    val provider = uiState.provider ?: return

    // Scrim backdrop
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // Bottom sheet
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(150)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks */ }
                    ),
                shape = RoundedCornerShape(
                    topStart = BrowseDesign.radiusXxl,
                    topEnd = BrowseDesign.radiusXxl
                ),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ═══════════════════════════════════════════════════
                    // Drag Handle Area (Clickable to dismiss)
                    // ═══════════════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(vertical = BrowseDesign.spacingMd),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    // ═══════════════════════════════════════════════════
                    // Header Section
                    // ═══════════════════════════════════════════════════
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = BrowseDesign.spacingXxl,
                                        vertical = BrowseDesign.spacingLg
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Filters",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    AnimatedContent(
                                        targetState = uiState.activeFilterCount,
                                        transitionSpec = {
                                            (slideInVertically { -it } + fadeIn())
                                                .togetherWith(slideOutVertically { it } + fadeOut())
                                        },
                                        label = "filter_count"
                                    ) { count ->
                                        Text(
                                            text = when {
                                                count == 0 -> "No active filters"
                                                count == 1 -> "1 filter applied"
                                                else -> "$count filters applied"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (count > 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Reset button
                                AnimatedVisibility(
                                    visible = uiState.hasActiveFilters,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut()
                                ) {
                                    Surface(
                                        onClick = onClearFilters,
                                        shape = RoundedCornerShape(BrowseDesign.radiusMd),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = BrowseDesign.spacingMd,
                                                vertical = BrowseDesign.spacingSm
                                            ),
                                            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Reset filters",
                                                modifier = Modifier.size(BrowseDesign.iconSm),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "Reset",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // ═══════════════════════════════════════════════════
                    // Scrollable Filter Content - FIXED
                    // ═══════════════════════════════════════════════════
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = BrowseDesign.spacingXxl)
                            .padding(top = BrowseDesign.spacingXl, bottom = BrowseDesign.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXxl)
                    ) {

                        // ─── Sort Section ───────────────────────────────
                        if (provider.orderBys.isNotEmpty()) {
                            OverlayFilterSection(
                                title = "Sort By",
                                icon = Icons.Rounded.Sort,
                                accentColor = MaterialTheme.colorScheme.primary
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                                ) {
                                    provider.orderBys.forEach { option ->
                                        OverlayFilterChip(
                                            text = option.label,
                                            selected = uiState.selectedSort == option.value,
                                            onClick = { onSortChange(option.value) }
                                        )
                                    }
                                }
                            }
                        }

                        // ─── Genre/Tags Section ─────────────────────────
                        if (provider.tags.isNotEmpty()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            OverlayFilterSection(
                                title = "Genres & Tags",
                                icon = Icons.Rounded.Category,
                                accentColor = MaterialTheme.colorScheme.tertiary
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                                ) {
                                    provider.tags.forEach { tag ->
                                        OverlayFilterChip(
                                            text = tag.label,
                                            selected = uiState.selectedTag == tag.value,
                                            onClick = { onTagChange(tag.value) }
                                        )
                                    }
                                }
                            }
                        }

                        // ─── Extra Filter Groups (Provider-specific) ────
                        uiState.provider?.extraFilterGroups?.forEach { group ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            OverlayFilterSection(
                                title = group.label,
                                icon = Icons.Rounded.Tune,
                                accentColor = MaterialTheme.colorScheme.secondary
                            ) {
                                val defaultValue = group.defaultValue ?: group.options.firstOrNull()?.value
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                                    verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm)
                                ) {
                                    group.options.forEach { option ->
                                        val selectedValue = uiState.selectedExtraFilters[group.key] ?: defaultValue
                                        OverlayFilterChip(
                                            text = option.label,
                                            selected = selectedValue == option.value,
                                            onClick = { onExtraFilterChange(group.key, option.value) }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacing for last item
                        Spacer(modifier = Modifier.height(BrowseDesign.spacingXl))
                    }

                    // ═══════════════════════════════════════════════════
                    // Apply Button (Sticky Bottom)
                    // ═══════════════════════════════════════════════════
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Button(
                                onClick = onApply,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(BrowseDesign.spacingXxl)
                                    .height(BrowseDesign.buttonHeight),
                                shape = RoundedCornerShape(BrowseDesign.radiusMd),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconMd)
                                )
                                Spacer(Modifier.width(BrowseDesign.spacingSm))
                                Text(
                                    text = "Apply Filters",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Helper Composables
// ═══════════════════════════════════════════════════════════════════


@Composable
private fun OverlayFilterSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(BrowseDesign.radiusSm),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = accentColor
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "chip_text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BrowseDesign.radiusMd),
        color = bgColor,
        modifier = Modifier.height(BrowseDesign.chipHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BrowseDesign.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally() + fadeIn() + scaleIn(),
                exit = shrinkHorizontally() + fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(BrowseDesign.iconSm),
                    tint = textColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ============================================================================
// Active Filters Chip Row (shown below top bar when filters active)
// ============================================================================

@Composable
private fun ActiveFiltersChipRow(
    uiState: ProviderBrowseUiState,
    onSortClear: () -> Unit,
    onTagClear: () -> Unit,
    onClearAll: () -> Unit
) {
    val provider = uiState.provider ?: return
    val defaultSort = provider.orderBys.firstOrNull()?.value
    val defaultTag = provider.tags.firstOrNull()?.value

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BrowseDesign.spacingLg, vertical = BrowseDesign.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Active:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val sortLabel = uiState.selectedSortLabel
            val tagLabel = uiState.selectedTagLabel

            // Sort chip
            if (uiState.selectedSort != defaultSort && sortLabel != null) {
                item {
                    DismissibleChip(
                        label = sortLabel,
                        color = MaterialTheme.colorScheme.primary,
                        onDismiss = onSortClear
                    )
                }
            }

            // Tag chip
            if (uiState.selectedTag != defaultTag && tagLabel != null) {
                item {
                    DismissibleChip(
                        label = tagLabel,
                        color = MaterialTheme.colorScheme.tertiary,
                        onDismiss = onTagClear
                    )
                }
            }

            // Clear all
            item {
                Surface(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(BrowseDesign.radiusSm),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Clear all",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleChip(
    label: String,
    color: Color,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(BrowseDesign.radiusSm),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(
                start = BrowseDesign.spacingSm,
                end = BrowseDesign.spacingXs,
                top = BrowseDesign.spacingXs,
                bottom = BrowseDesign.spacingXs
            ),
            horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(10.dp),
                        tint = color
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
        if (isSearchExpanded) focusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSearchExpanded) 2.dp else 0.dp,
        shadowElevation = if (isSearchExpanded) 4.dp else 0.dp
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
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
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
                    ProviderTitle(provider = provider, isSearchMode = isSearchMode)
                }
            }

            if (!isSearchExpanded) {
                // Search button with active indicator
                Box {
                    IconButton(
                        onClick = { isSearchExpanded = true },
                        modifier = Modifier.semantics { contentDescription = "Search novels" }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = if (isSearchMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isSearchMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenWebView,
                    modifier = Modifier.semantics { contentDescription = "Open in browser" }
                ) {
                    Icon(imageVector = Icons.Rounded.Language, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ProviderTitle(provider: MainProvider?, isSearchMode: Boolean) {
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
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
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
            modifier = Modifier.padding(
                horizontal = BrowseDesign.spacingLg,
                vertical = BrowseDesign.spacingMd
            ),
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

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn())
                        .togetherWith(slideOutVertically { it } + fadeOut())
                },
                label = "page_number"
            ) { page ->
                Text(
                    text = "Page $page",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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
        targetValue = if (enabled) 1f else 0.35f,
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
// Content States
// ============================================================================

@Composable
private fun LoadingContent(
    uiState: ProviderBrowseUiState,
    gridColumns: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            Surface(
                shape = CircleShape,
                color = when {
                    isSearchMode -> MaterialTheme.colorScheme.surfaceContainerHigh
                    hasActiveFilters -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
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
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                        hasActiveFilters -> "Your current filters returned no results. Try adjusting them."
                        else -> "This source may be temporarily unavailable"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Active filter info
            if (hasActiveFilters && !isSearchMode) {
                Surface(
                    shape = RoundedCornerShape(BrowseDesign.radiusMd),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(BrowseDesign.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconSm),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            selectedSortLabel?.let {
                                Text(
                                    text = "Sort: $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            selectedTagLabel?.let {
                                Text(
                                    text = "Genre: $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            when {
                isSearchMode -> { /* floating indicator handles this */ }

                hasActiveFilters -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)
                    ) {
                        Button(
                            onClick = onClearFilters,
                            shape = RoundedCornerShape(BrowseDesign.radiusMd),
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
                            Spacer(Modifier.width(BrowseDesign.spacingSm))
                            Text("Clear All Filters", fontWeight = FontWeight.SemiBold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
                            TextButton(onClick = onRetry) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconSm)
                                )
                                Spacer(Modifier.width(BrowseDesign.spacingXs))
                                Text("Retry")
                            }
                            TextButton(onClick = onOpenWebView) {
                                Icon(
                                    Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(BrowseDesign.iconSm)
                                )
                                Spacer(Modifier.width(BrowseDesign.spacingXs))
                                Text("WebView")
                            }
                        }
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(BrowseDesign.spacingMd)) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(BrowseDesign.radiusMd),
                            contentPadding = PaddingValues(
                                horizontal = BrowseDesign.spacingLg,
                                vertical = BrowseDesign.spacingMd
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(BrowseDesign.iconSm)
                            )
                            Spacer(Modifier.width(BrowseDesign.spacingSm))
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
                                Icons.Rounded.Language,
                                contentDescription = null,
                                modifier = Modifier.size(BrowseDesign.iconSm)
                            )
                            Spacer(Modifier.width(BrowseDesign.spacingSm))
                            Text("WebView")
                        }
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
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit,
    appSettings: AppSettings
) {
    val dimensions = NoveryTheme.dimensions

    when (appSettings.browseDisplayMode) {
        com.emptycastle.novery.domain.model.DisplayMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                // Extra bottom padding: space for FAB (56dp) + margin (20dp) + pagination (if shown)
                contentPadding = PaddingValues(
                    bottom = if (uiState.showPagination) 180.dp else 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "spacer_top") {
                    Spacer(Modifier.height(BrowseDesign.spacingSm))
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

        com.emptycastle.novery.domain.model.DisplayMode.LIST -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = if (uiState.showPagination) 180.dp else 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
            ) {
                item(key = "spacer_top") {
                    Spacer(Modifier.height(BrowseDesign.spacingSm))
                }
                items(uiState.displayNovels, key = { it.url }) { novel ->
                    com.emptycastle.novery.ui.components.NovelListItem(
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
}

// ============================================================================
// Error State
// ============================================================================

@Composable
private fun ErrorState(
    uiState: ProviderBrowseUiState,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isCloudflareError) Icons.Rounded.Security
                        else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(BrowseDesign.iconXl),
                        tint = if (isCloudflareError) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
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
                            Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconMd)
                        )
                        Spacer(Modifier.width(BrowseDesign.spacingSm))
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
                            Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconSm)
                        )
                        Spacer(Modifier.width(BrowseDesign.spacingSm))
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
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(BrowseDesign.iconSm)
                        )
                        Spacer(Modifier.width(BrowseDesign.spacingSm))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
