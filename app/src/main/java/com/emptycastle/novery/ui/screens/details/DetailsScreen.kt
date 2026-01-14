package com.emptycastle.novery.ui.screens.details

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.ui.components.FullScreenLoading
import kotlinx.coroutines.launch

// ================================================================
// COLORS
// ================================================================

private val Pink = Color(0xFFEC4899)
private val Success = Color(0xFF22C55E)
private val Warning = Color(0xFFF59E0B)
private val Dropped = Color(0xFFEF4444)

private val StatusReading = Color(0xFF3B82F6)
private val StatusCompleted = Color(0xFF22C55E)
private val StatusOnHold = Color(0xFFF59E0B)
private val StatusPlanToRead = Color(0xFF8B5CF6)
private val StatusDropped = Color(0xFFEF4444)

// ================================================================
// MAIN SCREEN
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    novelUrl: String,
    providerName: String,
    onBack: () -> Unit,
    onChapterClick: (String, String, String) -> Unit,
    viewModel: DetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by DownloadServiceManager.downloadState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isDownloadingThisNovel = downloadState.isActive && downloadState.novelUrl == novelUrl

    // Handle back press in selection mode
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.disableSelectionMode()
    }

    LaunchedEffect(novelUrl, providerName) {
        viewModel.loadNovel(novelUrl, providerName)
    }

    // Cover zoom dialog
    if (uiState.showCoverZoom && uiState.novelDetails?.posterUrl != null) {
        CoverZoomDialog(
            imageUrl = uiState.novelDetails!!.posterUrl!!,
            title = uiState.novelDetails!!.name,
            onDismiss = { viewModel.hideCoverZoom() }
        )
    }

    // Download menu bottom sheet
    if (uiState.showDownloadMenu) {
        DownloadBottomSheet(
            isDownloading = downloadState.isActive,
            isPaused = downloadState.isPaused,
            downloadProgress = downloadState.progressPercent,
            currentProgress = downloadState.currentProgress,
            totalChapters = downloadState.totalChapters,
            onDismiss = { viewModel.hideDownloadMenu() },
            onDownloadAll = { viewModel.downloadAll(context) },
            onDownloadNext100 = { viewModel.downloadNext100(context) },
            onDownloadUnread = { viewModel.downloadUnread(context) },
            onSelectChapters = {
                viewModel.hideDownloadMenu()
                viewModel.enableSelectionMode()
            },
            onPause = { DownloadServiceManager.pauseDownload() },
            onResume = { DownloadServiceManager.resumeDownload() },
            onCancel = { DownloadServiceManager.cancelDownload() }
        )
    }

    // Status menu bottom sheet
    if (uiState.showStatusMenu) {
        StatusBottomSheet(
            currentStatus = uiState.readingStatus,
            onStatusSelected = { viewModel.updateReadingStatus(it) },
            onDismiss = { viewModel.hideStatusMenu() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && !uiState.isRefreshing -> {
                FullScreenLoading(message = "Loading novel...")
            }
            uiState.error != null && uiState.novelDetails == null -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadNovel(novelUrl, providerName) },
                    onBack = onBack
                )
            }
            else -> {
                uiState.novelDetails?.let { details ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // Add bottom padding for selection bar when active
                                contentPadding = PaddingValues(
                                    bottom = if (uiState.isSelectionMode) 160.dp else 0.dp
                                )
                            ) {
                                // Header with cover and info
                                item(key = "header") {
                                    NovelHeader(
                                        details = details,
                                        isFavorite = uiState.isFavorite,
                                        readingStatus = uiState.readingStatus,
                                        readProgress = uiState.readProgress,
                                        onBack = onBack,
                                        onToggleFavorite = { viewModel.toggleFavorite() },
                                        onStatusClick = { viewModel.showStatusMenu() },
                                        onCoverClick = { viewModel.showCoverZoom() }
                                    )
                                }

                                // Action buttons
                                item(key = "actions") {
                                    ActionButtonsRow(
                                        hasStartedReading = uiState.hasStartedReading,
                                        lastReadChapterName = uiState.lastReadChapterName,
                                        isDownloading = isDownloadingThisNovel,
                                        downloadProgress = downloadState.progressPercent,
                                        onRead = {
                                            val chapterUrl = viewModel.getChapterToOpen()
                                            if (chapterUrl != null) {
                                                onChapterClick(chapterUrl, novelUrl, providerName)
                                            }
                                        },
                                        onDownload = { viewModel.showDownloadMenu() }
                                    )
                                }

                                // Stats row
                                item(key = "stats") {
                                    StatsRow(
                                        chapterCount = details.chapters.size,
                                        readCount = uiState.readChapters.size,
                                        downloadedCount = uiState.downloadedCount,
                                        rating = details.rating,
                                        peopleVoted = details.peopleVoted
                                    )
                                }

                                // Synopsis
                                if (!details.synopsis.isNullOrBlank()) {
                                    item(key = "synopsis") {
                                        SynopsisSection(
                                            synopsis = details.synopsis,
                                            isExpanded = uiState.isSynopsisExpanded,
                                            onToggle = { viewModel.toggleSynopsis() }
                                        )
                                    }
                                }

                                // Tags
                                if (!details.tags.isNullOrEmpty()) {
                                    item(key = "tags") {
                                        TagsRow(tags = details.tags)
                                    }
                                }

                                // Chapter list header with filters
                                item(key = "chapter_header") {
                                    ChapterListHeader(
                                        chapterCount = details.chapters.size,
                                        filteredCount = viewModel.getFilteredChapters().size,
                                        isDescending = uiState.isChapterSortDescending,
                                        isSelectionMode = uiState.isSelectionMode,
                                        currentFilter = uiState.chapterFilter,
                                        isSearchActive = uiState.isSearchActive,
                                        searchQuery = uiState.chapterSearchQuery,
                                        unreadCount = uiState.unreadCount,
                                        downloadedCount = uiState.downloadedCount,
                                        notDownloadedCount = uiState.notDownloadedCount,
                                        onToggleSort = { viewModel.toggleChapterSort() },
                                        onFilterChange = { viewModel.setChapterFilter(it) },
                                        onToggleSearch = { viewModel.toggleSearch() },
                                        onSearchQueryChange = { viewModel.setChapterSearchQuery(it) },
                                        onEnableSelection = { viewModel.enableSelectionMode() }
                                    )
                                }

                                // Chapter list
                                val filteredChapters = viewModel.getFilteredChapters()

                                if (filteredChapters.isEmpty()) {
                                    item(key = "empty_chapters") {
                                        EmptyChaptersMessage(
                                            filter = uiState.chapterFilter,
                                            hasSearch = uiState.chapterSearchQuery.isNotBlank()
                                        )
                                    }
                                } else {
                                    itemsIndexed(
                                        items = filteredChapters,
                                        key = { _, chapter -> chapter.url }
                                    ) { index, chapter ->
                                        ChapterItem(
                                            chapter = chapter,
                                            index = index,
                                            isRead = uiState.readChapters.contains(chapter.url),
                                            isDownloaded = uiState.downloadedChapters.contains(chapter.url),
                                            isLastRead = chapter.url == uiState.lastReadChapterUrl,
                                            isSelectionMode = uiState.isSelectionMode,
                                            isSelected = uiState.selectedChapters.contains(chapter.url),
                                            onTap = {
                                                if (uiState.isSelectionMode) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.toggleChapterSelection(index, chapter.url)
                                                } else {
                                                    onChapterClick(chapter.url, novelUrl, providerName)
                                                }
                                            },
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (uiState.isSelectionMode) {
                                                    viewModel.selectRange(index)
                                                } else {
                                                    viewModel.enableSelectionMode(chapter.url)
                                                }
                                            }
                                        )
                                    }
                                }

                                // Bottom padding
                                item(key = "bottom_spacer") {
                                    Spacer(
                                        modifier = Modifier
                                            .height(100.dp)
                                            .navigationBarsPadding()
                                    )
                                }
                            }
                        }

                        // Selection Mode Overlay - Top Bar
                        AnimatedVisibility(
                            visible = uiState.isSelectionMode,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(200)
                            ) + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            SelectionTopBar(
                                selectedCount = uiState.selectedChapters.size,
                                totalCount = viewModel.getFilteredChapters().size,
                                onSelectAll = { viewModel.selectAll() },
                                onDeselectAll = { viewModel.deselectAll() },
                                onInvertSelection = { viewModel.invertSelection() },
                                onCancel = { viewModel.disableSelectionMode() }
                            )
                        }

                        // Selection Mode Overlay - Bottom Bar
                        AnimatedVisibility(
                            visible = uiState.isSelectionMode,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(200)
                            ) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            SelectionBottomBar(
                                selectedCount = uiState.selectedChapters.size,
                                selectedNotDownloadedCount = uiState.selectedNotDownloadedCount,
                                selectedDownloadedCount = uiState.selectedDownloadedCount,
                                selectedUnreadCount = uiState.selectedUnreadCount,
                                selectedReadCount = uiState.selectedReadCount,
                                isDownloadActive = downloadState.isActive,
                                onDownload = { viewModel.downloadSelected(context) },
                                onDelete = { viewModel.deleteSelectedDownloads() },
                                onMarkAsRead = { viewModel.markSelectedAsRead() },
                                onMarkAsUnread = { viewModel.markSelectedAsUnread() }
                            )
                        }

                        // Floating scroll button (hidden during selection mode)
                        if (uiState.hasStartedReading && uiState.lastReadChapterIndex >= 0 && !uiState.isSelectionMode) {
                            val chapters = viewModel.getFilteredChapters()
                            val lastReadInFiltered = chapters.indexOfFirst {
                                it.url == uiState.lastReadChapterUrl
                            }

                            if (lastReadInFiltered >= 0) {
                                FloatingScrollButton(
                                    onClick = {
                                        scope.launch {
                                            listState.animateScrollToItem(lastReadInFiltered + 5)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .navigationBarsPadding()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// SELECTION TOP BAR - New Component
// ================================================================

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel selection",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                AnimatedContent(
                    targetState = selectedCount,
                    transitionSpec = {
                        (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                            .togetherWith(scaleOut() + fadeOut())
                    },
                    label = "selected_count"
                ) { count ->
                    Text(
                        text = "$count selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Select All
                IconButton(
                    onClick = onSelectAll,
                    enabled = selectedCount < totalCount
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select all",
                        tint = if (selectedCount < totalCount)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    )
                }

                // Invert Selection
                IconButton(onClick = onInvertSelection) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Invert selection",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Deselect All
                IconButton(
                    onClick = onDeselectAll,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Deselect,
                        contentDescription = "Deselect all",
                        tint = if (selectedCount > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ================================================================
// SELECTION BOTTOM BAR - New Component
// ================================================================

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    selectedNotDownloadedCount: Int,
    selectedDownloadedCount: Int,
    selectedUnreadCount: Int,
    selectedReadCount: Int,
    isDownloadActive: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection summary
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SelectionStat(
                            icon = Icons.Outlined.CloudDownload,
                            label = "To Download",
                            count = selectedNotDownloadedCount,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SelectionStat(
                            icon = Icons.Default.DownloadDone,
                            label = "Downloaded",
                            count = selectedDownloadedCount,
                            color = Success
                        )
                        SelectionStat(
                            icon = Icons.Outlined.VisibilityOff,
                            label = "Unread",
                            count = selectedUnreadCount,
                            color = Warning
                        )
                        SelectionStat(
                            icon = Icons.Outlined.Visibility,
                            label = "Read",
                            count = selectedReadCount,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action buttons grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download Button
                SelectionActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Download,
                    label = "Download",
                    count = selectedNotDownloadedCount,
                    enabled = selectedNotDownloadedCount > 0 && !isDownloadActive,
                    isPrimary = true,
                    onClick = onDownload
                )

                // Delete Button
                SelectionActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    count = selectedDownloadedCount,
                    enabled = selectedDownloadedCount > 0,
                    isDestructive = true,
                    onClick = onDelete
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mark as Read Button
                SelectionActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Visibility,
                    label = "Mark Read",
                    count = selectedUnreadCount,
                    enabled = selectedUnreadCount > 0,
                    onClick = onMarkAsRead
                )

                // Mark as Unread Button
                SelectionActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VisibilityOff,
                    label = "Mark Unread",
                    count = selectedReadCount,
                    enabled = selectedReadCount > 0,
                    onClick = onMarkAsUnread
                )
            }

            // Hint
            Text(
                text = "Tap chapters to select • Long press for range selection",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SelectionStat(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color.copy(alpha = if (count > 0) 1f else 0.4f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = if (count > 0) 1f else 0.4f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SelectionActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    count: Int,
    enabled: Boolean,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        isPrimary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                if (count > 0) {
                    Text(
                        text = "$count chapter${if (count > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ================================================================
// IMPROVED CHAPTER ITEM - With animated selection
// ================================================================

@Composable
private fun ChapterItem(
    chapter: Chapter,
    index: Int,
    isRead: Boolean,
    isDownloaded: Boolean,
    isLastRead: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isLastRead -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            isRead -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "bg_color"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelectionMode && isSelected -> MaterialTheme.colorScheme.primary
            isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "text_color"
    )

    val checkboxScale by animateFloatAsState(
        targetValue = if (isSelectionMode) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkbox_scale"
    )

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selected_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Selection checkbox with animation
                if (checkboxScale > 0.01f) {
                    Box(
                        modifier = Modifier
                            .scale(checkboxScale)
                            .padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(selectedScale)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Transparent
                                )
                                .then(
                                    if (!isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            this@Row.AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                                exit = scaleOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Last read indicator
                if (isLastRead && !isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Chapter name
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side status icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download status indicator
                AnimatedContent(
                    targetState = isDownloaded,
                    transitionSpec = {
                        (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "download_status"
                ) { downloaded ->
                    Icon(
                        imageVector = if (downloaded) Icons.Default.DownloadDone else Icons.Outlined.CloudDownload,
                        contentDescription = if (downloaded) "Downloaded" else "Not downloaded",
                        modifier = Modifier.size(18.dp),
                        tint = if (downloaded)
                            Success.copy(alpha = if (isRead) 0.5f else 1f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isRead) 0.3f else 0.5f
                            )
                    )
                }

                // Read status (only when not in selection mode)
                AnimatedVisibility(
                    visible = !isSelectionMode && isRead,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Read",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}



// ================================================================
// REST OF THE COMPONENTS (UNCHANGED OR MINIMALLY MODIFIED)
// ================================================================

@Composable
fun SimpleScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val totalItems = listState.layoutInfo.totalItemsCount
    val firstVisibleIndex = listState.firstVisibleItemIndex

    if (totalItems < 20) return

    val thumbPosition = if (totalItems > 1) {
        firstVisibleIndex.toFloat() / (totalItems - 1).toFloat()
    } else 0f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(8.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.15f)
                .align(Alignment.TopCenter)
                .offset(y = (thumbPosition * 0.85f * 100).dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        )
    }
}

// Cover Zoom Dialog
@Composable
private fun CoverZoomDialog(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onTap = { onDismiss() }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Double-tap to zoom • Pinch to adjust",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// Error Content
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Failed to load novel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Back")
            }

            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

// Novel Header (unchanged)
@Composable
private fun NovelHeader(
    details: NovelDetails,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    readProgress: Float,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStatusClick: () -> Unit,
    onCoverClick: () -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (!details.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = details.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp + statusBarHeight)
                    .blur(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp + statusBarHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from library" else "Add to library",
                        tint = if (isFavorite) Pink else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(2f / 3f)
                        .clickable { onCoverClick() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box {
                        if (!details.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = details.posterUrl,
                                contentDescription = details.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Tap to zoom",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }

                        if (readProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.Black.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(readProgress)
                                        .background(Success)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = details.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )

                    if (!details.author.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = details.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!details.status.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (details.status.lowercase()) {
                                "ongoing" -> StatusReading.copy(alpha = 0.2f)
                                "completed" -> StatusCompleted.copy(alpha = 0.2f)
                                "dropped", "cancelled", "canceled" -> StatusDropped.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                            }
                        ) {
                            Text(
                                text = details.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (details.status.lowercase()) {
                                    "ongoing" -> StatusReading
                                    "completed" -> StatusCompleted
                                    "dropped", "cancelled", "canceled" -> StatusDropped
                                    else -> Color.White.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isFavorite) {
                        Surface(
                            onClick = onStatusClick,
                            shape = RoundedCornerShape(20.dp),
                            color = getStatusColor(readingStatus).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, getStatusColor(readingStatus).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = getStatusIcon(readingStatus),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = getStatusColor(readingStatus)
                                )
                                Text(
                                    text = readingStatus.displayName(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = getStatusColor(readingStatus)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = getStatusColor(readingStatus)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Action Buttons Row (unchanged)
@Composable
private fun ActionButtonsRow(
    hasStartedReading: Boolean,
    lastReadChapterName: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    onRead: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (hasStartedReading && !lastReadChapterName.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdded,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = lastReadChapterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRead,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (hasStartedReading) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasStartedReading) "Continue" else "Start Reading",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDownloading) "${(downloadProgress * 100).toInt()}%" else "Download",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// Stats Row (unchanged)
@Composable
private fun StatsRow(
    chapterCount: Int,
    readCount: Int,
    downloadedCount: Int,
    rating: Int?,
    peopleVoted: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Outlined.MenuBook,
            value = chapterCount.toString(),
            label = "Chapters"
        )

        StatItem(
            icon = Icons.Outlined.Visibility,
            value = readCount.toString(),
            label = "Read",
            color = if (readCount > 0) Success else null
        )

        StatItem(
            icon = Icons.Outlined.CloudDownload,
            value = downloadedCount.toString(),
            label = "Saved",
            color = if (downloadedCount > 0) MaterialTheme.colorScheme.primary else null
        )

        if (rating != null) {
            StatItem(
                icon = Icons.Default.Star,
                value = String.format("%.1f", rating / 100f),
                label = if (peopleVoted != null) "$peopleVoted" else "Rating",
                color = Warning
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Synopsis Section (unchanged)
@Composable
private fun SynopsisSection(
    synopsis: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                },
                label = "synopsis"
            ) { expanded ->
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Tags Row (unchanged)
@Composable
private fun TagsRow(tags: List<String>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tags.size) { index ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = tags[index],
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// Chapter List Header (unchanged)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterListHeader(
    chapterCount: Int,
    filteredCount: Int,
    isDescending: Boolean,
    isSelectionMode: Boolean,
    currentFilter: ChapterFilter,
    isSearchActive: Boolean,
    searchQuery: String,
    unreadCount: Int,
    downloadedCount: Int,
    notDownloadedCount: Int,
    onToggleSort: () -> Unit,
    onFilterChange: (ChapterFilter) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEnableSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (filteredCount != chapterCount) {
                    Text(
                        text = " ($filteredCount/$chapterCount)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = " ($chapterCount)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onToggleSearch, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleSort, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = "Sort",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isSelectionMode) {
                    IconButton(onClick = onEnableSelection, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBox,
                            contentDescription = "Select",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                placeholder = { Text("Search chapters...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = currentFilter == ChapterFilter.ALL,
                    onClick = { onFilterChange(ChapterFilter.ALL) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == ChapterFilter.UNREAD,
                    onClick = { onFilterChange(ChapterFilter.UNREAD) },
                    label = { Text("Unread ($unreadCount)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == ChapterFilter.DOWNLOADED,
                    onClick = { onFilterChange(ChapterFilter.DOWNLOADED) },
                    label = { Text("Downloaded ($downloadedCount)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == ChapterFilter.NOT_DOWNLOADED,
                    onClick = { onFilterChange(ChapterFilter.NOT_DOWNLOADED) },
                    label = { Text("Not Downloaded ($notDownloadedCount)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

// Empty Chapters Message (unchanged)
@Composable
private fun EmptyChaptersMessage(
    filter: ChapterFilter,
    hasSearch: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when {
                    hasSearch -> Icons.Outlined.SearchOff
                    filter == ChapterFilter.UNREAD -> Icons.Outlined.DoneAll
                    filter == ChapterFilter.DOWNLOADED -> Icons.Outlined.CloudOff
                    filter == ChapterFilter.NOT_DOWNLOADED -> Icons.Outlined.CloudDone
                    else -> Icons.Outlined.MenuBook
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    hasSearch -> "No chapters match your search"
                    filter == ChapterFilter.UNREAD -> "All chapters read!"
                    filter == ChapterFilter.DOWNLOADED -> "No downloaded chapters"
                    filter == ChapterFilter.NOT_DOWNLOADED -> "All chapters downloaded!"
                    else -> "No chapters available"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Floating Scroll Button (unchanged)
@Composable
private fun FloatingScrollButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkAdded,
            contentDescription = "Go to last read"
        )
    }
}

// Download Bottom Sheet (unchanged)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadBottomSheet(
    isDownloading: Boolean,
    isPaused: Boolean,
    downloadProgress: Float,
    currentProgress: Int,
    totalChapters: Int,
    onDismiss: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadNext100: () -> Unit,
    onDownloadUnread: () -> Unit,
    onSelectChapters: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Download Chapters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isDownloading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPaused) "Paused" else "Downloading...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$currentProgress / $totalChapters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isPaused) {
                                Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume")
                                }
                            } else {
                                OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Pause, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onCancel()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            } else {
                DownloadOption(
                    icon = Icons.Default.CloudDownload,
                    title = "Download All",
                    subtitle = "Download all chapters for offline reading",
                    onClick = {
                        onDownloadAll()
                        onDismiss()
                    }
                )

                DownloadOption(
                    icon = Icons.Default.Download,
                    title = "Download Next 100",
                    subtitle = "Download the next 100 undownloaded chapters",
                    onClick = {
                        onDownloadNext100()
                        onDismiss()
                    }
                )

                DownloadOption(
                    icon = Icons.Default.Visibility,
                    title = "Download Unread",
                    subtitle = "Download all unread chapters",
                    onClick = {
                        onDownloadUnread()
                        onDismiss()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DownloadOption(
                    icon = Icons.Outlined.CheckBox,
                    title = "Select Chapters",
                    subtitle = "Choose specific chapters to download",
                    onClick = onSelectChapters
                )
            }
        }
    }
}

@Composable
private fun DownloadOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Status Bottom Sheet (unchanged)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusBottomSheet(
    currentStatus: ReadingStatus,
    onStatusSelected: (ReadingStatus) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Reading Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReadingStatus.entries.forEach { status ->
                StatusOption(
                    status = status,
                    isSelected = status == currentStatus,
                    onClick = { onStatusSelected(status) }
                )
            }
        }
    }
}

@Composable
private fun StatusOption(
    status: ReadingStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = getStatusColor(status)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getStatusIcon(status),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = color
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = status.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
        }
    }
}

// Utility Functions (unchanged)
private fun getStatusColor(status: ReadingStatus): Color = when (status) {
    ReadingStatus.READING -> StatusReading
    ReadingStatus.COMPLETED -> StatusCompleted
    ReadingStatus.ON_HOLD -> StatusOnHold
    ReadingStatus.PLAN_TO_READ -> StatusPlanToRead
    ReadingStatus.DROPPED -> StatusDropped
}

private fun getStatusIcon(status: ReadingStatus): ImageVector = when (status) {
    ReadingStatus.READING -> Icons.Default.MenuBook
    ReadingStatus.COMPLETED -> Icons.Default.CheckCircle
    ReadingStatus.ON_HOLD -> Icons.Default.Pause
    ReadingStatus.PLAN_TO_READ -> Icons.Default.Schedule
    ReadingStatus.DROPPED -> Icons.Default.Cancel
}