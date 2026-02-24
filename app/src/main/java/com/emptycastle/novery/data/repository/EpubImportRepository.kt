package com.emptycastle.novery.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.emptycastle.novery.data.epub.EpubBook
import com.emptycastle.novery.data.epub.EpubParser
import com.emptycastle.novery.data.local.dao.LibraryDao
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.ChapterEntity
import com.emptycastle.novery.data.local.entity.LibraryEntity
import com.emptycastle.novery.data.local.entity.NovelDetailsEntity
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.data.local.entity.OfflineNovelEntity
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.domain.model.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for importing and managing EPUB files
 */
class EpubImportRepository(
    private val context: Context,
    private val libraryDao: LibraryDao,
    private val offlineDao: OfflineDao
) {
    private val epubParser = EpubParser(context)

    companion object {
        const val IMPORTED_PROVIDER_NAME = "Local"
        const val IMPORTED_URL_PREFIX = "local://epub/"
        private const val COVER_DIR = "imported_covers"
    }

    /**
     * Import progress state
     */
    data class ImportProgress(
        val progress: Float,
        val message: String,
        val currentChapter: Int = 0,
        val totalChapters: Int = 0
    )

    /**
     * Import an EPUB file from URI
     *
     * @param uri The URI of the EPUB file
     * @param onProgress Progress callback
     * @return Result containing the imported Novel or an error
     */
    suspend fun importEpub(
        uri: Uri,
        onProgress: (ImportProgress) -> Unit = { }
    ): Result<Novel> = withContext(Dispatchers.IO) {
        try {
            onProgress(ImportProgress(0.05f, "Opening EPUB file..."))

            // Parse the EPUB
            val parseResult = epubParser.parseEpub(uri)
            val epubBook = parseResult.getOrElse { error ->
                return@withContext Result.failure(error)
            }

            onProgress(ImportProgress(0.2f, "Processing metadata..."))

            // Validate chapters
            if (epubBook.chapters.isEmpty()) {
                return@withContext Result.failure(
                    Exception("No readable chapters found in this EPUB")
                )
            }

            // Generate unique URL for this imported book
            val novelUrl = generateUniqueUrl(epubBook)

            // Check if already imported
            if (libraryDao.exists(novelUrl)) {
                return@withContext Result.failure(
                    Exception("This book has already been imported")
                )
            }

            onProgress(ImportProgress(0.25f, "Saving cover image..."))

            // Save cover image if available
            val coverPath = saveCoverImage(epubBook.coverImage, novelUrl)

            // Create chapter list
            val chapters = epubBook.chapters.map { chapter ->
                Chapter(
                    name = chapter.title,
                    url = "$novelUrl/chapter/${chapter.index}",
                    dateOfRelease = null
                )
            }

            onProgress(ImportProgress(0.3f, "Saving chapters...", 0, chapters.size))

            // Save each chapter as offline content
            val totalChapters = epubBook.chapters.size
            epubBook.chapters.forEachIndexed { index, chapter ->
                val chapterUrl = "$novelUrl/chapter/$index"

                offlineDao.saveChapter(
                    OfflineChapterEntity(
                        url = chapterUrl,
                        novelUrl = novelUrl,
                        title = chapter.title,
                        content = chapter.content,
                        downloadedAt = System.currentTimeMillis()
                    )
                )

                val progress = 0.3f + (0.6f * (index + 1) / totalChapters)
                onProgress(
                    ImportProgress(
                        progress = progress,
                        message = "Saving chapter ${index + 1}/$totalChapters",
                        currentChapter = index + 1,
                        totalChapters = totalChapters
                    )
                )
            }

            onProgress(ImportProgress(0.92f, "Finalizing import..."))

            // Create novel object
            val novel = Novel(
                name = epubBook.title,
                url = novelUrl,
                posterUrl = coverPath,
                apiName = IMPORTED_PROVIDER_NAME,
                latestChapter = chapters.lastOrNull()?.name
            )

            // Save novel metadata
            offlineDao.saveNovel(
                OfflineNovelEntity(
                    url = novelUrl,
                    name = epubBook.title,
                    coverUrl = coverPath
                )
            )

            // Save novel details with chapters
            val novelDetails = NovelDetails(
                url = novelUrl,
                name = epubBook.title,
                chapters = chapters,
                author = epubBook.author,
                posterUrl = coverPath,
                synopsis = epubBook.description,
                status = "Imported",
                tags = listOf("Imported", "Local")
            )

            offlineDao.saveNovelDetails(
                NovelDetailsEntity(
                    url = novelUrl,
                    name = epubBook.title,
                    author = epubBook.author,
                    posterUrl = coverPath,
                    synopsis = epubBook.description,
                    tags = listOf("Imported", "Local"),
                    status = "Imported",
                    chapters = chapters.map { ChapterEntity.fromChapter(it) },
                    apiName = IMPORTED_PROVIDER_NAME,
                    chapterCount = chapters.size,
                    cachedAt = System.currentTimeMillis()
                )
            )

            // Add to library with PLAN_TO_READ status
            libraryDao.insert(
                LibraryEntity(
                    url = novelUrl,
                    name = epubBook.title,
                    posterUrl = coverPath,
                    apiName = IMPORTED_PROVIDER_NAME,
                    latestChapter = chapters.lastOrNull()?.name,
                    addedAt = System.currentTimeMillis(),
                    readingStatus = ReadingStatus.PLAN_TO_READ.name,
                    totalChapterCount = chapters.size,
                    acknowledgedChapterCount = chapters.size
                )
            )

            onProgress(ImportProgress(1.0f, "Import complete!"))

            Result.success(novel)
        } catch (e: Exception) {
            Result.failure(Exception("Import failed: ${e.message}", e))
        }
    }

    /**
     * Get all imported novels
     */
    suspend fun getImportedNovels(): List<Novel> = withContext(Dispatchers.IO) {
        libraryDao.getAll()
            .filter { it.apiName == IMPORTED_PROVIDER_NAME }
            .map { it.toNovel() }
    }

    /**
     * Observe imported novels
     */
    fun observeImportedNovels(): Flow<List<Novel>> {
        return libraryDao.getAllFlow().map { entities ->
            entities
                .filter { it.apiName == IMPORTED_PROVIDER_NAME }
                .map { it.toNovel() }
        }
    }

    /**
     * Get imported novel count
     */
    suspend fun getImportedCount(): Int = withContext(Dispatchers.IO) {
        libraryDao.getAll().count { it.apiName == IMPORTED_PROVIDER_NAME }
    }

    /**
     * Delete an imported novel and all its data
     */
    suspend fun deleteImportedNovel(novelUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Verify it's an imported novel
            val entity = libraryDao.getByUrl(novelUrl)
            if (entity?.apiName != IMPORTED_PROVIDER_NAME) {
                return@withContext Result.failure(
                    Exception("Not an imported novel")
                )
            }

            // Delete cover image
            deleteCoverImage(novelUrl)

            // Delete chapters
            offlineDao.deleteChaptersForNovel(novelUrl)

            // Delete novel metadata
            offlineDao.deleteNovel(novelUrl)
            offlineDao.deleteNovelDetails(novelUrl)

            // Remove from library
            libraryDao.delete(novelUrl)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a novel is imported (local)
     */
    fun isImportedNovel(novelUrl: String): Boolean {
        return novelUrl.startsWith(IMPORTED_URL_PREFIX)
    }

    /**
     * Check if a novel is imported by provider name
     */
    fun isImportedProvider(providerName: String): Boolean {
        return providerName == IMPORTED_PROVIDER_NAME
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /**
     * Generate a unique URL for imported books
     */
    private fun generateUniqueUrl(book: EpubBook): String {
        val identifier = book.identifier
            ?: "${book.title}_${book.author ?: "unknown"}"

        val hash = identifier.hashCode().toUInt().toString(16)
        val timestamp = System.currentTimeMillis().toString(16).takeLast(4)
        return "$IMPORTED_URL_PREFIX${hash}_$timestamp"
    }

    /**
     * Save cover image to app's private storage
     */
    private suspend fun saveCoverImage(bitmap: Bitmap?, novelUrl: String): String? {
        if (bitmap == null) return null

        return withContext(Dispatchers.IO) {
            try {
                val coverDir = File(context.filesDir, COVER_DIR)
                if (!coverDir.exists()) {
                    coverDir.mkdirs()
                }

                val fileName = novelUrl
                    .removePrefix(IMPORTED_URL_PREFIX)
                    .replace("/", "_") + ".jpg"
                val coverFile = File(coverDir, fileName)

                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                // Return file:// URI that can be loaded by image loaders
                "file://${coverFile.absolutePath}"
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Delete cover image from storage
     */
    private fun deleteCoverImage(novelUrl: String) {
        try {
            val coverDir = File(context.filesDir, COVER_DIR)
            val fileName = novelUrl
                .removePrefix(IMPORTED_URL_PREFIX)
                .replace("/", "_") + ".jpg"
            val coverFile = File(coverDir, fileName)
            if (coverFile.exists()) {
                coverFile.delete()
            }
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }
}