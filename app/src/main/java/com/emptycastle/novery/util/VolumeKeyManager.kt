package com.emptycastle.novery.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Volume key events that can be handled by the reader
 */
enum class VolumeKeyEvent {
    VOLUME_UP,
    VOLUME_DOWN
}

/**
 * Manages volume key events for the reader.
 *
 * The Activity intercepts volume keys and emits events here.
 * The ReaderScreen observes these events and handles navigation.
 */
object VolumeKeyManager {

    private val _volumeKeyEvents = MutableSharedFlow<VolumeKeyEvent>(extraBufferCapacity = 1)
    val volumeKeyEvents: SharedFlow<VolumeKeyEvent> = _volumeKeyEvents.asSharedFlow()

    private var _isReaderActive = false
    val isReaderActive: Boolean get() = _isReaderActive

    private var _volumeKeyNavigationEnabled = false
    val volumeKeyNavigationEnabled: Boolean get() = _volumeKeyNavigationEnabled

    /**
     * Called when the reader screen becomes active/inactive
     */
    fun setReaderActive(active: Boolean) {
        _isReaderActive = active
    }

    /**
     * Called to enable/disable volume key navigation
     */
    fun setVolumeKeyNavigationEnabled(enabled: Boolean) {
        _volumeKeyNavigationEnabled = enabled
    }

    /**
     * Called by Activity when a volume key is pressed.
     * Returns true if the event was consumed (reader is active and feature enabled).
     */
    fun onVolumeKeyPressed(isVolumeUp: Boolean): Boolean {
        if (!_isReaderActive || !_volumeKeyNavigationEnabled) {
            return false
        }

        val event = if (isVolumeUp) VolumeKeyEvent.VOLUME_UP else VolumeKeyEvent.VOLUME_DOWN
        _volumeKeyEvents.tryEmit(event)
        return true
    }

    /**
     * Reset state when app is backgrounded or reader is closed
     */
    fun reset() {
        _isReaderActive = false
    }
}