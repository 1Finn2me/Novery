// com/emptycastle/novery/data/repository/RepositoryProvider.kt
// REPLACE YOUR ENTIRE FILE WITH THIS

package com.emptycastle.novery.data.repository

import android.content.Context
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager

/**
 * Provides singleton instances of repositories
 */
object RepositoryProvider {

    private var database: NovelDatabase? = null
    private var preferencesManager: PreferencesManager? = null

    // Repositories
    private var novelRepository: NovelRepository? = null
    private var libraryRepository: LibraryRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var offlineRepository: OfflineRepository? = null
    private var statsRepository: StatsRepository? = null
    private var bookmarkRepository: BookmarkRepository? = null
    private var notificationRepository: NotificationRepository? = null

    /**
     * Initialize the repository provider with application context
     */
    fun initialize(context: Context) {
        if (database == null) {
            database = NovelDatabase.getInstance(context)
        }
        if (preferencesManager == null) {
            preferencesManager = PreferencesManager(context)
        }
        notificationRepository = NotificationRepository(context)
    }

    fun getDatabase(): NovelDatabase {
        return database ?: throw IllegalStateException(
            "RepositoryProvider not initialized. Call initialize() first."
        )
    }

    fun getPreferencesManager(): PreferencesManager {
        return preferencesManager ?: throw IllegalStateException(
            "RepositoryProvider not initialized. Call initialize() first."
        )
    }

    fun getNovelRepository(): NovelRepository {
        return novelRepository ?: NovelRepository(
            offlineDao = getDatabase().offlineDao()
        ).also { novelRepository = it }
    }

    fun getLibraryRepository(): LibraryRepository {
        return libraryRepository ?: LibraryRepository(
            libraryDao = getDatabase().libraryDao(),
            offlineDao = getDatabase().offlineDao()
        ).also { libraryRepository = it }
    }

    fun getHistoryRepository(): HistoryRepository {
        return historyRepository ?: HistoryRepository(
            historyDao = getDatabase().historyDao()
        ).also { historyRepository = it }
    }

    fun getOfflineRepository(): OfflineRepository {
        return offlineRepository ?: OfflineRepository(
            offlineDao = getDatabase().offlineDao()
        ).also { offlineRepository = it }
    }

    fun getStatsRepository(): StatsRepository {
        return statsRepository ?: StatsRepository(
            statsDao = getDatabase().statsDao()
        ).also { statsRepository = it }
    }

    fun getBookmarkRepository(): BookmarkRepository {
        return bookmarkRepository ?: BookmarkRepository(
            bookmarkDao = getDatabase().bookmarkDao()
        ).also { bookmarkRepository = it }
    }

    fun getNotificationRepository(): NotificationRepository {
        return notificationRepository ?: throw IllegalStateException("NotificationRepository not initialized")
    }

    /**
     * Clear all cached repositories (for testing)
     */
    fun clear() {
        novelRepository = null
        libraryRepository = null
        historyRepository = null
        offlineRepository = null
        statsRepository = null
        bookmarkRepository = null
    }
}