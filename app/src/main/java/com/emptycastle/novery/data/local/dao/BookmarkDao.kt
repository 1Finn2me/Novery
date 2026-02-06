// com/emptycastle/novery/data/local/dao/BookmarkDao.kt

package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emptycastle.novery.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE novelUrl = :novelUrl ORDER BY createdAt DESC")
    fun observeForNovel(novelUrl: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE chapterUrl = :chapterUrl ORDER BY segmentIndex")
    suspend fun getForChapter(chapterUrl: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<BookmarkEntity>>

    @Query("SELECT DISTINCT category FROM bookmarks ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<BookmarkEntity>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE novelUrl = :novelUrl")
    suspend fun countForNovel(novelUrl: String): Int

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM bookmarks 
        WHERE note LIKE '%' || :query || '%' 
           OR textSnippet LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    suspend fun search(query: String): List<BookmarkEntity>
}