package com.emptycastle.novery.ui.screens.home.tabs.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.domain.model.AppSettings
import com.emptycastle.novery.domain.model.DisplayMode
import com.emptycastle.novery.domain.model.LibraryFilter
import com.emptycastle.novery.domain.model.ReadingStatus
import com.emptycastle.novery.domain.model.UiDensity
import com.emptycastle.novery.ui.components.NovelActionSheet
import com.emptycastle.novery.ui.components.NovelCard
import com.emptycastle.novery.ui.components.NovelCardSkeleton
import com.emptycastle.novery.ui.components.NovelListItem
import com.emptycastle.novery.ui.components.NovelListItemSkeleton
import com.emptycastle.novery.ui.theme.NoveryTheme
import com.emptycastle.novery.util.calculateGridColumns

// ============================================================================
// Colors
// ============================================================================

private object LibraryColors {
    val NewChapters = Color(0xFF10B981)
    val NewChaptersLight = Color(0xFF34D399)
    val Reading = Color(0xFF3B82F6)
    val Completed = Color(0xFF22C55E)
    val OnHold = Color(0xFFF59E0B)
    val PlanToRead = Color(0xFF8B5CF6)
    val Dropped = Color(0xFFEF4444)
    val Downloaded = Color(0xFF06B6D4)
}

// ============================================================================
// Main Library Tab
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    appSettings: AppSettings,
    onNavigateToDetails: (novelUrl: String, providerName: String) -> Unit,
    onNavigateToReader: (chapterUrl: String, novelUrl: String, providerName: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val actionSheetState by viewModel.actionSheetState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    val dimensions = NoveryTheme.dimensions
    val gridColumns = calculateGridColumns(appSettings.libraryGridColumns)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val pullToRefreshState = rememberPullToRefreshState()

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
                val position = viewModel.getReadingPosition(data.novel.url)
                if (position != null) {
                    onNavigateToReader(position.chapterUrl, data.novel.url, data.novel.apiName)
                } else {
                    onNavigateToDetails(data.novel.url, data.novel.apiName)
                }
            },
            onAddToLibrary = null,
            onRemoveFromLibrary = { viewModel.removeFromLibrary(data.novel.url) },
            onRemoveFromHistory = null,
            onStatusChange = { status -> viewModel.updateReadingStatus(status) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.refreshLibrary(context)
            },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            when {
                uiState.isLoading -> {
                    LibraryLoadingSkeleton(
                        gridColumns = gridColumns,
                        statusBarPadding = statusBarPadding,
                        modifier = Modifier.fillMaxSize(),
                        density = appSettings.uiDensity,
                        displayMode = appSettings.libraryDisplayMode
                    )
                }
                uiState.filteredItems.isEmpty() -> {
                    LibraryEmptyContent(
                        uiState = uiState,
                        onQueryChange = viewModel::setSearchQuery,
                        onNotificationClick = onNavigateToNotifications,
                        statusBarPadding = statusBarPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LibraryContent(
                        uiState = uiState,
                        gridColumns = gridColumns,
                        statusBarPadding = statusBarPadding,
                        onQueryChange = viewModel::setSearchQuery,
                        onNotificationClick = onNavigateToNotifications,
                        onNovelClick = { item ->
                            if (item.hasNewChapters) {
                                viewModel.acknowledgeNewChapters(item.novel.url)
                            }
                            val position = item.lastReadPosition
                            if (position != null) {
                                onNavigateToReader(
                                    position.chapterUrl,
                                    item.novel.url,
                                    item.novel.apiName
                                )
                            } else {
                                onNavigateToDetails(item.novel.url, item.novel.apiName)
                            }
                        },
                        onNovelLongClick = { item ->
                            viewModel.showActionSheet(item)
                        },
                        appSettings = appSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Filter Bar
        LibraryFilterBar(
            selectedFilter = uiState.filter,
            onFilterChange = { filter ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setFilter(filter)
            },
            itemCounts = uiState.getFilterCounts(),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================================================
// Library Header with Search and Notification Button
// ============================================================================

@Composable
fun LibraryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    notificationCount: Int,
    onNotificationClick: () -> Unit,
    resultCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibrarySearchBarCompact(
            query = query,
            onQueryChange = onQueryChange,
            resultCount = resultCount,
            totalCount = totalCount,
            modifier = Modifier.weight(1f)
        )

        NotificationButton(
            count = notificationCount,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNotificationClick()
            }
        )
    }
}

@Composable
private fun LibrarySearchBarCompact(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val hasQuery = query.isNotBlank()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search library...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            AnimatedVisibility(
                visible = hasQuery,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (resultCount > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Text(
                            text = "$resultCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (resultCount > 0) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasNotifications = count > 0

    val infiniteTransition = rememberInfiniteTransition(label = "notification_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNotifications) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (hasNotifications) {
            LibraryColors.NewChapters.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            BadgedBox(
                badge = {
                    if (hasNotifications) {
                        Badge(
                            containerColor = LibraryColors.NewChapters,
                            contentColor = Color.White,
                            modifier = Modifier
                                .scale(pulseScale)
                                .offset(x = (-2).dp, y = 2.dp)
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    tint = if (hasNotifications) {
                        LibraryColors.NewChapters
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ============================================================================
// Refresh Progress Card
// ============================================================================

@Composable
private fun RefreshProgressCard(
    progress: RefreshProgress,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "progress_animation"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = rotation },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Checking for updates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = progress.currentNovelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = progress.current,
                            transitionSpec = {
                                fadeIn() + slideInVertically { -it } togetherWith
                                        fadeOut() + slideOutVertically { it }
                            },
                            label = "progress_current"
                        ) { current ->
                            Text(
                                text = "$current",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "/${progress.total}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round
            )

            AnimatedVisibility(
                visible = progress.newChaptersFound > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LibraryColors.NewChapters.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = LibraryColors.NewChapters
                        )
                        Text(
                            text = "Found ${progress.newChaptersFound} new chapters in ${progress.novelsWithNewChapters} novels",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LibraryColors.NewChapters
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Library Content - FIXED: Using itemsIndexed with unique keys
// ============================================================================

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    gridColumns: Int,
    statusBarPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onNotificationClick: () -> Unit,
    onNovelClick: (LibraryItem) -> Unit,
    onNovelLongClick: (LibraryItem) -> Unit,
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions
    val showRefreshProgress = uiState.refreshProgress != null
    val novelsWithNewChapters = uiState.items.count { it.hasNewChapters }
    val displayMode = appSettings.libraryDisplayMode

    // Deduplicate items by URL to prevent key collisions
    val uniqueItems = remember(uiState.filteredItems) {
        uiState.filteredItems.distinctBy { it.novel.url }
    }

    when (displayMode) {
        DisplayMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = dimensions.gridPadding,
                    end = dimensions.gridPadding,
                    top = 6.dp,
                    bottom = 70.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                    LibraryHeader(
                        query = uiState.searchQuery,
                        onQueryChange = onQueryChange,
                        notificationCount = novelsWithNewChapters,
                        onNotificationClick = onNotificationClick,
                        resultCount = uniqueItems.size,
                        totalCount = uiState.items.size,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (showRefreshProgress) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "refresh_progress") {
                        uiState.refreshProgress?.let { progress ->
                            RefreshProgressCard(
                                progress = progress,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }, key = "spacer") {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Use itemsIndexed with composite key to guarantee uniqueness
                itemsIndexed(
                    items = uniqueItems,
                    key = { index, item -> "novel_${item.novel.url}_$index" }
                ) { _, item ->
                    NovelCard(
                        novel = item.novel,
                        onClick = { onNovelClick(item) },
                        onLongClick = { onNovelLongClick(item) },
                        newChapterCount = if (appSettings.showBadges) item.newChapterCount else 0,
                        readingStatus = if (appSettings.showBadges) item.readingStatus else null,
                        lastReadChapter = item.lastReadPosition?.chapterName,
                        density = appSettings.uiDensity
                    )
                }
            }
        }
        DisplayMode.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = dimensions.gridPadding,
                    end = dimensions.gridPadding,
                    top = 6.dp,
                    bottom = 70.dp
                ),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing)
            ) {
                item(key = "header") {
                    LibraryHeader(
                        query = uiState.searchQuery,
                        onQueryChange = onQueryChange,
                        notificationCount = novelsWithNewChapters,
                        onNotificationClick = onNotificationClick,
                        resultCount = uniqueItems.size,
                        totalCount = uiState.items.size,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (showRefreshProgress) {
                    item(key = "refresh_progress") {
                        uiState.refreshProgress?.let { progress ->
                            RefreshProgressCard(
                                progress = progress,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                item(key = "spacer") {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Use itemsIndexed with composite key to guarantee uniqueness
                itemsIndexed(
                    items = uniqueItems,
                    key = { index, item -> "novel_${item.novel.url}_$index" }
                ) { _, item ->
                    NovelListItem(
                        novel = item.novel,
                        onClick = { onNovelClick(item) },
                        onLongClick = { onNovelLongClick(item) },
                        newChapterCount = if (appSettings.showBadges) item.newChapterCount else 0,
                        readingStatus = if (appSettings.showBadges) item.readingStatus else null,
                        lastReadChapter = item.lastReadPosition?.chapterName,
                        density = appSettings.uiDensity
                    )
                }
            }
        }
    }
}

// ============================================================================
// Empty Content
// ============================================================================

@Composable
private fun LibraryEmptyContent(
    uiState: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onNotificationClick: () -> Unit,
    statusBarPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions
    val novelsWithNewChapters = uiState.items.count { it.hasNewChapters }

    Column(
        modifier = modifier.padding(
            top = 6.dp,
            start = dimensions.gridPadding,
            end = dimensions.gridPadding
        )
    ) {
        LibraryHeader(
            query = uiState.searchQuery,
            onQueryChange = onQueryChange,
            notificationCount = novelsWithNewChapters,
            onNotificationClick = onNotificationClick,
            resultCount = 0,
            totalCount = uiState.items.size
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 70.dp),
            contentAlignment = Alignment.Center
        ) {
            LibraryEmptyState(
                searchQuery = uiState.searchQuery,
                filter = uiState.filter,
                totalItems = uiState.items.size
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    searchQuery: String,
    filter: LibraryFilter,
    totalItems: Int
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
            when {
                searchQuery.isNotBlank() -> {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No novels match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                filter != LibraryFilter.ALL && totalItems > 0 -> {
                    val content = getFilterEmptyContent(filter)

                    Surface(
                        shape = CircleShape,
                        color = content.color.copy(alpha = 0.12f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = content.icon,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = content.color
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = content.message,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = content.hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.LibraryBooks,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Your library is empty",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add novels from Browse to build\nyour personal collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Go to Browse tab to discover novels",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class FilterEmptyContent(
    val icon: ImageVector,
    val color: Color,
    val message: String,
    val hint: String
)

@Composable
private fun getFilterEmptyContent(filter: LibraryFilter): FilterEmptyContent {
    return when (filter) {
        LibraryFilter.DOWNLOADED -> FilterEmptyContent(
            icon = Icons.Rounded.CloudDownload,
            color = LibraryColors.Downloaded,
            message = "No downloads yet",
            hint = "Download chapters to read offline"
        )
        LibraryFilter.READING -> FilterEmptyContent(
            icon = Icons.Rounded.MenuBook,
            color = LibraryColors.Reading,
            message = "Nothing in progress",
            hint = "Start reading a novel to see it here"
        )
        LibraryFilter.COMPLETED -> FilterEmptyContent(
            icon = Icons.Rounded.CheckCircle,
            color = LibraryColors.Completed,
            message = "No completed novels",
            hint = "Mark novels as completed when you finish them"
        )
        LibraryFilter.ON_HOLD -> FilterEmptyContent(
            icon = Icons.Rounded.PauseCircle,
            color = LibraryColors.OnHold,
            message = "Nothing on hold",
            hint = "Put novels on hold when you need a break"
        )
        LibraryFilter.PLAN_TO_READ -> FilterEmptyContent(
            icon = Icons.Rounded.BookmarkAdd,
            color = LibraryColors.PlanToRead,
            message = "Reading list empty",
            hint = "Add novels you plan to read later"
        )
        LibraryFilter.DROPPED -> FilterEmptyContent(
            icon = Icons.Rounded.Cancel,
            color = LibraryColors.Dropped,
            message = "No dropped novels",
            hint = "Novels you've stopped reading appear here"
        )
        else -> FilterEmptyContent(
            icon = Icons.Rounded.LibraryBooks,
            color = MaterialTheme.colorScheme.primary,
            message = "No novels",
            hint = ""
        )
    }
}

// ============================================================================
// Loading Skeleton
// ============================================================================

@Composable
private fun LibraryLoadingSkeleton(
    gridColumns: Int,
    statusBarPadding: PaddingValues,
    density: UiDensity,
    displayMode: DisplayMode,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions

    when (displayMode) {
        DisplayMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = dimensions.gridPadding,
                    end = dimensions.gridPadding,
                    top = 6.dp,
                    bottom = 70.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                userScrollEnabled = false
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(8) {
                    NovelCardSkeleton(density = density)
                }
            }
        }
        DisplayMode.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = dimensions.gridPadding,
                    end = dimensions.gridPadding,
                    top = 6.dp,
                    bottom = 70.dp
                ),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardSpacing),
                userScrollEnabled = false
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(6) {
                    NovelListItemSkeleton(density = density)
                }
            }
        }
    }
}

// ============================================================================
// Filter Bar
// ============================================================================

@Composable
private fun LibraryFilterBar(
    selectedFilter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit,
    itemCounts: Map<LibraryFilter, Int>,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimensions.gridPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryFilter.entries.forEach { filter ->
            LibraryFilterChip(
                filter = filter,
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                count = itemCounts[filter] ?: 0,
                showCount = filter == LibraryFilter.ALL || (itemCounts[filter] ?: 0) > 0
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(
    filter: LibraryFilter,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int,
    showCount: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val filterColor = getFilterColor(filter)

    val contentColor by animateColorAsState(
        targetValue = if (selected) filterColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "chip_content"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) filterColor else Color.Transparent,
        animationSpec = tween(250, easing = EaseOutCubic),
        label = "chip_border"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "chip_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = contentColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = getFilterIcon(filter)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
            }

            Text(
                text = filter.displayName(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )

            AnimatedVisibility(
                visible = showCount && count > 0 && selected,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150))
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = filterColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (count > 999) "999+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = filterColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getFilterColor(filter: LibraryFilter): Color {
    return when (filter) {
        LibraryFilter.ALL -> MaterialTheme.colorScheme.primary
        LibraryFilter.DOWNLOADED -> LibraryColors.Downloaded
        LibraryFilter.READING -> LibraryColors.Reading
        LibraryFilter.COMPLETED -> LibraryColors.Completed
        LibraryFilter.ON_HOLD -> LibraryColors.OnHold
        LibraryFilter.PLAN_TO_READ -> LibraryColors.PlanToRead
        LibraryFilter.DROPPED -> LibraryColors.Dropped
    }
}

private fun getFilterIcon(filter: LibraryFilter): ImageVector? {
    return when (filter) {
        LibraryFilter.ALL -> Icons.Rounded.LibraryBooks
        LibraryFilter.DOWNLOADED -> Icons.Rounded.CloudDownload
        LibraryFilter.READING -> Icons.Rounded.MenuBook
        LibraryFilter.COMPLETED -> Icons.Rounded.CheckCircle
        LibraryFilter.ON_HOLD -> Icons.Rounded.PauseCircle
        LibraryFilter.PLAN_TO_READ -> Icons.Rounded.BookmarkAdd
        LibraryFilter.DROPPED -> Icons.Rounded.Cancel
    }
}

private fun LibraryUiState.getFilterCounts(): Map<LibraryFilter, Int> {
    return mapOf(
        LibraryFilter.ALL to items.size,
        LibraryFilter.DOWNLOADED to items.count { (downloadCounts[it.novel.url] ?: 0) > 0 },
        LibraryFilter.READING to items.count { it.readingStatus == ReadingStatus.READING },
        LibraryFilter.COMPLETED to items.count { it.readingStatus == ReadingStatus.COMPLETED },
        LibraryFilter.ON_HOLD to items.count { it.readingStatus == ReadingStatus.ON_HOLD },
        LibraryFilter.PLAN_TO_READ to items.count { it.readingStatus == ReadingStatus.PLAN_TO_READ },
        LibraryFilter.DROPPED to items.count { it.readingStatus == ReadingStatus.DROPPED }
    )
}