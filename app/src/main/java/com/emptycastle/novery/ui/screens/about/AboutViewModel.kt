package com.emptycastle.novery.ui.screens.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AboutUiState(
    val currentVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateResult: UpdateChecker.UpdateResult? = null,
    val updateError: String? = null,
    val hasChecked: Boolean = false
)

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val updateChecker = UpdateChecker(application)

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(currentVersion = updateChecker.getCurrentVersion())
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isCheckingUpdate = true, updateError = null)
            }

            val result = updateChecker.checkForUpdate()

            result.onSuccess { updateResult ->
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateResult = updateResult,
                        hasChecked = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateError = error.message ?: "Failed to check for updates",
                        hasChecked = true
                    )
                }
            }
        }
    }
}