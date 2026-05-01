package com.emptycastle.novery.data.sync

import com.emptycastle.novery.data.backup.BackupData
import kotlinx.serialization.Serializable

/**
 * Supported remote sync services.
 */
enum class SyncServiceType {
    NONE,
    GOOGLE_DRIVE;

    companion object {
        fun fromName(value: String?): SyncServiceType {
            return entries.firstOrNull { it.name == value } ?: NONE
        }
    }
}

/**
 * Controls which parts of the app are included in sync payloads.
 */
data class SyncDataSelection(
    val syncLibrary: Boolean = true,
    val syncBookmarks: Boolean = true,
    val syncHistory: Boolean = true,
    val syncStatistics: Boolean = true,
    val syncSettings: Boolean = true
) {
    fun anyEnabled(): Boolean {
        return syncLibrary || syncBookmarks || syncHistory || syncStatistics || syncSettings
    }
}

/**
 * Controls when automatic one-shot sync requests are triggered.
 */
data class SyncTriggerOptions(
    val syncOnChapterRead: Boolean = false,
    val syncOnChapterOpen: Boolean = false,
    val syncOnAppStart: Boolean = false,
    val syncOnAppResume: Boolean = false
)

/**
 * Persisted sync settings shown in the storage screen.
 */
data class SyncSettings(
    val service: SyncServiceType = SyncServiceType.NONE,
    val intervalMinutes: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val showProgressNotifications: Boolean = true,
    val googleDriveSignedIn: Boolean = false
)

/**
 * In-memory execution state for active sync work.
 */
data class SyncExecutionState(
    val isRunning: Boolean = false,
    val trigger: SyncTrigger = SyncTrigger.MANUAL,
    val stage: String = "",
    val lastMessage: String? = null,
    val lastError: String? = null,
    val updatedAt: Long = 0L
)

/**
 * Describes where a sync request came from.
 */
enum class SyncTrigger {
    MANUAL,
    AUTO,
    APP_START,
    APP_RESUME,
    CHAPTER_OPEN,
    CHAPTER_READ
}

/**
 * Serialized payload stored on the remote sync service.
 */
@Serializable
data class SyncPayload(
    val version: Int = CURRENT_VERSION,
    val syncedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val backup: BackupData = BackupData()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
