package com.emptycastle.novery.ui.screens.reader.model

import androidx.compose.ui.text.AnnotatedString
import com.emptycastle.novery.ui.screens.reader.logic.BlockType
import com.emptycastle.novery.ui.screens.reader.logic.RuleStyle
import com.emptycastle.novery.ui.screens.reader.logic.SceneBreakStyle
import com.emptycastle.novery.util.ParsedSentence

/**
 * Content segment representing a parsed paragraph with sentences for TTS
 */
data class ContentSegment(
    val id: String,
    val html: String,
    val text: String,
    val styledText: AnnotatedString = AnnotatedString(text),
    val sentences: List<ParsedSentence> = emptyList(),
    val blockType: BlockType = BlockType.NORMAL
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
 * Represents a horizontal rule/divider
 */
data class ContentHorizontalRule(
    val id: String,
    val style: RuleStyle = RuleStyle.SOLID
)

/**
 * Represents a scene break (centered ornament or text)
 */
data class ContentSceneBreak(
    val id: String,
    val symbol: String = "* * *",
    val style: SceneBreakStyle = SceneBreakStyle.ASTERISKS
)

/**
 * Unified content item that preserves order from HTML parsing.
 */
sealed class ChapterContentItem {
    abstract val id: String
    abstract val orderIndex: Int

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

    data class HorizontalRule(
        override val id: String,
        override val orderIndex: Int,
        val rule: ContentHorizontalRule
    ) : ChapterContentItem()

    data class SceneBreak(
        override val id: String,
        override val orderIndex: Int,
        val sceneBreak: ContentSceneBreak
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
        val orderInChapter: Int = 0
    ) : ReaderDisplayItem("segment_${chapterIndex}_${segment.id}")

    data class Image(
        val chapterIndex: Int,
        val chapterUrl: String,
        val image: ContentImage,
        val imageIndexInChapter: Int,
        val orderInChapter: Int = 0
    ) : ReaderDisplayItem("image_${chapterIndex}_${image.id}")

    data class HorizontalRule(
        val chapterIndex: Int,
        val rule: ContentHorizontalRule,
        val orderInChapter: Int = 0
    ) : ReaderDisplayItem("rule_${chapterIndex}_${rule.id}")

    data class SceneBreak(
        val chapterIndex: Int,
        val sceneBreak: ContentSceneBreak,
        val orderInChapter: Int = 0
    ) : ReaderDisplayItem("scenebreak_${chapterIndex}_${sceneBreak.id}")

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