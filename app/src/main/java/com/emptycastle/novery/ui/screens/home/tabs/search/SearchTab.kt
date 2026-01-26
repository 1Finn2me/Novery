package com.emptycastle.novery.ui.screens.home.tabs.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.ui.components.EmptySearchResults
import com.emptycastle.novery.ui.components.LoadingIndicator
import com.emptycastle.novery.ui.components.NovelActionSheet
import com.emptycastle.novery.ui.components.NoverySearchBar
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.calculateGridColumns
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSheetState by viewModel.actionSheetState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dimensions = NoveryTheme.dimensions
    val gridColumns = calculateGridColumns(appSettings.searchGridColumns)
    val resultsPerProvider = appSettings.searchResultsPerProvider
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        NoverySearchBar(
            query = uiState.query,
            onQueryChange = viewModel::updateQuery,
            onSearch = viewModel::search,
            isLoading = uiState.isSearching,
            autoFocus = true,
            modifier = Modifier.padding(dimensions.gridPadding)
        )

        AnimatedContent(
            targetState = Triple(uiState.isSearching, uiState.results.isEmpty(), uiState.expandedProvider),
            transitionSpec = {
                if (targetState.third != null && initialState.third == null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else if (targetState.third == null && initialState.third != null) {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "search_content"
        ) { (isSearching, isEmpty, expandedProvider) ->
            when {
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(message = "Searching...")
                    }
                }

                isEmpty && uiState.query.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptySearchResults(query = uiState.query)
                    }
                }

                expandedProvider != null && uiState.results.containsKey(expandedProvider) -> {
                    ExpandedProviderResults(
                        providerName = expandedProvider,
                        novels = uiState.results[expandedProvider] ?: emptyList(),
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

                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = dimensions.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingXl)
                    ) {
                        uiState.results.forEach { (providerName, novels) ->
                            item(key = providerName) {
                                ProviderSearchResults(
                                    providerName = providerName,
                                    novels = novels,
                                    maxResults = resultsPerProvider,
                                    onNovelClick = { novel ->
                                        onNavigateToDetails(novel.url, novel.apiName)
                                    },
                                    onNovelLongClick = { novel ->
                                        viewModel.showActionSheet(novel)
                                    },
                                    onShowMore = { viewModel.expandProvider(providerName) },
                                    appSettings = appSettings
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}