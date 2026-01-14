package com.emptycastle.novery.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Zinc900

/**
 * Pull-to-refresh wrapper component
 * Updated for Material 3 1.3.0+ (2025)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoveryPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    // 1. Handle "enabled" manually.
    // The official component does not have an 'enabled' prop yet, so if it's disabled,
    // we simply fall back to a standard Box to prevent the gesture.
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    // 2. Fixed: rememberPullToRefreshState() takes no arguments in M3 1.3.0
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Zinc900,
                color = Orange500
            )
        },
        content = content
    )
}

/**
 * Simple pull-to-refresh indicator
 */
@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isRefreshing) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Orange500,
                strokeWidth = 2.dp
            )
        }
    }
}