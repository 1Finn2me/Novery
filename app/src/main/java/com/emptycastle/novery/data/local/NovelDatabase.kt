package com.emptycastle.novery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emptycastle.novery.data.local.dao.HistoryDao
import com.emptycastle.novery.data.local.dao.LibraryDao
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.*

@Database(
    entities = [
        LibraryEntity::class,
        HistoryEntity::class,
        ReadChapterEntity::class,
        OfflineChapterEntity::class,
        OfflineNovelEntity::class,
        NovelDetailsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovelDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao
    abstract fun historyDao(): HistoryDao
    abstract fun offlineDao(): OfflineDao

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
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}