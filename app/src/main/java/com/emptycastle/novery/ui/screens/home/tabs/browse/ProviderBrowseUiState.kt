// ProviderBrowseUiState.kt
package com.emptycastle.novery.ui.screens.home.tabs.browse

import com.emptycastle.novery.domain.model.Novel
import com.emptycastle.novery.provider.MainProvider

data class ProviderBrowseUiState(
    val provider: MainProvider? = null,
    val novels: List<Novel> = emptyList(),
    val currentPage: Int = 1,

    val selectedSort: String? = null,
    val selectedTag: String? = null,

    val searchQuery: String = "",
    val searchResults: List<Novel> = emptyList(),
    val isSearchMode: Boolean = false,
    val isSearching: Boolean = false,
    val searchError: String? = null,

    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isCloudflareError: Boolean = false
) {
    val displayNovels: List<Novel>
        get() = if (isSearchMode) searchResults else novels

    val isDisplayLoading: Boolean
        get() = when {
            isRefreshing -> false
            isSearchMode -> isSearching
            else -> isLoading
        }

    val displayError: String?
        get() = if (isSearchMode) searchError else error

    val isEmpty: Boolean
        get() = displayNovels.isEmpty() && !isDisplayLoading && displayError == null && !isRefreshing

    val hasActiveFilters: Boolean
        get() {
            val defaultSort = provider?.orderBys?.firstOrNull()?.value
            val defaultTag = provider?.tags?.firstOrNull()?.value
            return selectedSort != defaultSort || selectedTag != defaultTag
        }

    val providerUrl: String?
        get() = provider?.mainUrl

    val selectedSortLabel: String?
        get() = provider?.orderBys?.find { it.value == selectedSort }?.label

    val selectedTagLabel: String?
        get() = provider?.tags?.find { it.value == selectedTag }?.label

    val activeFilterCount: Int
        get() {
            var count = 0
            val defaultSort = provider?.orderBys?.firstOrNull()?.value
            val defaultTag = provider?.tags?.firstOrNull()?.value
            if (selectedSort != defaultSort) count++
            if (selectedTag != defaultTag) count++
            return count
        }

    val hasPreviousPage: Boolean
        get() = currentPage > 1 && !isSearchMode

    val showPagination: Boolean
        get() = displayNovels.isNotEmpty() && !isSearchMode && !isLoading && !isRefreshing

    val showSearchIndicator: Boolean
        get() = isSearchMode && searchResults.isNotEmpty() && !isSearching
}