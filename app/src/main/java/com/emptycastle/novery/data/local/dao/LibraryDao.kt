package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emptycastle.novery.data.local.entity.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // ============ BASIC QUERIES ============

    @Query("SELECT * FROM library ORDER BY lastReadAt DESC, addedAt DESC")
    fun getAllFlow(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library ORDER BY lastReadAt DESC, addedAt DESC")
    suspend fun getAll(): List<LibraryEntity>

    @Query("SELECT * FROM library WHERE url = :url")
    suspend fun getByUrl(url: String): LibraryEntity?

    @Query("SELECT * FROM library WHERE url = :url")
    fun getByUrlFlow(url: String): Flow<LibraryEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url)")
    suspend fun exists(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE url = :url)")
    fun existsFlow(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LibraryEntity)

    @Update
    suspend fun update(entity: LibraryEntity)

    @Query("UPDATE library SET readingStatus = :status WHERE url = :url")
    suspend fun updateStatus(url: String, status: String)

    // ============ READING POSITION ============

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

    // ============ NEW: CHAPTER COUNT & BADGE TRACKING ============

    /**
     * Update chapter counts after refreshing novel details
     */
    @Query("""
        UPDATE library SET 
            totalChapterCount = :totalCount,
            lastCheckedAt = :checkedAt,
            lastUpdatedAt = CASE 
                WHEN totalChapterCount != :totalCount THEN :checkedAt 
                ELSE lastUpdatedAt 
            END,
            latestChapter = :latestChapter
        WHERE url = :novelUrl
    """)
    suspend fun updateChapterCount(
        novelUrl: String,
        totalCount: Int,
        latestChapter: String?,
        checkedAt: Long = System.currentTimeMillis()
    )

    /**
     * Acknowledge new chapters (clear badge)
     * Called when user views the novel details
     */
    @Query("""
        UPDATE library SET 
            acknowledgedChapterCount = totalChapterCount
        WHERE url = :novelUrl
    """)
    suspend fun acknowledgeNewChapters(novelUrl: String)

    /**
     * Update unread chapter tracking
     */
    @Query("""
        UPDATE library SET 
            lastReadChapterIndex = :chapterIndex,
            unreadChapterCount = :unreadCount
        WHERE url = :novelUrl
    """)
    suspend fun updateUnreadTracking(
        novelUrl: String,
        chapterIndex: Int,
        unreadCount: Int
    )

    /**
     * Get novels with new chapters
     */
    @Query("""
        SELECT * FROM library 
        WHERE totalChapterCount > acknowledgedChapterCount
        ORDER BY lastUpdatedAt DESC
    """)
    suspend fun getNovelsWithNewChapters(): List<LibraryEntity>

    /**
     * Get novels with new chapters as Flow
     */
    @Query("""
        SELECT * FROM library 
        WHERE totalChapterCount > acknowledgedChapterCount
        ORDER BY lastUpdatedAt DESC
    """)
    fun observeNovelsWithNewChapters(): Flow<List<LibraryEntity>>

    /**
     * Get total count of new chapters across all novels
     */
    @Query("""
        SELECT COALESCE(SUM(totalChapterCount - acknowledgedChapterCount), 0)
        FROM library
        WHERE totalChapterCount > acknowledgedChapterCount
    """)
    suspend fun getTotalNewChapterCount(): Int

    /**
     * Get total count as Flow
     */
    @Query("""
        SELECT COALESCE(SUM(totalChapterCount - acknowledgedChapterCount), 0)
        FROM library
        WHERE totalChapterCount > acknowledgedChapterCount
    """)
    fun observeTotalNewChapterCount(): Flow<Int>

    /**
     * Get novels that need refresh (not checked recently)
     */
    @Query("""
        SELECT * FROM library 
        WHERE lastCheckedAt < :threshold
        ORDER BY lastCheckedAt ASC
        LIMIT :limit
    """)
    suspend fun getNovelsNeedingRefresh(threshold: Long, limit: Int = 10): List<LibraryEntity>

    // ============ DELETE ============

    @Query("DELETE FROM library WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM library")
    suspend fun deleteAll()

    // ============ SEARCH ============

    @Query("""
        SELECT * FROM library 
        WHERE name LIKE '%' || :query || '%' 
           OR apiName LIKE '%' || :query || '%'
        ORDER BY lastReadAt DESC, addedAt DESC
    """)
    suspend fun search(query: String): List<LibraryEntity>
}