package com.emptycastle.novery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Tab-specific scroll state
 */
data class TabScrollState(
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0
)

/**
 * Manages navigation state for the home screen with bottom tabs.
 * Handles back stack preservation per tab with scroll position memory.
 */
@Stable
class TabNavigationState(
    val navController: NavHostController
) {
    // Store scroll states per tab
    private val tabScrollStates = mutableMapOf<HomeTabs, TabScrollState>()

    /**
     * Get current tab from actual navigation state - NOT a separate tracked variable
     */
    @Composable
    fun currentTabAsState(): HomeTabs? {
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        return navBackStackEntry?.destination?.route?.let { HomeTabs.fromRoute(it) }
    }

    /**
     * Save scroll state for a specific tab
     */
    fun saveTabScrollState(tab: HomeTabs, scrollIndex: Int, scrollOffset: Int) {
        tabScrollStates[tab] = TabScrollState(scrollIndex, scrollOffset)
    }

    /**
     * Get saved scroll state for a tab
     */
    fun getTabScrollState(tab: HomeTabs): TabScrollState {
        return tabScrollStates[tab] ?: TabScrollState()
    }

    /**
     * Navigate to a tab, preserving back stack state
     */
    fun navigateToTab(tab: HomeTabs) {
        // Get current route from the actual navigation state
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        // Only skip if we're truly on the same route
        if (currentRoute == tab.route) {
            return
        }

        navController.navigate(tab.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    /**
     * Navigate to novel details with state preservation
     */
    fun navigateToDetails(
        novelUrl: String,
        providerName: String,
        currentTab: HomeTabs? = null,
        saveScrollState: (() -> Pair<Int, Int>)? = null
    ) {
        // Save current tab's scroll position
        if (currentTab != null) {
            saveScrollState?.invoke()?.let { (index, offset) ->
                tabScrollStates[currentTab] = TabScrollState(index, offset)
            }
        }

        navController.navigate(NavRoutes.Details.createRoute(novelUrl, providerName))
    }

    /**
     * Navigate to chapter reader
     */
    fun navigateToReader(
        chapterUrl: String,
        novelUrl: String,
        providerName: String
    ) {
        navController.navigate(NavRoutes.Reader.createRoute(chapterUrl, novelUrl, providerName))
    }

    /**
     * Navigate back with scroll restoration
     */
    fun navigateBack(): Boolean {
        return navController.popBackStack()
    }

    /**
     * Check if can navigate back
     */
    fun canNavigateBack(): Boolean {
        return navController.previousBackStackEntry != null
    }
}

@Composable
fun rememberTabNavigationState(
    navController: NavHostController = rememberNavController()
): TabNavigationState {
    return remember(navController) {
        TabNavigationState(navController)
    }
}