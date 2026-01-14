package com.emptycastle.novery.util

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * HTML utility functions for parsing and cleaning content.
 */
object HtmlUtils {

    /**
     * Sanitize HTML content, removing potentially dangerous elements.
     */
    fun sanitize(html: String): String {
        val safelist = Safelist.relaxed()
            .addTags("span", "div", "p", "br", "hr")
            .addAttributes(":all", "style", "class")
            .removeTags("script", "iframe", "form", "input")

        return Jsoup.clean(html, safelist)
    }

    /**
     * Extract plain text from HTML.
     */
    fun extractText(html: String): String {
        return Jsoup.parse(html).text()
    }

    /**
     * Clean novel chapter content (remove ads, site watermarks, etc.)
     */
    fun cleanChapterContent(html: String, siteName: String = ""): String {
        var cleaned = html

        // Remove common ad/watermark patterns
        cleaned = cleaned.replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")

        // Remove site-specific watermarks
        val watermarkPatterns = listOf(
            "\\[Updated from F r e e w e b n o v e l\\. c o m\\]",
            "If you find any errors \\( broken links.*?\\)",
            "Please let us know.*?report chapter.*?so we can fix it",
            "libread\\.com",
            "novelbin\\.com"
        )

        watermarkPatterns.forEach { pattern ->
            cleaned = cleaned.replace(Regex(pattern, RegexOption.IGNORE_CASE), "")
        }

        if (siteName.isNotBlank()) {
            cleaned = cleaned.replace(siteName, "", ignoreCase = true)
        }

        return cleaned.trim()
    }

    /**
     * Clean text for TTS reading.
     */
    fun cleanForTts(html: String): String {
        var text = html

        // Convert common HTML entities
        text = text.replace("&nbsp;", " ")
        text = text.replace("&amp;", "&")
        text = text.replace("&lt;", "<")
        text = text.replace("&gt;", ">")
        text = text.replace("...", "…")

        // Remove HTML tags
        text = Jsoup.parse(text).text()

        // Remove translator/editor notes
        text = text.replace(Regex("Translator:.*?Editor:.*", RegexOption.IGNORE_CASE), "")

        // Normalize whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }
}