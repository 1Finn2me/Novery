// com/emptycastle/novery/data/repository/BookmarkRepository.kt
package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.BookmarkDao
import com.emptycastle.novery.data.local.entity.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for bookmark operations
 */
class BookmarkRepository(
    private val bookmarkDao: BookmarkDao
) {

    // ================================================================
    // OBSERVE BOOKMARKS
    // ================================================================

    fun observeAll(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    fun observeForNovel(novelUrl: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.observeForNovel(novelUrl)
    }

    fun observeByCategory(category: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.observeByCategory(category)
    }

    fun observeCategories(): Flow<List<String>> = bookmarkDao.observeCategories()

    // ================================================================
    // GET BOOKMARKS
    // ================================================================

    suspend fun getForChapter(chapterUrl: String): List<BookmarkEntity> =
        withContext(Dispatchers.IO) { bookmarkDao.getForChapter(chapterUrl) }

    suspend fun getRecent(limit: Int = 20): List<BookmarkEntity> =
        withContext(Dispatchers.IO) { bookmarkDao.getRecent(limit) }

    suspend fun getById(id: Long): BookmarkEntity? =
        withContext(Dispatchers.IO) { bookmarkDao.getById(id) }

    suspend fun search(query: String): List<BookmarkEntity> =
        withContext(Dispatchers.IO) { bookmarkDao.search(query) }

    suspend fun countForNovel(novelUrl: String): Int =
        withContext(Dispatchers.IO) { bookmarkDao.countForNovel(novelUrl) }

    // ================================================================
    // VIEWMODEL-FRIENDLY API (matches your toggleBookmark())
    // ================================================================

    /**
     * Add (or update) a bookmark for a given novel+chapter.
     *
     * Maps:
     * - position -> BookmarkEntity.segmentIndex
     * - timestamp -> updatedAt (and createdAt for new rows)
     *
     * NOTE: your VM doesn’t pass novelName; we keep the existing one if present,
     * otherwise store an empty string.
     */
    suspend fun addBookmark(
        novelUrl: String,
        chapterUrl: String,
        chapterName: String,
        position: Int,
        timestamp: Long
    ): Long = withContext(Dispatchers.IO) {
        // DAO only has getForChapter(), so we filter by novelUrl here.
        val existing = bookmarkDao.getForChapter(chapterUrl)
            .firstOrNull { it.novelUrl == novelUrl }

        if (existing != null) {
            val updated = existing.copy(
                chapterName = chapterName,
                segmentIndex = position,
                updatedAt = timestamp
            )
            bookmarkDao.update(updated)
            existing.id
        } else {
            val entity = BookmarkEntity(
                novelUrl = novelUrl,
                novelName = "", // VM doesn't provide it; consider adding it later
                chapterUrl = chapterUrl,
                chapterName = chapterName,
                segmentIndex = position,
                // if your entity has createdAt/updatedAt fields (it has updatedAt for sure)
                createdAt = timestamp,
                updatedAt = timestamp
            )
            bookmarkDao.insert(entity)
        }
    }

    /**
     * Remove bookmark(s) for the given novel+chapter.
     */
    suspend fun removeBookmark(
        novelUrl: String,
        chapterUrl: String
    ) = withContext(Dispatchers.IO) {
        val matches = bookmarkDao.getForChapter(chapterUrl)
            .filter { it.novelUrl == novelUrl }

        matches.forEach { bookmarkDao.delete(it) }
    }

    /**
     * Optional helper if you need it elsewhere.
     */
    suspend fun isBookmarked(novelUrl: String, chapterUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            bookmarkDao.getForChapter(chapterUrl).any { it.novelUrl == novelUrl }
        }

    // ================================================================
    // EXISTING API (kept as-is)
    // ================================================================

    suspend fun createBookmark(
        novelUrl: String,
        novelName: String,
        chapterUrl: String,
        chapterName: String,
        segmentId: String? = null,
        segmentIndex: Int = 0,
        textSnippet: String? = null,
        note: String? = null,
        category: String = "default",
        color: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = BookmarkEntity(
            novelUrl = novelUrl,
            novelName = novelName,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            segmentId = segmentId,
            segmentIndex = segmentIndex,
            textSnippet = textSnippet?.take(200),
            note = note,
            category = category,
            color = color
        )
        bookmarkDao.insert(entity)
    }

    suspend fun quickBookmark(
        novelUrl: String,
        novelName: String,
        chapterUrl: String,
        chapterName: String,
        segmentIndex: Int = 0
    ): Long {
        return createBookmark(
            novelUrl = novelUrl,
            novelName = novelName,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            segmentIndex = segmentIndex
        )
    }

    // UPDATE / DELETE / CATEGORIES unchanged...

    suspend fun updateNote(id: Long, note: String?) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getById(id) ?: return@withContext
        val updated = existing.copy(
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        bookmarkDao.update(updated)
    }

    suspend fun updateCategory(id: Long, category: String) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getById(id) ?: return@withContext
        val updated = existing.copy(
            category = category,
            updatedAt = System.currentTimeMillis()
        )
        bookmarkDao.update(updated)
    }

    suspend fun updateColor(id: Long, color: String?) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getById(id) ?: return@withContext
        val updated = existing.copy(
            color = color,
            updatedAt = System.currentTimeMillis()
        )
        bookmarkDao.update(updated)
    }

    suspend fun update(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.update(bookmark.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    suspend fun delete(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmarkDao.delete(bookmark)
    }

    fun getDefaultCategories(): List<String> = listOf(
        "default",
        "favorites",
        "important",
        "quotes",
        "to-review"
    )

    fun getAvailableColors(): List<String> = listOf(
        "#FFEB3B", "#4CAF50", "#2196F3", "#E91E63",
        "#FF9800", "#9C27B0", "#00BCD4", "#F44336"
    )
}