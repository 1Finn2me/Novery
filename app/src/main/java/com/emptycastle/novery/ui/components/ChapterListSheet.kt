package com.emptycastle.novery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Chapter
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Zinc300
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc600
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900

/**
 * Chapter list bottom sheet for quick navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    readChapterUrls: Set<String> = emptySet(),
    onChapterSelected: (Int, Chapter) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Filter and sort chapters
    val displayedChapters = remember(chapters, searchQuery, sortDescending) {
        val filtered = if (searchQuery.isBlank()) {
            chapters
        } else {
            chapters.filter { chapter ->
                chapter.name.contains(searchQuery, ignoreCase = true)
            }
        }

        if (sortDescending) {
            filtered.reversed()
        } else {
            filtered
        }
    }

    // Scroll to current chapter on open
    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex >= 0 && searchQuery.isBlank()) {
            val targetIndex = if (sortDescending) {
                chapters.size - 1 - currentChapterIndex
            } else {
                currentChapterIndex
            }
            // Scroll with some offset to show context
            listState.scrollToItem(
                index = (targetIndex - 2).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Zinc900,
        contentColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Zinc600)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            // Header
            ChapterListHeader(
                totalChapters = chapters.size,
                currentChapter = currentChapterIndex + 1,
                sortDescending = sortDescending,
                onSortToggle = { sortDescending = !sortDescending },
                onClose = onDismiss
            )

            // Search bar
            ChapterSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = Zinc800)

            // Chapter list
            if (displayedChapters.isEmpty()) {
                EmptyChapterList(searchQuery = searchQuery)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = displayedChapters,
                        key = { _, chapter -> chapter.url }
                    ) { index, chapter ->
                        val originalIndex = if (sortDescending) {
                            chapters.size - 1 - chapters.indexOf(chapter)
                        } else {
                            chapters.indexOf(chapter)
                        }

                        val isCurrentChapter = originalIndex == currentChapterIndex
                        val isRead = readChapterUrls.contains(chapter.url)

                        ChapterListItem(
                            chapter = chapter,
                            chapterNumber = originalIndex + 1,
                            isCurrentChapter = isCurrentChapter,
                            isRead = isRead,
                            onClick = {
                                onChapterSelected(originalIndex, chapter)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListHeader(
    totalChapters: Int,
    currentChapter: Int,
    sortDescending: Boolean,
    onSortToggle: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$currentChapter of $totalChapters chapters",
                style = MaterialTheme.typography.bodySmall,
                color = Zinc400
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort toggle
            Surface(
                onClick = onSortToggle,
                shape = RoundedCornerShape(8.dp),
                color = Zinc800
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sortDescending)
                            Icons.Default.KeyboardArrowDown
                        else
                            Icons.Default.KeyboardArrowUp,
                        contentDescription = "Sort order",
                        tint = Zinc300,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sortDescending) "Newest" else "Oldest",
                        style = MaterialTheme.typography.labelMedium,
                        color = Zinc300
                    )
                }
            }

            // Close button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Zinc400
                )
            }
        }
    }
}

@Composable
private fun ChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("Search chapters...", color = Zinc500)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Zinc500
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Zinc500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Orange500,
            unfocusedBorderColor = Zinc700,
            focusedContainerColor = Zinc800,
            unfocusedContainerColor = Zinc800,
            cursorColor = Orange500
        ),
        singleLine = true
    )
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    chapterNumber: Int,
    isCurrentChapter: Boolean,
    isRead: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentChapter -> Orange500.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        label = "chapterBg"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isRead && !isCurrentChapter) 0.5f else 1f,
        label = "textAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter number badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isCurrentChapter) Orange500 else Zinc800
        ) {
            Text(
                text = chapterNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentChapter) Color.White else Zinc400,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Chapter name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrentChapter) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrentChapter) Orange400 else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(textAlpha)
            )
        }

        // Status indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRead) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Read",
                    tint = if (isCurrentChapter) Orange400 else Zinc600,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isCurrentChapter) {
                Surface(
                    shape = CircleShape,
                    color = Orange500
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Current",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChapterList(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Zinc600,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank())
                    "No chapters found for \"$searchQuery\""
                else
                    "No chapters available",
                style = MaterialTheme.typography.bodyMedium,
                color = Zinc400
            )
        }
    }
}