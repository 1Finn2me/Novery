package com.emptycastle.novery.ui.screens.home.tabs.search

import com.emptycastle.novery.domain.model.Novel

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: Map<String, List<Novel>> = emptyMap(),
    val providers: List<String> = emptyList(),
    val expandedProvider: String? = null
)