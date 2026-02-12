package com.emptycastle.novery.ui.screens.reader.logic

import androidx.compose.ui.text.AnnotatedString
import com.emptycastle.novery.ui.screens.reader.model.ChapterContentItem
import com.emptycastle.novery.ui.screens.reader.model.ContentAuthorNote
import com.emptycastle.novery.ui.screens.reader.model.ContentHorizontalRule
import com.emptycastle.novery.ui.screens.reader.model.ContentImage
import com.emptycastle.novery.ui.screens.reader.model.ContentList
import com.emptycastle.novery.ui.screens.reader.model.ContentSceneBreak
import com.emptycastle.novery.ui.screens.reader.model.ContentSegment
import com.emptycastle.novery.ui.screens.reader.model.ContentTable
import com.emptycastle.novery.util.HtmlUtils
import com.emptycastle.novery.util.SentenceParser

object TextProcessor {

    fun parseHtmlToOrderedContent(html: String): List<ChapterContentItem> {
        val cleanedHtml = HtmlUtils.sanitize(html)
        val items = mutableListOf<ChapterContentItem>()

        val parsedContent = RichTextParser.parseHtml(cleanedHtml)

        var segmentIndex = 0
        var imageIndex = 0
        var ruleIndex = 0
        var breakIndex = 0
        var noteIndex = 0
        var tableIndex = 0
        var listIndex = 0
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
                            sentences = parsedParagraph.sentences,
                            blockType = content.blockType
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

                is ParsedContent.AuthorNote -> {
                    if (content.plainText.isNotBlank()) {
                        val authorNote = ContentAuthorNote(
                            id = "note-$noteIndex",
                            sections = content.sections,
                            plainText = content.plainText,
                            position = content.position,
                            noteType = content.noteType,
                            authorName = content.authorName
                        )

                        items.add(
                            ChapterContentItem.AuthorNote(
                                id = "authornote-$orderIndex",
                                orderIndex = orderIndex,
                                authorNote = authorNote
                            )
                        )
                        noteIndex++
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

                is ParsedContent.HorizontalRule -> {
                    val rule = ContentHorizontalRule(
                        id = "rule-$ruleIndex",
                        style = content.style
                    )

                    items.add(
                        ChapterContentItem.HorizontalRule(
                            id = "hrule-$orderIndex",
                            orderIndex = orderIndex,
                            rule = rule
                        )
                    )
                    ruleIndex++
                    orderIndex++
                }

                is ParsedContent.SceneBreak -> {
                    val sceneBreak = ContentSceneBreak(
                        id = "break-$breakIndex",
                        symbol = content.symbol,
                        style = content.style
                    )

                    items.add(
                        ChapterContentItem.SceneBreak(
                            id = "sbreak-$orderIndex",
                            orderIndex = orderIndex,
                            sceneBreak = sceneBreak
                        )
                    )
                    breakIndex++
                    orderIndex++
                }

                is ParsedContent.Table -> {
                    items.add(
                        ChapterContentItem.Table(
                            id = "table_$orderIndex",
                            orderIndex = orderIndex,
                            table = ContentTable(
                                id = "table_$tableIndex",
                                rows = content.rows,
                                caption = content.caption,
                                plainText = content.plainText
                            )
                        )
                    )
                    tableIndex++
                    orderIndex++
                }

                is ParsedContent.ListBlock -> {
                    items.add(
                        ChapterContentItem.List(
                            id = "list_$orderIndex",
                            orderIndex = orderIndex,
                            list = ContentList(
                                id = "list_$listIndex",
                                list = content.list,
                                plainText = content.plainText
                            )
                        )
                    )
                    listIndex++
                    orderIndex++
                }
            }
        }

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

    fun parseHtmlToSegments(html: String): List<ContentSegment> {
        return parseHtmlToOrderedContent(html)
            .filterIsInstance<ChapterContentItem.Text>()
            .map { it.segment }
    }

    fun extractPlainText(html: String): String {
        return HtmlUtils.extractText(html)
    }
}