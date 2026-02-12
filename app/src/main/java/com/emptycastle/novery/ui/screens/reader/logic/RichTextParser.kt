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
import org.jsoup.nodes.Node
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
 * Represents a table cell
 */
data class TableCell(
    val content: AnnotatedString,
    val plainText: String,
    val isHeader: Boolean = false,
    val colspan: Int = 1,
    val rowspan: Int = 1,
    val alignment: CellAlignment = CellAlignment.START
)

enum class CellAlignment {
    START, CENTER, END
}

/**
 * Represents a table row
 */
data class TableRow(
    val cells: List<TableCell>,
    val isHeaderRow: Boolean = false
)

/**
 * Represents a list item which can contain text or nested lists
 */
sealed class ListItemContent {
    data class TextContent(
        val annotatedString: AnnotatedString,
        val plainText: String
    ) : ListItemContent()

    data class NestedList(
        val list: ParsedList
    ) : ListItemContent()
}

/**
 * Represents a parsed list
 */
data class ParsedList(
    val items: List<ListItemContent>,
    val isOrdered: Boolean,
    val startNumber: Int = 1,
    val listStyleType: ListStyleType = ListStyleType.DEFAULT
)

enum class ListStyleType {
    DEFAULT,      // disc for ul, decimal for ol
    DISC,
    CIRCLE,
    SQUARE,
    DECIMAL,
    LOWER_ALPHA,
    UPPER_ALPHA,
    LOWER_ROMAN,
    UPPER_ROMAN,
    NONE
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

    data class Table(
        val rows: List<TableRow>,
        val caption: String? = null,
        val plainText: String
    ) : ParsedContent()

    // Renamed from List to ListBlock to avoid conflict with kotlin.collections.List
    data class ListBlock(
        val list: ParsedList,
        val plainText: String
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

/**
 * Helper class to build styled text sections for author notes
 */
private class AuthorNoteTextBuilder {
    private var annotatedBuilder = AnnotatedString.Builder()
    private var plainBuilder = StringBuilder()

    val length: Int get() = annotatedBuilder.length
    val isEmpty: Boolean get() = plainBuilder.toString().isBlank()

    fun appendStyledText(text: String, style: StyleState) {
        if (text.isEmpty()) return

        val startIndex = annotatedBuilder.length
        annotatedBuilder.append(text)
        plainBuilder.append(text)

        val spanStyle = buildSpanStyleFromState(style)
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

    fun build(): Pair<AnnotatedString, String> {
        return Pair(annotatedBuilder.toAnnotatedString(), plainBuilder.toString().trim())
    }

    fun clear() {
        annotatedBuilder = AnnotatedString.Builder()
        plainBuilder = StringBuilder()
    }

    companion object {
        private fun buildSpanStyleFromState(state: StyleState): SpanStyle {
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
    }
}

object RichTextParser {

    private val boldTags = setOf("b", "strong")
    private val italicTags = setOf("i", "em", "cite", "dfn", "var")
    private val underlineTags = setOf("u", "ins")
    private val strikethroughTags = setOf("s", "del", "strike")
    private val codeTags = setOf("code", "kbd", "samp", "tt")
    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "article", "section", "aside",
        "figure", "figcaption", "header", "footer", "main", "nav"
    )
    private val blockquoteTags = setOf("blockquote")
    private val preformattedTags = setOf("pre")
    private val breakTags = setOf("br")
    private val tableTags = setOf("table")
    private val listTags = setOf("ul", "ol")
    private val descriptionListTags = setOf("dl")

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
                        firstPass.add(ParsedContent.HorizontalRule(RuleStyle.SOLID))
                    } else if (trimmedText.isNotBlank()) {
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
                            firstPass.add(content)
                        }
                    }
                }

                is ParsedContent.AuthorNote -> {
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
                is ParsedContent.Table -> content.rows.isNotEmpty()
                is ParsedContent.ListBlock -> content.list.items.isNotEmpty()
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
                        // Handle tables
                        tagName in tableTags -> {
                            flushText()
                            val table = parseTable(node, inheritedStyle)
                            if (table != null) {
                                results.add(table)
                            }
                        }

                        // Handle lists (ul, ol)
                        tagName in listTags -> {
                            flushText()
                            val list = parseList(node, inheritedStyle)
                            if (list != null) {
                                results.add(list)
                            }
                        }

                        // Handle description lists (dl)
                        tagName in descriptionListTags -> {
                            flushText()
                            val list = parseDescriptionList(node, inheritedStyle)
                            if (list != null) {
                                results.add(list)
                            }
                        }

                        // Handle author note CONTAINERS
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
     * Parse a table element into structured data
     */
    private fun parseTable(tableElement: Element, inheritedStyle: StyleState): ParsedContent.Table? {
        val rows = mutableListOf<TableRow>()
        val plainTextBuilder = StringBuilder()

        // Get caption if present
        val caption = tableElement.selectFirst("caption")?.text()?.takeIf { it.isNotBlank() }

        // Process thead, tbody, tfoot in order, or direct tr children
        val rowElements = mutableListOf<Element>()

        // First check for thead
        tableElement.select("> thead > tr").forEach { rowElements.add(it) }
        // Then tbody
        tableElement.select("> tbody > tr").forEach { rowElements.add(it) }
        // Then direct tr children (for tables without thead/tbody)
        if (rowElements.isEmpty()) {
            tableElement.select("> tr").forEach { rowElements.add(it) }
        }
        // Finally tfoot
        tableElement.select("> tfoot > tr").forEach { rowElements.add(it) }

        for (rowElement in rowElements) {
            val cells = mutableListOf<TableCell>()
            val isInHeader = rowElement.parent()?.tagName()?.lowercase() == "thead"
            var rowPlainText = ""

            for (cellElement in rowElement.select("> th, > td")) {
                val isHeader = cellElement.tagName().lowercase() == "th" || isInHeader
                val colspan = cellElement.attr("colspan").toIntOrNull() ?: 1
                val rowspan = cellElement.attr("rowspan").toIntOrNull() ?: 1
                val alignment = parseCellAlignment(cellElement)

                // Parse cell content with styling
                val (annotatedContent, plainContent) = parseCellContent(cellElement, inheritedStyle)

                cells.add(TableCell(
                    content = annotatedContent,
                    plainText = plainContent,
                    isHeader = isHeader,
                    colspan = colspan,
                    rowspan = rowspan,
                    alignment = alignment
                ))

                if (rowPlainText.isNotEmpty()) rowPlainText += "\t"
                rowPlainText += plainContent
            }

            if (cells.isNotEmpty()) {
                rows.add(TableRow(cells = cells, isHeaderRow = isInHeader))
                if (plainTextBuilder.isNotEmpty()) plainTextBuilder.append("\n")
                plainTextBuilder.append(rowPlainText)
            }
        }

        if (rows.isEmpty()) return null

        return ParsedContent.Table(
            rows = rows,
            caption = caption,
            plainText = plainTextBuilder.toString()
        )
    }

    /**
     * Parse alignment from cell element
     */
    private fun parseCellAlignment(element: Element): CellAlignment {
        // Check align attribute
        val alignAttr = element.attr("align").lowercase()
        if (alignAttr.isNotBlank()) {
            return when (alignAttr) {
                "center" -> CellAlignment.CENTER
                "right" -> CellAlignment.END
                else -> CellAlignment.START
            }
        }

        // Check style attribute
        val styleAttr = element.attr("style").lowercase()
        if (styleAttr.contains("text-align")) {
            return when {
                styleAttr.contains("center") -> CellAlignment.CENTER
                styleAttr.contains("right") -> CellAlignment.END
                else -> CellAlignment.START
            }
        }

        return CellAlignment.START
    }

    /**
     * Parse cell content preserving inline styles
     */
    private fun parseCellContent(element: Element, inheritedStyle: StyleState): Pair<AnnotatedString, String> {
        val annotatedBuilder = AnnotatedString.Builder()
        val plainBuilder = StringBuilder()

        fun processNode(node: Node, style: StyleState) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) {
                        val startIndex = annotatedBuilder.length
                        annotatedBuilder.append(text)
                        plainBuilder.append(text)

                        val spanStyle = buildSpanStyle(style)
                        if (spanStyle != SpanStyle()) {
                            annotatedBuilder.addStyle(spanStyle, startIndex, annotatedBuilder.length)
                        }

                        style.linkUrl?.let { url ->
                            annotatedBuilder.addStringAnnotation("URL", url, startIndex, annotatedBuilder.length)
                        }
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()
                    when (tagName) {
                        "br" -> {
                            annotatedBuilder.append("\n")
                            plainBuilder.append("\n")
                        }
                        else -> {
                            val newStyle = updateStyleForTag(style, node)
                            for (child in node.childNodes()) {
                                processNode(child, newStyle)
                            }
                        }
                    }
                }
            }
        }

        for (child in element.childNodes()) {
            processNode(child, inheritedStyle)
        }

        return Pair(annotatedBuilder.toAnnotatedString(), plainBuilder.toString().trim())
    }

    /**
     * Parse a list element (ul or ol)
     */
    private fun parseList(listElement: Element, inheritedStyle: StyleState): ParsedContent.ListBlock? {
        val tagName = listElement.tagName().lowercase()
        val isOrdered = tagName == "ol"
        val startNumber = listElement.attr("start").toIntOrNull() ?: 1
        val listStyleType = parseListStyleType(listElement, isOrdered)

        val items = mutableListOf<ListItemContent>()
        val plainTextBuilder = StringBuilder()

        val liElements = listElement.select("> li")
        for ((index, liElement) in liElements.withIndex()) {
            // Check for nested lists
            val nestedList = liElement.selectFirst("> ul, > ol")

            if (nestedList != null) {
                // Has nested list - parse text before nested list, then the nested list
                val textBeforeList = StringBuilder()
                val annotatedBeforeList = AnnotatedString.Builder()

                for (child in liElement.childNodes()) {
                    if (child is Element && child.tagName().lowercase() in listOf("ul", "ol")) {
                        break // Stop at nested list
                    }
                    when (child) {
                        is TextNode -> {
                            val text = child.wholeText.trim()
                            if (text.isNotEmpty()) {
                                val startIdx = annotatedBeforeList.length
                                annotatedBeforeList.append(text)
                                textBeforeList.append(text)

                                val spanStyle = buildSpanStyle(inheritedStyle)
                                if (spanStyle != SpanStyle()) {
                                    annotatedBeforeList.addStyle(spanStyle, startIdx, annotatedBeforeList.length)
                                }
                            }
                        }
                        is Element -> {
                            val (ann, plain) = parseCellContent(child, inheritedStyle)
                            if (plain.isNotBlank()) {
                                annotatedBeforeList.append(ann)
                                textBeforeList.append(plain)
                            }
                        }
                    }
                }

                if (textBeforeList.isNotBlank()) {
                    items.add(ListItemContent.TextContent(
                        annotatedBeforeList.toAnnotatedString(),
                        textBeforeList.toString()
                    ))

                    val prefix = if (isOrdered) "${startNumber + index}. " else "• "
                    if (plainTextBuilder.isNotEmpty()) plainTextBuilder.append("\n")
                    plainTextBuilder.append(prefix).append(textBeforeList)
                }

                // Parse nested list recursively
                val nestedParsed = parseList(nestedList, inheritedStyle)
                if (nestedParsed != null) {
                    items.add(ListItemContent.NestedList(nestedParsed.list))
                    // Add indented plain text
                    nestedParsed.plainText.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            plainTextBuilder.append("\n    ").append(line)
                        }
                    }
                }
            } else {
                // No nested list - parse entire content
                val (annotatedContent, plainContent) = parseCellContent(liElement, inheritedStyle)

                if (plainContent.isNotBlank()) {
                    items.add(ListItemContent.TextContent(annotatedContent, plainContent))

                    val prefix = if (isOrdered) "${startNumber + index}. " else "• "
                    if (plainTextBuilder.isNotEmpty()) plainTextBuilder.append("\n")
                    plainTextBuilder.append(prefix).append(plainContent)
                }
            }
        }

        if (items.isEmpty()) return null

        return ParsedContent.ListBlock(
            list = ParsedList(
                items = items,
                isOrdered = isOrdered,
                startNumber = startNumber,
                listStyleType = listStyleType
            ),
            plainText = plainTextBuilder.toString()
        )
    }

    /**
     * Parse description list (dl/dt/dd)
     */
    private fun parseDescriptionList(dlElement: Element, inheritedStyle: StyleState): ParsedContent.ListBlock? {
        val items = mutableListOf<ListItemContent>()
        val plainTextBuilder = StringBuilder()

        val children = dlElement.children()
        var i = 0

        while (i < children.size) {
            val child = children[i]
            val tagName = child.tagName().lowercase()

            when (tagName) {
                "dt" -> {
                    // Term - make it bold
                    val boldStyle = inheritedStyle.copy(isBold = true)
                    val (annotated, plain) = parseCellContent(child, boldStyle)

                    if (plain.isNotBlank()) {
                        items.add(ListItemContent.TextContent(annotated, plain))
                        if (plainTextBuilder.isNotEmpty()) plainTextBuilder.append("\n")
                        plainTextBuilder.append(plain)
                    }
                }
                "dd" -> {
                    // Description - indented
                    val (annotated, plain) = parseCellContent(child, inheritedStyle)

                    if (plain.isNotBlank()) {
                        // Add with indent marker (we'll handle display separately)
                        val indentedAnnotated = buildAnnotatedString {
                            append("    ")
                            append(annotated)
                        }
                        items.add(ListItemContent.TextContent(indentedAnnotated, "    $plain"))
                        if (plainTextBuilder.isNotEmpty()) plainTextBuilder.append("\n")
                        plainTextBuilder.append("    ").append(plain)
                    }
                }
            }
            i++
        }

        if (items.isEmpty()) return null

        return ParsedContent.ListBlock(
            list = ParsedList(
                items = items,
                isOrdered = false,
                listStyleType = ListStyleType.NONE
            ),
            plainText = plainTextBuilder.toString()
        )
    }

    /**
     * Parse list style type from element
     */
    private fun parseListStyleType(element: Element, isOrdered: Boolean): ListStyleType {
        // Check type attribute (for ol)
        val typeAttr = element.attr("type").lowercase()
        if (typeAttr.isNotBlank()) {
            return when (typeAttr) {
                "1" -> ListStyleType.DECIMAL
                "a" -> ListStyleType.LOWER_ALPHA
                "A" -> ListStyleType.UPPER_ALPHA
                "i" -> ListStyleType.LOWER_ROMAN
                "I" -> ListStyleType.UPPER_ROMAN
                "disc" -> ListStyleType.DISC
                "circle" -> ListStyleType.CIRCLE
                "square" -> ListStyleType.SQUARE
                else -> ListStyleType.DEFAULT
            }
        }

        // Check style attribute
        val styleAttr = element.attr("style").lowercase()
        if (styleAttr.contains("list-style-type")) {
            return when {
                styleAttr.contains("decimal") -> ListStyleType.DECIMAL
                styleAttr.contains("lower-alpha") -> ListStyleType.LOWER_ALPHA
                styleAttr.contains("upper-alpha") -> ListStyleType.UPPER_ALPHA
                styleAttr.contains("lower-roman") -> ListStyleType.LOWER_ROMAN
                styleAttr.contains("upper-roman") -> ListStyleType.UPPER_ROMAN
                styleAttr.contains("disc") -> ListStyleType.DISC
                styleAttr.contains("circle") -> ListStyleType.CIRCLE
                styleAttr.contains("square") -> ListStyleType.SQUARE
                styleAttr.contains("none") -> ListStyleType.NONE
                else -> ListStyleType.DEFAULT
            }
        }

        return ListStyleType.DEFAULT
    }

    /**
     * Parse an entire author note container as a single unit
     */
    private fun parseAuthorNoteContainer(container: Element): ParsedContent.AuthorNote? {
        val sections = mutableListOf<AuthorNoteSection>()
        val plainTextBuilder = StringBuilder()
        var authorName: String? = null
        var noteType = "Author's Note"

        container.selectFirst(".portlet-title .caption-subject, .author-note-title, .note-title")?.let { titleElement ->
            val titleText = titleElement.text()
            authorName = AuthorNoteDetector.extractAuthorName(titleText)
            noteType = AuthorNoteDetector.extractNoteTypeLabel(titleText)
        }

        val contentElements = container.select(".portlet-body, .author-note-content, .author-note")
            .ifEmpty { listOf(container) }

        for (contentElement in contentElements) {
            parseAuthorNoteContent(contentElement, sections, plainTextBuilder, StyleState())
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

    private fun parseAuthorNoteContent(
        element: Element,
        sections: MutableList<AuthorNoteSection>,
        plainTextBuilder: StringBuilder,
        inheritedStyle: StyleState
    ) {
        val textBuilder = AuthorNoteTextBuilder()

        fun flushTextSection() {
            if (!textBuilder.isEmpty) {
                val (annotated, plain) = textBuilder.build()
                sections.add(AuthorNoteSection.TextSection(annotated, plain))

                if (plainTextBuilder.isNotEmpty() && !plainTextBuilder.endsWith("\n")) {
                    plainTextBuilder.append("\n")
                }
                plainTextBuilder.append(plain)

                textBuilder.clear()
            }
        }

        fun processNode(node: Node, currentStyle: StyleState) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) {
                        textBuilder.appendStyledText(text, currentStyle)
                    }
                }

                is Element -> {
                    val tagName = node.tagName().lowercase()

                    when {
                        tagName == "img" -> {
                            flushTextSection()
                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                sections.add(AuthorNoteSection.ImageSection(src, alt))
                            }
                        }

                        tagName == "br" -> {
                            textBuilder.appendStyledText("\n", currentStyle)
                        }

                        tagName == "hr" || node.hasClass("author-note-separator") -> {
                            // Skip
                        }

                        tagName in setOf("p", "div") -> {
                            val newStyle = updateStyleForTag(currentStyle, node)
                            for (child in node.childNodes()) {
                                processNode(child, newStyle)
                            }
                            if (textBuilder.length > 0) {
                                textBuilder.appendStyledText("\n", currentStyle)
                            }
                        }

                        else -> {
                            val newStyle = updateStyleForTag(currentStyle, node)
                            for (child in node.childNodes()) {
                                processNode(child, newStyle)
                            }
                        }
                    }
                }
            }
        }

        for (node in element.childNodes()) {
            processNode(node, inheritedStyle)
        }

        flushTextSection()
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
                        // Handle tables inline
                        tagName in tableTags -> {
                            flushText()
                            val table = parseTable(node, style)
                            if (table != null) {
                                results.add(table)
                            }
                        }

                        // Handle lists inline
                        tagName in listTags -> {
                            flushText()
                            val list = parseList(node, style)
                            if (list != null) {
                                results.add(list)
                            }
                        }

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
                    is ParsedContent.Table -> {
                        if (index > 0) append("\n\n")
                        append(content.plainText)
                    }
                    is ParsedContent.ListBlock -> {
                        if (index > 0) append("\n\n")
                        append(content.plainText)
                    }
                    else -> { }
                }
            }
        }
    }
}