// com/emptycastle/novery/ui/screens/details/components/SelectionModeOverlay.kt
// Improved visual design while keeping functionality
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.ui.screens.details.util.DetailsColors

// ================================================================
// DATA CLASSES
// ================================================================

data class SelectionState(
    val selectedCount: Int,
    val totalCount: Int,
    val selectedNotDownloadedCount: Int,
    val selectedDownloadedCount: Int,
    val selectedUnreadCount: Int,
    val selectedReadCount: Int,
    val isDownloadActive: Boolean
) {
    val hasSelection: Boolean get() = selectedCount > 0
    val allSelected: Boolean get() = selectedCount == totalCount && totalCount > 0
    val selectionRatio: Float get() = if (totalCount > 0) selectedCount.toFloat() / totalCount else 0f
    val showMarkAsRead: Boolean get() = selectedUnreadCount >= selectedReadCount
    val canMarkRead: Boolean get() = selectedUnreadCount > 0
    val canMarkUnread: Boolean get() = selectedReadCount > 0
}

data class SelectionCallbacks(
    val onSelectAll: () -> Unit,
    val onSelectAllUnread: () -> Unit,
    val onSelectAllNotDownloaded: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onInvertSelection: () -> Unit,
    val onCancel: () -> Unit,
    val onDownload: () -> Unit,
    val onDelete: () -> Unit,
    val onMarkAsRead: () -> Unit,
    val onMarkAsUnread: () -> Unit,
    val onMarkAsLastRead: () -> Unit
)

// ================================================================
// MAIN OVERLAY
// ================================================================

@Composable
fun SelectionModeOverlay(
    isVisible: Boolean,
    selectionState: SelectionState,
    callbacks: SelectionCallbacks,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top Bar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            SelectionTopBar(
                selectionState = selectionState,
                callbacks = callbacks
            )
        }

        // Bottom Bar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                selectionState = selectionState,
                callbacks = callbacks
            )
        }
    }
}

// ================================================================
// TOP BAR
// ================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionTopBar(
    selectionState: SelectionState,
    callbacks: SelectionCallbacks,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Close button
                    Surface(
                        onClick = callbacks.onCancel,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel selection",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Selection counter
                    SelectionCounterPill(
                        selectedCount = selectionState.selectedCount,
                        totalCount = selectionState.totalCount
                    )
                }

                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TopBarIconButton(
                        onClick = callbacks.onSelectAll,
                        enabled = !selectionState.allSelected,
                        icon = Icons.Outlined.SelectAll,
                        contentDescription = "Select all"
                    )
                    TopBarIconButton(
                        onClick = callbacks.onInvertSelection,
                        enabled = selectionState.hasSelection,
                        icon = Icons.Outlined.SwapVert,
                        contentDescription = "Invert selection"
                    )
                    TopBarIconButton(
                        onClick = callbacks.onDeselectAll,
                        enabled = selectionState.hasSelection,
                        icon = Icons.Outlined.Deselect,
                        contentDescription = "Deselect all"
                    )
                }
            }

            // Quick filter chips
            AnimatedVisibility(
                visible = selectionState.totalCount > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickFilterChip(
                            label = "All",
                            count = selectionState.totalCount,
                            isSelected = selectionState.allSelected,
                            onClick = callbacks.onSelectAll
                        )
                        QuickFilterChip(
                            label = "Unread",
                            count = selectionState.totalCount - selectionState.selectedReadCount,
                            icon = Icons.Outlined.VisibilityOff,
                            onClick = callbacks.onSelectAllUnread
                        )
                        QuickFilterChip(
                            label = "Not Downloaded",
                            count = selectionState.totalCount - selectionState.selectedDownloadedCount,
                            icon = Icons.Outlined.CloudDownload,
                            onClick = callbacks.onSelectAllNotDownloaded
                        )
                    }
                }
            }

            // Progress indicator
            if (selectionState.hasSelection) {
                LinearProgressIndicator(
                    progress = { selectionState.selectionRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SelectionCounterPill(
    selectedCount: Int,
    totalCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                        .togetherWith(scaleOut() + fadeOut())
                },
                label = "count"
            ) { count ->
                Text(
                    text = if (count > 999) "999+" else count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = "/ $totalCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(38.dp)
            .scale(scale),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun QuickFilterChip(
    label: String,
    count: Int,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chip_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
        },
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "chip_content"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )

            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (count > 99) "99" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ================================================================
// BOTTOM BAR
// ================================================================

@Composable
private fun SelectionBottomBar(
    selectionState: SelectionState,
    callbacks: SelectionCallbacks,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status indicators row
            StatusIndicatorsRow(selectionState = selectionState)

            // Action buttons row
            ActionButtonsRow(
                selectionState = selectionState,
                callbacks = callbacks
            )
        }
    }
}

@Composable
private fun StatusIndicatorsRow(selectionState: SelectionState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusIndicator(
            icon = Icons.Rounded.CheckCircle,
            count = selectionState.selectedCount,
            label = "Selected",
            color = MaterialTheme.colorScheme.primary,
            isActive = selectionState.hasSelection
        )

        StatusIndicator(
            icon = Icons.Rounded.DownloadDone,
            count = selectionState.selectedDownloadedCount,
            label = "Downloaded",
            color = DetailsColors.Success,
            isActive = selectionState.selectedDownloadedCount > 0
        )

        StatusIndicator(
            icon = Icons.Rounded.VisibilityOff,
            count = selectionState.selectedUnreadCount,
            label = "Unread",
            color = DetailsColors.Warning,
            isActive = selectionState.selectedUnreadCount > 0
        )

        StatusIndicator(
            icon = Icons.Rounded.Download,
            count = selectionState.selectedNotDownloadedCount,
            label = "To Download",
            color = MaterialTheme.colorScheme.tertiary,
            isActive = selectionState.selectedNotDownloadedCount > 0
        )
    }
}

@Composable
private fun StatusIndicator(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    isActive: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.35f,
        animationSpec = tween(200),
        label = "indicator_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = color.copy(alpha = alpha)
            )

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "count"
            ) { targetCount ->
                Text(
                    text = targetCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = alpha)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)
        )
    }
}

@Composable
private fun ActionButtonsRow(
    selectionState: SelectionState,
    callbacks: SelectionCallbacks
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Download
        ActionButton(
            icon = Icons.Rounded.Download,
            label = "Download",
            enabled = selectionState.selectedNotDownloadedCount > 0 && !selectionState.isDownloadActive,
            color = MaterialTheme.colorScheme.primary,
            onClick = callbacks.onDownload
        )

        // Mark Read/Unread
        ReadStatusButton(
            showMarkAsRead = selectionState.showMarkAsRead,
            canMarkRead = selectionState.canMarkRead,
            canMarkUnread = selectionState.canMarkUnread,
            onMarkAsRead = callbacks.onMarkAsRead,
            onMarkAsUnread = callbacks.onMarkAsUnread
        )

        // Mark as Last Read
        ActionButton(
            icon = Icons.Rounded.Bookmark,
            label = "Last Read",
            enabled = selectionState.selectedCount == 1,
            color = DetailsColors.Warning,
            onClick = callbacks.onMarkAsLastRead
        )

        // Delete
        ActionButton(
            icon = Icons.Rounded.Delete,
            label = "Delete",
            enabled = selectionState.selectedDownloadedCount > 0,
            color = MaterialTheme.colorScheme.error,
            onClick = callbacks.onDelete
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "action_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(150),
        label = "action_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        animationSpec = tween(150),
        label = "action_content"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ReadStatusButton(
    showMarkAsRead: Boolean,
    canMarkRead: Boolean,
    canMarkUnread: Boolean,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val enabled = if (showMarkAsRead) canMarkRead else canMarkUnread
    val icon = if (showMarkAsRead) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
    val label = if (showMarkAsRead) "Read" else "Unread"
    val color = if (showMarkAsRead) DetailsColors.Success else MaterialTheme.colorScheme.secondary

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "read_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(150),
        label = "read_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        animationSpec = tween(150),
        label = "read_content"
    )

    Surface(
        onClick = { if (showMarkAsRead) onMarkAsRead() else onMarkAsUnread() },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedContent(
                targetState = showMarkAsRead,
                transitionSpec = {
                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "read_icon"
            ) { isRead ->
                Icon(
                    imageVector = if (isRead) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor
                )
            }

            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label = "read_label"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

// ================================================================
// HELPER FUNCTIONS
// ================================================================

fun createSelectionState(
    selectedCount: Int,
    totalCount: Int,
    selectedNotDownloadedCount: Int,
    selectedDownloadedCount: Int,
    selectedUnreadCount: Int,
    selectedReadCount: Int,
    isDownloadActive: Boolean
): SelectionState = SelectionState(
    selectedCount = selectedCount,
    totalCount = totalCount,
    selectedNotDownloadedCount = selectedNotDownloadedCount,
    selectedDownloadedCount = selectedDownloadedCount,
    selectedUnreadCount = selectedUnreadCount,
    selectedReadCount = selectedReadCount,
    isDownloadActive = isDownloadActive
)

fun createSelectionCallbacks(
    onSelectAll: () -> Unit,
    onSelectAllUnread: () -> Unit = onSelectAll,
    onSelectAllNotDownloaded: () -> Unit = onSelectAll,
    onDeselectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancel: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onMarkAsLastRead: () -> Unit = {}
): SelectionCallbacks = SelectionCallbacks(
    onSelectAll = onSelectAll,
    onSelectAllUnread = onSelectAllUnread,
    onSelectAllNotDownloaded = onSelectAllNotDownloaded,
    onDeselectAll = onDeselectAll,
    onInvertSelection = onInvertSelection,
    onCancel = onCancel,
    onDownload = onDownload,
    onDelete = onDelete,
    onMarkAsRead = onMarkAsRead,
    onMarkAsUnread = onMarkAsUnread,
    onMarkAsLastRead = onMarkAsLastRead
)