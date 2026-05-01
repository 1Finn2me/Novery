package com.emptycastle.novery.data.sync

import android.content.Context
import com.emptycastle.novery.data.backup.BackupManager
import com.emptycastle.novery.data.backup.BackupSelection
import com.emptycastle.novery.data.backup.RestoreOptions
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager
import com.google.api.client.auth.oauth2.TokenResponseException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

/**
 * Creates local sync payloads, reconciles remote data, and applies merged results.
 */
class SyncManager(
    context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager.getInstance(context),
    private val backupManager: BackupManager = BackupManager(
        context.applicationContext,
        NovelDatabase.getInstance(context.applicationContext),
        PreferencesManager.getInstance(context.applicationContext)
    ),
    private val googleDriveSyncService: GoogleDriveSyncService = GoogleDriveSyncService(context.applicationContext)
) {
    suspend fun sync(trigger: SyncTrigger): Result<String> {
        val settings = preferencesManager.getSyncSettings()
        if (settings.service == SyncServiceType.NONE) {
            return Result.failure(IllegalStateException("Sync is disabled."))
        }
        if (!settings.googleDriveSignedIn) {
            return Result.failure(IllegalStateException("Google Drive is not connected."))
        }

        val selection = preferencesManager.getSyncDataSelection()
        if (!selection.anyEnabled()) {
            return Result.failure(IllegalStateException("Select at least one sync category."))
        }

        return when (settings.service) {
            SyncServiceType.GOOGLE_DRIVE -> syncWithGoogleDrive(trigger, selection)
            SyncServiceType.NONE -> Result.failure(IllegalStateException("Sync is disabled."))
        }
    }

    private suspend fun syncWithGoogleDrive(
        trigger: SyncTrigger,
        selection: SyncDataSelection
    ): Result<String> {
        SyncStatusTracker.start(trigger, "Preparing local backup")

        return try {
            currentCoroutineContext().ensureActive()
            val localBackup = backupManager.createBackup(selection.toBackupSelection())
            val localPayload = SyncPayload(
                syncedAt = System.currentTimeMillis(),
                deviceId = preferencesManager.getUniqueDeviceId(),
                backup = localBackup
            )

            SyncStatusTracker.update("Checking Google Drive")
            currentCoroutineContext().ensureActive()
            val remotePayload = googleDriveSyncService.pullSyncPayload()

            val mergedPayload = when {
                remotePayload == null -> localPayload
                else -> {
                    SyncStatusTracker.update("Merging remote changes")
                    currentCoroutineContext().ensureActive()
                    SyncPayload(
                        syncedAt = System.currentTimeMillis(),
                        deviceId = localPayload.deviceId,
                        backup = SyncBackupMerger.merge(localBackup, remotePayload.backup)
                    )
                }
            }

            SyncStatusTracker.update("Uploading merged data")
            currentCoroutineContext().ensureActive()
            googleDriveSyncService.pushSyncPayload(mergedPayload)

            if (remotePayload != null) {
                SyncStatusTracker.update("Applying merged data locally")
                currentCoroutineContext().ensureActive()
                val restoreResult = backupManager.restoreBackupData(
                    mergedPayload.backup,
                    selection.toRestoreOptions()
                )

                if (!restoreResult.success) {
                    error(restoreResult.error ?: "Failed to restore merged sync data.")
                }
            }

            val syncedAt = System.currentTimeMillis()
            preferencesManager.setLastSyncTime(syncedAt)
            val message = "Last synced ${selection.enabledCountLabel()} to Google Drive"
            SyncStatusTracker.finishSuccess(message)
            Result.success(message)
        } catch (error: Exception) {
            val safeMessage = error.toSyncMessage()
            SyncStatusTracker.finishError(safeMessage)
            Result.failure(IllegalStateException(safeMessage, error))
        }
    }

    fun getGoogleDriveService(): GoogleDriveSyncService {
        return googleDriveSyncService
    }

    companion object {
        fun shouldTriggerSync(
            preferencesManager: PreferencesManager,
            trigger: SyncTrigger
        ): Boolean {
            val settings = preferencesManager.getSyncSettings()
            if (settings.service == SyncServiceType.NONE || !settings.googleDriveSignedIn) {
                return false
            }

            return when (trigger) {
                SyncTrigger.MANUAL,
                SyncTrigger.AUTO -> true
                SyncTrigger.APP_START -> preferencesManager.getSyncTriggerOptions().syncOnAppStart
                SyncTrigger.APP_RESUME -> preferencesManager.getSyncTriggerOptions().syncOnAppResume
                SyncTrigger.CHAPTER_OPEN -> preferencesManager.getSyncTriggerOptions().syncOnChapterOpen
                SyncTrigger.CHAPTER_READ -> preferencesManager.getSyncTriggerOptions().syncOnChapterRead
            }
        }
    }
}

private fun Throwable.toSyncMessage(): String {
    return when (this) {
        is TokenResponseException -> {
            if (details?.error == "invalid_grant") {
                "Google Drive sign-in expired. Connect again."
            } else {
                "Google Drive authorization failed."
            }
        }
        is IOException -> "Network error while syncing. Try again later."
        is IllegalStateException -> message ?: "Sync failed."
        else -> "Sync failed. Try again later."
    }
}

private fun SyncDataSelection.toBackupSelection(): BackupSelection {
    return BackupSelection(
        includeLibrary = syncLibrary,
        includeBookmarks = syncBookmarks,
        includeHistory = syncHistory,
        includeStatistics = syncStatistics,
        includeSettings = syncSettings
    )
}

private fun SyncDataSelection.toRestoreOptions(): RestoreOptions {
    return RestoreOptions(
        restoreLibrary = syncLibrary,
        restoreBookmarks = syncBookmarks,
        restoreHistory = syncHistory,
        restoreStatistics = syncStatistics,
        restoreSettings = syncSettings,
        mergeWithExisting = true
    )
}

private fun SyncDataSelection.enabledCountLabel(): String {
    val enabledCount = listOf(
        syncLibrary,
        syncBookmarks,
        syncHistory,
        syncStatistics,
        syncSettings
    ).count { it }

    return when (enabledCount) {
        1 -> "1 section"
        else -> "$enabledCount sections"
    }
}
