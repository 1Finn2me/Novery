package com.emptycastle.novery.ui.screens.home.tabs.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.emptycastle.novery.provider.MainProvider

/**
 * ViewModel for the provider selection grid
 */
class BrowseViewModel : ViewModel() {

    private val novelRepository = RepositoryProvider.getNovelRepository()
    private val preferencesManager = RepositoryProvider.getPreferencesManager()

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        // Initial load
        refreshProviders()

        // Observe preferences and provider registry to refresh automatically
        viewModelScope.launch {
            launch {
                preferencesManager.appSettings.collect { refreshProviders() }
            }
            launch {
                MainProvider.providersState().collect { refreshProviders() }
            }
        }
    }

    private fun refreshProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val providers = novelRepository.getProviders()
                _uiState.update {
                    it.copy(
                        providers = providers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load providers",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun retry() {
        refreshProviders()
    }
}