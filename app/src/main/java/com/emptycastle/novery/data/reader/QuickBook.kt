package com.emptycastle.novery.data.reader

import com.emptycastle.novery.domain.model.NovelDetails
import com.emptycastle.novery.provider.MainProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class QuickBook(
    private val details: NovelDetails,
    private val provider: MainProvider
) : AbstractBook() {

    private val novelRepository = com.emptycastle.novery.data.repository.RepositoryProvider.getNovelRepository()

    override val canReload: Boolean = true

    override fun resolveUrl(url: String): String {
        return url
    }

    override fun size(): Int = details.chapters.size

    override fun title(): String = details.name

    override fun getChapterTitle(index: Int): String {
        return details.chapters.getOrNull(index)?.name ?: "Chapter ${index + 1}"
    }

    override fun getLoadingStatus(index: Int): String? {
        return details.chapters.getOrNull(index)?.url
    }

    override suspend fun getChapterData(index: Int, reload: Boolean): String {
        val chapter = details.chapters.getOrNull(index) ?: throw Exception("Chapter not found")
        
        // Use repository to prioritize offline content
        val result = novelRepository.loadChapterContent(
            provider = provider,
            chapterUrl = chapter.url,
            novelUrl = details.url,
            chapterTitle = chapter.name,
            mode = if (reload) com.emptycastle.novery.data.repository.LoadingMode.NETWORK_ONLY 
                   else com.emptycastle.novery.data.repository.LoadingMode.OFFLINE_FIRST,
            translationEnabled = false // Let ReaderViewModel/ChapterLoader handle translation
        )
        
        return result.getOrThrow()
    }

    override fun expand(last: String): Boolean {
        return false
    }

    override fun author(): String? = details.author

    override suspend fun posterBytes(): ByteArray? = withContext(Dispatchers.IO) {
        details.posterUrl?.let { url ->
            try {
                URL(url).openStream().use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        }
    }
}
