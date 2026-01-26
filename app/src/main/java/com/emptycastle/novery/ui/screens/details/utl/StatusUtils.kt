package com.emptycastle.novery.ui.screens.details.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.emptycastle.novery.domain.model.ReadingStatus

/**
 * Utility functions for ReadingStatus display
 */
object StatusUtils {

    fun getStatusColor(status: ReadingStatus): Color = when (status) {
        ReadingStatus.READING -> DetailsColors.StatusReading
        ReadingStatus.COMPLETED -> DetailsColors.StatusCompleted
        ReadingStatus.ON_HOLD -> DetailsColors.StatusOnHold
        ReadingStatus.PLAN_TO_READ -> DetailsColors.StatusPlanToRead
        ReadingStatus.DROPPED -> DetailsColors.StatusDropped
    }

    fun getStatusIcon(status: ReadingStatus): ImageVector = when (status) {
        ReadingStatus.READING -> Icons.Default.MenuBook
        ReadingStatus.COMPLETED -> Icons.Default.CheckCircle
        ReadingStatus.ON_HOLD -> Icons.Default.Pause
        ReadingStatus.PLAN_TO_READ -> Icons.Default.Schedule
        ReadingStatus.DROPPED -> Icons.Default.Cancel
    }

    fun getStatusDescription(status: ReadingStatus): String = when (status) {
        ReadingStatus.READING -> "Currently reading this novel"
        ReadingStatus.COMPLETED -> "Finished reading all chapters"
        ReadingStatus.ON_HOLD -> "Paused reading temporarily"
        ReadingStatus.PLAN_TO_READ -> "Added to reading list"
        ReadingStatus.DROPPED -> "Stopped reading"
    }
}

/**
 * Extension function for ReadingStatus display name
 */
fun ReadingStatus.displayName(): String = when (this) {
    ReadingStatus.READING -> "Reading"
    ReadingStatus.COMPLETED -> "Completed"
    ReadingStatus.ON_HOLD -> "On Hold"
    ReadingStatus.PLAN_TO_READ -> "Plan to Read"
    ReadingStatus.DROPPED -> "Dropped"
}