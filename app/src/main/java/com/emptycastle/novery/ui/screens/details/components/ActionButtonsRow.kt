package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


// ================================================================
// MAIN ACTION BUTTONS ROW
// ================================================================

@Composable
fun ActionButtonsRow(
    hasStartedReading: Boolean,
    lastReadChapterName: String?,
    isDownloading: Boolean,
    downloadProgress: Float,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onViewDownloads: (() -> Unit)? = null  // New parameter for navigation
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Last read indicator (shows where user left off)
        LastReadIndicator(
            isVisible = hasStartedReading && !lastReadChapterName.isNullOrBlank(),
            chapterName = lastReadChapterName
        )

        // Main action buttons row
        MainActionButtons(
            hasStartedReading = hasStartedReading,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onRead = onRead,
            onDownload = onDownload,
            onViewDownloads = onViewDownloads
        )
    }
}

// ================================================================
// LAST READ INDICATOR
// ================================================================

@Composable
private fun LastReadIndicator(
    isVisible: Boolean,
    chapterName: String?
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bookmark icon
                LastReadIcon()

                // Chapter info
                LastReadInfo(chapterName = chapterName ?: "")
            }
        }
    }
}

@Composable
private fun LastReadIcon() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.BookmarkAdded,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LastReadInfo(
    chapterName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Continue from",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Text(
            text = chapterName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ================================================================
// MAIN ACTION BUTTONS
// ================================================================

@Composable
private fun MainActionButtons(
    hasStartedReading: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onViewDownloads: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Read/Continue button
        ReadButton(
            hasStartedReading = hasStartedReading,
            onClick = onRead,
            modifier = Modifier.weight(1f)
        )

        // Download button
        DownloadButton(
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onClick = onDownload,
            onViewDownloads = onViewDownloads
        )
    }
}

@Composable
private fun ReadButton(
    hasStartedReading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = if (hasStartedReading) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (hasStartedReading) "Continue" else "Start Reading",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun DownloadButton(
    isDownloading: Boolean,
    downloadProgress: Float,
    onClick: () -> Unit,
    onViewDownloads: (() -> Unit)?
) {
    OutlinedButton(
        onClick = if (isDownloading && onViewDownloads != null) onViewDownloads else onClick,
        modifier = Modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.5.dp,
            if (isDownloading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        // Icon or progress indicator
        DownloadButtonIcon(
            isDownloading = isDownloading,
            downloadProgress = downloadProgress
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Text - show "View" when downloading with navigation available
        if (isDownloading && onViewDownloads != null) {
            Text(
                text = "${(downloadProgress * 100).toInt()}%",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "View downloads",
                modifier = Modifier.size(14.dp)
            )
        } else if (isDownloading) {
            Text(
                text = "${(downloadProgress * 100).toInt()}%",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Text(
                text = "Download",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun DownloadButtonIcon(
    isDownloading: Boolean,
    downloadProgress: Float
) {
    Box(contentAlignment = Alignment.Center) {
        if (isDownloading) {
            CircularProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ================================================================
// ALTERNATIVE COMPACT VERSION (Optional)
// ================================================================

/**
 * A more compact version of the action buttons for smaller screens
 * or when space is limited
 */
@Composable
fun CompactActionButtonsRow(
    hasStartedReading: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onViewDownloads: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Read button - takes more space
        Button(
            onClick = onRead,
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (hasStartedReading) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasStartedReading) "Continue" else "Read",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
        }

        // Download button - icon only when downloading
        OutlinedButton(
            onClick = if (isDownloading && onViewDownloads != null) onViewDownloads else onDownload,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (isDownloading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}