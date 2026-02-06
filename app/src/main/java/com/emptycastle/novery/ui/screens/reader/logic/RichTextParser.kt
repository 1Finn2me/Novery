package com.emptycastle.novery.ui.screens.reader.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Represents a parsed content item - text, image, or structural element
 */
sealed class ParsedContent {
    data class Text(
        val annotatedString: AnnotatedString,
        val plainText: String,
        val blockType: BlockType = BlockType.NORMAL
    ) : ParsedContent()

    data class Image(
        val url: String,
        val altText: String? = null
    ) : ParsedContent()

    data class HorizontalRule(
        val style: RuleStyle = RuleStyle.SOLID
    ) : ParsedContent()

    data class SceneBreak(
        val symbol: String = "* * *",
        val style: SceneBreakStyle = SceneBreakStyle.ASTERISKS
    ) : ParsedContent()
}

/**
 * Type of text block for special rendering
 */
enum class BlockType {
    NORMAL,
    BLOCKQUOTE,
    CODE_BLOCK,
    SYSTEM_MESSAGE  // For LitRPG [System] messages
}

/**
 * Style of horizontal rule
 */
enum class RuleStyle {
    SOLID,
    DASHED,
    DOTTED
}

/**
 * Style of scene break
 */
enum class SceneBreakStyle {
    ASTERISKS,      // * * * or ***
    DASHES,         // ---
    ORNAMENT,       // ⁂ or other symbols
    CUSTOM
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
    val isCode: Boolean = false,
    val isSmallCaps: Boolean = false,
    val linkUrl: String? = null,
    val textColor: Color? = null,
    val backgroundColor: Color? = null
)

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

    // Tags that indicate code/monospace
    private val codeTags = setOf("code", "kbd", "samp", "tt")

    // Block-level tags that create paragraph breaks
    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "li", "tr", "article", "section", "aside",
        "figure", "figcaption", "header", "footer", "main", "nav"
    )

    // Special block tags that need different rendering
    private val blockquoteTags = setOf("blockquote")
    private val preformattedTags = setOf("pre")

    // Tags that should be treated as line breaks
    private val breakTags = setOf("br")

    // Scene break patterns (plain text)
    private val sceneBreakPatterns = listOf(
        Regex("""^\s*[*]{3,}\s*$"""),                    // ***
        Regex("""^\s*[-]{3,}\s*$"""),                    // ---
        Regex("""^\s*[_]{3,}\s*$"""),                    // ___
        Regex("""^\s*\*\s+\*\s+\*\s*$"""),              // * * *
        Regex("""^\s*-\s+-\s+-\s*$"""),                  // - - -
        Regex("""^\s*[⁂✧◇❧§†‡•◆★☆♦♠♣♥]\s*$"""),        // Single ornament
        Regex("""^\s*~+\s*$"""),                         // ~~~
        Regex("""^\s*#\s*#\s*#\s*$"""),                  // # # #
        Regex("""^\s*[=]{3,}\s*$""")                     // ===
    )

    // LitRPG system message patterns
    private val systemMessagePatterns = listOf(
        Regex("""^\s*\[.*?\]\s*$""", RegexOption.DOT_MATCHES_ALL),  // [System Message]
        Regex("""^\s*<.*?>\s*$""", RegexOption.DOT_MATCHES_ALL),    // <System>
        Regex("""^\s*『.*?』\s*$""", RegexOption.DOT_MATCHES_ALL),  // 『Status』
        Regex("""^\s*【.*?】\s*$""", RegexOption.DOT_MATCHES_ALL)   // 【Skill】
    )

    /**
     * Parses an HTML string into a list of ParsedContent items.
     * Text and images are returned in their exact document order.
     */
    fun parseHtml(html: String): List<ParsedContent> {
        if (html.isBlank()) return emptyList()

        val document = Jsoup.parse(html)
        val results = mutableListOf<ParsedContent>()

        // Process the body using ordered traversal
        processNodeChildren(document.body(), results, StyleState(), BlockType.NORMAL)

        // Post-process to detect scene breaks in text
        return postProcessResults(results)
    }

    /**
     * Post-process results to detect scene breaks in plain text
     */
    private fun postProcessResults(results: List<ParsedContent>): List<ParsedContent> {
        return results.flatMap { content ->
            when (content) {
                is ParsedContent.Text -> {
                    val trimmedText = content.plainText.trim()

                    // Check if this is a scene break
                    val sceneBreak = detectSceneBreak(trimmedText)
                    if (sceneBreak != null) {
                        listOf(sceneBreak)
                    } else {
                        listOf(content)
                    }
                }
                else -> listOf(content)
            }
        }.filter { content ->
            when (content) {
                is ParsedContent.Text -> content.plainText.isNotBlank()
                is ParsedContent.Image -> content.url.isNotBlank()
                is ParsedContent.HorizontalRule -> true
                is ParsedContent.SceneBreak -> true
            }
        }
    }

    /**
     * Detect if text is a scene break pattern
     */
    private fun detectSceneBreak(text: String): ParsedContent.SceneBreak? {
        val trimmed = text.trim()

        for (pattern in sceneBreakPatterns) {
            if (pattern.matches(trimmed)) {
                val style = when {
                    trimmed.contains('*') -> SceneBreakStyle.ASTERISKS
                    trimmed.contains('-') -> SceneBreakStyle.DASHES
                    trimmed.any { it in "⁂✧◇❧§†‡•◆★☆♦♠♣♥" } -> SceneBreakStyle.ORNAMENT
                    else -> SceneBreakStyle.CUSTOM
                }
                return ParsedContent.SceneBreak(
                    symbol = trimmed,
                    style = style
                )
            }
        }
        return null
    }

    /**
     * Detect if text is a LitRPG system message
     */
    private fun isSystemMessage(text: String): Boolean {
        return systemMessagePatterns.any { it.matches(text.trim()) }
    }

    /**
     * Process all children of a node in document order.
     */
    private fun processNodeChildren(
        parent: Element,
        results: MutableList<ParsedContent>,
        inheritedStyle: StyleState,
        blockType: BlockType
    ) {
        var currentTextBuilder: AnnotatedString.Builder? = null
        var currentPlainBuilder: StringBuilder? = null
        var currentBlockType = blockType

        fun flushText() {
            val annotated = currentTextBuilder?.toAnnotatedString()
            val plain = currentPlainBuilder?.toString()?.trim()

            if (annotated != null && !plain.isNullOrBlank()) {
                // Check if this looks like a system message
                val finalBlockType = if (currentBlockType == BlockType.NORMAL && isSystemMessage(plain)) {
                    BlockType.SYSTEM_MESSAGE
                } else {
                    currentBlockType
                }

                results.add(ParsedContent.Text(annotated, plain, finalBlockType))
            }

            currentTextBuilder = null
            currentPlainBuilder = null
        }

        fun ensureTextBuilder(): Pair<AnnotatedString.Builder, StringBuilder> {
            if (currentTextBuilder == null) {
                currentTextBuilder = AnnotatedString.Builder()
                currentPlainBuilder = StringBuilder()
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
                        // Handle images
                        tagName == "img" -> {
                            flushText()
                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                results.add(ParsedContent.Image(src, alt))
                            }
                        }

                        // Handle horizontal rules
                        tagName == "hr" -> {
                            flushText()
                            results.add(ParsedContent.HorizontalRule(RuleStyle.SOLID))
                        }

                        // Handle line breaks
                        tagName in breakTags -> {
                            val (annotatedBuilder, plainBuilder) = ensureTextBuilder()
                            annotatedBuilder.append("\n")
                            plainBuilder.append("\n")
                        }

                        // Handle blockquotes
                        tagName in blockquoteTags -> {
                            flushText()
                            currentBlockType = BlockType.BLOCKQUOTE
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                                .copy(isItalic = true)  // Blockquotes are often italic
                            processNodeChildren(node, results, newStyle, BlockType.BLOCKQUOTE)
                            currentBlockType = blockType
                        }

                        // Handle preformatted/code blocks
                        tagName in preformattedTags -> {
                            flushText()
                            currentBlockType = BlockType.CODE_BLOCK
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                                .copy(isCode = true)
                            processNodeChildren(node, results, newStyle, BlockType.CODE_BLOCK)
                            currentBlockType = blockType
                        }

                        // Handle block elements
                        tagName in blockTags -> {
                            flushText()
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                            processNodeChildren(node, results, newStyle, currentBlockType)
                        }

                        // Handle inline elements
                        else -> {
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                            processInlineNode(node, newStyle, ::appendText, ::flushText, results, currentBlockType)
                        }
                    }
                }
            }
        }

        flushText()
    }

    /**
     * Process an inline element and its children.
     */
    private fun processInlineNode(
        element: Element,
        style: StyleState,
        appendText: (String, StyleState) -> Unit,
        flushText: () -> Unit,
        results: MutableList<ParsedContent>,
        blockType: BlockType
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
                            flushText()
                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                results.add(ParsedContent.Image(src, alt))
                            }
                        }

                        tagName == "hr" -> {
                            flushText()
                            results.add(ParsedContent.HorizontalRule(RuleStyle.SOLID))
                        }

                        tagName in breakTags -> {
                            appendText("\n", style)
                        }

                        tagName in blockTags || tagName in blockquoteTags || tagName in preformattedTags -> {
                            flushText()
                            val newBlockType = when (tagName) {
                                in blockquoteTags -> BlockType.BLOCKQUOTE
                                in preformattedTags -> BlockType.CODE_BLOCK
                                else -> blockType
                            }
                            val newStyle = updateStyleForTag(style, node)
                            processNodeChildren(node, results, newStyle, newBlockType)
                        }

                        else -> {
                            val newStyle = updateStyleForTag(style, node)
                            processInlineNode(node, newStyle, appendText, flushText, results, blockType)
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
            tagName in codeTags -> style = style.copy(isCode = true)
            tagName == "sub" -> style = style.copy(isSubscript = true)
            tagName == "sup" -> style = style.copy(isSuperscript = true)
            tagName == "mark" -> style = style.copy(backgroundColor = Color(0xFFFFEB3B))
            tagName == "small" -> style = style.copy(isSmallCaps = true)
            tagName == "a" -> {
                val href = element.attr("href")
                if (href.isNotBlank()) {
                    style = style.copy(linkUrl = href, isUnderline = true)
                }
            }
        }

        // Check for CSS classes that might indicate special styling
        val classAttr = element.attr("class").lowercase()
        if (classAttr.contains("system") || classAttr.contains("status") ||
            classAttr.contains("notification") || classAttr.contains("alert")) {
            style = style.copy(isCode = true)
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
                "font-variant" -> {
                    if ("small-caps" in value) {
                        style = style.copy(isSmallCaps = true)
                    }
                }
                "font-family" -> {
                    if ("monospace" in value || "courier" in value || "consolas" in value) {
                        style = style.copy(isCode = true)
                    }
                }
                "color" -> {
                    parseColor(value)?.let { color ->
                        style = style.copy(textColor = color)
                    }
                }
                "background-color", "background" -> {
                    parseColor(value)?.let { color ->
                        style = style.copy(backgroundColor = color)
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
            fontFamily = if (state.isCode) FontFamily.Monospace else null,
            fontSize = when {
                state.isSuperscript || state.isSubscript -> 0.75.em
                state.isSmallCaps -> 0.85.em
                else -> androidx.compose.ui.unit.TextUnit.Unspecified
            },
            baselineShift = when {
                state.isSuperscript -> BaselineShift.Superscript
                state.isSubscript -> BaselineShift.Subscript
                else -> null
            },
            textDecoration = if (decorations.isNotEmpty()) {
                TextDecoration.combine(decorations)
            } else null,
            color = state.textColor ?: Color.Unspecified,
            background = state.backgroundColor ?: Color.Unspecified,
            fontFeatureSettings = if (state.isSmallCaps) "smcp" else null
        )
    }

    /**
     * Simple helper to parse a single paragraph of HTML into an AnnotatedString.
     */
    fun parseToAnnotatedString(html: String): AnnotatedString {
        if (html.isBlank()) return AnnotatedString("")

        val results = parseHtml(html)

        return buildAnnotatedString {
            results.filterIsInstance<ParsedContent.Text>().forEachIndexed { index, text ->
                if (index > 0) append("\n\n")
                append(text.annotatedString)
            }
        }
    }
}