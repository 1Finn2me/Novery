package com.emptycastle.novery.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.data.sync.SyncManager
import com.emptycastle.novery.data.sync.SyncServiceType
import com.emptycastle.novery.data.sync.SyncStatusTracker
import com.emptycastle.novery.data.sync.SyncTrigger
import com.emptycastle.novery.data.sync.SyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageSyncSection(
    preferencesManager: PreferencesManager,
    syncManager: SyncManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val syncSettings by preferencesManager.syncSettings.collectAsStateWithLifecycle()
    val syncSelection by preferencesManager.syncDataSelection.collectAsStateWithLifecycle()
    val triggerOptions by preferencesManager.syncTriggerOptions.collectAsStateWithLifecycle()
    val syncState by SyncStatusTracker.state.collectAsStateWithLifecycle()
    val googleDriveSync = remember(syncManager) { syncManager.getGoogleDriveService() }

    var showServiceMenu by remember { mutableStateOf(false) }
    var showIntervalMenu by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, preferencesManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                preferencesManager.refreshSyncSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(syncState.isRunning, syncState.updatedAt) {
        if (!syncState.isRunning) {
            preferencesManager.refreshSyncSettings()
        }
    }

    val isGoogleDriveConfigured = googleDriveSync.isConfigured()
    val statusText = remember(syncState.isRunning, syncState.stage, syncState.lastError, syncState.lastMessage) {
        syncStatusLabel(
            isRunning = syncState.isRunning,
            stage = syncState.stage,
            lastError = syncState.lastError,
            lastMessage = syncState.lastMessage
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Google Drive?") },
            text = { Text("This removes the Google Drive sign-in from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        googleDriveSync.clearLocalAccount()
                        SyncWorker.schedule(context, forceUpdate = true)
                    }
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            title = { Text("Delete remote sync data?") },
            text = { Text("This removes Novery sync data from your Google Drive app data.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        showPurgeDialog = false
                        scope.launch {
                            runCatching {
                                googleDriveSync.purgeRemotePayload()
                            }.onSuccess { deleted ->
                                Toast.makeText(
                                    context,
                                    if (deleted) {
                                        "Remote sync data deleted"
                                    } else {
                                        "No remote sync data found"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Failed to delete remote sync data",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(16.dp)
        ) {
            val compactActions = maxWidth < 420.dp

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sync your library, reading progress, bookmarks, stats, and settings with Google Drive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isGoogleDriveConfigured) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Google Drive setup is missing for this build.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Service",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        OptionField(
                            text = syncSettings.service.displayName(),
                            icon = { Icon(Icons.Outlined.CloudSync, contentDescription = null) },
                            onClick = { showServiceMenu = true }
                        )

                        DropdownMenu(
                            expanded = showServiceMenu,
                            onDismissRequest = { showServiceMenu = false }
                        ) {
                            SyncServiceType.entries.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service.displayName()) },
                                    onClick = {
                                        showServiceMenu = false
                                        preferencesManager.setSyncService(service)
                                        SyncWorker.schedule(context, forceUpdate = true)
                                    },
                                    trailingIcon = if (service == syncSettings.service) {
                                        {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null
                                            )
                                        }
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                }

                if (syncSettings.service != SyncServiceType.NONE) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusRows(
                            lastSync = syncSettings.lastSyncTimestamp.formatTimestamp(),
                            status = statusText
                        )

                        SyncActions(
                            compact = compactActions,
                            isRunning = syncState.isRunning,
                            signedIn = syncSettings.googleDriveSignedIn,
                            configured = isGoogleDriveConfigured,
                            onSync = { SyncWorker.triggerNow(context, SyncTrigger.MANUAL) },
                            onSignIn = {
                                runCatching {
                                    context.startActivity(googleDriveSync.getSignInIntent())
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Unable to open Google Drive sign-in",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onPurge = { showPurgeDialog = true }
                        )

                        if (syncSettings.googleDriveSignedIn) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !syncState.isRunning,
                                shape = RoundedCornerShape(12.dp),
                                onClick = { showDisconnectDialog = true }
                            ) {
                                Text(
                                    text = "Disconnect",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Automatic sync",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Box {
                                OptionField(
                                    text = syncSettings.intervalMinutes.syncIntervalLabel(),
                                    icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                                    onClick = { showIntervalMenu = true }
                                )

                                DropdownMenu(
                                    expanded = showIntervalMenu,
                                    onDismissRequest = { showIntervalMenu = false }
                                ) {
                                    SYNC_INTERVAL_OPTIONS.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                showIntervalMenu = false
                                                preferencesManager.setSyncIntervalMinutes(option.minutes)
                                                SyncWorker.schedule(context, forceUpdate = true)
                                            },
                                            trailingIcon = if (option.minutes == syncSettings.intervalMinutes) {
                                                {
                                                    Icon(
                                                        Icons.Outlined.Check,
                                                        contentDescription = null
                                                    )
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }

                            ToggleRow(
                                title = "Notifications",
                                subtitle = "Show sync progress",
                                checked = syncSettings.showProgressNotifications,
                                onCheckedChange = { preferencesManager.setSyncProgressNotifications(it) }
                            )
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Triggers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            ToggleRow(
                                title = "Chapter read",
                                subtitle = "Queue after reading",
                                checked = triggerOptions.syncOnChapterRead,
                                onCheckedChange = {
                                    preferencesManager.updateSyncTriggerOptions(
                                        triggerOptions.copy(syncOnChapterRead = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "Chapter open",
                                subtitle = "Queue after opening",
                                checked = triggerOptions.syncOnChapterOpen,
                                onCheckedChange = {
                                    preferencesManager.updateSyncTriggerOptions(
                                        triggerOptions.copy(syncOnChapterOpen = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "App start",
                                subtitle = "Sync after launch",
                                checked = triggerOptions.syncOnAppStart,
                                onCheckedChange = {
                                    preferencesManager.updateSyncTriggerOptions(
                                        triggerOptions.copy(syncOnAppStart = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "App resume",
                                subtitle = "Sync after returning",
                                checked = triggerOptions.syncOnAppResume,
                                onCheckedChange = {
                                    preferencesManager.updateSyncTriggerOptions(
                                        triggerOptions.copy(syncOnAppResume = it)
                                    )
                                }
                            )
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Content",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            ToggleRow(
                                title = "Library",
                                subtitle = "Shelves and progress",
                                checked = syncSelection.syncLibrary,
                                onCheckedChange = {
                                    preferencesManager.updateSyncDataSelection(
                                        syncSelection.copy(syncLibrary = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "Bookmarks",
                                subtitle = "Saved passages and notes",
                                checked = syncSelection.syncBookmarks,
                                onCheckedChange = {
                                    preferencesManager.updateSyncDataSelection(
                                        syncSelection.copy(syncBookmarks = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "History",
                                subtitle = "Recent reads and read chapters",
                                checked = syncSelection.syncHistory,
                                onCheckedChange = {
                                    preferencesManager.updateSyncDataSelection(
                                        syncSelection.copy(syncHistory = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "Stats",
                                subtitle = "Reading stats and streaks",
                                checked = syncSelection.syncStatistics,
                                onCheckedChange = {
                                    preferencesManager.updateSyncDataSelection(
                                        syncSelection.copy(syncStatistics = it)
                                    )
                                }
                            )
                            ToggleRow(
                                title = "Settings",
                                subtitle = "App and reader preferences",
                                checked = syncSelection.syncSettings,
                                onCheckedChange = {
                                    preferencesManager.updateSyncDataSelection(
                                        syncSelection.copy(syncSettings = it)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionField(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            icon()
        }
    }
}

@Composable
private fun StatusRows(
    lastSync: String,
    status: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoLine("Last sync", lastSync)
        InfoLine("Status", status)
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SyncActions(
    compact: Boolean,
    isRunning: Boolean,
    signedIn: Boolean,
    configured: Boolean,
    onSync: () -> Unit,
    onSignIn: () -> Unit,
    onPurge: () -> Unit
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SyncPrimaryAction(
                isRunning = isRunning,
                signedIn = signedIn,
                configured = configured,
                onSync = onSync,
                onSignIn = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
            if (signedIn) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRunning,
                    shape = RoundedCornerShape(12.dp),
                    onClick = onPurge
                ) {
                    ButtonLabel("Delete remote")
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SyncPrimaryAction(
                isRunning = isRunning,
                signedIn = signedIn,
                configured = configured,
                onSync = onSync,
                onSignIn = onSignIn,
                modifier = Modifier.weight(1f)
            )
            if (signedIn) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    shape = RoundedCornerShape(12.dp),
                    onClick = onPurge
                ) {
                    ButtonLabel("Delete remote")
                }
            }
        }
    }
}

@Composable
private fun SyncPrimaryAction(
    isRunning: Boolean,
    signedIn: Boolean,
    configured: Boolean,
    onSync: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (signedIn) {
        Button(
            modifier = modifier,
            enabled = !isRunning && configured,
            shape = RoundedCornerShape(12.dp),
            onClick = onSync
        ) {
            ButtonLabel(if (isRunning) "Syncing" else "Sync")
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            enabled = configured,
            shape = RoundedCornerShape(12.dp),
            onClick = onSignIn
        ) {
            ButtonLabel("Sign in")
        }
    }
}

@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private data class SyncIntervalOption(
    val minutes: Int,
    val label: String
)

private val SYNC_INTERVAL_OPTIONS = listOf(
    SyncIntervalOption(0, "Off"),
    SyncIntervalOption(30, "Every 30 minutes"),
    SyncIntervalOption(60, "Hourly"),
    SyncIntervalOption(180, "Every 3 hours"),
    SyncIntervalOption(360, "Every 6 hours"),
    SyncIntervalOption(720, "Every 12 hours"),
    SyncIntervalOption(1440, "Daily"),
    SyncIntervalOption(2880, "Every 2 days"),
    SyncIntervalOption(10080, "Weekly")
)

private fun SyncServiceType.displayName(): String {
    return when (this) {
        SyncServiceType.NONE -> "Off"
        SyncServiceType.GOOGLE_DRIVE -> "Google Drive"
    }
}

private fun Int.syncIntervalLabel(): String {
    return SYNC_INTERVAL_OPTIONS.firstOrNull { it.minutes == this }?.label
        ?: "${this}m"
}

private fun Long.formatTimestamp(): String {
    if (this <= 0L) return "Never"
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(this))
}

private fun syncStatusLabel(
    isRunning: Boolean,
    stage: String,
    lastError: String?,
    lastMessage: String?
): String {
    if (isRunning) {
        return when {
            stage.contains("Preparing", ignoreCase = true) -> "Preparing"
            stage.contains("Checking", ignoreCase = true) -> "Checking Drive"
            stage.contains("Merging", ignoreCase = true) -> "Merging"
            stage.contains("Uploading", ignoreCase = true) -> "Uploading"
            stage.contains("Applying", ignoreCase = true) -> "Applying"
            else -> "Syncing"
        }
    }

    return when {
        lastError != null -> "Failed"
        lastMessage != null -> "Synced"
        else -> "Idle"
    }
}
