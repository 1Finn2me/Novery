package com.emptycastle.novery.ui.screens.home.tabs.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.components.EmptyHistory
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.components.HistoryListItem
import com.emptycastle.novery.ui.components.HistoryListItemCompact
import com.emptycastle.novery.ui.theme.NoveryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    val dimensions = NoveryTheme.dimensions
    val useCompactLayout = appSettings.uiDensity == UiDensity.COMPACT
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Header
        if (uiState.items.isNotEmpty()) {
            HistoryHeader(
                itemCount = uiState.items.size,
                onClearHistory = viewModel::clearHistory
            )
        }

        // Content
        if (uiState.items.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyHistory()
            }
        } else {
            HistoryList(
                items = uiState.items,
                useCompactLayout = useCompactLayout,
                onContinueReading = { item ->
                    onNavigateToReader(item.chapterUrl, item.novel.url, item.novel.apiName)
                },
                onRemoveFromHistory = { item ->
                    viewModel.removeFromHistory(item.novel.url)
                },
                onNovelClick = { item ->
                    onNavigateToDetails(item.novel.url, item.novel.apiName)
                },
            )
        }
    }
}

@Composable
private fun HistoryHeader(
    itemCount: Int,
    onClearHistory: () -> Unit
) {
    val dimensions = NoveryTheme.dimensions

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
                text = "$itemCount ${if (itemCount == 1) "entry" else "entries"}",
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

@Composable
private fun HistoryList(
    items: List<HistoryItem>,
    useCompactLayout: Boolean,
    onContinueReading: (HistoryItem) -> Unit,
    onRemoveFromHistory: (HistoryItem) -> Unit,
    onNovelClick: (HistoryItem) -> Unit,
) {
    val dimensions = NoveryTheme.dimensions

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
                    onRemoveClick = { onRemoveFromHistory(item) },
                    onItemClick = { onNovelClick(item) },
                )
            } else {
                HistoryListItem(
                    item = item,
                    onContinueClick = { onContinueReading(item) },
                    onRemoveClick = { onRemoveFromHistory(item) },
                    onItemClick = { onNovelClick(item) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}