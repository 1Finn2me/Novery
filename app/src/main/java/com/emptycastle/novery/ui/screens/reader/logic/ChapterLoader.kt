package com.emptycastle.novery.ui.screens.reader.logic

import android.content.Context
import com.emptycastle.novery.data.reader.AbstractBook
import com.emptycastle.novery.data.repository.NovelRepository
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.screens.reader.model.LoadedChapter
import com.emptycastle.novery.util.reader.QNTSHelper
import com.emptycastle.novery.util.reader.QNTranslationHelper
import com.emptycastle.novery.util.reader.splitByParagraphs
import com.emptycastle.novery.util.reader.toAnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of a chapter load operation.
 */
sealed class ChapterLoadResult {
    data class Success(val loadedChapter: LoadedChapter) : ChapterLoadResult()
    data class Error(val chapterIndex: Int, val chapter: Chapter, val message: String) : ChapterLoadResult()
}

/**
 * Handles loading individual chapter content.
 * Single responsibility: Load and parse one chapter.
 */
class ChapterLoader(
    private val novelRepository: NovelRepository
) {
    private var currentProvider: MainProvider? = null

    /**
     * Configure the loader with the current provider.
     */
    fun configure(provider: MainProvider) {
        currentProvider = provider
    }

    /**
     * Loads and parses chapter content using AbstractBook.
     */
    suspend fun loadChapter(
        context: Context,
        book: AbstractBook,
        chapterIndex: Int,
        sourceLang: String = "auto",
        targetLang: String = "es",
        translationEnabled: Boolean = false,
        useOnline: Boolean = false,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): ChapterLoadResult = withContext(Dispatchers.IO) {
        try {
            val chapterName = book.getChapterTitle(chapterIndex)
            val chapter = Chapter(name = chapterName, url = book.getLoadingStatus(chapterIndex) ?: "epub://$chapterIndex")

            val rawHtml = book.getChapterData(chapterIndex, reload = false)
            
            // 1. Pre-parse HTML (Strip junk, author notes)
            val cleanHtml = QNTSHelper.preParseHtml(rawHtml, authorNotes = true) 
            
            // 2. Parse into logical segments using Markwon for rich text
            val markwon = io.noties.markwon.Markwon.builder(context)
                .usePlugin(io.noties.markwon.html.HtmlPlugin.create())
                .build()
            
            val spanned = markwon.render(markwon.parse(cleanHtml))
            
            // Split spanned into paragraphs (blocks)
            val paragraphs = spanned.splitByParagraphs()
            
            // 3. Translation with Cache
            val translatedParagraphs: List<androidx.compose.ui.text.AnnotatedString> = if (translationEnabled && paragraphs.isNotEmpty()) {
                val plainTexts = paragraphs.map { it.toString() }
                val translatedTexts = QNTranslationHelper.translateWithCache(
                    context = context,
                    paragraphs = plainTexts,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    useOnline = useOnline,
                    onProgress = onProgress
                )
                
                // Re-create Spanned/AnnotatedString from translated text
                translatedTexts.map { it.toAnnotatedString() }
            } else {
                paragraphs.map { it.toAnnotatedString() }
            }

            // 4. Re-construct ordered content
            val orderedContent = translatedParagraphs.mapIndexed { index, annotatedString ->
                val text = annotatedString.text
                val segment = com.emptycastle.novery.ui.screens.reader.model.ContentSegment(
                    id = "seg-$chapterIndex-$index",
                    html = "",
                    text = text,
                    styledText = annotatedString,
                    sentences = com.emptycastle.novery.util.SentenceParser.parse(text).sentences
                )
                com.emptycastle.novery.ui.screens.reader.model.ChapterContentItem.Text(
                    id = "text-$chapterIndex-$index",
                    orderIndex = index,
                    segment = segment
                )
            }

            val isFromCache = !chapter.url.startsWith("epub://") && novelRepository.isChapterOffline(chapter.url)

            ChapterLoadResult.Success(
                LoadedChapter(
                    chapter = chapter,
                    chapterIndex = chapterIndex,
                    contentItems = orderedContent,
                    isLoading = false,
                    isFromCache = isFromCache
                )
            )
        } catch (e: Exception) {
            val chapterName = try { book.getChapterTitle(chapterIndex) } catch(ex: Exception) { "Chapter $chapterIndex" }
            val chapter = Chapter(name = chapterName, url = "error://$chapterIndex")
            ChapterLoadResult.Error(
                chapterIndex = chapterIndex,
                chapter = chapter,
                message = e.message ?: "Failed to load chapter"
            )
        }
    }

    /**
     * Creates a loading placeholder for a chapter.
     */
    fun createLoadingChapter(chapter: Chapter, chapterIndex: Int): LoadedChapter {
        return LoadedChapter(
            chapter = chapter,
            chapterIndex = chapterIndex,
            contentItems = emptyList(),
            isLoading = true
        )
    }

    /**
     * Creates an error placeholder for a chapter.
     */
    fun createErrorChapter(chapter: Chapter, chapterIndex: Int, error: String): LoadedChapter {
        return LoadedChapter(
            chapter = chapter,
            chapterIndex = chapterIndex,
            contentItems = emptyList(),
            isLoading = false,
            error = error
        )
    }
}
