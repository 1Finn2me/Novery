package com.emptycastle.novery.ui.screens.reader.logic

import com.emptycastle.novery.ui.screens.reader.model.ContentSegment
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.SentenceParser
import org.jsoup.Jsoup

/**
 * Processes raw HTML content into structured segments for the reader.
 * Each segment represents a paragraph with parsed sentences for TTS.
 */
object TextProcessor {

    /**
     * Parses HTML content into a list of ContentSegments.
     * Each segment contains the original HTML, plain text, and parsed sentences.
     */
    fun parseHtmlToSegments(html: String): List<ContentSegment> {
        val cleanedHtml = HtmlUtils.sanitize(html)
        val segments = mutableListOf<ContentSegment>()

        val doc = Jsoup.parse(cleanedHtml)
        val elements = doc.body().children()

        elements.forEachIndexed { index, element ->
            val elementHtml = element.outerHtml()
            val elementText = element.text()

            if (elementText.isNotBlank() || element.tagName() == "img") {
                val parsedParagraph = SentenceParser.parse(elementText)

                segments.add(
                    ContentSegment(
                        id = "seg-$index",
                        html = elementHtml,
                        text = elementText,
                        sentences = parsedParagraph.sentences
                    )
                )
            }
        }

        // Fallback: if no segments were created but we have content
        if (segments.isEmpty() && cleanedHtml.isNotBlank()) {
            val text = HtmlUtils.extractText(cleanedHtml)
            val parsedParagraph = SentenceParser.parse(text)

            segments.add(
                ContentSegment(
                    id = "seg-0",
                    html = cleanedHtml,
                    text = text,
                    sentences = parsedParagraph.sentences
                )
            )
        }

        return segments
    }

    /**
     * Extracts plain text from HTML content.
     */
    fun extractPlainText(html: String): String {
        return HtmlUtils.extractText(html)
    }
}