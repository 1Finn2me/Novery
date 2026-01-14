package com.emptycastle.novery.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.emptycastle.novery.domain.model.GridColumns

/**
 * Calculate the number of grid columns based on settings and screen width
 */
@Composable
fun calculateGridColumns(
    gridColumns: GridColumns,
    minColumnWidth: Int = 150
): Int {
    return when (gridColumns) {
        is GridColumns.Auto -> {
            val screenWidth = LocalConfiguration.current.screenWidthDp
            (screenWidth / minColumnWidth).coerceIn(2, 5)
        }
        is GridColumns.Fixed -> gridColumns.count
    }
}