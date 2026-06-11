package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {

    // ============ OFFLINE CHAPTERS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapter(chapter: OfflineChapterEntity)

    @Query("SELECT * FROM offline_chapters WHERE url = :url")
    suspend fun getChapter(url: String): OfflineChapterEntity?

    @Query("SELECT url FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getDownloadedChapterUrls(novelUrl: String): List<String>

    @Query("SELECT url FROM offline_chapters WHERE novelUrl = :novelUrl")
    fun getDownloadedChapterUrlsFlow(novelUrl: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun getDownloadedCount(novelUrl: String): Int

    @Query("""
        SELECT oc.novelUrl, COUNT(*) as count 
        FROM offline_chapters oc
        INNER JOIN offline_novels ON oc.novelUrl = offline_novels.url
        GROUP BY oc.novelUrl
    """)
    suspend fun getAllNovelCounts(): List<NovelChapterCount>

    @Query("""
        SELECT oc.novelUrl, 
               COUNT(*) as count, 
               MAX(oc.downloadedAt) as lastDownloadedAt 
        FROM offline_chapters oc
        INNER JOIN offline_novels ON oc.novelUrl = offline_novels.url
        GROUP BY oc.novelUrl
    """)
    suspend fun getAllNovelDownloadData(): List<NovelDownloadData>

    @Query("DELETE FROM offline_chapters WHERE novelUrl = :novelUrl")
    suspend fun deleteChaptersForNovel(novelUrl: String)

    @Query("DELETE FROM offline_chapters WHERE url IN (:urls)")
    suspend fun deleteChapters(urls: List<String>)

    // ============ OFFLINE NOVELS (Metadata) ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNovel(novel: OfflineNovelEntity)

    @Query("SELECT * FROM offline_novels WHERE url = :url")
    suspend fun getNovel(url: String): OfflineNovelEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM offline_novels WHERE url = :url)")
    suspend fun isNovelDownloaded(url: String): Boolean

    @Query("DELETE FROM offline_novels WHERE url = :url")
    suspend fun deleteNovel(url: String)

    // ============ NOVEL DETAILS CACHE ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNovelDetails(details: NovelDetailsEntity)

    @Query("SELECT * FROM novel_details WHERE url = :url")
    suspend fun getNovelDetails(url: String): NovelDetailsEntity?

    @Query("DELETE FROM novel_details WHERE url = :url")
    suspend fun deleteNovelDetails(url: String)

    @Query("SELECT * FROM novel_details")
    suspend fun getAllNovelDetails(): List<NovelDetailsEntity>

    @Query("SELECT * FROM offline_chapters")
    suspend fun getAllChapters(): List<OfflineChapterEntity>

    @Query("DELETE FROM offline_chapters")
    suspend fun deleteAllChapters()

    @Query("DELETE FROM offline_novels")
    suspend fun deleteAllNovels()

    @Query("DELETE FROM novel_details")
    suspend fun deleteAllNovelDetails()
    /**
     * Get download info only for novels that have been explicitly registered as downloaded.
     * This excludes novels that only have chapters in the temporary reading cache.
     */
    @Query("""
    SELECT oc.novelUrl, 
           COUNT(*) as chapterCount, 
           MAX(oc.downloadedAt) as lastDownloadedAt 
    FROM offline_chapters oc
    INNER JOIN offline_novels ON oc.novelUrl = offline_novels.url
    GROUP BY oc.novelUrl
""")
    suspend fun getAllDownloadInfo(): List<DownloadInfoTuple>

    /**
     * Tuple for download info query result
     */
    data class DownloadInfoTuple(
        val novelUrl: String,
        val chapterCount: Int,
        val lastDownloadedAt: Long
    )

    // ============ CUSTOM COVER ============

    @Query("UPDATE novel_details SET customCoverUrl = :coverUrl WHERE url = :novelUrl")
    suspend fun updateNovelDetailsCustomCover(novelUrl: String, coverUrl: String?)

    @Query("UPDATE offline_novels SET customCoverUrl = :coverUrl WHERE url = :novelUrl")
    suspend fun updateOfflineNovelCustomCover(novelUrl: String, coverUrl: String?)
}

/**
 * Helper class for count query results
 */
data class NovelChapterCount(
    val novelUrl: String,
    val count: Int
)

/**
 * Helper class for download data with timestamps
 */
data class NovelDownloadData(
    val novelUrl: String,
    val count: Int,
    val lastDownloadedAt: Long?
)