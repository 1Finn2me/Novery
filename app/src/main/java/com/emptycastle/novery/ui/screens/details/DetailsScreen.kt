// com/emptycastle/novery/ui/screens/details/DetailsScreen.kt
// Updated to use the new simplified components
package com.emptycastle.novery.ui.screens.details

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.UserReview
import com.emptycastle.novery.service.DownloadServiceManager
import com.emptycastle.novery.service.DownloadState
import com.emptycastle.novery.ui.components.FullScreenLoading
import com.emptycastle.novery.ui.screens.details.components.ActionButtonsRow
import com.emptycastle.novery.ui.screens.details.components.ChapterItem
import com.emptycastle.novery.ui.screens.details.components.ChapterListHeader
import com.emptycastle.novery.ui.screens.details.components.CoverZoomDialog
import com.emptycastle.novery.ui.screens.details.components.DetailsTabRow
import com.emptycastle.novery.ui.screens.details.components.DownloadBottomSheet
import com.emptycastle.novery.ui.screens.details.components.EmptyChaptersMessage
import com.emptycastle.novery.ui.screens.details.components.EmptyRelatedMessage
import com.emptycastle.novery.ui.screens.details.components.EmptyReviewsMessage
import com.emptycastle.novery.ui.screens.details.components.ErrorContent
import com.emptycastle.novery.ui.screens.details.components.FastScrollerContainer
import com.emptycastle.novery.ui.screens.details.components.FloatingScrollButton
import com.emptycastle.novery.ui.screens.details.components.LoadMoreReviewsButton
import com.emptycastle.novery.ui.screens.details.components.NovelHeader
import com.emptycastle.novery.ui.screens.details.components.PaginationControls
import com.emptycastle.novery.ui.screens.details.components.RelatedNovelRow
import com.emptycastle.novery.ui.screens.details.components.ReviewCard
import com.emptycastle.novery.ui.screens.details.components.ReviewsHeader
import com.emptycastle.novery.ui.screens.details.components.ReviewsLoadingIndicator
import com.emptycastle.novery.ui.screens.details.components.SelectionModeOverlay
import com.emptycastle.novery.ui.screens.details.components.StatsRow
import com.emptycastle.novery.ui.screens.details.components.StatusBottomSheet
import com.emptycastle.novery.ui.screens.details.components.SynopsisSection
import com.emptycastle.novery.ui.screens.details.components.TagsRow
import com.emptycastle.novery.ui.screens.details.components.createSelectionCallbacks
import com.emptycastle.novery.ui.screens.details.components.createSelectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ================================================================
// MAIN DETAILS SCREEN
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    novelUrl: String,
    providerName: String,
    onBack: () -> Unit,
    onChapterClick: (String, String, String) -> Unit,
    onNovelClick: (String, String) -> Unit = { _, _ -> },
    onOpenInWebView: (String, String) -> Unit = { _, _ -> },
    viewModel: DetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by DownloadServiceManager.downloadState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isDownloadingThisNovel = downloadState.isActive && downloadState.novelUrl == novelUrl
    val filteredChapters = uiState.filteredChapters
    val displayedChapters = uiState.displayedChapters

    // Handle back press in selection mode
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.disableSelectionMode()
    }

    // Load novel on first composition
    LaunchedEffect(novelUrl, providerName) {
        viewModel.loadNovel(novelUrl, providerName)
    }

    // Reset scroll position when page changes in paginated mode
    LaunchedEffect(uiState.paginationState.currentPage, uiState.chapterDisplayMode) {
        if (uiState.chapterDisplayMode == ChapterDisplayMode.PAGINATED &&
            uiState.selectedTab == DetailsTab.CHAPTERS
        ) {
            val headerCount = calculateHeaderItemCount(uiState)
            listState.scrollToItem(headerCount)
        }
    }

    // Dialogs and Bottom Sheets
    DetailsDialogs(
        uiState = uiState,
        downloadState = downloadState,
        viewModel = viewModel,
        context = context,
        novelUrl = novelUrl
    )

    // Main content
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
                            // Wrap in FastScrollerContainer for scroll mode in chapters tab
                            val usesFastScroller = uiState.selectedTab == DetailsTab.CHAPTERS &&
                                    uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL &&
                                    filteredChapters.size > 20

                            if (usesFastScroller) {
                                FastScrollerContainer(
                                    listState = listState,
                                    totalItems = filteredChapters.size
                                ) {
                                    DetailsContent(
                                        listState = listState,
                                        uiState = uiState,
                                        details = details,
                                        filteredChapters = filteredChapters,
                                        displayedChapters = displayedChapters,
                                        isDownloadingThisNovel = isDownloadingThisNovel,
                                        downloadProgress = downloadState.progressPercent,
                                        novelUrl = novelUrl,
                                        providerName = providerName,
                                        onBack = onBack,
                                        onChapterClick = onChapterClick,
                                        onNovelClick = onNovelClick,
                                        onOpenInWebView = onOpenInWebView,
                                        onHapticFeedback = { type ->
                                            haptic.performHapticFeedback(type)
                                        },
                                        onTabSelected = { viewModel.selectTab(it) },
                                        viewModel = viewModel,
                                        scope = scope
                                    )
                                }
                            } else {
                                DetailsContent(
                                    listState = listState,
                                    uiState = uiState,
                                    details = details,
                                    filteredChapters = filteredChapters,
                                    displayedChapters = displayedChapters,
                                    isDownloadingThisNovel = isDownloadingThisNovel,
                                    downloadProgress = downloadState.progressPercent,
                                    novelUrl = novelUrl,
                                    providerName = providerName,
                                    onBack = onBack,
                                    onChapterClick = onChapterClick,
                                    onNovelClick = onNovelClick,
                                    onOpenInWebView = onOpenInWebView,
                                    onHapticFeedback = { type ->
                                        haptic.performHapticFeedback(type)
                                    },
                                    onTabSelected = { viewModel.selectTab(it) },
                                    viewModel = viewModel,
                                    scope = scope
                                )
                            }
                        }

                        // Selection mode overlay (only in chapters tab)
                        if (uiState.isSelectionMode && uiState.selectedTab == DetailsTab.CHAPTERS) {
                            SelectionModeOverlay(
                                isVisible = true,
                                selectionState = createSelectionState(
                                    selectedCount = uiState.selectedChapters.size,
                                    totalCount = filteredChapters.size,
                                    selectedNotDownloadedCount = uiState.selectedNotDownloadedCount,
                                    selectedDownloadedCount = uiState.selectedDownloadedCount,
                                    selectedUnreadCount = uiState.selectedUnreadCount,
                                    selectedReadCount = uiState.selectedReadCount,
                                    isDownloadActive = downloadState.isActive
                                ),
                                callbacks = createSelectionCallbacks(
                                    onSelectAll = { viewModel.selectAll() },
                                    onSelectAllUnread = { viewModel.selectAllUnread() },
                                    onSelectAllNotDownloaded = { viewModel.selectAllNotDownloaded() },
                                    onDeselectAll = { viewModel.deselectAll() },
                                    onInvertSelection = { viewModel.invertSelection() },
                                    onCancel = { viewModel.disableSelectionMode() },
                                    onDownload = { viewModel.downloadSelected(context) },
                                    onDelete = { viewModel.deleteSelectedDownloads() },
                                    onMarkAsRead = { viewModel.markSelectedAsRead() },
                                    onMarkAsUnread = { viewModel.markSelectedAsUnread() },
                                    onMarkAsLastRead = { viewModel.setAsLastReadAndMarkPrevious() }
                                )
                            )
                        }

                        // Floating scroll to last read button (only in chapters tab, scroll mode)
                        if (uiState.selectedTab == DetailsTab.CHAPTERS &&
                            uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL &&
                            !uiState.isSelectionMode
                        ) {
                            FloatingScrollButtonContainer(
                                uiState = uiState,
                                filteredChapters = filteredChapters,
                                listState = listState,
                                scope = scope
                            )
                        }

                        // Scroll to top FAB
                        ScrollToTopFab(
                            listState = listState,
                            uiState = uiState,
                            scope = scope
                        )
                    }
                }
            }
        }
    }
}

// ================================================================
// SCROLL TO TOP FAB
// ================================================================

@Composable
private fun ScrollToTopFab(
    listState: LazyListState,
    uiState: DetailsUiState,
    scope: CoroutineScope
) {
    val showScrollToTop by remember {
        derivedStateOf {
            uiState.selectedTab == DetailsTab.CHAPTERS &&
                    uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL &&
                    listState.firstVisibleItemIndex > 10 &&
                    !uiState.isSelectionMode
        }
    }

    AnimatedVisibility(
        visible = showScrollToTop,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}

// ================================================================
// DIALOGS AND BOTTOM SHEETS CONTAINER
// ================================================================

@Composable
private fun DetailsDialogs(
    uiState: DetailsUiState,
    downloadState: DownloadState,
    viewModel: DetailsViewModel,
    context: android.content.Context,
    novelUrl: String
) {
    if (uiState.showCoverZoom && uiState.novelDetails?.posterUrl != null) {
        CoverZoomDialog(
            imageUrl = uiState.novelDetails.posterUrl!!,
            title = uiState.novelDetails.name,
            onDismiss = { viewModel.hideCoverZoom() }
        )
    }

    if (uiState.showDownloadMenu) {
        val totalChapters = uiState.novelDetails?.chapters?.size ?: 0
        val downloadedCount = uiState.downloadedChapters.size
        val undownloadedCount = (totalChapters - downloadedCount).coerceAtLeast(0)

        val unreadCount = uiState.novelDetails?.chapters?.count { chapter ->
            !uiState.readChapters.contains(chapter.url) &&
                    !uiState.downloadedChapters.contains(chapter.url)
        } ?: 0

        DownloadBottomSheet(
            novelUrl = novelUrl,
            isDownloading = downloadState.isActive,
            isPaused = downloadState.isPaused,
            downloadProgress = downloadState.progressPercent,
            currentProgress = downloadState.currentProgress,
            totalChapters = downloadState.totalChapters,
            totalChapterCount = totalChapters,
            undownloadedCount = undownloadedCount,
            unreadCount = unreadCount,
            downloadSpeed = downloadState.formattedSpeed,
            estimatedTime = downloadState.estimatedTimeRemaining,
            activeNovelUrl = downloadState.novelUrl,
            activeNovelName = downloadState.novelName,
            activeChapterName = downloadState.currentChapterName,
            queuedDownloads = downloadState.queuedDownloads,
            onDismiss = { viewModel.hideDownloadMenu() },
            onDownloadAll = { viewModel.downloadAll(context) },
            onDownloadNext = { count -> viewModel.downloadNextN(context, count) },
            onDownloadUnread = { viewModel.downloadUnread(context) },
            onSelectChapters = {
                viewModel.hideDownloadMenu()
                viewModel.enableSelectionMode()
            },
            onPause = { DownloadServiceManager.pauseDownload() },
            onResume = { DownloadServiceManager.resumeDownload() },
            onCancel = { DownloadServiceManager.cancelDownload() },
            onRemoveFromQueue = { url -> DownloadServiceManager.removeFromQueue(url) },
            onClearQueue = { DownloadServiceManager.clearQueue() }
        )
    }

    if (uiState.showStatusMenu) {
        StatusBottomSheet(
            currentStatus = uiState.readingStatus,
            onStatusSelected = { viewModel.updateReadingStatus(it) },
            onDismiss = { viewModel.hideStatusMenu() }
        )
    }
}

// ================================================================
// MAIN CONTENT WITH STICKY TABS
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsContent(
    listState: LazyListState,
    uiState: DetailsUiState,
    details: NovelDetails,
    filteredChapters: List<Chapter>,
    displayedChapters: List<Chapter>,
    isDownloadingThisNovel: Boolean,
    downloadProgress: Float,
    novelUrl: String,
    providerName: String,
    onBack: () -> Unit,
    onChapterClick: (String, String, String) -> Unit,
    onNovelClick: (String, String) -> Unit,
    onOpenInWebView: (String, String) -> Unit,
    onHapticFeedback: (HapticFeedbackType) -> Unit,
    onTabSelected: (DetailsTab) -> Unit,
    viewModel: DetailsViewModel,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = if (uiState.isSelectionMode) 200.dp else 0.dp
        )
    ) {
        // ============================================================
        // HEADER SECTION (scrolls normally)
        // ============================================================

        // Header with cover and info
        item(key = "header") {
            NovelHeader(
                details = details,
                providerName = providerName,
                isFavorite = uiState.isFavorite,
                readingStatus = uiState.readingStatus,
                readProgress = uiState.readProgress,
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite() },
                onStatusClick = { viewModel.showStatusMenu() },
                onCoverClick = { viewModel.showCoverZoom() },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, details.name)
                        putExtra(Intent.EXTRA_TEXT, "${details.name}\n${details.url}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share novel"))
                },
                onOpenInWebView = {
                    onOpenInWebView(providerName, details.url)
                }
            )
        }

        // Action buttons
        item(key = "actions") {
            ActionButtonsRow(
                hasStartedReading = uiState.hasStartedReading,
                lastReadChapterName = uiState.lastReadChapterName,
                isDownloading = isDownloadingThisNovel,
                downloadProgress = downloadProgress,
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
                peopleVoted = details.peopleVoted,
                views = details.views
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

        // ============================================================
        // STICKY TAB BAR
        // ============================================================

        stickyHeader(key = "tabs") {
            DetailsTabRow(
                modifier = Modifier.statusBarsPadding(),
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
                chapterCount = details.chapters.size,
                relatedCount = uiState.relatedNovels.size,
                reviewCount = uiState.reviews.size,
                hasReviewsSupport = uiState.hasReviewsSupport
            )
        }

        // ============================================================
        // TAB CONTENT
        // ============================================================

        when (uiState.selectedTab) {
            DetailsTab.CHAPTERS -> {
                chaptersTabContent(
                    uiState = uiState,
                    filteredChapters = filteredChapters,
                    displayedChapters = displayedChapters,
                    novelUrl = novelUrl,
                    providerName = providerName,
                    onChapterClick = onChapterClick,
                    onHapticFeedback = onHapticFeedback,
                    viewModel = viewModel,
                    scope = scope,
                    listState = listState,
                    context = context
                )
            }

            DetailsTab.RELATED -> {
                relatedTabContent(
                    novels = uiState.relatedNovels,
                    onNovelClick = onNovelClick
                )
            }

            DetailsTab.REVIEWS -> {
                reviewsTabContent(
                    reviews = uiState.reviews,
                    isLoading = uiState.isLoadingReviews,
                    hasMore = uiState.hasMoreReviews,
                    showSpoilers = uiState.showSpoilers,
                    onLoadMore = { viewModel.loadMoreReviews() },
                    onToggleSpoilers = { viewModel.toggleSpoilers() }
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

// ================================================================
// CHAPTERS TAB CONTENT (LazyListScope extension)
// ================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.chaptersTabContent(
    uiState: DetailsUiState,
    filteredChapters: List<Chapter>,
    displayedChapters: List<Chapter>,
    novelUrl: String,
    providerName: String,
    onChapterClick: (String, String, String) -> Unit,
    onHapticFeedback: (HapticFeedbackType) -> Unit,
    viewModel: DetailsViewModel,
    scope: CoroutineScope,
    listState: LazyListState,
    context: android.content.Context
) {
    // Chapter list header with filters and display options
    item(key = "chapter_header") {
        ChapterListHeader(
            chapterCount = uiState.novelDetails?.chapters?.size ?: 0,
            filteredCount = filteredChapters.size,
            isDescending = uiState.isChapterSortDescending,
            isSelectionMode = uiState.isSelectionMode,
            currentFilter = uiState.chapterFilter,
            isSearchActive = uiState.isSearchActive,
            searchQuery = uiState.chapterSearchQuery,
            unreadCount = uiState.unreadCount,
            downloadedCount = uiState.downloadedCount,
            notDownloadedCount = uiState.notDownloadedCount,
            displayMode = uiState.chapterDisplayMode,
            paginationState = uiState.paginationState,
            onToggleSort = { viewModel.toggleChapterSort() },
            onFilterChange = { viewModel.setChapterFilter(it) },
            onToggleSearch = { viewModel.toggleSearch() },
            onSearchQueryChange = { viewModel.setChapterSearchQuery(it) },
            onEnableSelection = { viewModel.enableSelectionMode() },
            onDisplayModeChange = { viewModel.setChapterDisplayMode(it) },
            onChaptersPerPageChange = { viewModel.setChaptersPerPage(it) },
            onJumpToFirstUnread = {
                scope.launch {
                    val index = viewModel.jumpToFirstUnread()
                    if (uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL && index != null) {
                        val headerCount = calculateHeaderItemCount(uiState)
                        listState.animateScrollToItem(headerCount + index)
                    }
                }
            },
            onJumpToLastRead = {
                scope.launch {
                    val index = viewModel.jumpToLastRead()
                    if (uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL && index != null) {
                        val headerCount = calculateHeaderItemCount(uiState)
                        listState.animateScrollToItem(headerCount + index)
                    }
                }
            }
        )
    }

    // Pagination controls (top) for paginated mode
    if (uiState.chapterDisplayMode == ChapterDisplayMode.PAGINATED && filteredChapters.isNotEmpty()) {
        item(key = "pagination_top") {
            PaginationControls(
                paginationState = uiState.paginationState,
                totalChapters = filteredChapters.size,
                onPageChange = { viewModel.setCurrentPage(it) }
            )
        }
    }

    // Chapter list or empty message
    if (displayedChapters.isEmpty()) {
        item(key = "empty_chapters") {
            EmptyChaptersMessage(
                filter = uiState.chapterFilter,
                hasSearch = uiState.chapterSearchQuery.isNotBlank()
            )
        }
    } else {
        itemsIndexed(
            items = displayedChapters,
            key = { _, chapter -> "chapter_${chapter.url}" }
        ) { displayIndex, chapter ->
            // Calculate the actual index in the full filtered list
            val actualIndex = when (uiState.chapterDisplayMode) {
                ChapterDisplayMode.SCROLL -> displayIndex
                ChapterDisplayMode.PAGINATED -> {
                    uiState.paginationState.getPageRange(filteredChapters.size).first + displayIndex
                }
            }

            val isRead = uiState.readChapters.contains(chapter.url)
            val isDownloaded = uiState.downloadedChapters.contains(chapter.url)

            ChapterItem(
                chapter = chapter,
                index = actualIndex,
                isRead = isRead,
                isDownloaded = isDownloaded,
                isLastRead = chapter.url == uiState.lastReadChapterUrl,
                isSelectionMode = uiState.isSelectionMode,
                isSelected = uiState.selectedChapters.contains(chapter.url),
                onTap = {
                    if (uiState.isSelectionMode) {
                        onHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleChapterSelection(actualIndex, chapter.url)
                    } else {
                        onChapterClick(chapter.url, novelUrl, providerName)
                    }
                },
                onLongPress = {
                    onHapticFeedback(HapticFeedbackType.LongPress)
                    if (uiState.isSelectionMode) {
                        viewModel.selectRange(actualIndex)
                    } else {
                        viewModel.enableSelectionMode(chapter.url)
                    }
                },
                onSwipeToRead = {
                    viewModel.toggleChapterReadStatus(chapter.url, isRead)
                },
                onSwipeToDownload = {
                    if (isDownloaded) {
                        viewModel.deleteChapterDownload(chapter.url)
                    } else {
                        viewModel.downloadSingleChapter(context, chapter)
                    }
                }
            )
        }
    }

    // Pagination controls (bottom) for paginated mode
    if (uiState.chapterDisplayMode == ChapterDisplayMode.PAGINATED && filteredChapters.isNotEmpty()) {
        item(key = "pagination_bottom") {
            PaginationControls(
                paginationState = uiState.paginationState,
                totalChapters = filteredChapters.size,
                onPageChange = { viewModel.setCurrentPage(it) }
            )
        }
    }
}

// ================================================================
// RELATED TAB CONTENT (LazyListScope extension)
// ================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.relatedTabContent(
    novels: List<Novel>,
    onNovelClick: (novelUrl: String, providerName: String) -> Unit
) {
    if (novels.isEmpty()) {
        item(key = "empty_related") {
            EmptyRelatedMessage()
        }
    } else {
        val rows = novels.chunked(2)
        itemsIndexed(
            items = rows,
            key = { index, _ -> "related_row_$index" }
        ) { _, rowNovels ->
            RelatedNovelRow(
                novels = rowNovels,
                onNovelClick = onNovelClick
            )
        }
    }
}

// ================================================================
// REVIEWS TAB CONTENT (LazyListScope extension)
// ================================================================

private fun androidx.compose.foundation.lazy.LazyListScope.reviewsTabContent(
    reviews: List<UserReview>,
    isLoading: Boolean,
    hasMore: Boolean,
    showSpoilers: Boolean,
    onLoadMore: () -> Unit,
    onToggleSpoilers: () -> Unit
) {
    item(key = "reviews_header") {
        ReviewsHeader(
            reviewCount = reviews.size,
            showSpoilers = showSpoilers,
            onToggleSpoilers = onToggleSpoilers
        )
    }

    if (reviews.isEmpty() && !isLoading) {
        item(key = "empty_reviews") {
            EmptyReviewsMessage()
        }
    } else {
        itemsIndexed(
            items = reviews,
            key = { index, review -> "review_${index}_${review.username}_${review.time}" }
        ) { _, review ->
            ReviewCard(
                review = review,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    if (isLoading) {
        item(key = "reviews_loading") {
            ReviewsLoadingIndicator()
        }
    }

    if (!isLoading && hasMore && reviews.isNotEmpty()) {
        item(key = "reviews_load_more") {
            LoadMoreReviewsButton(onClick = onLoadMore)
        }
    }
}

// ================================================================
// FLOATING SCROLL BUTTON CONTAINER
// ================================================================

@Composable
private fun FloatingScrollButtonContainer(
    uiState: DetailsUiState,
    filteredChapters: List<Chapter>,
    listState: LazyListState,
    scope: CoroutineScope
) {
    val showButton = uiState.hasStartedReading &&
            uiState.lastReadChapterIndex >= 0 &&
            !uiState.isSelectionMode &&
            uiState.chapterDisplayMode == ChapterDisplayMode.SCROLL

    if (showButton) {
        val lastReadInFiltered = filteredChapters.indexOfFirst {
            it.url == uiState.lastReadChapterUrl
        }

        if (lastReadInFiltered >= 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingScrollButton(
                    onClick = {
                        scope.launch {
                            val headerItemCount = calculateHeaderItemCount(uiState)
                            listState.animateScrollToItem(headerItemCount + lastReadInFiltered)
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Calculate the number of items before chapters in the LazyColumn
 */
private fun calculateHeaderItemCount(uiState: DetailsUiState): Int {
    var count = 1 // header
    count++ // actions
    count++ // stats
    if (!uiState.novelDetails?.synopsis.isNullOrBlank()) count++ // synopsis
    if (!uiState.novelDetails?.tags.isNullOrEmpty()) count++ // tags
    count++ // sticky tabs
    count++ // chapter header

    if (uiState.chapterDisplayMode == ChapterDisplayMode.PAGINATED &&
        (uiState.filteredChapters.isNotEmpty())
    ) {
        count++ // pagination_top
    }

    return count
}