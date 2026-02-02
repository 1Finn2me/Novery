package com.emptycastle.novery.ui.screens.notification

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.emptycastle.novery.ui.components.SwipeDeleteState
import com.emptycastle.novery.ui.components.TwoStageSwipeToDelete

// ============================================================================
// Colors
// ============================================================================

private object NotificationColors {
    val NewChapters = Color(0xFF10B981)
    val NewChaptersLight = Color(0xFF34D399)
    val NewChaptersDark = Color(0xFF059669)
    val Download = Color(0xFF3B82F6)
    val DownloadLight = Color(0xFF60A5FA)
    val Continue = Color(0xFFE85609)
    val ContinueLight = Color(0xFFF97316)
    val MarkSeen = Color(0xFF8B5CF6)
    val MarkSeenLight = Color(0xFFA78BFA)
    val Acknowledged = Color(0xFF6B7280)
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

    // Clear confirmation dialog
    if (uiState.showClearConfirmation) {
        ClearNotificationsDialog(
            itemCount = uiState.totalNovelsCount,
            onConfirm = viewModel::confirmClearAll,
            onDismiss = viewModel::dismissClearConfirmation
        )
    }

    Scaffold(
        topBar = {
            NotificationTopBar(
                onNavigateBack = onNavigateBack,
                onClearAll = if (uiState.displayItems.isNotEmpty()) viewModel::requestClearAll else null
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
            uiState.displayItems.isEmpty() -> {
                NotificationEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                NotificationContent(
                    uiState = uiState,
                    onDownload = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.downloadNewChapters(context, item)
                    },
                    onDownloadAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.downloadAllNewChapters(context)
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
                    onMarkAsSeen = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.markAsSeen(item.novel.url)
                    },
                    onMarkAllSeen = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.markAllAsSeen()
                    },
                    onRemoveFromNotifications = { item ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.removeFromNotifications(item.novel.url)
                    },
                    onNovelClick = { item ->
                        // Don't auto-mark as seen on click - let user decide
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
    onNavigateBack: () -> Unit,
    onClearAll: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Text(
                text = "Updates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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
            if (onClearAll != null) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.error
                    )
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
    uiState: NotificationUiState,
    onDownload: (LibraryItem) -> Unit,
    onDownloadAll: () -> Unit,
    onContinue: (LibraryItem) -> Unit,
    onMarkAsSeen: (LibraryItem) -> Unit,
    onMarkAllSeen: () -> Unit,
    onRemoveFromNotifications: (LibraryItem) -> Unit,
    onNovelClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card (only show if there are unacknowledged items)
        if (uiState.unacknowledgedCount > 0) {
            item(key = "summary_header") {
                UpdatesSummaryCard(
                    totalNewChapters = uiState.totalNewChapters,
                    novelsCount = uiState.unacknowledgedCount,
                    isDownloadingAll = uiState.isDownloadingAll,
                    isMarkingAllSeen = uiState.isMarkingAllSeen,
                    onDownloadAll = onDownloadAll,
                    onMarkAllSeen = onMarkAllSeen,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // Section Header
        item(key = "section_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.unacknowledgedCount > 0) "Recent Updates" else "Update History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show unacknowledged count if any
                    if (uiState.unacknowledgedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NotificationColors.NewChapters.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${uiState.unacknowledgedCount} new",
                                style = MaterialTheme.typography.labelMedium,
                                color = NotificationColors.NewChapters,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "${uiState.totalNovelsCount} total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Swipe hint (show only once at top)
        item(key = "swipe_hint") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "← Swipe left to remove from list",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Notification Items with swipe to delete
        itemsIndexed(
            items = uiState.displayItems,
            key = { _, item -> item.libraryItem.novel.url }
        ) { index, displayItem ->
            TwoStageSwipeToDelete(
                onDelete = { onRemoveFromNotifications(displayItem.libraryItem) },
                deleteButtonWidth = 80.dp,
                shape = RoundedCornerShape(20.dp)
            ) { swipeState, onResetSwipe ->
                NotificationItemCard(
                    displayItem = displayItem,
                    isDownloading = uiState.downloadingNovelUrls.contains(displayItem.libraryItem.novel.url),
                    swipeState = swipeState,
                    onDownload = { onDownload(displayItem.libraryItem) },
                    onContinue = { onContinue(displayItem.libraryItem) },
                    onMarkAsSeen = { onMarkAsSeen(displayItem.libraryItem) },
                    onClick = {
                        if (swipeState == SwipeDeleteState.Primed) {
                            onResetSwipe()
                        } else {
                            onNovelClick(displayItem.libraryItem)
                        }
                    }
                )
            }
        }

        // Bottom spacer
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// Notification Item Card
// ============================================================================

@Composable
private fun NotificationItemCard(
    displayItem: NotificationDisplayItem,
    isDownloading: Boolean,
    swipeState: SwipeDeleteState,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    onMarkAsSeen: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = displayItem.libraryItem
    val isNew = displayItem.isNew

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    // Visual feedback when primed for deletion
    val cardColor by animateColorAsState(
        targetValue = when {
            swipeState == SwipeDeleteState.Primed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isNew -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f)
        },
        animationSpec = tween(200),
        label = "card_color"
    )

    val borderColor = if (isNew && swipeState != SwipeDeleteState.Primed) {
        NotificationColors.NewChapters.copy(alpha = 0.3f)
    } else if (swipeState == SwipeDeleteState.Primed) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNew) 2.dp else 1.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Cover, Info, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Cover Image with Badge overlay
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.novel.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.novel.name,
                        modifier = Modifier
                            .size(width = 64.dp, height = 90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .alpha(if (isNew) 1f else 0.7f),
                        contentScale = ContentScale.Crop
                    )

                    // Badge overlay - different style for new vs seen
                    if (item.newChapterCount > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isNew) NotificationColors.NewChapters else NotificationColors.Acknowledged,
                            shadowElevation = if (isNew) 4.dp else 0.dp
                        ) {
                            Text(
                                text = "+${item.newChapterCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // "Seen" indicator for acknowledged items
                    if (!isNew) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp),
                            shape = CircleShape,
                            color = NotificationColors.Acknowledged
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "Seen",
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(3.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Title and Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Novel Title
                    Text(
                        text = item.novel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isNew) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        lineHeight = 22.sp
                    )

                    // Provider Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = item.novel.apiName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // New chapters info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isNew) NotificationColors.NewChapters else NotificationColors.Acknowledged
                        )
                        Text(
                            text = "${item.newChapterCount} new chapter${if (item.newChapterCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isNew) NotificationColors.NewChapters else NotificationColors.Acknowledged
                        )

                        if (!isNew) {
                            Text(
                                text = "• Seen",
                                style = MaterialTheme.typography.bodySmall,
                                color = NotificationColors.Acknowledged
                            )
                        }
                    }

                    // Last read info
                    item.lastReadPosition?.chapterName?.let { chapterName ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = chapterName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Action Buttons Row - hide when primed for delete
            if (swipeState != SwipeDeleteState.Primed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Continue Reading Button (Primary)
                    NotificationPrimaryButton(
                        text = if (item.lastReadPosition != null) "Continue" else "Start",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onContinue,
                        containerColor = NotificationColors.Continue,
                        modifier = Modifier.weight(1f)
                    )

                    // Download Button
                    NotificationSecondaryButton(
                        icon = Icons.Rounded.Download,
                        contentDescription = "Download new chapters",
                        onClick = onDownload,
                        isLoading = isDownloading,
                        containerColor = NotificationColors.Download.copy(alpha = 0.12f),
                        contentColor = NotificationColors.Download
                    )

                    // Mark as Seen Button (only show if still new)
                    if (isNew) {
                        NotificationSecondaryButton(
                            icon = Icons.Rounded.Visibility,
                            contentDescription = "Mark as seen",
                            onClick = onMarkAsSeen,
                            isLoading = false,
                            containerColor = NotificationColors.MarkSeen.copy(alpha = 0.12f),
                            contentColor = NotificationColors.MarkSeen
                        )
                    }
                }
            } else {
                // Show hint when primed
                Text(
                    text = "← Swipe again or tap delete to remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ... (keep all the existing helper composables: UpdatesSummaryCard, StatBadge,
// SummaryActionButton, NotificationPrimaryButton, NotificationSecondaryButton,
// NotificationEmptyState, NotificationLoadingState, skeleton composables)

// ============================================================================
// Clear Confirmation Dialog
// ============================================================================

@Composable
private fun ClearNotificationsDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Clear All Notifications?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "This will remove all $itemCount ${if (itemCount == 1) "novel" else "novels"} from your update notifications. This won't affect your library.",
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

// Keep all existing composables (UpdatesSummaryCard, StatBadge, etc.)
// ... (I'll include them for completeness)

@Composable
private fun UpdatesSummaryCard(
    totalNewChapters: Int,
    novelsCount: Int,
    isDownloadingAll: Boolean,
    isMarkingAllSeen: Boolean,
    onDownloadAll: () -> Unit,
    onMarkAllSeen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NotificationColors.NewChapters,
                            NotificationColors.NewChaptersDark
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(80.dp)
                    .scale(sparkleScale)
                    .graphicsLayer { alpha = 0.15f }
                    .background(Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 40.dp, bottom = 20.dp)
                    .size(40.dp)
                    .graphicsLayer { alpha = 0.1f }
                    .background(Color.White, CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "New Updates!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "You have new chapters waiting",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(sparkleScale),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBadge(
                        value = totalNewChapters.toString(),
                        label = "chapter${if (totalNewChapters != 1) "s" else ""}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        value = novelsCount.toString(),
                        label = "novel${if (novelsCount != 1) "s" else ""}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryActionButton(
                        text = "Download All",
                        icon = Icons.Rounded.CloudDownload,
                        isLoading = isDownloadingAll,
                        onClick = onDownloadAll,
                        containerColor = Color.White,
                        contentColor = NotificationColors.Download,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryActionButton(
                        text = "Mark Seen",
                        icon = Icons.Rounded.CheckCircle,
                        isLoading = isMarkingAllSeen,
                        onClick = onMarkAllSeen,
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (fadeIn() + slideInVertically { -it }) togetherWith
                            (fadeOut() + slideOutVertically { it })
                },
                label = "stat_value"
            ) { targetValue ->
                Text(
                    text = targetValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SummaryActionButton(
    text: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        interactionSource = interactionSource,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun NotificationPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun NotificationSecondaryButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        interactionSource = interactionSource,
        enabled = !isLoading
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
private fun NotificationEmptyState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = NotificationColors.NewChapters.copy(alpha = 0.08f),
                        modifier = Modifier.size(120.dp).scale(iconScale)
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No update notifications.\nCheck back later for new chapters!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pull to refresh in Library",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        // Skeleton for summary card
        item {
            NotificationSummarySkeleton()
        }

        // Section header skeleton
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }
        }

        // Skeleton items
        items(4) {
            NotificationItemSkeleton()
        }
    }
}

@Composable
private fun NotificationSummarySkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = shimmerAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = shimmerAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cover skeleton
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )

                // Info skeleton
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }

            // Buttons skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }
        }
    }
}