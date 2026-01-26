package com.emptycastle.novery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emptycastle.novery.data.local.dao.BookmarkDao
import com.emptycastle.novery.data.local.dao.HistoryDao
import com.emptycastle.novery.data.local.dao.LibraryDao
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.dao.StatsDao
import com.emptycastle.novery.data.local.entity.BookmarkEntity
import com.emptycastle.novery.data.local.entity.HistoryEntity
import com.emptycastle.novery.data.local.entity.LibraryEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import com.emptycastle.novery.data.local.entity.ReadChapterEntity
import com.emptycastle.novery.data.local.entity.ReadingStatsEntity
import com.emptycastle.novery.data.local.entity.ReadingStreakEntity

/**
 * Type converters for Room database
 */
class DatabaseConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split("|||")
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
        BookmarkEntity::class
    ],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                INSTANCE = instance
                instance
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
        }
    }
}