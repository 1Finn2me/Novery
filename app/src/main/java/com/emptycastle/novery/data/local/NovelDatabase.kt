package com.emptycastle.novery.data.local

/**
 * Type converters for Room database
 */
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emptycastle.novery.data.local.dao.AuthorPreferenceDao
import com.emptycastle.novery.data.local.dao.BookmarkDao
import com.emptycastle.novery.data.local.dao.HistoryDao
import com.emptycastle.novery.data.local.dao.LibraryDao
import com.emptycastle.novery.data.local.dao.NetworkBudgetDao
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.dao.RecommendationDao
import com.emptycastle.novery.data.local.dao.StatsDao
import com.emptycastle.novery.data.local.dao.UserFilterDao
import com.emptycastle.novery.data.local.entity.AuthorPreferenceEntity
import com.emptycastle.novery.data.local.entity.BlockedAuthorEntity
import com.emptycastle.novery.data.local.entity.BookmarkEntity
import com.emptycastle.novery.data.local.entity.ChapterEntity
import com.emptycastle.novery.data.local.entity.DiscoveredNovelEntity
import com.emptycastle.novery.data.local.entity.DiscoveryChainEntity
import com.emptycastle.novery.data.local.entity.HiddenNovelEntity
import com.emptycastle.novery.data.local.entity.HistoryEntity
import com.emptycastle.novery.data.local.entity.LibraryEntity
import com.emptycastle.novery.data.local.entity.NetworkBudgetEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import com.emptycastle.novery.data.local.entity.ReadChapterEntity
import com.emptycastle.novery.data.local.entity.ReadingStatsEntity
import com.emptycastle.novery.data.local.entity.ReadingStreakEntity
import com.emptycastle.novery.data.local.entity.UserPreferenceEntity
import com.emptycastle.novery.data.local.entity.UserTagFilterEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { gson.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, type)
        }
    }

    // Chapter list converters
    @TypeConverter
    fun fromChapterList(value: List<ChapterEntity>?): String {
        return value?.let { gson.toJson(it) } ?: ""
    }

    @TypeConverter
    fun toChapterList(value: String): List<ChapterEntity> {
        if (value.isBlank()) return emptyList()
        val type = object : TypeToken<List<ChapterEntity>>() {}.type
        return gson.fromJson(value, type)
    }
}

@Database(
    entities = [
        // Core entities
        LibraryEntity::class,
        HistoryEntity::class,

        // Offline/Cache entities
        OfflineNovelEntity::class,
        OfflineChapterEntity::class,
        NovelDetailsEntity::class,
        ReadChapterEntity::class,

        // Stats & Tracking entities
        ReadingStatsEntity::class,
        ReadingStreakEntity::class,
        BookmarkEntity::class,
        UserPreferenceEntity::class,
        DiscoveredNovelEntity::class,
        NetworkBudgetEntity::class,
        DiscoveryChainEntity::class,
        UserTagFilterEntity::class,
        HiddenNovelEntity::class,
        BlockedAuthorEntity::class,
        AuthorPreferenceEntity::class,
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class NovelDatabase : RoomDatabase() {

    // Core DAOs
    abstract fun libraryDao(): LibraryDao
    abstract fun historyDao(): HistoryDao
    abstract fun offlineDao(): OfflineDao

    // Stats & Tracking DAOs
    abstract fun statsDao(): StatsDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun networkBudgetDao(): NetworkBudgetDao
    abstract fun userFilterDao(): UserFilterDao
    abstract fun authorPreferenceDao(): AuthorPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: NovelDatabase? = null

        fun getInstance(context: Context): NovelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovelDatabase::class.java,
                    "novery_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                INSTANCE = instance
                instance
            }
        }
        private fun safeAddColumn(
            database: SupportSQLiteDatabase,
            table: String,
            column: String,
            type: String
        ) {
            try {
                database.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            } catch (e: Exception) {
                // Column might already exist - ignore
            }
        }

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to library table for chapter tracking
                safeAddColumn(database, "library", "totalChapterCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(database, "library", "acknowledgedChapterCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(database, "library", "lastCheckedAt", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(database, "library", "lastUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(database, "library", "lastReadChapterIndex", "INTEGER NOT NULL DEFAULT -1")
                safeAddColumn(database, "library", "unreadChapterCount", "INTEGER NOT NULL DEFAULT 0")

                // Add new columns to novel_details table
                safeAddColumn(database, "novel_details", "views", "INTEGER")
                safeAddColumn(database, "novel_details", "relatedNovelsJson", "TEXT")

                // Create reading_stats table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        novelUrl TEXT NOT NULL,
                        novelName TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        readingTimeSeconds INTEGER NOT NULL DEFAULT 0,
                        chaptersRead INTEGER NOT NULL DEFAULT 0,
                        wordsRead INTEGER NOT NULL DEFAULT 0,
                        sessionsCount INTEGER NOT NULL DEFAULT 0,
                        longestSessionSeconds INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reading_stats_date ON reading_stats(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reading_stats_novelUrl ON reading_stats(novelUrl)")

                // Create reading_streak table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_streak (
                        id INTEGER PRIMARY KEY NOT NULL,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        longestStreak INTEGER NOT NULL DEFAULT 0,
                        lastReadDate INTEGER NOT NULL DEFAULT 0,
                        totalDaysRead INTEGER NOT NULL DEFAULT 0,
                        totalReadingTimeSeconds INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // Create bookmarks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        novelUrl TEXT NOT NULL,
                        novelName TEXT NOT NULL,
                        chapterUrl TEXT NOT NULL,
                        chapterName TEXT NOT NULL,
                        segmentId TEXT,
                        segmentIndex INTEGER NOT NULL DEFAULT 0,
                        textSnippet TEXT,
                        note TEXT,
                        category TEXT NOT NULL DEFAULT 'default',
                        color TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_novelUrl ON bookmarks(novelUrl)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_chapterUrl ON bookmarks(chapterUrl)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_category ON bookmarks(category)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_preferences (
                tag TEXT PRIMARY KEY NOT NULL,
                affinityScore INTEGER NOT NULL DEFAULT 0,
                novelCount INTEGER NOT NULL DEFAULT 0,
                chaptersRead INTEGER NOT NULL DEFAULT 0,
                readingTimeSeconds INTEGER NOT NULL DEFAULT 0,
                completedCount INTEGER NOT NULL DEFAULT 0,
                droppedCount INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_preferences_tag ON user_preferences(tag)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create discovered_novels table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS discovered_novels (
                url TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                apiName TEXT NOT NULL,
                posterUrl TEXT,
                rating INTEGER,
                tagsString TEXT,
                author TEXT,
                status TEXT,
                synopsis TEXT,
                source TEXT NOT NULL DEFAULT 'browse',
                discoveredAt INTEGER NOT NULL DEFAULT 0,
                lastVerifiedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_discovered_novels_apiName ON discovered_novels(apiName)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_discovered_novels_discoveredAt ON discovered_novels(discoveredAt)")

                // Also add columns to novel_details if not already there
                safeAddColumn(database, "novel_details", "apiName", "TEXT NOT NULL DEFAULT ''")
                safeAddColumn(database, "novel_details", "chapterCount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(database, "novel_details", "chapters", "TEXT")
                safeAddColumn(database, "novel_details", "cachedAt", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create network_budget table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS network_budget (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                providerName TEXT NOT NULL,
                date INTEGER NOT NULL,
                requestCount INTEGER NOT NULL DEFAULT 0,
                failedCount INTEGER NOT NULL DEFAULT 0,
                inCooldown INTEGER NOT NULL DEFAULT 0,
                cooldownUntil INTEGER NOT NULL DEFAULT 0,
                lastRequestAt INTEGER NOT NULL DEFAULT 0,
                consecutiveFailures INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_network_budget_providerName_date ON network_budget(providerName, date)")

                // Create discovery_chains table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS discovery_chains (
                novelUrl TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT NOT NULL,
                depth INTEGER NOT NULL,
                discoveredAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_discovery_chains_sessionId ON discovery_chains(sessionId)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user_tag_filters table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_tag_filters (
                tag TEXT PRIMARY KEY NOT NULL,
                filterType TEXT NOT NULL,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_tag_filters_filterType ON user_tag_filters(filterType)")

                // Create hidden_novels table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS hidden_novels (
                novelUrl TEXT PRIMARY KEY NOT NULL,
                novelName TEXT NOT NULL,
                reason TEXT NOT NULL,
                hiddenAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hidden_novels_hiddenAt ON hidden_novels(hiddenAt)")

                // Create blocked_authors table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS blocked_authors (
                authorNormalized TEXT PRIMARY KEY NOT NULL,
                displayName TEXT NOT NULL,
                blockedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_blocked_authors_blockedAt ON blocked_authors(blockedAt)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create author_preferences table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS author_preferences (
                authorNormalized TEXT PRIMARY KEY NOT NULL,
                displayName TEXT NOT NULL,
                affinityScore INTEGER NOT NULL DEFAULT 500,
                novelsRead INTEGER NOT NULL DEFAULT 0,
                novelsCompleted INTEGER NOT NULL DEFAULT 0,
                novelsDropped INTEGER NOT NULL DEFAULT 0,
                totalChaptersRead INTEGER NOT NULL DEFAULT 0,
                totalReadingTimeSeconds INTEGER NOT NULL DEFAULT 0,
                novelUrlsInLibrary TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_author_preferences_affinityScore ON author_preferences(affinityScore)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_author_preferences_displayName ON author_preferences(displayName)")
            }
        }
    }
}