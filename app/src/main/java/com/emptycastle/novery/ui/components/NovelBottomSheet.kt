package com.emptycastle.novery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.emptycastle.novery.data.repository.LibraryItem

/**
 * Bottom sheet for novel actions (long-press menu)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelBottomSheet(
    item: LibraryItem,
    synopsis: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onContinueReading: () -> Unit,
    onRemoveFromLibrary: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with cover and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cover
                Card(
                    modifier = Modifier
                        .width(80.dp)
                        .aspectRatio(2f / 3f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    if (!item.novel.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.novel.posterUrl,
                            contentDescription = item.novel.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.novel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.novel.apiName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Reading status chip
                    FilterChip(
                        text = item.readingStatus.displayName(),
                        selected = true,
                        onClick = {}
                    )

                    // Last read info
                    item.lastReadPosition?.let { position ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last: ${position.chapterName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Synopsis
            if (!synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Continue Reading
                PrimaryButton(
                    text = if (item.lastReadPosition != null) "Continue Reading" else "Start Reading",
                    onClick = {
                        onDismiss()
                        onContinueReading()
                    },
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )

                // View Details
                SecondaryButton(
                    text = "View Details",
                    onClick = {
                        onDismiss()
                        onViewDetails()
                    },
                    icon = Icons.Default.Info,
                    modifier = Modifier.fillMaxWidth()
                )

                // Remove from Library
                DangerButton(
                    text = "Remove from Library",
                    onClick = {
                        onDismiss()
                        onRemoveFromLibrary()
                    },
                    icon = Icons.Default.Delete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Bottom sheet for chapter actions (long-press on chapter)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterBottomSheet(
    chapterName: String,
    isRead: Boolean,
    isDownloaded: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onDownload: () -> Unit,
    onMarkPreviousAsRead: (() -> Unit)? = null // New optional parameter
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Chapter name
            Text(
                text = chapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Status indicators
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRead) {
                    FilterChip(text = "Read", selected = true, onClick = {})
                }
                if (isDownloaded) {
                    FilterChip(text = "Downloaded", selected = true, onClick = {})
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mark as Read/Unread
                if (isRead) {
                    SecondaryButton(
                        text = "Mark as Unread",
                        onClick = {
                            onDismiss()
                            onMarkAsUnread()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SecondaryButton(
                        text = "Mark as Read",
                        onClick = {
                            onDismiss()
                            onMarkAsRead()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mark Previous as Read (optional)
                if (onMarkPreviousAsRead != null) {
                    SecondaryButton(
                        text = "Mark Previous as Read",
                        onClick = {
                            onDismiss()
                            onMarkPreviousAsRead()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Download (if not downloaded)
                if (!isDownloaded) {
                    PrimaryButton(
                        text = "Download",
                        onClick = {
                            onDismiss()
                            onDownload()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}