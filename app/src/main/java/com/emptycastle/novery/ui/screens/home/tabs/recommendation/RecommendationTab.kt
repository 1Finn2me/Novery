package com.emptycastle.novery.ui.screens.home.tabs.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.recommendation.model.Recommendation
import com.emptycastle.novery.recommendation.model.RecommendationType
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.EmptyRecommendations
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.NovelActionMenu
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.ProfileHeader
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.RecommendationSection
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.RecommendationSettingsSheet
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.SourceRecommendationsSection
import com.emptycastle.novery.ui.screens.home.tabs.recommendation.components.TagFilterSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationTab(
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit = { _, _ -> },
    onNavigateToBrowse: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {}
) {
    val viewModel: RecommendationViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val tagFilters by viewModel.tagFilters.collectAsState()
    val hiddenNovels by viewModel.hiddenNovels.collectAsState()
    val blockedAuthors by viewModel.blockedAuthors.collectAsState()
    val favoriteAuthors by viewModel.favoriteAuthors.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var selectedRecommendation by remember { mutableStateOf<Recommendation?>(null) }
    var lastHiddenNovel by remember { mutableStateOf<Pair<String, String>?>(null) }

    val hasSettingsChanges = hiddenNovels.isNotEmpty() ||
            blockedAuthors.isNotEmpty() ||
            uiState.showCrossProvider

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isSeeding -> {
                    SeedingScreen(
                        progress = uiState.seedingProgress,
                        modifier = Modifier.padding(top = statusBarPadding.calculateTopPadding())
                    )
                }

                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "Loading recommendations...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                !uiState.hasRecommendations && !uiState.hasLibrarySources -> {
                    EmptyRecommendations(
                        profileMaturity = uiState.profileMaturity,
                        novelsInProfile = uiState.novelsInProfile,
                        poolSize = uiState.poolSize,
                        onBrowseClick = onNavigateToBrowse,
                        modifier = Modifier.padding(top = statusBarPadding.calculateTopPadding())
                    )
                }

                else -> {
                    // Filter out BECAUSE_YOU_READ from regular groups since we handle it separately
                    val regularGroups = remember(uiState.recommendationGroups) {
                        uiState.recommendationGroups
                            .filter { it.type != RecommendationType.BECAUSE_YOU_READ }
                            .distinctBy { it.type }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = statusBarPadding.calculateTopPadding(),
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Profile Header
                        item(key = "header") {
                            ProfileHeader(
                                profileMaturity = uiState.profileMaturity,
                                topPreferences = uiState.topPreferences,
                                onFilterClick = { showFilterSheet = true },
                                onSettingsClick = { showSettingsSheet = true },
                                hasActiveFilters = tagFilters.isNotEmpty(),
                                filterCount = tagFilters.size,
                                settingsIndicator = hasSettingsChanges,
                                favoriteAuthors = favoriteAuthors
                            )
                        }

                        // === Library-Based "Because You Read" Section ===
                        if (uiState.hasLibrarySources) {
                            item(key = "source_recommendations") {
                                SourceRecommendationsSection(
                                    selectedSource = uiState.selectedSourceNovel,
                                    otherSources = uiState.otherSourceNovels,
                                    recommendations = uiState.sourceNovelRecommendations,
                                    isExpanded = uiState.isSourceSelectorExpanded,
                                    isLoading = uiState.isLoadingSourceRecommendations,
                                    onToggleExpanded = { viewModel.toggleSourceSelector() },
                                    onSelectSource = { source -> viewModel.selectSourceNovel(source) },
                                    onNovelClick = { novelUrl, providerName ->
                                        viewModel.onRecommendationClicked(novelUrl)
                                        onNavigateToDetails(novelUrl, providerName)
                                    },
                                    onNovelLongClick = { recommendation ->
                                        selectedRecommendation = recommendation
                                    },
                                    onQuickDismiss = { recommendation ->
                                        lastHiddenNovel = recommendation.novel.url to recommendation.novel.name
                                        viewModel.hideNovel(recommendation.novel.url, recommendation.novel.name)

                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Hidden: ${recommendation.novel.name}",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                lastHiddenNovel?.let { (url, _) ->
                                                    viewModel.unhideNovel(url)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Regular recommendation sections
                        itemsIndexed(
                            items = regularGroups,
                            key = { index, group -> "group_${group.type.name}_$index" }
                        ) { _, group ->
                            RecommendationSection(
                                group = group,
                                onNovelClick = { novelUrl, providerName ->
                                    viewModel.onRecommendationClicked(novelUrl)
                                    onNavigateToDetails(novelUrl, providerName)
                                },
                                onNovelLongClick = { recommendation ->
                                    selectedRecommendation = recommendation
                                },
                                onQuickDismiss = { recommendation ->
                                    lastHiddenNovel = recommendation.novel.url to recommendation.novel.name
                                    viewModel.hideNovel(recommendation.novel.url, recommendation.novel.name)

                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Hidden: ${recommendation.novel.name}",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            lastHiddenNovel?.let { (url, _) ->
                                                viewModel.unhideNovel(url)
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Footer
                        item(key = "footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Text(
                                        text = "${uiState.totalRecommendations} recommendations • ${uiState.poolSize} novels indexed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Tag Filter Sheet
    if (showFilterSheet) {
        TagFilterSheet(
            tagFilters = tagFilters,
            onSetFilter = { tag, type -> viewModel.setTagFilter(tag, type) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Settings Sheet
    if (showSettingsSheet) {
        RecommendationSettingsSheet(
            showCrossProvider = uiState.showCrossProvider,
            onCrossProviderChange = { viewModel.toggleCrossProvider() },
            hiddenNovels = hiddenNovels,
            blockedAuthors = blockedAuthors,
            onUnhideNovel = { viewModel.unhideNovel(it) },
            onUnblockAuthor = { viewModel.unblockAuthor(it) },
            onClearAllHidden = { viewModel.clearAllHiddenNovels() },
            onClearAllBlocked = { viewModel.clearAllBlockedAuthors() },
            onResetPreferences = onNavigateToOnboarding,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Novel Action Menu
    selectedRecommendation?.let { recommendation ->
        var authorInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

        LaunchedEffect(recommendation.novel.url) {
            authorInfo = viewModel.getAuthorForNovel(recommendation.novel.url)
        }

        NovelActionMenu(
            recommendation = recommendation,
            onDismiss = { selectedRecommendation = null },
            onNotInterested = {
                lastHiddenNovel = recommendation.novel.url to recommendation.novel.name
                viewModel.hideNovel(recommendation.novel.url, recommendation.novel.name)

                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Hidden: ${recommendation.novel.name}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        lastHiddenNovel?.let { (url, _) ->
                            viewModel.unhideNovel(url)
                        }
                    }
                }
            },
            onBlockAuthor = authorInfo?.let { (normalized, display) ->
                {
                    viewModel.blockAuthor(normalized, display)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Blocked author: $display",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onViewDetails = {
                onNavigateToDetails(recommendation.novel.url, recommendation.novel.apiName)
            }
        )
    }
}

@Composable
private fun SeedingScreen(
    progress: SeedingProgress?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp,
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Building Your Recommendations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            progress?.let { p ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Discovering novels from ${p.currentProvider}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { p.currentIndex.toFloat() / p.totalProviders.coerceAtLeast(1) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Text(
                                text = "${p.currentIndex} of ${p.totalProviders} sources",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This only happens once. We're indexing novels from all your sources to find the best recommendations for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
            )
        }
    }
}