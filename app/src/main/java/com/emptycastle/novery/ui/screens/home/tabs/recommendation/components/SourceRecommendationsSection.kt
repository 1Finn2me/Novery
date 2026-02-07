package com.emptycastle.novery.ui.screens.home.tabs.recommendation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.recommendation.model.LibrarySourceNovel
import com.emptycastle.novery.recommendation.model.Recommendation

/**
 * Section showing recommendations based on a selected library novel.
 * Includes the header with source selection and the recommendation cards.
 */
@Composable
fun SourceRecommendationsSection(
    selectedSource: LibrarySourceNovel?,
    otherSources: List<LibrarySourceNovel>,
    recommendations: List<Recommendation>,
    isExpanded: Boolean,
    isLoading: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectSource: (LibrarySourceNovel) -> Unit,
    onNovelClick: (novelUrl: String, providerName: String) -> Unit,
    onNovelLongClick: (Recommendation) -> Unit,
    onQuickDismiss: (Recommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedSource == null && otherSources.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with source selection
        BecauseYouReadHeader(
            selectedSource = selectedSource,
            otherSources = otherSources,
            isExpanded = isExpanded,
            isLoading = isLoading,
            onToggleExpanded = onToggleExpanded,
            onSelectSource = onSelectSource
        )

        // Recommendations content
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (recommendations.isEmpty()) {
                // Empty state
                NoSourceRecommendationsFound(
                    sourceName = selectedSource?.novel?.name ?: "this novel"
                )
            } else {
                // Recommendation cards
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = recommendations,
                        key = { it.novel.url }
                    ) { recommendation ->
                        RecommendationCard(
                            recommendation = recommendation,
                            onClick = {
                                onNovelClick(
                                    recommendation.novel.url,
                                    recommendation.novel.apiName
                                )
                            },
                            onLongClick = { onNovelLongClick(recommendation) },
                            onQuickDismiss = { onQuickDismiss(recommendation) }
                        )
                    }
                }
            }
        }

        // Loading state
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Finding similar novels...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoSourceRecommendationsFound(
    sourceName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = "No similar novels found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "We couldn't find novels similar to \"$sourceName\". Try selecting another novel from your library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}