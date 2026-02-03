// com/emptycastle/novery/data/repository/RepositoryProvider.kt
// REPLACE YOUR ENTIRE FILE WITH THIS

package com.emptycastle.novery.data.repository

import android.content.Context
import com.emptycastle.novery.data.local.NovelDatabase
import com.emptycastle.novery.data.local.PreferencesManager
import com.emptycastle.novery.recommendation.AuthorPreferenceManager
import com.emptycastle.novery.recommendation.DiscoveryManager
import com.emptycastle.novery.recommendation.NetworkBudgetManager
import com.emptycastle.novery.recommendation.RecommendationEngine
import com.emptycastle.novery.recommendation.TagEnhancementManager
import com.emptycastle.novery.recommendation.UserFilterManager
import com.emptycastle.novery.recommendation.UserPreferenceManager

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

    private var userPreferenceManager: UserPreferenceManager? = null
    private var recommendationEngine: RecommendationEngine? = null
    private var discoveryManager: DiscoveryManager? = null
    private var networkBudgetManager: NetworkBudgetManager? = null
    private var tagEnhancementManager: TagEnhancementManager? = null
    private var authorPreferenceManager: AuthorPreferenceManager? = null


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

    fun getUserPreferenceManager(): UserPreferenceManager {
        return userPreferenceManager ?: UserPreferenceManager(
            recommendationDao = getDatabase().recommendationDao(),
            authorPreferenceManager = getAuthorPreferenceManager()
        ).also { userPreferenceManager = it }
    }

    fun getDiscoveryManager(): DiscoveryManager {
        return discoveryManager ?: DiscoveryManager(
            recommendationDao = getDatabase().recommendationDao(),
            novelRepository = getNovelRepository(),
            networkBudgetManager = getNetworkBudgetManager(),
            offlineDao = getDatabase().offlineDao()
        ).also { discoveryManager = it }
    }

    private var userFilterManager: UserFilterManager? = null

    fun getUserFilterManager(): UserFilterManager {
        return userFilterManager ?: UserFilterManager(
            filterDao = getDatabase().userFilterDao()
        ).also { userFilterManager = it }
    }

    fun getRecommendationEngine(): RecommendationEngine {
        return recommendationEngine ?: RecommendationEngine(
            userPreferenceManager = getUserPreferenceManager(),
            libraryRepository = getLibraryRepository(),
            offlineDao = getDatabase().offlineDao(),
            recommendationDao = getDatabase().recommendationDao(),
            userFilterManager = getUserFilterManager(),
            authorPreferenceManager = getAuthorPreferenceManager()  // ADD THIS
        ).also { recommendationEngine = it }
    }

    fun getNetworkBudgetManager(): NetworkBudgetManager {
        return networkBudgetManager ?: NetworkBudgetManager(
            budgetDao = getDatabase().networkBudgetDao()
        ).also { networkBudgetManager = it }
    }

    fun getTagEnhancementManager(): TagEnhancementManager {
        return tagEnhancementManager ?: TagEnhancementManager(
            recommendationDao = getDatabase().recommendationDao(),
            offlineDao = getDatabase().offlineDao()
        ).also { tagEnhancementManager = it }
    }

    fun getAuthorPreferenceManager(): AuthorPreferenceManager {
        return authorPreferenceManager ?: AuthorPreferenceManager(
            authorDao = getDatabase().authorPreferenceDao()
        ).also { authorPreferenceManager = it }
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