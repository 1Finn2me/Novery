package com.emptycastle.novery.ui.screens.reader.model

import com.emptycastle.novery.util.ParsedSentence

/**
 * Content segment representing a parsed paragraph with sentences for TTS
 */
data class ContentSegment(
    val id: String,
    val html: String,
    val text: String,
    val sentences: List<ParsedSentence> = emptyList()
) {
    val sentenceCount: Int get() = sentences.size
    fun getSentenceText(index: Int): String? = sentences.getOrNull(index)?.text
    fun getSentence(index: Int): ParsedSentence? = sentences.getOrNull(index)
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
        val globalSegmentIndex: Int = 0
    ) : ReaderDisplayItem("segment_${chapterIndex}_${segment.id}")

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