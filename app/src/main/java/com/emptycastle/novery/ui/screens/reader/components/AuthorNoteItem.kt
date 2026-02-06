package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.emptycastle.novery.ui.screens.reader.logic.AuthorNoteDisplayMode
import com.emptycastle.novery.ui.screens.reader.logic.AuthorNoteSection
import com.emptycastle.novery.ui.screens.reader.model.ReaderDisplayItem
import com.emptycastle.novery.ui.screens.reader.theme.ReaderColors

@Composable
fun AuthorNoteItem(
    item: ReaderDisplayItem.AuthorNote,
    colors: ReaderColors,
    displayMode: AuthorNoteDisplayMode,
    fontFamily: FontFamily?,
    fontSize: Int,
    horizontalPadding: Dp,
    paragraphSpacing: Dp,
    modifier: Modifier = Modifier
) {
    if (displayMode == AuthorNoteDisplayMode.HIDDEN) {
        return
    }

    val authorNote = item.authorNote

    var isExpanded by rememberSaveable(item.itemId) {
        mutableStateOf(displayMode == AuthorNoteDisplayMode.EXPANDED)
    }

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0.08f else 0.04f,
        label = "background_alpha"
    )

    val noteBackground = if (colors.isDarkTheme) {
        Color.White.copy(alpha = backgroundAlpha)
    } else {
        Color.Black.copy(alpha = backgroundAlpha)
    }

    // Build display title
    val displayTitle = buildString {
        append(authorNote.noteType)
        authorNote.authorName?.let { name ->
            append(" from ")
            append(name)
        }
    }

    // Check if there are any images in the note
    val hasImages = authorNote.sections.any { it is AuthorNoteSection.ImageSection }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = paragraphSpacing / 2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(noteBackground)
                .clickable { isExpanded = !isExpanded }
                .animateContentSize()
                .padding(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(0.7f)
                    )
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(0.7f)
                )
            }

            // Preview text when collapsed
            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val previewText = authorNote.plainText
                    .replace("\n", " ")
                    .take(150)
                    .trim() + if (authorNote.plainText.length > 150) "..." else ""

                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontFamily = fontFamily,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasImages) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📷 Contains images",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Full content when expanded
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    authorNote.sections.forEach { section ->
                        when (section) {
                            is AuthorNoteSection.TextSection -> {
                                Text(
                                    text = section.annotatedString,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = (fontSize - 1).sp,
                                        lineHeight = (fontSize * 1.5f).sp
                                    ),
                                    color = colors.text,
                                    fontFamily = fontFamily
                                )
                            }

                            is AuthorNoteSection.ImageSection -> {
                                AuthorNoteImage(
                                    url = section.url,
                                    altText = section.altText,
                                    colors = colors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorNoteImage(
    url: String,
    altText: String?,
    colors: ReaderColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = url,
            contentDescription = altText,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background.copy(alpha = 0.5f)),
            contentScale = ContentScale.FillWidth
        )

        if (!altText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = altText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}