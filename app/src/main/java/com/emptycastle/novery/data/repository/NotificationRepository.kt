package com.emptycastle.novery.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "NotificationRepository"
private const val NOTIFICATIONS_FILE = "notifications.json"

/**
 * Represents a notification entry that persists independently of library state
 */
@Serializable
data class NotificationEntry(
    val novelUrl: String,
    val providerName: String,
    val addedAt: Long,
    val lastUpdatedAt: Long,
    val acknowledgedAt: Long? = null
)

@Serializable
private data class NotificationData(
    val entries: List<NotificationEntry> = emptyList()
)

/**
 * Repository for managing notification persistence using simple file storage
 */
class NotificationRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private val notificationsFile = File(context.filesDir, NOTIFICATIONS_FILE)

    private val _entriesFlow = MutableStateFlow<List<NotificationEntry>>(emptyList())

    init {
        // Load initial data
        loadFromFile()
    }

    private fun loadFromFile() {
        try {
            if (notificationsFile.exists()) {
                val jsonStr = notificationsFile.readText()
                val data = json.decodeFromString<NotificationData>(jsonStr)
                _entriesFlow.value = data.entries.sortedByDescending { it.lastUpdatedAt }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notifications", e)
            _entriesFlow.value = emptyList()
        }
    }

    private suspend fun saveToFile() {
        try {
            val data = NotificationData(entries = _entriesFlow.value)
            val jsonStr = json.encodeToString(data)
            notificationsFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notifications", e)
        }
    }

    /**
     * Observe all notification entries
     */
    fun observeNotificationEntries(): Flow<List<NotificationEntry>> {
        return _entriesFlow.asStateFlow()
    }

    /**
     * Add or update a novel in notifications
     */
    suspend fun addOrUpdateNotification(novelUrl: String, providerName: String) {
        mutex.withLock {
            val currentEntries = _entriesFlow.value.toMutableList()
            val existingIndex = currentEntries.indexOfFirst { it.novelUrl == novelUrl }
            val now = System.currentTimeMillis()

            if (existingIndex >= 0) {
                val existing = currentEntries[existingIndex]
                currentEntries[existingIndex] = existing.copy(
                    lastUpdatedAt = now,
                    acknowledgedAt = null
                )
            } else {
                currentEntries.add(
                    NotificationEntry(
                        novelUrl = novelUrl,
                        providerName = providerName,
                        addedAt = now,
                        lastUpdatedAt = now,
                        acknowledgedAt = null
                    )
                )
            }

            _entriesFlow.value = currentEntries.sortedByDescending { it.lastUpdatedAt }
            saveToFile()
        }
    }

    /**
     * Mark a notification as seen
     */
    suspend fun markAsSeen(novelUrl: String) {
        mutex.withLock {
            val currentEntries = _entriesFlow.value.toMutableList()
            val existingIndex = currentEntries.indexOfFirst { it.novelUrl == novelUrl }

            if (existingIndex >= 0) {
                val existing = currentEntries[existingIndex]
                currentEntries[existingIndex] = existing.copy(
                    acknowledgedAt = System.currentTimeMillis()
                )
                _entriesFlow.value = currentEntries.sortedByDescending { it.lastUpdatedAt }
                saveToFile()
            }
        }
    }

    /**
     * Mark all notifications as seen
     */
    suspend fun markAllAsSeen() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val updated = _entriesFlow.value.map { it.copy(acknowledgedAt = now) }
            _entriesFlow.value = updated
            saveToFile()
        }
    }

    /**
     * Remove a notification
     */
    suspend fun removeNotification(novelUrl: String) {
        mutex.withLock {
            val filtered = _entriesFlow.value.filter { it.novelUrl != novelUrl }
            _entriesFlow.value = filtered
            saveToFile()
        }
    }

    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications() {
        mutex.withLock {
            _entriesFlow.value = emptyList()
            saveToFile()
        }
    }

    /**
     * Get notification entry for a specific novel
     */
    fun getNotificationEntry(novelUrl: String): NotificationEntry? {
        return _entriesFlow.value.find { it.novelUrl == novelUrl }
    }
}