package com.emptycastle.novery.ui.screens.reader.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.screens.reader.logic.RuleStyle
import com.emptycastle.novery.ui.screens.reader.logic.SceneBreakStyle
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.SentenceHighlight
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc800

// =============================================================================
// CHAPTER HEADER
// =============================================================================

@Composable
fun ChapterHeaderItem(
    item: ReaderDisplayItem.ChapterHeader,
    colors: ReaderColors,
    fontFamily: FontFamily,
    horizontalPadding: Dp,
    largerTouchTargets: Boolean = false
) {
    val verticalPadding = if (largerTouchTargets) 32.dp else 24.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = verticalPadding, bottom = verticalPadding + 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colors.divider
            )
            Text(
                text = " ✦ ",
                color = colors.accent,
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colors.divider
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chapter ${item.chapterNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.chapterName,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = fontFamily
            ),
            fontWeight = FontWeight.Bold,
            color = colors.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${item.chapterNumber} of ${item.totalChapters}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.text.copy(alpha = 0.5f)
        )
    }
}

// =============================================================================
// SEGMENT / PARAGRAPH WITH CLICKABLE LINKS
// =============================================================================

@Composable
fun SegmentItem(
    item: ReaderDisplayItem.Segment,
    displayIndex: Int,
    currentSentenceHighlight: SentenceHighlight?,
    isTTSActive: Boolean,
    highlightEnabled: Boolean,
    settings: ReaderSettings,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    textColor: Color,
    highlightColor: Color,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    linkColor: Color = Color(0xFF1976D2), // Default link color
    onLinkClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val segment = item.segment

    val hasSentenceHighlight = isTTSActive &&
            highlightEnabled &&
            currentSentenceHighlight != null &&
            currentSentenceHighlight.segmentDisplayIndex == displayIndex

    val firstLineIndent = if (settings.paragraphIndent > 0f) {
        (settings.fontSize * settings.paragraphIndent).sp
    } else {
        0.sp
    }

    val letterSpacingSp = (settings.letterSpacing * settings.fontSize).sp

    // Use styled text from segment, applying word spacing if needed
    val baseStyledText = segment.styledText

    // Apply word spacing to the styled text
    val processedStyledText = remember(baseStyledText, settings.wordSpacing) {
        if (settings.wordSpacing != 1.0f) {
            applyWordSpacingToAnnotatedString(baseStyledText, settings.wordSpacing)
        } else {
            baseStyledText
        }
    }

    // Hyphenation
    val hyphens = remember(settings.hyphenation, settings.textAlign) {
        if (settings.hyphenation &&
            settings.textAlign == com.emptycastle.novery.domain.model.TextAlign.JUSTIFY
        ) {
            Hyphens.Auto
        } else {
            Hyphens.None
        }
    }

    val lineBreak = remember(settings.textAlign) {
        when (settings.textAlign) {
            com.emptycastle.novery.domain.model.TextAlign.JUSTIFY -> LineBreak.Paragraph
            else -> LineBreak.Simple
        }
    }

    // Build final annotated string with link styling and TTS highlight
    val annotatedText = remember(
        processedStyledText,
        hasSentenceHighlight,
        currentSentenceHighlight,
        firstLineIndent,
        hyphens,
        lineBreak,
        textColor,
        linkColor
    ) {
        buildAnnotatedString {
            // Apply paragraph style
            withStyle(
                ParagraphStyle(
                    textIndent = if (firstLineIndent.value > 0) {
                        TextIndent(firstLine = firstLineIndent)
                    } else {
                        TextIndent.None
                    },
                    hyphens = hyphens,
                    lineBreak = lineBreak
                )
            ) {
                append(processedStyledText)
            }

            // Apply link color to all URL annotations
            processedStyledText.getStringAnnotations("URL", 0, processedStyledText.length)
                .forEach { annotation ->
                    addStyle(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = annotation.start,
                        end = annotation.end
                    )
                }

            // Apply TTS highlight if active
            if (hasSentenceHighlight && currentSentenceHighlight != null) {
                val sentence = currentSentenceHighlight.sentence
                val adjustedStart = if (settings.wordSpacing != 1.0f) {
                    adjustIndexForWordSpacing(segment.text, sentence.startIndex, settings.wordSpacing)
                } else {
                    sentence.startIndex
                }
                val adjustedEnd = if (settings.wordSpacing != 1.0f) {
                    adjustIndexForWordSpacing(segment.text, sentence.endIndex, settings.wordSpacing)
                } else {
                    sentence.endIndex
                }

                val textLength = processedStyledText.length
                val start = adjustedStart.coerceIn(0, textLength)
                val end = adjustedEnd.coerceIn(0, textLength)

                if (start < end) {
                    addStyle(
                        style = SpanStyle(background = highlightColor),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    // Check if text contains links
    val hasLinks = remember(processedStyledText) {
        processedStyledText.getStringAnnotations("URL", 0, processedStyledText.length).isNotEmpty()
    }

    // Extra vertical padding for larger touch targets
    val extraPadding = if (settings.largerTouchTargets) 4.dp else 0.dp

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = settings.fontSize.sp,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        lineHeight = (settings.fontSize * settings.lineHeight).sp,
        letterSpacing = letterSpacingSp,
        color = textColor,
        textAlign = textAlign
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = paragraphSpacing / 2 + extraPadding)
    ) {
        if (hasLinks) {
            // Use ClickableText for segments with links
            ClickableText(
                text = annotatedText,
                style = textStyle,
                onClick = { offset ->
                    // Check if click is on a link
                    annotatedText.getStringAnnotations(
                        tag = "URL",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        if (onLinkClick != null) {
                            onLinkClick(url)
                        } else {
                            // Default: open in browser
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle invalid URL or no browser available
                            }
                        }
                    }
                }
            )
        } else {
            // Use regular Text for segments without links (better performance)
            Text(
                text = annotatedText,
                style = textStyle
            )
        }
    }
}

/**
 * Apply word spacing to an AnnotatedString while preserving styles
 */
private fun applyWordSpacingToAnnotatedString(
    annotatedString: AnnotatedString,
    multiplier: Float
): AnnotatedString {
    if (multiplier == 1.0f) return annotatedString

    val extraSpaces = when {
        multiplier <= 0.9f -> ""
        multiplier <= 1.0f -> ""
        multiplier <= 1.2f -> "\u200A" // Hair space
        multiplier <= 1.5f -> "\u2009" // Thin space
        multiplier <= 1.8f -> "\u2009\u200A"
        else -> "\u2009\u2009"
    }

    if (extraSpaces.isEmpty()) return annotatedString

    return buildAnnotatedString {
        val originalText = annotatedString.text
        val newTextBuilder = StringBuilder()

        // Build new text with extra spaces
        for (i in originalText.indices) {
            val char = originalText[i]
            newTextBuilder.append(char)
            if (char == ' ' && i < originalText.length - 1) {
                newTextBuilder.append(extraSpaces)
            }
        }

        val newText = newTextBuilder.toString()
        append(newText)

        // Re-apply span styles with adjusted indices
        annotatedString.spanStyles.forEach { spanStyle ->
            val originalStart = spanStyle.start
            val originalEnd = spanStyle.end

            val spacesBeforeStart = originalText.substring(0, originalStart.coerceAtMost(originalText.length)).count { it == ' ' }
            val spacesBeforeEnd = originalText.substring(0, originalEnd.coerceAtMost(originalText.length)).count { it == ' ' }

            val extraCharsPerSpace = extraSpaces.length
            val newStart = originalStart + (spacesBeforeStart * extraCharsPerSpace)
            val newEnd = originalEnd + (spacesBeforeEnd * extraCharsPerSpace)

            if (newStart < newText.length && newEnd <= newText.length && newStart < newEnd) {
                addStyle(spanStyle.item, newStart, newEnd)
            }
        }

        // Re-apply string annotations (for URLs)
        annotatedString.getStringAnnotations(0, annotatedString.length).forEach { annotation ->
            val originalStart = annotation.start
            val originalEnd = annotation.end

            val spacesBeforeStart = originalText.substring(0, originalStart.coerceAtMost(originalText.length)).count { it == ' ' }
            val spacesBeforeEnd = originalText.substring(0, originalEnd.coerceAtMost(originalText.length)).count { it == ' ' }

            val extraCharsPerSpace = extraSpaces.length
            val newStart = originalStart + (spacesBeforeStart * extraCharsPerSpace)
            val newEnd = originalEnd + (spacesBeforeEnd * extraCharsPerSpace)

            if (newStart < newText.length && newEnd <= newText.length && newStart < newEnd) {
                addStringAnnotation(annotation.tag, annotation.item, newStart, newEnd)
            }
        }
    }
}

private fun adjustIndexForWordSpacing(originalText: String, index: Int, multiplier: Float): Int {
    if (multiplier == 1.0f) return index

    val spacesBefore = originalText.substring(0, index.coerceAtMost(originalText.length))
        .count { it == ' ' }

    val extraCharsPerSpace = when {
        multiplier <= 1.0f -> 0
        multiplier <= 1.2f -> 1
        multiplier <= 1.5f -> 1
        multiplier <= 1.8f -> 2
        else -> 2
    }

    return index + (spacesBefore * extraCharsPerSpace)
}

// =============================================================================
// CHAPTER DIVIDER
// =============================================================================

@Composable
fun ChapterDividerItem(
    item: ReaderDisplayItem.ChapterDivider,
    colors: ReaderColors,
    infiniteScrollEnabled: Boolean,
    horizontalPadding: Dp,
    largerTouchTargets: Boolean = false,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToDetails: () -> Unit
) {
    val buttonHeight = if (largerTouchTargets) 64.dp else 56.dp
    val iconSize = if (largerTouchTargets) 24.dp else 20.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = colors.divider
        )

        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = colors.text.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "End of Chapter ${item.chapterNumber}",
            style = MaterialTheme.typography.titleSmall,
            color = colors.text.copy(alpha = 0.6f)
        )

        Text(
            text = item.chapterName,
            style = MaterialTheme.typography.bodySmall,
            color = colors.text.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!infiniteScrollEnabled) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChapterNavButton(
                    text = "Previous",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = item.chapterNumber > 1,
                    isPrimary = false,
                    colors = colors,
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    height = buttonHeight,
                    iconSize = iconSize
                )

                ChapterNavButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = item.hasNextChapter,
                    isPrimary = true,
                    colors = colors,
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    height = buttonHeight,
                    iconSize = iconSize
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GhostButton(
                text = "Back to Novel",
                onClick = onBackToDetails
            )
        } else if (!item.hasNextChapter) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.accent.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "🎉 You've reached the end!",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GhostButton(
                text = "Back to Novel",
                onClick = onBackToDetails
            )
        }
    }
}

@Composable
fun ChapterNavButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    isPrimary: Boolean,
    colors: ReaderColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    iconSize: Dp = 20.dp
) {
    val backgroundColor = when {
        !enabled -> Zinc800.copy(alpha = 0.5f)
        isPrimary -> colors.accent
        else -> Zinc800
    }

    val contentColor = when {
        !enabled -> Zinc600
        isPrimary -> Color.White
        else -> colors.text
    }

    Surface(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isPrimary) Arrangement.End else Arrangement.Start
        ) {
            if (!isPrimary) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            if (isPrimary) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

// =============================================================================
// HORIZONTAL RULE
// =============================================================================

@Composable
fun HorizontalRuleItem(
    item: ReaderDisplayItem.HorizontalRule,
    colors: ReaderColors,
    horizontalPadding: Dp
) {
    val ruleColor = colors.divider.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (item.rule.style) {
            RuleStyle.SOLID -> {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    thickness = 1.dp,
                    color = ruleColor
                )
            }
            RuleStyle.DASHED -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                ) {
                    val dashWidth = 8.dp.toPx()
                    val gapWidth = 4.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = ruleColor,
                            start = Offset(x, 0f),
                            end = Offset(minOf(x + dashWidth, size.width), 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                        x += dashWidth + gapWidth
                    }
                }
            }
            RuleStyle.DOTTED -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                ) {
                    val dotRadius = 2.dp.toPx()
                    val gapWidth = 8.dp.toPx()
                    var x = dotRadius
                    while (x < size.width) {
                        drawCircle(
                            color = ruleColor,
                            radius = dotRadius,
                            center = Offset(x, size.height / 2)
                        )
                        x += dotRadius * 2 + gapWidth
                    }
                }
            }
        }
    }
}

// =============================================================================
// SCENE BREAK
// =============================================================================

@Composable
fun SceneBreakItem(
    item: ReaderDisplayItem.SceneBreak,
    colors: ReaderColors,
    horizontalPadding: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (item.sceneBreak.style) {
            SceneBreakStyle.ASTERISKS -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.accent.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            SceneBreakStyle.DASHES -> {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.4f),
                    thickness = 2.dp,
                    color = colors.divider
                )
            }
            SceneBreakStyle.ORNAMENT -> {
                Text(
                    text = item.sceneBreak.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.accent.copy(alpha = 0.8f)
                )
            }
            SceneBreakStyle.CUSTOM -> {
                Text(
                    text = item.sceneBreak.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// BLOCKQUOTE SEGMENT
// =============================================================================

@Composable
fun BlockquoteSegmentItem(
    item: ReaderDisplayItem.Segment,
    settings: ReaderSettings,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    textColor: Color,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    colors: ReaderColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = paragraphSpacing / 2)
    ) {
        // Left border
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(IntrinsicSize.Min)
                .background(
                    colors.accent.copy(alpha = 0.5f),
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Quoted content
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    colors.surface.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = item.segment.styledText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = settings.fontSize.sp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = FontStyle.Italic,
                    lineHeight = (settings.fontSize * settings.lineHeight).sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            )
        }
    }
}

// =============================================================================
// CODE BLOCK SEGMENT (simplified - just monospace font)
// =============================================================================

@Composable
fun CodeBlockSegmentItem(
    item: ReaderDisplayItem.Segment,
    displayIndex: Int,
    currentSentenceHighlight: SentenceHighlight?,
    isTTSActive: Boolean,
    highlightEnabled: Boolean,
    settings: ReaderSettings,
    textColor: Color,
    highlightColor: Color,
    horizontalPadding: Dp,
    paragraphSpacing: Dp
) {
    val segment = item.segment

    val hasSentenceHighlight = isTTSActive &&
            highlightEnabled &&
            currentSentenceHighlight != null &&
            currentSentenceHighlight.segmentDisplayIndex == displayIndex

    // Build annotated string with TTS highlight support
    val annotatedText = remember(
        segment.styledText,
        hasSentenceHighlight,
        currentSentenceHighlight,
        textColor
    ) {
        buildAnnotatedString {
            append(segment.styledText)

            // Apply TTS highlight if active
            if (hasSentenceHighlight && currentSentenceHighlight != null) {
                val sentence = currentSentenceHighlight.sentence
                val textLength = segment.styledText.length
                val start = sentence.startIndex.coerceIn(0, textLength)
                val end = sentence.endIndex.coerceIn(0, textLength)

                if (start < end) {
                    addStyle(
                        style = SpanStyle(background = highlightColor),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = paragraphSpacing / 2)
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineHeight).sp,
                color = textColor
            )
        )
    }
}

// =============================================================================
// SYSTEM MESSAGE SEGMENT (simplified - just monospace font)
// =============================================================================

@Composable
fun SystemMessageSegmentItem(
    item: ReaderDisplayItem.Segment,
    displayIndex: Int,
    currentSentenceHighlight: SentenceHighlight?,
    isTTSActive: Boolean,
    highlightEnabled: Boolean,
    settings: ReaderSettings,
    textColor: Color,
    highlightColor: Color,
    horizontalPadding: Dp,
    paragraphSpacing: Dp
) {
    val segment = item.segment

    val hasSentenceHighlight = isTTSActive &&
            highlightEnabled &&
            currentSentenceHighlight != null &&
            currentSentenceHighlight.segmentDisplayIndex == displayIndex

    // Build annotated string with TTS highlight support
    val annotatedText = remember(
        segment.styledText,
        hasSentenceHighlight,
        currentSentenceHighlight,
        textColor
    ) {
        buildAnnotatedString {
            append(segment.styledText)

            // Apply TTS highlight if active
            if (hasSentenceHighlight && currentSentenceHighlight != null) {
                val sentence = currentSentenceHighlight.sentence
                val textLength = segment.styledText.length
                val start = sentence.startIndex.coerceIn(0, textLength)
                val end = sentence.endIndex.coerceIn(0, textLength)

                if (start < end) {
                    addStyle(
                        style = SpanStyle(background = highlightColor),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = paragraphSpacing / 2)
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineHeight).sp,
                color = textColor
            )
        )
    }
}