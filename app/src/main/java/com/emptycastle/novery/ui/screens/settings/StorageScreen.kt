// com/emptycastle/novery/ui/screens/settings/StorageScreen.kt

package com.emptycastle.novery.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    cacheManager: CacheManager,
    backupManager: BackupManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cache state
    var cacheInfo by remember { mutableStateOf<CacheInfo?>(null) }
    var isLoadingCache by remember { mutableStateOf(true) }
    var novelDownloads by remember { mutableStateOf<List<NovelDownloadInfo>>(emptyList()) }
    var showDownloadsDetail by remember { mutableStateOf(false) }

    // Backup/Restore state
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreOptions by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var backupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var restoreOptions by remember { mutableStateOf(RestoreOptions()) }

    // Dialogs
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearNovelDialog by remember { mutableStateOf<NovelDownloadInfo?>(null) }

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
                    snackbarHostState.showSnackbar("Failed to create backup: ${result.exceptionOrNull()?.message}")
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
                val metadataResult = backupManager.parseBackupMetadata(it)
                if (metadataResult.isSuccess) {
                    backupMetadata = metadataResult.getOrNull()
                    showRestoreOptions = true
                } else {
                    snackbarHostState.showSnackbar("Invalid backup file: ${metadataResult.exceptionOrNull()?.message}")
                }
            }
        }
    }

    // Load cache info
    LaunchedEffect(Unit) {
        isLoadingCache = true
        cacheInfo = cacheManager.getCacheInfo()
        novelDownloads = cacheManager.getNovelDownloads()
        isLoadingCache = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============ BACKUP/RESTORE SECTION ============
            item {
                SectionHeader(
                    title = "Backup & Restore",
                    icon = Icons.Outlined.CloudUpload
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Backup includes your library, bookmarks, reading history, statistics, and settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    backupLauncher.launch(backupManager.generateBackupFileName())
                                },
                                enabled = !isCreatingBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCreatingBackup) {
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Backup")
                            }

                            Button(
                                onClick = {
                                    restoreLauncher.launch(arrayOf(BackupData.MIME_TYPE, "application/json"))
                                },
                                enabled = !isRestoring,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRestoring) {
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            // ============ STORAGE OVERVIEW ============
            item {
                SectionHeader(
                    title = "Storage",
                    icon = Icons.Outlined.Storage
                )
            }

            item {
                if (isLoadingCache) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    cacheInfo?.let { info ->
                        StorageOverviewCard(
                            cacheInfo = info,
                            onClearAll = { showClearCacheDialog = true }
                        )
                    }
                }
            }

            // ============ CACHE CATEGORIES ============
            cacheInfo?.let { info ->
                // Downloaded Chapters
                item {
                    CacheCategoryCard(
                        title = "Downloaded Chapters",
                        subtitle = "${info.downloadedChapters.itemCount} chapters from ${info.downloadedChapters.novelCount} novels",
                        size = info.downloadedChapters.formattedSize(),
                        icon = Icons.Outlined.DownloadDone,
                        onClick = { showDownloadsDetail = !showDownloadsDetail },
                        onClear = if (info.downloadedChapters.sizeBytes > 0) {
                            { showClearDownloadsDialog = true }
                        } else null,
                        expanded = showDownloadsDetail
                    )
                }

                // Novel downloads detail
                if (showDownloadsDetail && novelDownloads.isNotEmpty()) {
                    items(novelDownloads) { download ->
                        NovelDownloadItem(
                            download = download,
                            onClear = { showClearNovelDialog = download }
                        )
                    }
                }

                // Novel Details Cache
                item {
                    CacheCategoryCard(
                        title = "Novel Details Cache",
                        subtitle = "${info.novelDetailsCache.itemCount} novels cached",
                        size = info.novelDetailsCache.formattedSize(),
                        icon = Icons.Outlined.Description,
                        onClear = if (info.novelDetailsCache.sizeBytes > 0) {
                            {
                                scope.launch {
                                    val result = cacheManager.clearNovelDetailsCache()
                                    if (result.success) {
                                        cacheInfo = cacheManager.getCacheInfo()
                                        snackbarHostState.showSnackbar("Cleared ${result.formattedClearedSize()}")
                                    }
                                }
                            }
                        } else null
                    )
                }

                // Image Cache
                item {
                    CacheCategoryCard(
                        title = "Image Cache",
                        subtitle = "${info.imageCache.itemCount} images",
                        size = info.imageCache.formattedSize(),
                        icon = Icons.Outlined.Image,
                        onClear = if (info.imageCache.sizeBytes > 0) {
                            {
                                scope.launch {
                                    val result = cacheManager.clearImageCache()
                                    if (result.success) {
                                        cacheInfo = cacheManager.getCacheInfo()
                                        snackbarHostState.showSnackbar("Cleared ${result.formattedClearedSize()}")
                                    }
                                }
                            }
                        } else null
                    )
                }

                // Other Cache
                if (info.otherCache.sizeBytes > 0) {
                    item {
                        CacheCategoryCard(
                            title = "Other Cache",
                            subtitle = "${info.otherCache.itemCount} files",
                            size = info.otherCache.formattedSize(),
                            icon = Icons.Outlined.Folder
                        )
                    }
                }
            }
        }
    }

    // ============ DIALOGS ============

    // Clear all downloads dialog
    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text("Clear All Downloads?") },
            text = {
                Text(
                    "This will delete all downloaded chapters. " +
                            "You'll need to download them again for offline reading."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDownloadsDialog = false
                        scope.launch {
                            val result = cacheManager.clearDownloadedChapters()
                            if (result.success) {
                                cacheInfo = cacheManager.getCacheInfo()
                                novelDownloads = emptyList()
                                snackbarHostState.showSnackbar("Cleared ${result.formattedClearedSize()}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all cache dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            title = { Text("Clear All Cache?") },
            text = {
                Column {
                    Text("This will clear:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• All downloaded chapters")
                    Text("• Novel details cache")
                    Text("• Image cache")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your library, history, and bookmarks will be kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        scope.launch {
                            val result = cacheManager.clearAllCaches()
                            if (result.success) {
                                cacheInfo = cacheManager.getCacheInfo()
                                novelDownloads = emptyList()
                                snackbarHostState.showSnackbar("Cleared ${result.formattedClearedSize()}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear specific novel dialog
    showClearNovelDialog?.let { novel ->
        AlertDialog(
            onDismissRequest = { showClearNovelDialog = null },
            title = { Text("Clear Downloads?") },
            text = {
                Text("Delete ${novel.chapterCount} downloaded chapters from \"${novel.novelName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val novelUrl = novel.novelUrl
                        showClearNovelDialog = null
                        scope.launch {
                            val result = cacheManager.clearNovelDownloads(novelUrl)
                            if (result.success) {
                                cacheInfo = cacheManager.getCacheInfo()
                                novelDownloads = cacheManager.getNovelDownloads()
                                snackbarHostState.showSnackbar("Cleared ${result.formattedClearedSize()}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearNovelDialog = null }) {
                    Text("Cancel")
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
                            snackbarHostState.showSnackbar(
                                "Restored ${result.totalItemsRestored} items successfully"
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                "Restore failed: ${result.error}"
                            )
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

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StorageOverviewCard(
    cacheInfo: CacheInfo,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    FilledTonalButton(onClick = onClearAll) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All")
                    }
                }
            }

            if (cacheInfo.totalSize > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                // Storage bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    val total = cacheInfo.totalSize.toFloat()

                    // Downloads
                    Box(
                        modifier = Modifier
                            .weight((cacheInfo.downloadedChapters.sizeBytes / total).coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    // Details cache
                    Box(
                        modifier = Modifier
                            .weight((cacheInfo.novelDetailsCache.sizeBytes / total).coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary)
                    )

                    // Image cache
                    Box(
                        modifier = Modifier
                            .weight((cacheInfo.imageCache.sizeBytes / total).coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.tertiary)
                    )

                    // Other
                    if (cacheInfo.otherCache.sizeBytes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((cacheInfo.otherCache.sizeBytes / total).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Downloads"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.secondary,
                        label = "Details"
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = "Images"
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
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
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }
    }
}

@Composable
private fun NovelDownloadItem(
    download: NovelDownloadInfo,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                Text(
                    text = "${download.chapterCount} chapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = download.formattedSize(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onClear) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RestoreOptionsDialog(
    metadata: BackupMetadata,
    options: RestoreOptions,
    onOptionsChange: (RestoreOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Column {
                // Backup info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Backup Info",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created: ${dateFormat.format(Date(metadata.createdAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "App Version: ${metadata.appVersion}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Device: ${metadata.deviceInfo}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "What to restore:",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options
                RestoreOptionRow(
                    label = "Library (${metadata.libraryCount} novels)",
                    checked = options.restoreLibrary,
                    onCheckedChange = { onOptionsChange(options.copy(restoreLibrary = it)) }
                )

                RestoreOptionRow(
                    label = "Bookmarks (${metadata.bookmarkCount})",
                    checked = options.restoreBookmarks,
                    onCheckedChange = { onOptionsChange(options.copy(restoreBookmarks = it)) }
                )

                RestoreOptionRow(
                    label = "History (${metadata.historyCount} entries)",
                    checked = options.restoreHistory,
                    onCheckedChange = { onOptionsChange(options.copy(restoreHistory = it)) }
                )

                RestoreOptionRow(
                    label = "Statistics",
                    enabled = metadata.hasStatistics,
                    checked = options.restoreStatistics && metadata.hasStatistics,
                    onCheckedChange = { onOptionsChange(options.copy(restoreStatistics = it)) }
                )

                RestoreOptionRow(
                    label = "Settings",
                    enabled = metadata.hasSettings,
                    checked = options.restoreSettings && metadata.hasSettings,
                    onCheckedChange = { onOptionsChange(options.copy(restoreSettings = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                RestoreOptionRow(
                    label = "Merge with existing data",
                    checked = options.mergeWithExisting,
                    onCheckedChange = { onOptionsChange(options.copy(mergeWithExisting = it)) }
                )

                if (!options.mergeWithExisting) {
                    Text(
                        text = "⚠️ Existing data will be replaced",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

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
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
        )
    }
}