// com/emptycastle/novery/ui/screens/details/components/RelatedTabContent.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.domain.model.Novel

/**
 * Standalone grid content for Related tab (if used outside LazyColumn)
 * @param onNovelClick Called with (novelUrl, providerName) for navigation
 */
@Composable
fun RelatedTabContent(
    novels: List<Novel>,
    onNovelClick: (novelUrl: String, providerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (novels.isEmpty()) {
        EmptyRelatedMessage(modifier = modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = novels,
                key = { it.url }
            ) { novel ->
                RelatedNovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel.url, novel.apiName) }
                )
            }
        }
    }
}