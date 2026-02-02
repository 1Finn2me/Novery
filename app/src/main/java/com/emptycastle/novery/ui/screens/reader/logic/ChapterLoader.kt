package com.emptycastle.novery.ui.screens.reader.logic

import com.emptycastle.novery.data.repository.NovelRepository
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.ui.screens.reader.model.LoadedChapter

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
     * Loads and parses chapter content.
     * Content items (text and images) are returned in their original HTML order.
     */
    suspend fun loadChapter(
        chapter: Chapter,
        chapterIndex: Int
    ): ChapterLoadResult {
        val provider = currentProvider
            ?: return ChapterLoadResult.Error(
                chapterIndex = chapterIndex,
                chapter = chapter,
                message = "No provider configured"
            )

        return novelRepository.loadChapterContent(provider, chapter.url)
            .fold(
                onSuccess = { content ->
                    // Parse HTML into ordered content items (text + images interleaved)
                    val orderedContent = TextProcessor.parseHtmlToOrderedContent(content)
                    val isFromCache = novelRepository.isChapterOffline(chapter.url)

                    ChapterLoadResult.Success(
                        LoadedChapter(
                            chapter = chapter,
                            chapterIndex = chapterIndex,
                            contentItems = orderedContent,
                            isLoading = false,
                            isFromCache = isFromCache
                        )
                    )
                },
                onFailure = { error ->
                    ChapterLoadResult.Error(
                        chapterIndex = chapterIndex,
                        chapter = chapter,
                        message = error.message ?: "Failed to load chapter"
                    )
                }
            )
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