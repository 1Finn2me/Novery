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
 * A section within an author note - can be text or image
 */
sealed class AuthorNoteSection {
    data class TextSection(
        val annotatedString: AnnotatedString,
        val plainText: String
    ) : AuthorNoteSection()

    data class ImageSection(
        val url: String,
        val altText: String? = null
    ) : AuthorNoteSection()
}

/**
 * Represents a parsed content item
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

    data class AuthorNote(
        val sections: List<AuthorNoteSection>,
        val plainText: String,
        val position: AuthorNotePosition = AuthorNotePosition.INLINE,
        val noteType: String = "Author's Note",
        val authorName: String? = null
    ) : ParsedContent()
}

enum class BlockType {
    NORMAL,
    BLOCKQUOTE,
    CODE_BLOCK,
    SYSTEM_MESSAGE
}

enum class RuleStyle {
    SOLID,
    DASHED,
    DOTTED
}

enum class SceneBreakStyle {
    ASTERISKS,
    DASHES,
    ORNAMENT,
    CUSTOM
}

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

object RichTextParser {

    private val boldTags = setOf("b", "strong")
    private val italicTags = setOf("i", "em", "cite", "dfn", "var")
    private val underlineTags = setOf("u", "ins")
    private val strikethroughTags = setOf("s", "del", "strike")
    private val codeTags = setOf("code", "kbd", "samp", "tt")
    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "li", "tr", "article", "section", "aside",
        "figure", "figcaption", "header", "footer", "main", "nav"
    )
    private val blockquoteTags = setOf("blockquote")
    private val preformattedTags = setOf("pre")
    private val breakTags = setOf("br")

    private val sceneBreakPatterns = listOf(
        Regex("""^\s*[*]{3,}\s*$"""),
        Regex("""^\s*[-]{3,}\s*$"""),
        Regex("""^\s*[_]{3,}\s*$"""),
        Regex("""^\s*\*\s+\*\s+\*\s*$"""),
        Regex("""^\s*-\s+-\s+-\s*$"""),
        Regex("""^\s*[⁂✧◇❧§†‡•◆★☆♦♠♣♥]\s*$"""),
        Regex("""^\s*~+\s*$"""),
        Regex("""^\s*#\s*#\s*#\s*$"""),
        Regex("""^\s*[=]{3,}\s*$""")
    )

    private val systemMessagePatterns = listOf(
        Regex("""^\s*\[.*?\]\s*$""", RegexOption.DOT_MATCHES_ALL),
        Regex("""^\s*<.*?>\s*$""", RegexOption.DOT_MATCHES_ALL),
        Regex("""^\s*『.*?』\s*$""", RegexOption.DOT_MATCHES_ALL),
        Regex("""^\s*【.*?】\s*$""", RegexOption.DOT_MATCHES_ALL)
    )

    fun parseHtml(html: String): List<ParsedContent> {
        if (html.isBlank()) return emptyList()

        val document = Jsoup.parse(html)
        val results = mutableListOf<ParsedContent>()

        processNodeChildren(document.body(), results, StyleState(), BlockType.NORMAL)

        return postProcessResults(results)
    }

    /**
     * Post-process to detect scene breaks, merge author notes, and handle text patterns
     */
    private fun postProcessResults(results: List<ParsedContent>): List<ParsedContent> {
        val processed = mutableListOf<ParsedContent>()

        // First pass: only convert EXPLICIT text-based author notes
        val firstPass = mutableListOf<ParsedContent>()

        results.forEachIndexed { index, content ->
            when (content) {
                is ParsedContent.Text -> {
                    val trimmedText = content.plainText.trim()

                    // Check for scene breaks
                    val sceneBreak = detectSceneBreak(trimmedText)
                    if (sceneBreak != null) {
                        firstPass.add(sceneBreak)
                    } else if (AuthorNoteDetector.isSeparatorLine(trimmedText)) {
                        // It's just a separator line, add as horizontal rule
                        firstPass.add(ParsedContent.HorizontalRule(RuleStyle.SOLID))
                    } else if (trimmedText.isNotBlank()) {
                        // Only convert to author note if it EXPLICITLY starts with author note markers
                        // This is very conservative - most text will pass through as normal
                        if (AuthorNoteDetector.isExplicitAuthorNote(trimmedText)) {
                            val position = AuthorNoteDetector.detectPosition(
                                itemIndex = index,
                                totalItems = results.size
                            )
                            val noteType = AuthorNoteDetector.extractNoteTypeLabel(trimmedText)
                            val cleanedText = AuthorNoteDetector.cleanNoteText(trimmedText)

                            firstPass.add(ParsedContent.AuthorNote(
                                sections = listOf(AuthorNoteSection.TextSection(
                                    AnnotatedString(cleanedText),
                                    cleanedText
                                )),
                                plainText = cleanedText,
                                position = position,
                                noteType = noteType
                            ))
                        } else {
                            // Normal text - keep as is
                            firstPass.add(content)
                        }
                    }
                }

                is ParsedContent.AuthorNote -> {
                    // CSS-detected author notes pass through
                    firstPass.add(content)
                }

                is ParsedContent.HorizontalRule -> {
                    firstPass.add(content)
                }

                else -> {
                    firstPass.add(content)
                }
            }
        }

        // Second pass: merge consecutive author notes
        var i = 0
        while (i < firstPass.size) {
            val current = firstPass[i]

            if (current is ParsedContent.AuthorNote) {
                // Collect consecutive author notes
                val consecutiveNotes = mutableListOf(current)
                var j = i + 1

                while (j < firstPass.size) {
                    val next = firstPass[j]
                    if (next is ParsedContent.AuthorNote) {
                        consecutiveNotes.add(next)
                        j++
                    } else {
                        break
                    }
                }

                if (consecutiveNotes.size > 1) {
                    // Merge all consecutive notes into one
                    val mergedSections = consecutiveNotes.flatMap { it.sections }
                    val mergedPlainText = consecutiveNotes.joinToString("\n\n") { it.plainText }
                    val firstNote = consecutiveNotes.first()

                    processed.add(ParsedContent.AuthorNote(
                        sections = mergedSections,
                        plainText = mergedPlainText,
                        position = firstNote.position,
                        noteType = firstNote.noteType,
                        authorName = consecutiveNotes.firstNotNullOfOrNull { it.authorName }
                    ))
                    i = j
                } else {
                    processed.add(current)
                    i++
                }
            } else {
                processed.add(current)
                i++
            }
        }

        return processed.filter { content ->
            when (content) {
                is ParsedContent.Text -> content.plainText.isNotBlank()
                is ParsedContent.AuthorNote -> content.plainText.isNotBlank()
                is ParsedContent.Image -> content.url.isNotBlank()
                is ParsedContent.HorizontalRule -> true
                is ParsedContent.SceneBreak -> true
            }
        }
    }

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
                return ParsedContent.SceneBreak(symbol = trimmed, style = style)
            }
        }
        return null
    }

    private fun isSystemMessage(text: String): Boolean {
        return systemMessagePatterns.any { it.matches(text.trim()) }
    }

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
                    val classAttr = node.attr("class")

                    when {
                        // Handle author note CONTAINERS (extract entire content as one unit)
                        AuthorNoteDetector.isAuthorNoteContainer(classAttr) -> {
                            flushText()
                            val authorNote = parseAuthorNoteContainer(node)
                            if (authorNote != null) {
                                results.add(authorNote)
                            }
                        }

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

                        tagName in breakTags -> {
                            val (annotatedBuilder, plainBuilder) = ensureTextBuilder()
                            annotatedBuilder.append("\n")
                            plainBuilder.append("\n")
                        }

                        tagName in blockquoteTags -> {
                            flushText()
                            currentBlockType = BlockType.BLOCKQUOTE
                            val newStyle = updateStyleForTag(inheritedStyle, node).copy(isItalic = true)
                            processNodeChildren(node, results, newStyle, BlockType.BLOCKQUOTE)
                            currentBlockType = blockType
                        }

                        tagName in preformattedTags -> {
                            flushText()
                            currentBlockType = BlockType.CODE_BLOCK
                            val newStyle = updateStyleForTag(inheritedStyle, node).copy(isCode = true)
                            processNodeChildren(node, results, newStyle, BlockType.CODE_BLOCK)
                            currentBlockType = blockType
                        }

                        tagName in blockTags -> {
                            flushText()
                            val newStyle = updateStyleForTag(inheritedStyle, node)
                            processNodeChildren(node, results, newStyle, currentBlockType)
                        }

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
     * Parse an entire author note container as a single unit
     */
    private fun parseAuthorNoteContainer(container: Element): ParsedContent.AuthorNote? {
        val sections = mutableListOf<AuthorNoteSection>()
        val plainTextBuilder = StringBuilder()
        var authorName: String? = null
        var noteType = "Author's Note"

        // Try to extract author name from title elements (RoyalRoad style)
        container.selectFirst(".portlet-title .caption-subject, .author-note-title, .note-title")?.let { titleElement ->
            val titleText = titleElement.text()
            authorName = AuthorNoteDetector.extractAuthorName(titleText)
            noteType = AuthorNoteDetector.extractNoteTypeLabel(titleText)
        }

        // Find the content element(s)
        val contentElements = container.select(".portlet-body, .author-note-content, .author-note")
            .ifEmpty { listOf(container) }

        for (contentElement in contentElements) {
            parseAuthorNoteContent(contentElement, sections, plainTextBuilder)
        }

        if (sections.isEmpty()) {
            return null
        }

        return ParsedContent.AuthorNote(
            sections = sections,
            plainText = plainTextBuilder.toString().trim(),
            position = AuthorNotePosition.INLINE,
            noteType = noteType,
            authorName = authorName
        )
    }

    /**
     * Recursively parse content inside an author note, collecting text and images
     */
    private fun parseAuthorNoteContent(
        element: Element,
        sections: MutableList<AuthorNoteSection>,
        plainTextBuilder: StringBuilder
    ) {
        // Process all child nodes
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText.trim()
                    if (text.isNotBlank()) {
                        // Add to current text section or create new one
                        addTextToSections(text, sections, plainTextBuilder)
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()

                    when {
                        // Handle images inside author notes
                        tagName == "img" -> {
                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                sections.add(AuthorNoteSection.ImageSection(src, alt))
                            }
                        }

                        // Handle paragraphs and divs - process their content
                        tagName in setOf("p", "div", "span", "a", "strong", "b", "em", "i", "u") -> {
                            parseAuthorNoteContent(node, sections, plainTextBuilder)
                            // Add paragraph break after block elements
                            if (tagName == "p" || tagName == "div") {
                                if (plainTextBuilder.isNotEmpty() && !plainTextBuilder.endsWith("\n\n")) {
                                    plainTextBuilder.append("\n\n")
                                }
                            }
                        }

                        // Handle line breaks
                        tagName == "br" -> {
                            plainTextBuilder.append("\n")
                        }

                        // Handle tables and other complex elements - extract text
                        tagName in setOf("table", "tbody", "tr", "td", "th") -> {
                            parseAuthorNoteContent(node, sections, plainTextBuilder)
                        }

                        // Skip separator elements
                        tagName == "hr" || node.hasClass("author-note-separator") -> {
                            // Skip
                        }

                        else -> {
                            // For other elements, try to get text content
                            parseAuthorNoteContent(node, sections, plainTextBuilder)
                        }
                    }
                }
            }
        }
    }

    /**
     * Add text to sections, merging with the last text section if possible
     */
    private fun addTextToSections(
        text: String,
        sections: MutableList<AuthorNoteSection>,
        plainTextBuilder: StringBuilder
    ) {
        val lastSection = sections.lastOrNull()

        if (lastSection is AuthorNoteSection.TextSection) {
            // Merge with existing text section
            val combinedText = lastSection.plainText + " " + text
            val combinedAnnotated = buildAnnotatedString {
                append(lastSection.annotatedString)
                append(" ")
                append(text)
            }
            sections[sections.lastIndex] = AuthorNoteSection.TextSection(combinedAnnotated, combinedText)
        } else {
            // Create new text section
            sections.add(AuthorNoteSection.TextSection(AnnotatedString(text), text))
        }

        if (plainTextBuilder.isNotEmpty() && !plainTextBuilder.endsWith(" ") && !plainTextBuilder.endsWith("\n")) {
            plainTextBuilder.append(" ")
        }
        plainTextBuilder.append(text)
    }

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
                    val classAttr = node.attr("class")

                    when {
                        AuthorNoteDetector.isAuthorNoteContainer(classAttr) -> {
                            flushText()
                            val authorNote = parseAuthorNoteContainer(node)
                            if (authorNote != null) {
                                results.add(authorNote)
                            }
                        }

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

        val classAttr = element.attr("class").lowercase()
        if (classAttr.contains("system") || classAttr.contains("status") ||
            classAttr.contains("notification") || classAttr.contains("alert")) {
            style = style.copy(isCode = true)
        }

        val styleAttr = element.attr("style")
        if (styleAttr.isNotBlank()) {
            style = parseInlineStyle(style, styleAttr)
        }

        return style
    }

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
                    if ("underline" in value) style = style.copy(isUnderline = true)
                    if ("line-through" in value) style = style.copy(isStrikethrough = true)
                }
                "font-variant" -> {
                    if ("small-caps" in value) style = style.copy(isSmallCaps = true)
                }
                "font-family" -> {
                    if ("monospace" in value || "courier" in value || "consolas" in value) {
                        style = style.copy(isCode = true)
                    }
                }
                "color" -> {
                    parseColor(value)?.let { color -> style = style.copy(textColor = color) }
                }
                "background-color", "background" -> {
                    parseColor(value)?.let { color -> style = style.copy(backgroundColor = color) }
                }
            }
        }

        return style
    }

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
                        6, 8 -> Color(android.graphics.Color.parseColor(value))
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
            textDecoration = if (decorations.isNotEmpty()) TextDecoration.combine(decorations) else null,
            color = state.textColor ?: Color.Unspecified,
            background = state.backgroundColor ?: Color.Unspecified,
            fontFeatureSettings = if (state.isSmallCaps) "smcp" else null
        )
    }

    fun parseToAnnotatedString(html: String): AnnotatedString {
        if (html.isBlank()) return AnnotatedString("")

        val results = parseHtml(html)

        return buildAnnotatedString {
            results.forEachIndexed { index, content ->
                when (content) {
                    is ParsedContent.Text -> {
                        if (index > 0) append("\n\n")
                        append(content.annotatedString)
                    }
                    is ParsedContent.AuthorNote -> {
                        if (index > 0) append("\n\n")
                        append(content.plainText)
                    }
                    else -> { }
                }
            }
        }
    }
}