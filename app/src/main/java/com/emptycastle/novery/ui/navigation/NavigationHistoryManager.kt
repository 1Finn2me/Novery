// com/emptycastle/novery/ui/navigation/NavigationHistoryManager.kt

package com.emptycastle.novery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a screen state that can be restored
 */
data class ScreenState(
    val route: String,
    val scrollPosition: Int = 0,
    val scrollOffset: Int = 0,
    val extras: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages navigation history and screen state preservation
 */
class NavigationHistoryManager : ViewModel() {

    private val _screenStates = MutableStateFlow<Map<String, ScreenState>>(emptyMap())
    val screenStates: StateFlow<Map<String, ScreenState>> = _screenStates.asStateFlow()

    private val _navigationStack = MutableStateFlow<List<String>>(emptyList())
    val navigationStack: StateFlow<List<String>> = _navigationStack.asStateFlow()

    /**
     * Save state for a specific screen
     */
    fun saveScreenState(
        route: String,
        scrollPosition: Int = 0,
        scrollOffset: Int = 0,
        extras: Map<String, Any> = emptyMap()
    ) {
        val state = ScreenState(
            route = route,
            scrollPosition = scrollPosition,
            scrollOffset = scrollOffset,
            extras = extras
        )
        _screenStates.value = _screenStates.value + (route to state)
    }

    /**
     * Get saved state for a screen
     */
    fun getScreenState(route: String): ScreenState? {
        return _screenStates.value[route]
    }

    /**
     * Clear state for a screen
     */
    fun clearScreenState(route: String) {
        _screenStates.value = _screenStates.value - route
    }

    /**
     * Record navigation to a screen
     */
    fun onNavigate(route: String) {
        val current = _navigationStack.value.toMutableList()
        // Remove if already in stack (to move to top)
        current.remove(route)
        current.add(route)
        // Keep only last 20 entries
        _navigationStack.value = current.takeLast(20)
    }

    /**
     * Get previous screen in history
     */
    fun getPreviousRoute(): String? {
        val stack = _navigationStack.value
        return if (stack.size >= 2) stack[stack.size - 2] else null
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        _screenStates.value = emptyMap()
        _navigationStack.value = emptyList()
    }
}

/**
 * Composable helper to auto-save scroll state
 */
@Composable
fun rememberScrollStateForRoute(
    route: String,
    historyManager: NavigationHistoryManager = viewModel()
): Pair<Int, Int> {
    val savedState = remember(route) {
        historyManager.getScreenState(route)
    }

    return Pair(
        savedState?.scrollPosition ?: 0,
        savedState?.scrollOffset ?: 0
    )
}

/**
 * Effect to save scroll state when leaving screen
 */
@Composable
fun SaveScrollStateOnDispose(
    route: String,
    scrollPosition: () -> Int,
    scrollOffset: () -> Int,
    historyManager: NavigationHistoryManager = viewModel()
) {
    DisposableEffect(route) {
        onDispose {
            historyManager.saveScreenState(
                route = route,
                scrollPosition = scrollPosition(),
                scrollOffset = scrollOffset()
            )
        }
    }
}