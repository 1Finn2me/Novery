package com.emptycastle.novery.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.data.backup.BackupData
import com.emptycastle.novery.data.backup.BackupManager
import com.emptycastle.novery.data.backup.BackupMetadata
import com.emptycastle.novery.data.backup.RestoreOptions
import com.emptycastle.novery.data.cache.CacheInfo
import com.emptycastle.novery.data.cache.CacheManager
import com.emptycastle.novery.data.cache.NovelDownloadInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Sort Order ─────────────────────────────────────────────────────────────────

private enum class DownloadSortOrder(val label: String) {
    SIZE_DESC("Largest first"),
    SIZE_ASC("Smallest first"),
    NAME_ASC("Name A–Z"),
    CHAPTERS_DESC("Most chapters")
}

// ─── Main Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    cacheManager: CacheManager,
    backupManager: BackupManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Cache state
    var cacheInfo by remember { mutableStateOf<CacheInfo?>(null) }
    var isLoadingCache by remember { mutableStateOf(true) }
    var novelDownloads by remember { mutableStateOf<List<NovelDownloadInfo>>(emptyList()) }
    var showDownloadsDetail by remember { mutableStateOf(false) }
    var downloadSortOrder by remember { mutableStateOf(DownloadSortOrder.SIZE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Backup/Restore state
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isLoadingMetadata by remember { mutableStateOf(false) }
    var showRestoreOptions by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var backupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var restoreOptions by remember { mutableStateOf(RestoreOptions()) }

    // Dialogs
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearNovelDialog by remember { mutableStateOf<NovelDownloadInfo?>(null) }

    // Sorted downloads
    val sortedDownloads = remember(novelDownloads, downloadSortOrder) {
        when (downloadSortOrder) {
            DownloadSortOrder.SIZE_DESC -> novelDownloads.sortedByDescending { it.sizeBytes }
            DownloadSortOrder.SIZE_ASC -> novelDownloads.sortedBy { it.sizeBytes }
            DownloadSortOrder.NAME_ASC -> novelDownloads.sortedBy { it.novelName.lowercase() }
            DownloadSortOrder.CHAPTERS_DESC -> novelDownloads.sortedByDescending { it.chapterCount }
        }
    }

    // Auto-initialize restore options when metadata is loaded
    LaunchedEffect(backupMetadata) {
        backupMetadata?.let { meta ->
            restoreOptions = RestoreOptions(
                restoreLibrary = meta.libraryCount > 0,
                restoreBookmarks = meta.bookmarkCount > 0,
                restoreHistory = meta.historyCount > 0,
                restoreStatistics = meta.hasStatistics,
                restoreSettings = meta.hasSettings,
                mergeWithExisting = true
            )
        }
    }

    // Refresh helper
    suspend fun refreshCacheInfo() {
        cacheInfo = cacheManager.getCacheInfo()
        novelDownloads = cacheManager.getNovelDownloads()
    }

    // File pickers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupData.MIME_TYPE)
    ) { uri ->
        uri?.let {
            scope.launch {
                isCreatingBackup = true
                val result = backupManager.exportToUri(it)
                isCreatingBackup = false
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Backup created successfully")
                } else {
                    snackbarHostState.showSnackbar(
                        "Failed to create backup: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            scope.launch {
                isLoadingMetadata = true
                val metadataResult = backupManager.parseBackupMetadata(it)
                isLoadingMetadata = false
                if (metadataResult.isSuccess) {
                    backupMetadata = metadataResult.getOrNull()
                    showRestoreOptions = true
                } else {
                    snackbarHostState.showSnackbar(
                        "Invalid backup file: ${metadataResult.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        isLoadingCache = true
        refreshCacheInfo()
        isLoadingCache = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Storage & Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoadingCache = true
                                refreshCacheInfo()
                                isLoadingCache = false
                            }
                        },
                        enabled = !isLoadingCache
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════ BACKUP & RESTORE ═══════════════

            item(key = "backup_header") {
                SectionHeader(title = "Backup & Restore", icon = Icons.Outlined.CloudUpload)
            }

            item(key = "backup_card") {
                BackupRestoreCard(
                    isCreatingBackup = isCreatingBackup,
                    isRestoring = isRestoring,
                    onCreateBackup = {
                        backupLauncher.launch(backupManager.generateBackupFileName())
                    },
                    onRestore = {
                        restoreLauncher.launch(
                            arrayOf(BackupData.MIME_TYPE, "application/json", "*/*")
                        )
                    }
                )
            }

            // ═══════════════ STORAGE ═══════════════

            item(key = "storage_header") {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "Storage", icon = Icons.Outlined.Storage)
            }

            item(key = "storage_overview") {
                AnimatedContent(
                    targetState = isLoadingCache,
                    transitionSpec = {
                        (fadeIn(tween(300)) togetherWith fadeOut(tween(200)))
                    },
                    label = "storage-loading"
                ) { loading ->
                    if (loading) {
                        StorageLoadingPlaceholder()
                    } else {
                        cacheInfo?.let { info ->
                            StorageOverviewCard(
                                cacheInfo = info,
                                onClearAll = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showClearCacheDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // ═══════════════ CACHE CATEGORIES ═══════════════

            cacheInfo?.let { info ->
                // Downloaded Chapters
                item(key = "downloads_category") {
                    CacheCategoryCard(
                        title = "Downloaded Chapters",
                        subtitle = "${info.downloadedChapters.itemCount} chapters · " +
                                "${info.downloadedChapters.novelCount} novels",
                        size = info.downloadedChapters.formattedSize(),
                        icon = Icons.Outlined.DownloadDone,
                        onClick = { showDownloadsDetail = !showDownloadsDetail },
                        onClear = if (info.downloadedChapters.sizeBytes > 0) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearDownloadsDialog = true
                            }
                        } else null,
                        expanded = showDownloadsDetail
                    )
                }

                // Animated downloads detail
                item(key = "downloads_detail") {
                    AnimatedVisibility(
                        visible = showDownloadsDetail,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(tween(200)),
                        exit = shrinkVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut(tween(150))
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (sortedDownloads.isNotEmpty()) {
                                // Sort bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sortedDownloads.size} novels",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box {
                                        TextButton(
                                            onClick = { showSortMenu = true },
                                            contentPadding = PaddingValues(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.Sort,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                downloadSortOrder.label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            DownloadSortOrder.entries.forEach { order ->
                                                DropdownMenuItem(
                                                    text = { Text(order.label) },
                                                    onClick = {
                                                        downloadSortOrder = order
                                                        showSortMenu = false
                                                    },
                                                    trailingIcon = if (order == downloadSortOrder) {
                                                        {
                                                            Icon(
                                                                Icons.Outlined.Check,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }

                                // Download items
                                sortedDownloads.forEach { download ->
                                    NovelDownloadItem(
                                        download = download,
                                        onClear = {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                            showClearNovelDialog = download
                                        }
                                    )
                                }
                            } else {
                                // Empty state
                                EmptyDownloadsPlaceholder()
                            }
                        }
                    }
                }

                // Novel Details Cache
                item(key = "details_cache") {
                    CacheCategoryCard(
                        title = "Novel Details Cache",
                        subtitle = "${info.novelDetailsCache.itemCount} novels cached",
                        size = info.novelDetailsCache.formattedSize(),
                        icon = Icons.Outlined.Description,
                        onClear = if (info.novelDetailsCache.sizeBytes > 0) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val result = cacheManager.clearNovelDetailsCache()
                                    if (result.success) {
                                        refreshCacheInfo()
                                        snackbarHostState.showSnackbar(
                                            "Cleared ${result.formattedClearedSize()}"
                                        )
                                    }
                                }
                            }
                        } else null
                    )
                }

                // Image Cache
                item(key = "image_cache") {
                    CacheCategoryCard(
                        title = "Image Cache",
                        subtitle = "${info.imageCache.itemCount} images",
                        size = info.imageCache.formattedSize(),
                        icon = Icons.Outlined.Image,
                        onClear = if (info.imageCache.sizeBytes > 0) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val result = cacheManager.clearImageCache()
                                    if (result.success) {
                                        refreshCacheInfo()
                                        snackbarHostState.showSnackbar(
                                            "Cleared ${result.formattedClearedSize()}"
                                        )
                                    }
                                }
                            }
                        } else null
                    )
                }

                // Other Cache
                if (info.otherCache.sizeBytes > 0) {
                    item(key = "other_cache") {
                        CacheCategoryCard(
                            title = "Other Cache",
                            subtitle = "${info.otherCache.itemCount} files",
                            size = info.otherCache.formattedSize(),
                            icon = Icons.Outlined.Folder
                        )
                    }
                }

                // Bottom spacer
                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ═══════════════ DIALOGS ═══════════════

    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear All Downloads?") },
            text = {
                Text(
                    "This will delete all downloaded chapters. " +
                            "You'll need to download them again for offline reading."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDownloadsDialog = false
                        scope.launch {
                            val result = cacheManager.clearDownloadedChapters()
                            if (result.success) {
                                refreshCacheInfo()
                                snackbarHostState.showSnackbar(
                                    "Cleared ${result.formattedClearedSize()}"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear All Cache?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will clear:")
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        BulletText("All downloaded chapters")
                        BulletText("Novel details cache")
                        BulletText("Image cache")
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Your library, history, and bookmarks will be kept.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        scope.launch {
                            val result = cacheManager.clearAllCaches()
                            if (result.success) {
                                refreshCacheInfo()
                                snackbarHostState.showSnackbar(
                                    "Cleared ${result.formattedClearedSize()}"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showClearNovelDialog?.let { novel ->
        AlertDialog(
            onDismissRequest = { showClearNovelDialog = null },
            title = { Text("Clear Downloads?") },
            text = {
                Text(
                    "Delete ${novel.chapterCount} downloaded chapters " +
                            "from \"${novel.novelName}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val novelUrl = novel.novelUrl
                        showClearNovelDialog = null
                        scope.launch {
                            val result = cacheManager.clearNovelDownloads(novelUrl)
                            if (result.success) {
                                refreshCacheInfo()
                                snackbarHostState.showSnackbar(
                                    "Cleared ${result.formattedClearedSize()}"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearNovelDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Loading metadata dialog
    if (isLoadingMetadata) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Reading backup file…")
                }
            }
        )
    }

    // Restore options dialog
    if (showRestoreOptions && backupMetadata != null) {
        RestoreOptionsDialog(
            metadata = backupMetadata!!,
            options = restoreOptions,
            onOptionsChange = { restoreOptions = it },
            onConfirm = {
                showRestoreOptions = false
                pendingRestoreUri?.let { uri ->
                    scope.launch {
                        isRestoring = true
                        val result = backupManager.restoreFromUri(uri, restoreOptions)
                        isRestoring = false
                        if (result.success) {
                            val action =
                                if (backupMetadata?.isQuickNovelBackup == true) "Imported"
                                else "Restored"
                            snackbarHostState.showSnackbar(
                                "$action ${result.totalItemsRestored} items successfully"
                            )
                            // Refresh cache info after restore
                            refreshCacheInfo()
                        } else {
                            snackbarHostState.showSnackbar("Restore failed: ${result.error}")
                        }
                    }
                }
                pendingRestoreUri = null
                backupMetadata = null
            },
            onDismiss = {
                showRestoreOptions = false
                pendingRestoreUri = null
                backupMetadata = null
            }
        )
    }
}

// ─── Section Header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ─── Backup & Restore Card ──────────────────────────────────────────────────────

@Composable
private fun BackupRestoreCard(
    isCreatingBackup: Boolean,
    isRestoring: Boolean,
    onCreateBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Backup includes your library, bookmarks, reading history, " +
                        "statistics, and settings. You can also import backups from QuickNovel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateBackup,
                    enabled = !isCreatingBackup && !isRestoring,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AnimatedContent(
                        targetState = isCreatingBackup,
                        label = "backup-btn"
                    ) { creating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (creating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (creating) "Creating…" else "Create Backup")
                        }
                    }
                }

                Button(
                    onClick = onRestore,
                    enabled = !isRestoring && !isCreatingBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AnimatedContent(
                        targetState = isRestoring,
                        label = "restore-btn"
                    ) { restoring ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (restoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (restoring) "Restoring…" else "Restore")
                        }
                    }
                }
            }
        }
    }
}

// ─── Storage Overview Card ──────────────────────────────────────────────────────

@Composable
private fun StorageOverviewCard(
    cacheInfo: CacheInfo,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Cache",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cacheInfo.formattedTotalSize(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (cacheInfo.totalSize > 0) {
                    FilledTonalButton(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All")
                    }
                }
            }

            if (cacheInfo.totalSize > 0) {
                Spacer(Modifier.height(20.dp))

                // Animated storage bar
                val total = cacheInfo.totalSize.toFloat()
                val downloadFraction = cacheInfo.downloadedChapters.sizeBytes / total
                val detailsFraction = cacheInfo.novelDetailsCache.sizeBytes / total
                val imageFraction = cacheInfo.imageCache.sizeBytes / total
                val otherFraction = cacheInfo.otherCache.sizeBytes / total

                val animatedDownload by animateFloatAsState(
                    targetValue = downloadFraction.coerceAtLeast(0.01f),
                    animationSpec = tween(600),
                    label = "dl"
                )
                val animatedDetails by animateFloatAsState(
                    targetValue = detailsFraction.coerceAtLeast(0.01f),
                    animationSpec = tween(600, delayMillis = 100),
                    label = "det"
                )
                val animatedImages by animateFloatAsState(
                    targetValue = imageFraction.coerceAtLeast(0.01f),
                    animationSpec = tween(600, delayMillis = 200),
                    label = "img"
                )
                val animatedOther by animateFloatAsState(
                    targetValue = if (otherFraction > 0) otherFraction.coerceAtLeast(0.01f) else 0f,
                    animationSpec = tween(600, delayMillis = 300),
                    label = "other"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(animatedDownload)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(1.dp))
                    Box(
                        modifier = Modifier
                            .weight(animatedDetails)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(Modifier.width(1.dp))
                    Box(
                        modifier = Modifier
                            .weight(animatedImages)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                    if (animatedOther > 0f) {
                        Spacer(Modifier.width(1.dp))
                        Box(
                            modifier = Modifier
                                .weight(animatedOther)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Legend with percentages
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Downloads",
                        percentage = (downloadFraction * 100).toInt()
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.secondary,
                        label = "Details",
                        percentage = (detailsFraction * 100).toInt()
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Images",
                        percentage = (imageFraction * 100).toInt()
                    )
                    if (otherFraction > 0) {
                        LegendItem(
                            color = MaterialTheme.colorScheme.outline,
                            label = "Other",
                            percentage = (otherFraction * 100).toInt()
                        )
                    }
                }
            }
        }
    }
}

// ─── Loading Placeholder ────────────────────────────────────────────────────────

@Composable
private fun StorageLoadingPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(placeholderColor)
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(placeholderColor)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(placeholderColor)
                    )
                }
            }
        }
    }
}

// ─── Legend Item ─────────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    percentage: Int = -1
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (percentage >= 0) "$label ${percentage}%" else label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Cache Category Card ────────────────────────────────────────────────────────

@Composable
private fun CacheCategoryCard(
    title: String,
    subtitle: String,
    size: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    expanded: Boolean = false
) {
    val expandRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "expand-rotate"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = size,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (onClear != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Clear",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(expandRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Novel Download Item ────────────────────────────────────────────────────────

@Composable
private fun NovelDownloadItem(
    download: NovelDownloadInfo,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${download.chapterCount} chapters · ${download.formattedSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─── Empty Downloads Placeholder ────────────────────────────────────────────────

@Composable
private fun EmptyDownloadsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Download,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "No downloaded chapters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Download chapters from your library for offline reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Bullet Text ────────────────────────────────────────────────────────────────

@Composable
private fun BulletText(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

// ─── Restore Options Dialog ─────────────────────────────────────────────────────

@Composable
private fun RestoreOptionsDialog(
    metadata: BackupMetadata,
    options: RestoreOptions,
    onOptionsChange: (RestoreOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    }

    val hasAnySelected = options.restoreLibrary || options.restoreBookmarks ||
            options.restoreHistory || options.restoreStatistics || options.restoreSettings

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (metadata.isQuickNovelBackup) "Import from QuickNovel"
                else "Restore Backup"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Backup info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Backup Info",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (metadata.isQuickNovelBackup) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "QuickNovel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        InfoRow("Created", dateFormat.format(Date(metadata.createdAt)))
                        if (!metadata.isQuickNovelBackup) {
                            InfoRow("Version", metadata.appVersion)
                        }
                        InfoRow("Device", metadata.deviceInfo)
                    }
                }

                // QuickNovel import notice
                if (metadata.isQuickNovelBackup) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                alpha = 0.5f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Reading positions and chapter progress will be converted automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Section title with select all/none
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What to restore",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                onOptionsChange(
                                    options.copy(
                                        restoreLibrary = metadata.libraryCount > 0,
                                        restoreBookmarks = metadata.bookmarkCount > 0,
                                        restoreHistory = metadata.historyCount > 0,
                                        restoreStatistics = metadata.hasStatistics,
                                        restoreSettings = metadata.hasSettings
                                    )
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DoneAll,
                                contentDescription = "Select all",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                onOptionsChange(
                                    options.copy(
                                        restoreLibrary = false,
                                        restoreBookmarks = false,
                                        restoreHistory = false,
                                        restoreStatistics = false,
                                        restoreSettings = false
                                    )
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.RemoveDone,
                                contentDescription = "Deselect all",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Restore options
                Column {
                    RestoreOptionRow(
                        label = "Library (${metadata.libraryCount} novels)",
                        checked = options.restoreLibrary,
                        enabled = metadata.libraryCount > 0,
                        onCheckedChange = {
                            onOptionsChange(options.copy(restoreLibrary = it))
                        }
                    )
                    RestoreOptionRow(
                        label = "Bookmarks (${metadata.bookmarkCount})",
                        checked = options.restoreBookmarks,
                        enabled = metadata.bookmarkCount > 0,
                        onCheckedChange = {
                            onOptionsChange(options.copy(restoreBookmarks = it))
                        }
                    )
                    RestoreOptionRow(
                        label = "History (${metadata.historyCount} entries)",
                        checked = options.restoreHistory,
                        enabled = metadata.historyCount > 0,
                        onCheckedChange = {
                            onOptionsChange(options.copy(restoreHistory = it))
                        }
                    )
                    if (metadata.readChaptersCount > 0) {
                        Text(
                            text = "└ ${metadata.readChaptersCount} read chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 52.dp, bottom = 4.dp)
                        )
                    }
                    RestoreOptionRow(
                        label = "Statistics",
                        checked = options.restoreStatistics && metadata.hasStatistics,
                        enabled = metadata.hasStatistics,
                        onCheckedChange = {
                            onOptionsChange(options.copy(restoreStatistics = it))
                        }
                    )
                    RestoreOptionRow(
                        label = "Settings",
                        checked = options.restoreSettings && metadata.hasSettings,
                        enabled = metadata.hasSettings,
                        onCheckedChange = {
                            onOptionsChange(options.copy(restoreSettings = it))
                        }
                    )
                }

                HorizontalDivider()

                // Merge option
                RestoreOptionRow(
                    label = "Merge with existing data",
                    checked = options.mergeWithExisting,
                    onCheckedChange = {
                        onOptionsChange(options.copy(mergeWithExisting = it))
                    }
                )

                AnimatedVisibility(visible = !options.mergeWithExisting) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.6f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Existing data will be replaced",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = hasAnySelected
            ) {
                Text(if (metadata.isQuickNovelBackup) "Import" else "Restore")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Info Row ───────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// ─── Restore Option Row ─────────────────────────────────────────────────────────

@Composable
private fun RestoreOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}