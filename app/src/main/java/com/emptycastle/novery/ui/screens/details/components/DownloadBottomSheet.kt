package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emptycastle.novery.service.QueuedDownload

// ================================================================
// DATA CLASSES
// ================================================================

data class DownloadSheetState(
    // This novel's info
    val novelUrl: String = "",
    val totalChapterCount: Int = 0,
    val undownloadedCount: Int = 0,
    val unreadCount: Int = 0,

    // Global download state
    val isAnyDownloadActive: Boolean = false,
    val activeDownloadNovelUrl: String = "",
    val activeDownloadNovelName: String = "",
    val activeDownloadChapterName: String = "",
    val downloadProgress: Float = 0f,
    val currentProgress: Int = 0,
    val totalChapters: Int = 0,
    val isPaused: Boolean = false,
    val downloadSpeed: String = "",
    val estimatedTime: String = "",

    // Queue info
    val queuedDownloads: List<QueuedDownload> = emptyList()
) {
    // Computed properties
    val isThisNovelDownloading: Boolean
        get() = isAnyDownloadActive && activeDownloadNovelUrl == novelUrl

    val isThisNovelQueued: Boolean
        get() = queuedDownloads.any { it.novelUrl == novelUrl }

    val thisNovelQueuePosition: Int
        get() = queuedDownloads.indexOfFirst { it.novelUrl == novelUrl } + 1

    val canDownload: Boolean
        get() = undownloadedCount > 0 && !isThisNovelDownloading && !isThisNovelQueued

    val hasUndownloaded: Boolean
        get() = undownloadedCount > 0

    val hasUnread: Boolean
        get() = unreadCount > 0
}

data class DownloadSheetCallbacks(
    val onDismiss: () -> Unit,
    val onDownloadAll: () -> Unit,
    val onDownloadNext: (Int) -> Unit,
    val onDownloadUnread: () -> Unit,
    val onSelectChapters: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onCancel: () -> Unit,
    val onRemoveFromQueue: (String) -> Unit = {},
    val onClearQueue: () -> Unit = {}
)

// ================================================================
// MAIN BOTTOM SHEET
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadBottomSheet(
    state: DownloadSheetState,
    callbacks: DownloadSheetCallbacks,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomDialog by remember { mutableStateOf(false) }

    // Custom amount dialog
    if (showCustomDialog) {
        CustomAmountDialog(
            maxAmount = state.undownloadedCount,
            onConfirm = { amount ->
                callbacks.onDownloadNext(amount)
                showCustomDialog = false
                callbacks.onDismiss()
            },
            onDismiss = { showCustomDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = callbacks.onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            item(key = "header") {
                DownloadSheetHeader(state = state)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Content based on state
            when {
                // This novel is currently downloading
                state.isThisNovelDownloading -> {
                    item(key = "active_download") {
                        ActiveDownloadCard(
                            state = state,
                            onPause = callbacks.onPause,
                            onResume = callbacks.onResume,
                            onCancel = {
                                callbacks.onCancel()
                                callbacks.onDismiss()
                            }
                        )
                    }

                    // Show queue if not empty
                    if (state.queuedDownloads.isNotEmpty()) {
                        item(key = "queue_section") {
                            Spacer(modifier = Modifier.height(20.dp))
                            QueueSection(
                                queuedDownloads = state.queuedDownloads,
                                onRemoveFromQueue = callbacks.onRemoveFromQueue,
                                onClearQueue = callbacks.onClearQueue
                            )
                        }
                    }
                }

                // This novel is in the queue
                state.isThisNovelQueued -> {
                    item(key = "queued_status") {
                        QueuedStatusCard(
                            queuePosition = state.thisNovelQueuePosition,
                            totalInQueue = state.queuedDownloads.size,
                            onRemoveFromQueue = {
                                callbacks.onRemoveFromQueue(state.novelUrl)
                                callbacks.onDismiss()
                            }
                        )
                    }

                    // Show current download info
                    if (state.isAnyDownloadActive) {
                        item(key = "current_download_info") {
                            Spacer(modifier = Modifier.height(16.dp))
                            CurrentDownloadInfoCard(
                                novelName = state.activeDownloadNovelName,
                                progress = state.downloadProgress,
                                currentProgress = state.currentProgress,
                                totalChapters = state.totalChapters
                            )
                        }
                    }
                }

                // No download active for this novel - show options
                else -> {
                    // If another download is active, show info banner
                    if (state.isAnyDownloadActive) {
                        item(key = "other_download_banner") {
                            OtherDownloadBanner(
                                novelName = state.activeDownloadNovelName,
                                progress = state.downloadProgress
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item(key = "download_options") {
                        DownloadOptionsContent(
                            state = state,
                            willQueue = state.isAnyDownloadActive,
                            onDownloadAll = {
                                callbacks.onDownloadAll()
                                callbacks.onDismiss()
                            },
                            onDownloadNext = { count ->
                                callbacks.onDownloadNext(count)
                                callbacks.onDismiss()
                            },
                            onDownloadUnread = {
                                callbacks.onDownloadUnread()
                                callbacks.onDismiss()
                            },
                            onSelectChapters = {
                                callbacks.onSelectChapters()
                                callbacks.onDismiss()
                            },
                            onCustomAmount = { showCustomDialog = true }
                        )
                    }
                }
            }
        }
    }
}

// ================================================================
// BACKWARD COMPATIBILITY OVERLOAD
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadBottomSheet(
    novelUrl: String,
    isDownloading: Boolean,
    isPaused: Boolean,
    downloadProgress: Float,
    currentProgress: Int,
    totalChapters: Int,
    totalChapterCount: Int = 0,
    undownloadedCount: Int = 0,
    unreadCount: Int = 0,
    downloadSpeed: String = "",
    estimatedTime: String = "",
    activeNovelUrl: String = "",
    activeNovelName: String = "",
    activeChapterName: String = "",
    queuedDownloads: List<QueuedDownload> = emptyList(),
    onDismiss: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadNext: (Int) -> Unit,
    onDownloadUnread: () -> Unit,
    onSelectChapters: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemoveFromQueue: (String) -> Unit = {},
    onClearQueue: () -> Unit = {}
) {
    DownloadBottomSheet(
        state = DownloadSheetState(
            novelUrl = novelUrl,
            totalChapterCount = totalChapterCount,
            undownloadedCount = undownloadedCount,
            unreadCount = unreadCount,
            isAnyDownloadActive = isDownloading,
            activeDownloadNovelUrl = activeNovelUrl,
            activeDownloadNovelName = activeNovelName,
            activeDownloadChapterName = activeChapterName,
            downloadProgress = downloadProgress,
            currentProgress = currentProgress,
            totalChapters = totalChapters,
            isPaused = isPaused,
            downloadSpeed = downloadSpeed,
            estimatedTime = estimatedTime,
            queuedDownloads = queuedDownloads
        ),
        callbacks = DownloadSheetCallbacks(
            onDismiss = onDismiss,
            onDownloadAll = onDownloadAll,
            onDownloadNext = onDownloadNext,
            onDownloadUnread = onDownloadUnread,
            onSelectChapters = onSelectChapters,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
            onRemoveFromQueue = onRemoveFromQueue,
            onClearQueue = onClearQueue
        )
    )
}

// ================================================================
// HEADER
// ================================================================

@Composable
private fun DownloadSheetHeader(
    state: DownloadSheetState,
    modifier: Modifier = Modifier
) {
    val isActive = state.isThisNovelDownloading

    val iconColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary
            state.isThisNovelQueued -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(300),
        label = "header_icon_color"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isActive -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = iconColor
                        )
                    }
                    state.isThisNovelQueued -> {
                        Icon(
                            imageVector = Icons.Rounded.HourglassTop,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    isActive -> "Downloading..."
                    state.isThisNovelQueued -> "Queued"
                    else -> "Download Chapters"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    isActive && state.queuedDownloads.isNotEmpty() ->
                        "${state.queuedDownloads.size} more in queue"
                    isActive -> "Download in progress"
                    state.isThisNovelQueued ->
                        "Position ${state.thisNovelQueuePosition} of ${state.queuedDownloads.size + 1}"
                    state.isAnyDownloadActive -> "Will be added to queue"
                    else -> "Save chapters for offline reading"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================================================================
// ACTIVE DOWNLOAD CARD (This Novel)
// ================================================================

@Composable
private fun ActiveDownloadCard(
    state: DownloadSheetState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = MaterialTheme.colorScheme.primary
    val animatedProgress by animateFloatAsState(
        targetValue = state.downloadProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "download_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status and count row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isPaused) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                    Text(
                        text = if (state.isPaused) "Paused" else "Downloading",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${state.currentProgress} / ${state.totalChapters}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Current chapter name
            if (state.activeDownloadChapterName.isNotBlank()) {
                Text(
                    text = state.activeDownloadChapterName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        progressColor,
                                        progressColor.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.3f),
                                    radius = size.height * 0.8f,
                                    center = Offset(size.width - 4.dp.toPx(), size.height / 2)
                                )
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.downloadSpeed.isNotBlank() && state.downloadSpeed != "--" && !state.isPaused) {
                            Text(
                                text = state.downloadSpeed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (state.estimatedTime.isNotBlank() && state.estimatedTime != "--:--" && !state.isPaused) {
                            Text(
                                text = state.estimatedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Resume", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Rounded.Pause, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pause", fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ================================================================
// QUEUED STATUS CARD
// ================================================================

@Composable
private fun QueuedStatusCard(
    queuePosition: Int,
    totalInQueue: Int,
    onRemoveFromQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "#$queuePosition",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "In Download Queue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Waiting for $queuePosition download${if (queuePosition > 1) "s" else ""} to complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onRemoveFromQueue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Close, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remove from Queue", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ================================================================
// CURRENT DOWNLOAD INFO (When viewing queued novel)
// ================================================================

@Composable
private fun CurrentDownloadInfoCard(
    novelName: String,
    progress: Float,
    currentProgress: Int,
    totalChapters: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Currently Downloading",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$currentProgress/$totalChapters",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// ================================================================
// OTHER DOWNLOAD BANNER (Shown when adding to queue)
// ================================================================

@Composable
private fun OtherDownloadBanner(
    novelName: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Queue,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Another download in progress",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$novelName (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ================================================================
// DOWNLOAD OPTIONS CONTENT
// ================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadOptionsContent(
    state: DownloadSheetState,
    willQueue: Boolean,
    onDownloadAll: () -> Unit,
    onDownloadNext: (Int) -> Unit,
    onDownloadUnread: () -> Unit,
    onSelectChapters: () -> Unit,
    onCustomAmount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = if (willQueue) "Add to Queue" else "Download"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Quick download chips
        if (state.hasUndownloaded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (willQueue) "Quick Add to Queue" else "Quick Download",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.undownloadedCount >= 10) {
                        QuickDownloadChip(
                            label = "Next 10",
                            onClick = { onDownloadNext(10) }
                        )
                    }
                    if (state.undownloadedCount >= 25) {
                        QuickDownloadChip(
                            label = "Next 25",
                            onClick = { onDownloadNext(25) }
                        )
                    }
                    if (state.undownloadedCount >= 50) {
                        QuickDownloadChip(
                            label = "Next 50",
                            onClick = { onDownloadNext(50) }
                        )
                    }
                    if (state.undownloadedCount >= 100) {
                        QuickDownloadChip(
                            label = "Next 100",
                            onClick = { onDownloadNext(100) }
                        )
                    }
                    QuickDownloadChip(
                        label = "Custom",
                        icon = Icons.Rounded.Edit,
                        onClick = onCustomAmount
                    )
                }
            }
        }

        // Main download options
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DownloadOptionItem(
                icon = Icons.Rounded.AllInclusive,
                title = if (willQueue) "Queue All" else "Download All",
                subtitle = if (state.hasUndownloaded) {
                    "${state.undownloadedCount} chapters remaining"
                } else {
                    "All chapters already downloaded"
                },
                enabled = state.hasUndownloaded,
                onClick = onDownloadAll
            )

            DownloadOptionItem(
                icon = Icons.Outlined.VisibilityOff,
                title = if (willQueue) "Queue Unread" else "Download Unread",
                subtitle = if (state.hasUnread) {
                    "${state.unreadCount} unread chapters"
                } else {
                    "No unread chapters"
                },
                enabled = state.hasUnread,
                onClick = onDownloadUnread
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            DownloadOptionItem(
                icon = Icons.Outlined.CheckBox,
                title = "Select Chapters",
                subtitle = "Choose specific chapters to download",
                enabled = true,  // Always enabled
                onClick = onSelectChapters
            )
        }

        // Info text
        if (!state.hasUndownloaded) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ All ${state.totalChapterCount} chapters are already downloaded",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ================================================================
// QUEUE SECTION
// ================================================================

@Composable
private fun QueueSection(
    queuedDownloads: List<QueuedDownload>,
    onRemoveFromQueue: (String) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Queue,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Queue (${queuedDownloads.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (queuedDownloads.size > 1) {
                Surface(
                    onClick = onClearQueue,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Clear All",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        queuedDownloads.take(5).forEachIndexed { index, item ->
            QueueItem(
                position = index + 1,
                download = item,
                onRemove = { onRemoveFromQueue(item.novelUrl) }
            )
        }

        if (queuedDownloads.size > 5) {
            Text(
                text = "+${queuedDownloads.size - 5} more...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun QueueItem(
    position: Int,
    download: QueuedDownload,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${download.chapterCount} chapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove from queue",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ================================================================
// REUSABLE COMPONENTS
// ================================================================

@Composable
private fun QuickDownloadChip(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chip_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon ?: Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DownloadOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "option_scale"
    )

    val contentAlpha = if (enabled) 1f else 0.5f

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f * contentAlpha),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f * contentAlpha)
            )
        }
    }
}

// ================================================================
// CUSTOM AMOUNT DIALOG
// ================================================================

@Composable
private fun CustomAmountDialog(
    maxAmount: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val inputValue by remember(inputText) {
        derivedStateOf { inputText.toIntOrNull()?.coerceIn(1, maxAmount) }
    }

    val isValid = inputValue != null && inputValue!! > 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Download Custom Amount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter the number of chapters to download",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            inputText = newValue.take(5)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Number of chapters") },
                    placeholder = { Text("e.g., 50") },
                    supportingText = { Text("Available: $maxAmount chapters") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (isValid) onConfirm(inputValue!!)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 50, 100).forEach { amount ->
                        if (amount <= maxAmount) {
                            Surface(
                                onClick = { inputText = amount.toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (inputText == amount.toString()) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = amount.toString(),
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = if (inputText == amount.toString()) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { inputValue?.let { onConfirm(it) } },
                        modifier = Modifier.weight(1f),
                        enabled = isValid,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isValid) "Download $inputValue" else "Download",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}