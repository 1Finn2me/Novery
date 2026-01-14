package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.HistoryEntity
import com.emptycastle.novery.data.local.entity.ReadChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    fun getAllFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 50")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE novelUrl = :novelUrl")
    suspend fun getByNovelUrl(novelUrl: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE novelUrl = :novelUrl")
    suspend fun deleteByNovelUrl(novelUrl: String)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    // Read chapters tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markChapterRead(entity: ReadChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markChaptersRead(entities: List<ReadChapterEntity>)

    @Query("SELECT chapterUrl FROM read_chapters WHERE novelUrl = :novelUrl")
    suspend fun getReadChapterUrls(novelUrl: String): List<String>

    @Query("SELECT chapterUrl FROM read_chapters WHERE novelUrl = :novelUrl")
    fun getReadChapterUrlsFlow(novelUrl: String): Flow<List<String>>

    @Query("DELETE FROM read_chapters WHERE novelUrl = :novelUrl AND chapterUrl = :chapterUrl")
    suspend fun markChapterUnread(novelUrl: String, chapterUrl: String)

    @Query("DELETE FROM read_chapters WHERE novelUrl = :novelUrl AND chapterUrl IN (:chapterUrls)")
    suspend fun markChaptersUnread(novelUrl: String, chapterUrls: List<String>)

    @Query("DELETE FROM read_chapters WHERE novelUrl = :novelUrl")
    suspend fun clearReadChapters(novelUrl: String)

    @Query("SELECT COUNT(*) FROM read_chapters WHERE novelUrl = :novelUrl")
    suspend fun getReadCountForNovel(novelUrl: String): Int
}