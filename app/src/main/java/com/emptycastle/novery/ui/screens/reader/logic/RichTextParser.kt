package com.emptycastle.novery.ui.screens.reader.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Represents a parsed content item - either styled text or an image
 */
sealed class ParsedContent {
    data class Text(
        val annotatedString: AnnotatedString,
        val plainText: String
    ) : ParsedContent()

    data class Image(
        val url: String,
        val altText: String? = null
    ) : ParsedContent()
}

/**
 * Style state tracking during HTML parsing
 */
private data class StyleState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isSubscript: Boolean = false,
    val isSuperscript: Boolean = false,
    val linkUrl: String? = null,
    val textColor: Color? = null
)

/**
 * Accumulator for building text segments
 */
private class TextAccumulator {
    private val annotatedBuilder = AnnotatedString.Builder()
    private val plainBuilder = StringBuilder()

    val length: Int get() = annotatedBuilder.length
    val isEmpty: Boolean get() = plainBuilder.toString().isBlank()
    val isNotEmpty: Boolean get() = !isEmpty

    fun append(text: String, style: StyleState) {
        if (text.isEmpty()) return

        val startIndex = annotatedBuilder.length
        annotatedBuilder.append(text)
        plainBuilder.append(text)

        val spanStyle = buildSpanStyle(style)
        if (spanStyle != SpanStyle()) {
            annotatedBuilder.addStyle(spanStyle, startIndex, annotatedBuilder.length)
        }

        style.linkUrl?.let { url ->
            annotatedBuilder.addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = startIndex,
                end = annotatedBuilder.length
            )
        }
    }

    fun appendNewline() {
        annotatedBuilder.append("\n")
        plainBuilder.append("\n")
    }

    fun build(): ParsedContent.Text? {
        val plain = plainBuilder.toString().trim()
        if (plain.isBlank()) return null
        return ParsedContent.Text(
            annotatedString = annotatedBuilder.toAnnotatedString(),
            plainText = plain
        )
    }

    fun clear() {
        // AnnotatedString.Builder doesn't have clear(), so we just track that we need a new one
    }

    private fun buildSpanStyle(state: StyleState): SpanStyle {
        val decorations = mutableListOf<TextDecoration>()
        if (state.isUnderline) decorations.add(TextDecoration.Underline)
        if (state.isStrikethrough) decorations.add(TextDecoration.LineThrough)

        return SpanStyle(
            fontWeight = if (state.isBold) FontWeight.Bold else null,
            fontStyle = if (state.isItalic) FontStyle.Italic else null,
            textDecoration = if (decorations.isNotEmpty()) {
                TextDecoration.combine(decorations)
            } else null,
            color = state.textColor ?: Color.Unspecified
        )
    }
}

/**
 * Parses HTML content into styled AnnotatedString and extracts images.
 * Images are placed in their exact position within the content flow.
 */
object RichTextParser {

    // Tags that indicate bold text
    private val boldTags = setOf("b", "strong")

    // Tags that indicate italic text
    private val italicTags = setOf("i", "em", "cite", "dfn", "var")

    // Tags that indicate underline
    private val underlineTags = setOf("u", "ins")

    // Tags that indicate strikethrough
    private val strikethroughTags = setOf("s", "del", "strike")

    // Block-level tags that create paragraph breaks
    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "li", "tr", "article", "section", "aside",
        "figure", "figcaption", "header", "footer", "main", "nav"
    )

    // Tags that should be treated as line breaks
    private val breakTags = setOf("br", "hr")

    /**
     * Parses an HTML string into a list of ParsedContent items.
     * Text and images are returned in their exact document order.
     */
    fun parseHtml(html: String): List<ParsedContent> {
        if (html.isBlank()) return emptyList()

        val document = Jsoup.parse(html)
        val results = mutableListOf<ParsedContent>()

        // Process the body using ordered traversal
        processNodeChildren(document.body(), results, StyleState())

        return results.filter { content ->
            when (content) {
                is ParsedContent.Text -> content.plainText.isNotBlank()
                is ParsedContent.Image -> content.url.isNotBlank()
            }
        }
    }

    /**
     * Process all children of a node in document order.
     * This is the main entry point for recursive traversal.
     */
    private fun processNodeChildren(
        parent: Element,
        results: MutableList<ParsedContent>,
        inheritedStyle: StyleState
    ) {
        var currentTextBuilder: AnnotatedString.Builder? = null
        var currentPlainBuilder: StringBuilder? = null
        var textStartStyle = inheritedStyle

        fun flushText() {
            val annotated = currentTextBuilder?.toAnnotatedString()
            val plain = currentPlainBuilder?.toString()?.trim()

            if (annotated != null && !plain.isNullOrBlank()) {
                results.add(ParsedContent.Text(annotated, plain))
            }

            currentTextBuilder = null
            currentPlainBuilder = null
        }

        fun ensureTextBuilder(): Pair<AnnotatedString.Builder, StringBuilder> {
            if (currentTextBuilder == null) {
                currentTextBuilder = AnnotatedString.Builder()
                currentPlainBuilder = StringBuilder()
                textStartStyle = inheritedStyle
            }
            return Pair(currentTextBuilder!!, currentPlainBuilder!!)
        }

        fun appendText(text: String, style: StyleState) {
            if (text.isEmpty()) return

            val (annotatedBuilder, plainBuilder) = ensureTextBuilder()
            val startIndex = annotatedBuilder.length
            annotatedBuilder.append(text)
            plainBuilder.append(text)

            val spanStyle = buildSpanStyle(style)
            if (spanStyle != SpanStyle()) {
                annotatedBuilder.addStyle(spanStyle, startIndex, annotatedBuilder.length)
            }

            style.linkUrl?.let { url ->
                annotatedBuilder.addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = startIndex,
                    end = annotatedBuilder.length
                )
            }
        }

        for (node in parent.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) {
                        appendText(text, inheritedStyle)
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()

                    when {
                        // Handle images - flush text, add image, continue
                        tagName == "img" -> {
                            flushText()

                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                results.add(ParsedContent.Image(src, alt))
                            }
                        }

                        // Handle line breaks
                        tagName in breakTags -> {
                            val (annotatedBuilder, plainBuilder) = ensureTextBuilder()
                            annotatedBuilder.append("\n")
                            plainBuilder.append("\n")
                        }

                        // Handle block elements - they create paragraph breaks
                        tagName in blockTags -> {
                            flushText()

                            // Recursively process this block's children
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                            processNodeChildren(node, results, newStyle)
                        }

                        // Handle inline elements - they continue the current text flow
                        else -> {
                            val newStyle = updateStyleForTag(inheritedStyle, node)

                            // Process inline element children
                            processInlineNode(node, newStyle, ::appendText, ::flushText, results)
                        }
                    }
                }
            }
        }

        // Flush any remaining text
        flushText()
    }

    /**
     * Process an inline element and its children.
     * Inline elements don't break the text flow, but may contain images.
     */
    private fun processInlineNode(
        element: Element,
        style: StyleState,
        appendText: (String, StyleState) -> Unit,
        flushText: () -> Unit,
        results: MutableList<ParsedContent>
    ) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) {
                        appendText(text, style)
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()

                    when {
                        tagName == "img" -> {
                            // Image inside inline element - flush and add
                            flushText()

                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                results.add(ParsedContent.Image(src, alt))
                            }
                        }

                        tagName in breakTags -> {
                            appendText("\n", style)
                        }

                        tagName in blockTags -> {
                            // Unexpected block inside inline - treat as block
                            flushText()
                            val newStyle = updateStyleForTag(style, node)
                            processNodeChildren(node, results, newStyle)
                        }

                        else -> {
                            // Nested inline element
                            val newStyle = updateStyleForTag(style, node)
                            processInlineNode(node, newStyle, appendText, flushText, results)
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the style state based on the current element's tag
     */
    private fun updateStyleForTag(current: StyleState, element: Element): StyleState {
        val tagName = element.tagName().lowercase()

        var style = current

        when {
            tagName in boldTags -> style = style.copy(isBold = true)
            tagName in italicTags -> style = style.copy(isItalic = true)
            tagName in underlineTags -> style = style.copy(isUnderline = true)
            tagName in strikethroughTags -> style = style.copy(isStrikethrough = true)
            tagName == "sub" -> style = style.copy(isSubscript = true)
            tagName == "sup" -> style = style.copy(isSuperscript = true)
            tagName == "a" -> {
                val href = element.attr("href")
                if (href.isNotBlank()) {
                    // Only mark as link, don't apply color here - UI will handle it
                    style = style.copy(linkUrl = href, isUnderline = true)
                }
            }
        }

        // Check inline styles
        val styleAttr = element.attr("style")
        if (styleAttr.isNotBlank()) {
            style = parseInlineStyle(style, styleAttr)
        }

        return style
    }

    /**
     * Parse CSS inline styles
     */
    private fun parseInlineStyle(current: StyleState, styleAttr: String): StyleState {
        var style = current

        val declarations = styleAttr.split(";").map { it.trim() }

        for (declaration in declarations) {
            val parts = declaration.split(":").map { it.trim().lowercase() }
            if (parts.size != 2) continue

            val property = parts[0]
            val value = parts[1]

            when (property) {
                "font-weight" -> {
                    if (value == "bold" || value.toIntOrNull()?.let { it >= 600 } == true) {
                        style = style.copy(isBold = true)
                    }
                }
                "font-style" -> {
                    if (value == "italic" || value == "oblique") {
                        style = style.copy(isItalic = true)
                    }
                }
                "text-decoration" -> {
                    if ("underline" in value) {
                        style = style.copy(isUnderline = true)
                    }
                    if ("line-through" in value) {
                        style = style.copy(isStrikethrough = true)
                    }
                }
                "color" -> {
                    parseColor(value)?.let { color ->
                        style = style.copy(textColor = color)
                    }
                }
            }
        }

        return style
    }

    /**
     * Parse a CSS color value
     */
    private fun parseColor(value: String): Color? {
        return try {
            when {
                value.startsWith("#") -> {
                    val hex = value.removePrefix("#")
                    when (hex.length) {
                        3 -> {
                            val r = hex[0].toString().repeat(2).toInt(16)
                            val g = hex[1].toString().repeat(2).toInt(16)
                            val b = hex[2].toString().repeat(2).toInt(16)
                            Color(r, g, b)
                        }
                        6, 8 -> {
                            Color(android.graphics.Color.parseColor(value))
                        }
                        else -> null
                    }
                }
                value.startsWith("rgb") -> {
                    val match = Regex("""rgba?\((\d+),\s*(\d+),\s*(\d+)""").find(value)
                    match?.let {
                        val (r, g, b) = it.destructured
                        Color(r.toInt(), g.toInt(), b.toInt())
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build a SpanStyle from the current style state
     */
    private fun buildSpanStyle(state: StyleState): SpanStyle {
        val decorations = mutableListOf<TextDecoration>()
        if (state.isUnderline) decorations.add(TextDecoration.Underline)
        if (state.isStrikethrough) decorations.add(TextDecoration.LineThrough)

        return SpanStyle(
            fontWeight = if (state.isBold) FontWeight.Bold else null,
            fontStyle = if (state.isItalic) FontStyle.Italic else null,
            textDecoration = if (decorations.isNotEmpty()) {
                TextDecoration.combine(decorations)
            } else null,
            color = state.textColor ?: Color.Unspecified
        )
    }

    /**
     * Simple helper to parse a single paragraph of HTML into an AnnotatedString.
     * Used when you just need styled text without image extraction.
     */
    fun parseToAnnotatedString(html: String): AnnotatedString {
        if (html.isBlank()) return AnnotatedString("")

        val results = parseHtml(html)

        // Combine all text segments
        return buildAnnotatedString {
            results.filterIsInstance<ParsedContent.Text>().forEachIndexed { index, text ->
                if (index > 0) append("\n\n")
                append(text.annotatedString)
            }
        }
    }
}