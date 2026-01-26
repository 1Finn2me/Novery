package com.emptycastle.novery.ui.screens.home.tabs.browse

import com.emptycastle.novery.provider.MainProvider

/**
 * UI State for the provider selection grid
 */
data class BrowseUiState(
    val providers: List<MainProvider> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Provider info for display in the grid
 */
data class ProviderDisplayInfo(
    val name: String,
    val mainUrl: String,
    val tagCount: Int,
    val hasFilters: Boolean,
    val color: Long // For the icon background
)