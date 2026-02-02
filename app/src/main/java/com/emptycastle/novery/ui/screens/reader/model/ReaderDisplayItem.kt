package com.emptycastle.novery.ui.screens.reader.model

import androidx.compose.ui.text.AnnotatedString
import com.emptycastle.novery.util.ParsedSentence

/**
 * Content segment representing a parsed paragraph with sentences for TTS
 */
data class ContentSegment(
    val id: String,
    val html: String,
    val text: String,
    val styledText: AnnotatedString = AnnotatedString(text),
    val sentences: List<ParsedSentence> = emptyList()
) {
    val sentenceCount: Int get() = sentences.size
    fun getSentenceText(index: Int): String? = sentences.getOrNull(index)?.text
    fun getSentence(index: Int): ParsedSentence? = sentences.getOrNull(index)
}

/**
 * Represents an image extracted from chapter content
 */
data class ContentImage(
    val id: String,
    val url: String,
    val altText: String? = null
)

/**
 * Unified content item that preserves order from HTML parsing.
 * Can be either text or an image.
 */
sealed class ChapterContentItem {
    abstract val id: String
    abstract val orderIndex: Int // Position in the original HTML

    data class Text(
        override val id: String,
        override val orderIndex: Int,
        val segment: ContentSegment
    ) : ChapterContentItem()

    data class Image(
        override val id: String,
        override val orderIndex: Int,
        val image: ContentImage
    ) : ChapterContentItem()
}

/**
 * Sealed class representing different types of items displayed in the reader's LazyColumn
 */
sealed class ReaderDisplayItem(open val itemId: String) {

    data class ChapterHeader(
        val chapterIndex: Int,
        val chapterName: String,
        val chapterNumber: Int,
        val totalChapters: Int
    ) : ReaderDisplayItem("header_$chapterIndex")

    data class Segment(
        val chapterIndex: Int,
        val chapterUrl: String,
        val segment: ContentSegment,
        val segmentIndexInChapter: Int,
        val globalSegmentIndex: Int = 0,
        val orderInChapter: Int = 0 // Position including images
    ) : ReaderDisplayItem("segment_${chapterIndex}_${segment.id}")

    data class Image(
        val chapterIndex: Int,
        val chapterUrl: String,
        val image: ContentImage,
        val imageIndexInChapter: Int,
        val orderInChapter: Int = 0 // Position including text segments
    ) : ReaderDisplayItem("image_${chapterIndex}_${image.id}")

    data class ChapterDivider(
        val chapterIndex: Int,
        val chapterName: String,
        val chapterNumber: Int,
        val totalChapters: Int,
        val hasNextChapter: Boolean
    ) : ReaderDisplayItem("divider_$chapterIndex")

    data class LoadingIndicator(
        val chapterIndex: Int
    ) : ReaderDisplayItem("loading_$chapterIndex")

    data class ErrorIndicator(
        val chapterIndex: Int,
        val error: String
    ) : ReaderDisplayItem("error_$chapterIndex")
}