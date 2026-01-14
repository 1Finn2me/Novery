package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library ORDER BY lastReadAt DESC, addedAt DESC")
    fun getAllFlow(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library ORDER BY lastReadAt DESC, addedAt DESC")
    suspend fun getAll(): List<LibraryEntity>

    @Query("SELECT * FROM library WHERE url = :url")
    suspend fun getByUrl(url: String): LibraryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url)")
    suspend fun exists(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url)")
    fun existsFlow(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LibraryEntity)

    @Query("UPDATE library SET readingStatus = :status WHERE url = :url")
    suspend fun updateStatus(url: String, status: String)

    @Query("""
        UPDATE library SET 
            lastChapterUrl = :chapterUrl,
            lastChapterName = :chapterName,
            lastReadAt = :timestamp,
            lastScrollIndex = :scrollIndex,
            lastScrollOffset = :scrollOffset
        WHERE url = :novelUrl
    """)
    suspend fun updateReadingPosition(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        timestamp: Long,
        scrollIndex: Int,
        scrollOffset: Int
    )

    @Query("""
        UPDATE library SET 
            lastChapterUrl = :chapterUrl,
            lastChapterName = :chapterName,
            lastReadAt = :timestamp
        WHERE url = :novelUrl
    """)
    suspend fun updateLastChapter(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        timestamp: Long
    )

    @Query("DELETE FROM library WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM library")
    suspend fun deleteAll()

    // Search query
    @Query("""
        SELECT * FROM library 
        WHERE name LIKE '%' || :query || '%' 
           OR apiName LIKE '%' || :query || '%'
        ORDER BY lastReadAt DESC, addedAt DESC
    """)
    suspend fun search(query: String): List<LibraryEntity>
}