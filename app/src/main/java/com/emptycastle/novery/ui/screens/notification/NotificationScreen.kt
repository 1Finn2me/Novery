package com.emptycastle.novery.ui.screens.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emptycastle.novery.data.repository.LibraryItem

// ============================================================================
// Colors
// ============================================================================

private object NotificationColors {
    val NewChapters = Color(0xFF10B981)
    val NewChaptersLight = Color(0xFF34D399)
    val Download = Color(0xFF3B82F6)
    val Continue = Color(0xFFE85609)
}

// ============================================================================
// Main Notification Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Scaffold(
        topBar = {
            NotificationTopBar(
                totalNewChapters = uiState.totalNewChapters,
                novelsCount = uiState.novelsCount,
                isDownloadingAll = uiState.isDownloadingAll,
                isMarkingAllSeen = uiState.isMarkingAllSeen,
                onNavigateBack = onNavigateBack,
                onDownloadAll = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.downloadAllNewChapters(context)
                },
                onMarkAllSeen = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.markAllAsSeen()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                NotificationLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            uiState.novelsWithUpdates.isEmpty() -> {
                NotificationEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                NotificationContent(
                    novels = uiState.novelsWithUpdates,
                    downloadingNovelUrls = uiState.downloadingNovelUrls,
                    onDownload = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.downloadNewChapters(context, item)
                    },
                    onContinue = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val position = item.lastReadPosition
                        if (position != null) {
                            viewModel.markAsSeen(item.novel.url)
                            onNavigateToReader(
                                position.chapterUrl,
                                item.novel.url,
                                item.novel.apiName
                            )
                        } else {
                            viewModel.markAsSeen(item.novel.url)
                            onNavigateToDetails(item.novel.url, item.novel.apiName)
                        }
                    },
                    onNovelClick = { item ->
                        viewModel.markAsSeen(item.novel.url)
                        onNavigateToDetails(item.novel.url, item.novel.apiName)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    totalNewChapters: Int,
    novelsCount: Int,
    isDownloadingAll: Boolean,
    isMarkingAllSeen: Boolean,
    onNavigateBack: () -> Unit,
    onDownloadAll: () -> Unit,
    onMarkAllSeen: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (totalNewChapters > 0) {
                    Text(
                        text = "$totalNewChapters new chapter${if (totalNewChapters != 1) "s" else ""} in $novelsCount novel${if (novelsCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NotificationColors.NewChapters
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Download All button
            AnimatedVisibility(
                visible = totalNewChapters > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = onDownloadAll,
                    enabled = !isDownloadingAll
                ) {
                    if (isDownloadingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = NotificationColors.Download
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = "Download all",
                            tint = NotificationColors.Download
                        )
                    }
                }
            }

            // Mark All Seen button
            AnimatedVisibility(
                visible = totalNewChapters > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = onMarkAllSeen,
                    enabled = !isMarkingAllSeen
                ) {
                    if (isMarkingAllSeen) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = NotificationColors.NewChapters
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Mark all as seen",
                            tint = NotificationColors.NewChapters
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ============================================================================
// Content
// ============================================================================

@Composable
private fun NotificationContent(
    novels: List<LibraryItem>,
    downloadingNovelUrls: Set<String>,
    onDownload: (LibraryItem) -> Unit,
    onContinue: (LibraryItem) -> Unit,
    onNovelClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(novels, key = { it.novel.url }) { item ->
            NotificationItem(
                item = item,
                isDownloading = downloadingNovelUrls.contains(item.novel.url),
                onDownload = { onDownload(item) },
                onContinue = { onContinue(item) },
                onClick = { onNovelClick(item) }
            )
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// Notification Item
// ============================================================================

@Composable
private fun NotificationItem(
    item: LibraryItem,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.novel.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.novel.name,
                modifier = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Title and Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.novel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // New chapters badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NotificationColors.NewChapters.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = NotificationColors.NewChapters
                        )
                        Text(
                            text = "${item.newChapterCount} new",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NotificationColors.NewChapters
                        )
                    }
                }

                // Last read info
                item.lastReadPosition?.chapterName?.let { chapterName ->
                    Text(
                        text = "Last: $chapterName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Download Button
                NotificationActionButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = "Download new chapters",
                    onClick = onDownload,
                    isLoading = isDownloading,
                    containerColor = NotificationColors.Download.copy(alpha = 0.15f),
                    contentColor = NotificationColors.Download
                )

                // Continue Button
                NotificationActionButton(
                    icon = Icons.Rounded.PlayArrow,
                    contentDescription = "Continue reading",
                    onClick = onContinue,
                    isLoading = false,
                    containerColor = NotificationColors.Continue.copy(alpha = 0.15f),
                    contentColor = NotificationColors.Continue
                )
            }
        }
    }
}

@Composable
private fun NotificationActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        enabled = !isLoading
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
            }
        }
    }
}

// ============================================================================
// Empty State
// ============================================================================

@Composable
private fun NotificationEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No new chapters available.\nPull to refresh your library to check for updates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// Loading State
// ============================================================================

@Composable
private fun NotificationLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = NotificationColors.NewChapters
            )
            Text(
                text = "Loading updates...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}