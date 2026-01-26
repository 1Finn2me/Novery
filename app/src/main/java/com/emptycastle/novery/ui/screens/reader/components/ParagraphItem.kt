package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.domain.model.ReaderSettings
import com.emptycastle.novery.ui.components.GhostButton
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.model.SentenceHighlight
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors
import com.emptycastle.novery.ui.screens.reader.theme.ReaderDefaults
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc800

// =============================================================================
// CHAPTER HEADER
// =============================================================================

@Composable
fun ChapterHeaderItem(
    item: ReaderDisplayItem.ChapterHeader,
    colors: ReaderColors,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding)
            .padding(top = 24.dp, bottom = 32.dp),
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
// SEGMENT / PARAGRAPH
// =============================================================================

@Composable
fun SegmentItem(
    item: ReaderDisplayItem.Segment,
    displayIndex: Int,
    isSegmentActive: Boolean,
    currentSentenceHighlight: SentenceHighlight?,
    isTTSActive: Boolean,
    highlightEnabled: Boolean,
    settings: ReaderSettings,
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign,
    textColor: Color,
    highlightColor: Color,
    onClick: () -> Unit,
    onSentenceClick: (Int) -> Unit
) {
    val segment = item.segment

    val hasSentenceHighlight = isTTSActive &&
            highlightEnabled &&
            currentSentenceHighlight != null &&
            currentSentenceHighlight.segmentDisplayIndex == displayIndex

    val annotatedText = remember(segment.text, hasSentenceHighlight, currentSentenceHighlight) {
        buildAnnotatedString {
            append(segment.text)

            if (hasSentenceHighlight && currentSentenceHighlight != null) {
                val sentence = currentSentenceHighlight.sentence
                val start = sentence.startIndex.coerceIn(0, segment.text.length)
                val end = sentence.endIndex.coerceIn(0, segment.text.length)

                if (start < end) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor
                        ),
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
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding)
            .padding(vertical = ReaderDefaults.SegmentSpacing / 2)
            .clip(RoundedCornerShape(ReaderDefaults.CornerRadius))
            .background(
                if (isSegmentActive && !hasSentenceHighlight) {
                    Orange500.copy(alpha = ReaderDefaults.ActiveSegmentBackgroundAlpha)
                } else {
                    Color.Transparent
                }
            )
            .then(
                if (isSegmentActive && !hasSentenceHighlight) {
                    Modifier.border(
                        width = ReaderDefaults.ActiveSegmentBorderWidth,
                        color = Orange500.copy(alpha = ReaderDefaults.ActiveSegmentBorderAlpha),
                        shape = RoundedCornerShape(ReaderDefaults.CornerRadius)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = settings.fontSize.sp,
                fontFamily = fontFamily,
                lineHeight = (settings.fontSize * settings.lineHeight).sp
            ),
            color = textColor,
            textAlign = textAlign
        )
    }
}

// =============================================================================
// CHAPTER DIVIDER
// =============================================================================

@Composable
fun ChapterDividerItem(
    item: ReaderDisplayItem.ChapterDivider,
    colors: ReaderColors,
    infiniteScrollEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ReaderDefaults.ChapterDividerVerticalPadding)
            .padding(horizontal = ReaderDefaults.ContentHorizontalPadding),
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
                    modifier = Modifier.weight(1f)
                )

                ChapterNavButton(
                    text = "Next",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = item.hasNextChapter,
                    isPrimary = true,
                    colors = colors,
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
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
            .height(56.dp)
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
                    modifier = Modifier.size(20.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}