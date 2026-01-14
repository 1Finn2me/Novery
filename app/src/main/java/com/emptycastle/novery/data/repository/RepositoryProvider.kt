package com.emptycastle.novery.data.repository

import android.content.Context
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager

/**
 * Provides repository instances with proper dependencies.
 * This is a simple service locator pattern.
 *
 * In a larger app, you might use Hilt/Dagger for dependency injection.
 */
object RepositoryProvider {

    private var novelRepository: NovelRepository? = null
    private var libraryRepository: LibraryRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var offlineRepository: OfflineRepository? = null
    private var preferencesManager: PreferencesManager? = null

    /**
     * Initialize repositories with application context
     */
    fun initialize(context: Context) {
        val database = NovelDatabase.getInstance(context)

        novelRepository = NovelRepository(database.offlineDao())
        libraryRepository = LibraryRepository(database.libraryDao(), database.offlineDao())
        historyRepository = HistoryRepository(database.historyDao())
        offlineRepository = OfflineRepository(database.offlineDao())
        preferencesManager = PreferencesManager.getInstance(context)
    }

    fun getNovelRepository(): NovelRepository {
        return novelRepository
            ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun getLibraryRepository(): LibraryRepository {
        return libraryRepository
            ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun getHistoryRepository(): HistoryRepository {
        return historyRepository
            ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun getOfflineRepository(): OfflineRepository {
        return offlineRepository
            ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun getPreferencesManager(): PreferencesManager {
        return preferencesManager
            ?: throw IllegalStateException("RepositoryProvider not initialized")
    }
}