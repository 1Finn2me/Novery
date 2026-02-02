package com.emptycastle.novery.ui.screens.reader.logic

import androidx.compose.ui.text.AnnotatedString
import com.emptycastle.novery.ui.screens.reader.model.ChapterContentItem
import com.emptycastle.novery.ui.screens.reader.model.ContentImage
import com.emptycastle.novery.ui.screens.reader.model.ContentSegment
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.SentenceParser

/**
 * Processes raw HTML content into structured segments for the reader.
 * Each segment represents a paragraph with parsed sentences for TTS.
 * Images are extracted and placed in their original positions.
 */
object TextProcessor {

    /**
     * Parses HTML content into an ordered list of content items.
     * Text and images are interleaved in their original HTML order.
     * This is the primary method to use for proper content ordering.
     */
    fun parseHtmlToOrderedContent(html: String): List<ChapterContentItem> {
        val cleanedHtml = HtmlUtils.sanitize(html)
        val items = mutableListOf<ChapterContentItem>()

        val parsedContent = RichTextParser.parseHtml(cleanedHtml)

        var segmentIndex = 0
        var imageIndex = 0
        var orderIndex = 0

        parsedContent.forEach { content ->
            when (content) {
                is ParsedContent.Text -> {
                    if (content.plainText.isNotBlank()) {
                        val parsedParagraph = SentenceParser.parse(content.plainText)

                        val segment = ContentSegment(
                            id = "seg-$segmentIndex",
                            html = "",
                            text = content.plainText,
                            styledText = content.annotatedString,
                            sentences = parsedParagraph.sentences
                        )

                        items.add(
                            ChapterContentItem.Text(
                                id = "text-$orderIndex",
                                orderIndex = orderIndex,
                                segment = segment
                            )
                        )
                        segmentIndex++
                        orderIndex++
                    }
                }

                is ParsedContent.Image -> {
                    val image = ContentImage(
                        id = "img-$imageIndex",
                        url = content.url,
                        altText = content.altText
                    )

                    items.add(
                        ChapterContentItem.Image(
                            id = "image-$orderIndex",
                            orderIndex = orderIndex,
                            image = image
                        )
                    )
                    imageIndex++
                    orderIndex++
                }
            }
        }

        // Fallback: if no items were created but we have content
        if (items.isEmpty() && cleanedHtml.isNotBlank()) {
            val text = HtmlUtils.extractText(cleanedHtml)
            val parsedParagraph = SentenceParser.parse(text)

            val segment = ContentSegment(
                id = "seg-0",
                html = cleanedHtml,
                text = text,
                styledText = AnnotatedString(text),
                sentences = parsedParagraph.sentences
            )

            items.add(
                ChapterContentItem.Text(
                    id = "text-0",
                    orderIndex = 0,
                    segment = segment
                )
            )
        }

        return items
    }

    /**
     * Legacy method for backward compatibility.
     * Parses HTML content into segments only (no images).
     */
    fun parseHtmlToSegments(html: String): List<ContentSegment> {
        return parseHtmlToOrderedContent(html)
            .filterIsInstance<ChapterContentItem.Text>()
            .map { it.segment }
    }

    /**
     * Extracts plain text from HTML content.
     */
    fun extractPlainText(html: String): String {
        return HtmlUtils.extractText(html)
    }
}