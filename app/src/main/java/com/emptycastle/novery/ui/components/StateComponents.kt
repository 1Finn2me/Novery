package com.emptycastle.novery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.theme.*

/**
 * Error message with retry option
 */
@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Error.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Error.copy(alpha = 0.2f))
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Error.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.width(12.dp))

                TextButton(onClick = onRetry) {
                    Text("Retry", color = Error)
                }
            }
        }
    }
}

/**
 * Empty state placeholder
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = Zinc900.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Zinc700
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Zinc300
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Zinc500,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange600
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Empty library state
 */
@Composable
fun EmptyLibrary(
    onBrowse: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.LibraryBooks,
        title = "Nothing here",
        message = "Start browsing to add novels to your collection.",
        actionLabel = "Go to Browse",
        onAction = onBrowse
    )
}

/**
 * Empty history state
 */
@Composable
fun EmptyHistory() {
    EmptyState(
        icon = Icons.Default.History,
        title = "No history yet",
        message = "Novels you read will appear here."
    )
}

/**
 * Empty search results
 */
@Composable
fun EmptySearchResults(
    query: String
) {
    EmptyState(
        icon = Icons.Default.SearchOff,
        title = "No results found",
        message = "No novels found for \"$query\". Try a different search term."
    )
}

/**
 * No internet connection state
 */
@Composable
fun NoConnection(
    onRetry: () -> Unit
) {
    EmptyState(
        icon = Icons.Default.WifiOff,
        title = "No Connection",
        message = "Please check your internet connection and try again.",
        actionLabel = "Retry",
        onAction = onRetry
    )
}