package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.screens.details.ChapterFilter

@Composable
fun EmptyChaptersMessage(
    filter: ChapterFilter,
    hasSearch: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (icon, message, hint) = getEmptyStateContent(filter, hasSearch)

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class EmptyStateContent(
    val icon: ImageVector,
    val message: String,
    val hint: String
)

private fun getEmptyStateContent(
    filter: ChapterFilter,
    hasSearch: Boolean
): EmptyStateContent {
    return when {
        hasSearch -> EmptyStateContent(
            icon = Icons.Outlined.SearchOff,
            message = "No chapters found",
            hint = "Try a different search term"
        )
        filter == ChapterFilter.UNREAD -> EmptyStateContent(
            icon = Icons.Outlined.DoneAll,
            message = "All caught up!",
            hint = "You've read all chapters"
        )
        filter == ChapterFilter.DOWNLOADED -> EmptyStateContent(
            icon = Icons.Outlined.CloudOff,
            message = "No downloads yet",
            hint = "Download chapters for offline reading"
        )
        filter == ChapterFilter.NOT_DOWNLOADED -> EmptyStateContent(
            icon = Icons.Outlined.CloudDone,
            message = "All downloaded!",
            hint = "Every chapter is saved locally"
        )
        else -> EmptyStateContent(
            icon = Icons.Outlined.MenuBook,
            message = "No chapters available",
            hint = "Check back later for updates"
        )
    }
}