package com.emptycastle.novery.ui.screens.home.tabs.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.HistoryItem
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.components.HistoryListItem
import com.emptycastle.novery.ui.components.HistoryListItemCompact
import com.emptycastle.novery.ui.theme.NoveryTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryTab(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimensions = NoveryTheme.dimensions
    val useCompactLayout = appSettings.uiDensity == UiDensity.COMPACT
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    // Clear confirmation dialog
    if (uiState.showClearConfirmation) {
        ClearHistoryConfirmationDialog(
            itemCount = uiState.totalCount,
            onConfirm = viewModel::confirmClearHistory,
            onDismiss = viewModel::dismissClearConfirmation
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Header with count and clear button
        AnimatedVisibility(
            visible = uiState.totalCount > 0,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            HistoryHeader(
                itemCount = uiState.totalCount,
                onClearHistory = viewModel::requestClearHistory
            )
        }

        // Content
        when {
            uiState.isLoading -> {
                HistoryLoadingState()
            }
            uiState.totalCount == 0 -> {
                EmptyHistoryState()
            }
            else -> {
                GroupedHistoryList(
                    groupedItems = uiState.groupedItems,
                    useCompactLayout = useCompactLayout,
                    onContinueReading = { item ->
                        onNavigateToReader(item.chapterUrl, item.novel.url, item.novel.apiName)
                    },
                    onRemoveFromHistory = { item ->
                        viewModel.removeFromHistory(item.novel.url)
                    },
                    onNovelClick = { item ->
                        onNavigateToDetails(item.novel.url, item.novel.apiName)
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    itemCount: Int,
    onClearHistory: () -> Unit
) {
    val dimensions = NoveryTheme.dimensions

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.gridPadding,
                    vertical = dimensions.spacingMd
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon container
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Reading History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "novel" else "novels"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clear button
            Surface(
                onClick = onClearHistory,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedHistoryList(
    groupedItems: Map<HistoryDateGroup, List<HistoryItem>>,
    useCompactLayout: Boolean,
    onContinueReading: (HistoryItem) -> Unit,
    onRemoveFromHistory: (HistoryItem) -> Unit,
    onNovelClick: (HistoryItem) -> Unit
) {
    val dimensions = NoveryTheme.dimensions
    val orderedGroups = listOf(
        HistoryDateGroup.TODAY,
        HistoryDateGroup.YESTERDAY,
        HistoryDateGroup.THIS_WEEK,
        HistoryDateGroup.THIS_MONTH,
        HistoryDateGroup.EARLIER
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dimensions.gridPadding,
            end = dimensions.gridPadding,
            top = dimensions.spacingSm,
            bottom = dimensions.spacingXl + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        orderedGroups.forEach { group ->
            val items = groupedItems[group] ?: emptyList()
            if (items.isNotEmpty()) {
                // Section header
                stickyHeader(key = "header_$group") {
                    DateGroupHeader(
                        group = group,
                        itemCount = items.size
                    )
                }

                // Items in this group
                items(
                    items = items,
                    key = { "${it.novel.url}_${it.timestamp}" }
                ) { item ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (useCompactLayout) {
                            HistoryListItemCompact(
                                item = item,
                                onContinueClick = { onContinueReading(item) },
                                onRemoveClick = { onRemoveFromHistory(item) },
                                onItemClick = { onNovelClick(item) }
                            )
                        } else {
                            HistoryListItem(
                                item = item,
                                onContinueClick = { onContinueReading(item) },
                                onRemoveClick = { onRemoveFromHistory(item) },
                                onItemClick = { onNovelClick(item) }
                            )
                        }
                    }
                }

                // Spacer between groups
                item(key = "spacer_$group") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DateGroupHeader(
    group: HistoryDateGroup,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = itemCount.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .alpha(0.5f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No Reading History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Novels you read will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HistoryLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ClearHistoryConfirmationDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Clear Reading History?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "This will permanently remove all $itemCount ${if (itemCount == 1) "entry" else "entries"} from your reading history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}