package com.emptycastle.novery.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current in-memory sync execution state for the UI.
 */
object SyncStatusTracker {
    private val _state = MutableStateFlow(SyncExecutionState())
    val state: StateFlow<SyncExecutionState> = _state.asStateFlow()

    fun start(trigger: SyncTrigger, stage: String) {
        _state.value = SyncExecutionState(
            isRunning = true,
            trigger = trigger,
            stage = stage,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun update(stage: String, message: String? = null) {
        val current = _state.value
        _state.value = current.copy(
            isRunning = true,
            stage = stage,
            lastMessage = message,
            lastError = null,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun finishSuccess(message: String) {
        val current = _state.value
        _state.value = current.copy(
            isRunning = false,
            lastMessage = message,
            lastError = null,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun finishError(message: String) {
        val current = _state.value
        _state.value = current.copy(
            isRunning = false,
            lastError = message,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun finishCancelled(message: String = "Sync cancelled") {
        val current = _state.value
        _state.value = current.copy(
            isRunning = false,
            lastMessage = message,
            lastError = null,
            updatedAt = System.currentTimeMillis()
        )
    }
}
