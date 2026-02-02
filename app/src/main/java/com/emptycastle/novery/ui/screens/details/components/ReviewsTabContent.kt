// com/emptycastle/novery/ui/screens/details/components/ReviewsTabContent.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.UserReview
import java.util.Locale

@Composable
fun ReviewsTabContent(
    reviews: List<UserReview>,
    isLoading: Boolean,
    hasMore: Boolean,
    showSpoilers: Boolean,
    onLoadMore: () -> Unit,
    onToggleSpoilers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Detect review style based on content
    val reviewStyle by remember(reviews) {
        derivedStateOf {
            if (reviews.any { it.overallScore != null || it.advancedScores.isNotEmpty() }) {
                ReviewStyle.DETAILED
            } else {
                ReviewStyle.SIMPLE
            }
        }
    }

    // Calculate average rating if available
    val averageRating by remember(reviews) {
        derivedStateOf {
            val scores = reviews.mapNotNull { it.overallScore }
            if (scores.isNotEmpty()) scores.average().toFloat() / 200f else null
        }
    }

    // Auto-load more when reaching the end
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasMore && !isLoading && reviews.isNotEmpty()) {
                onLoadMore()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        ReviewsTabHeader(
            reviewCount = reviews.size,
            averageRating = averageRating,
            reviewStyle = reviewStyle,
            showSpoilers = showSpoilers,
            onToggleSpoilers = onToggleSpoilers
        )

        when {
            reviews.isEmpty() && isLoading -> {
                LoadingState(modifier = Modifier.weight(1f))
            }
            reviews.isEmpty() && !isLoading -> {
                EmptyState(
                    reviewStyle = reviewStyle,
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = reviews,
                        key = { index, review ->
                            "review_${index}_${review.username}_${review.content.hashCode()}"
                        }
                    ) { _, review ->
                        ReviewCard(
                            review = review,
                            style = reviewStyle
                        )
                    }

                    // Loading indicator
                    if (isLoading) {
                        item(key = "loading") {
                            LoadingIndicator()
                        }
                    }

                    // Load more button
                    if (!isLoading && hasMore && reviews.isNotEmpty()) {
                        item(key = "load_more") {
                            LoadMoreButton(
                                reviewStyle = reviewStyle,
                                onClick = onLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsTabHeader(
    reviewCount: Int,
    averageRating: Float?,
    reviewStyle: ReviewStyle,
    showSpoilers: Boolean,
    onToggleSpoilers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (reviewStyle == ReviewStyle.DETAILED)
                            Icons.Filled.Reviews
                        else
                            Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = when {
                            reviewCount == 0 -> if (reviewStyle == ReviewStyle.DETAILED) "Reviews" else "Comments"
                            reviewStyle == ReviewStyle.DETAILED -> "$reviewCount Reviews"
                            else -> "$reviewCount Comments"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Average rating
                    if (averageRating != null && reviewCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f average", averageRating),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Spoiler toggle
            FilterChip(
                selected = showSpoilers,
                onClick = onToggleSpoilers,
                label = {
                    Text(
                        text = if (showSpoilers) "Visible" else "Hidden",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (showSpoilers)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun LoadMoreButton(
    reviewStyle: ReviewStyle,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (reviewStyle == ReviewStyle.DETAILED)
                    "Load More Reviews"
                else
                    "Load More Comments"
            )
        }
    }
}

@Composable
private fun EmptyState(
    reviewStyle: ReviewStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reviewStyle == ReviewStyle.DETAILED)
                        Icons.Filled.Reviews
                    else
                        Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (reviewStyle == ReviewStyle.DETAILED)
                    "No reviews yet"
                else
                    "No comments yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (reviewStyle == ReviewStyle.DETAILED)
                    "Be the first to share your thoughts!"
                else
                    "Start the conversation!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}