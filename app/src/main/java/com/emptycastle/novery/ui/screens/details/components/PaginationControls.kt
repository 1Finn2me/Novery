// com/emptycastle/novery/ui/screens/details/components/PaginationControls.kt
package com.emptycastle.novery.ui.screens.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emptycastle.novery.ui.screens.details.PaginationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginationControls(
    paginationState: PaginationState,
    totalChapters: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = paginationState.getTotalPages(totalChapters)
    val currentPage = paginationState.currentPage
    var showJumpDialog by remember { mutableStateOf(false) }

    // Slider state
    var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
    var isSliding by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Page info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current page indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Page $currentPage of $totalPages",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Range indicator
                val range = paginationState.getDisplayRange(totalChapters)
                Text(
                    text = "Chapters ${range.first}-${range.second}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Page slider (for quick navigation)
            if (totalPages > 3) {
                Column {
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            isSliding = true
                        },
                        onValueChangeFinished = {
                            isSliding = false
                            onPageChange(sliderValue.toInt().coerceIn(1, totalPages))
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = totalPages - 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        thumb = {
                            // Change Box to Column
                            Column(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                // Use Arrangement and Alignment to center the text instead of contentAlignment
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = isSliding,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut()
                                ) {
                                    Text(
                                        text = sliderValue.toInt().toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )

                    // Slider labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalPages.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First page
                PaginationButton(
                    icon = Icons.Default.FirstPage,
                    enabled = paginationState.canGoPrevious(),
                    onClick = { onPageChange(1) },
                    contentDescription = "First page"
                )

                // Previous page
                PaginationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    enabled = paginationState.canGoPrevious(),
                    onClick = { onPageChange(currentPage - 1) },
                    contentDescription = "Previous page",
                    size = PaginationButtonSize.Large
                )

                // Page number buttons
                SmartPageButtons(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageClick = onPageChange,
                    onEllipsisClick = { showJumpDialog = true }
                )

                // Next page
                PaginationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    enabled = paginationState.canGoNext(totalChapters),
                    onClick = { onPageChange(currentPage + 1) },
                    contentDescription = "Next page",
                    size = PaginationButtonSize.Large
                )

                // Last page
                PaginationButton(
                    icon = Icons.Default.LastPage,
                    enabled = paginationState.canGoNext(totalChapters),
                    onClick = { onPageChange(totalPages) },
                    contentDescription = "Last page"
                )
            }
        }
    }

    // Jump to page dialog
    if (showJumpDialog) {
        EnhancedJumpToPageDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onDismiss = { showJumpDialog = false },
            onConfirm = { page ->
                onPageChange(page)
                showJumpDialog = false
            }
        )
    }
}

enum class PaginationButtonSize { Regular, Large }

@Composable
private fun PaginationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    size: PaginationButtonSize = PaginationButtonSize.Regular
) {
    val buttonSize = if (size == PaginationButtonSize.Large) 44.dp else 36.dp
    val iconSize = if (size == PaginationButtonSize.Large) 28.dp else 22.dp

    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        label = "scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled)
            MaterialTheme.colorScheme.surfaceContainerHigh
        else
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = Modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SmartPageButtons(
    currentPage: Int,
    totalPages: Int,
    onPageClick: (Int) -> Unit,
    onEllipsisClick: () -> Unit
) {
    val pageItems = remember(currentPage, totalPages) {
        generateSmartPageNumbers(currentPage, totalPages)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pageItems.forEach { item ->
            when (item) {
                is PageItem.Number -> {
                    PageNumberButton(
                        number = item.value,
                        isSelected = item.value == currentPage,
                        onClick = { onPageClick(item.value) }
                    )
                }
                is PageItem.Ellipsis -> {
                    EllipsisButton(onClick = onEllipsisClick)
                }
            }
        }
    }
}

@Composable
private fun PageNumberButton(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "page_bg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "page_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EllipsisButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "•••",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnhancedJumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val isValidInput = inputText.toIntOrNull()?.let { it in 1..totalPages } == true

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FindInPage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Jump to Page",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter a page number (1-$totalPages)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            inputText = newValue
                        }
                    },
                    placeholder = {
                        Text(
                            currentPage.toString(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (isValidInput) {
                                focusManager.clearFocus()
                                onConfirm(inputText.toInt())
                            }
                        }
                    ),
                    isError = inputText.isNotEmpty() && !isValidInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                // Quick jump buttons
                Text(
                    text = "Quick Jump",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, totalPages / 4, totalPages / 2, (totalPages * 3) / 4, totalPages)
                        .distinct()
                        .filter { it in 1..totalPages }
                        .take(5)
                        .forEach { page ->
                            Surface(
                                onClick = { onConfirm(page) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (page == currentPage)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = page.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isValidInput) {
                                onConfirm(inputText.toInt())
                            }
                        },
                        enabled = isValidInput,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go")
                    }
                }
            }
        }
    }
}

private sealed class PageItem {
    data class Number(val value: Int) : PageItem()
    data object Ellipsis : PageItem()
}

private fun generateSmartPageNumbers(currentPage: Int, totalPages: Int): List<PageItem> {
    if (totalPages <= 5) {
        return (1..totalPages).map { PageItem.Number(it) }
    }

    val items = mutableListOf<PageItem>()

    when {
        currentPage <= 3 -> {
            items.addAll((1..minOf(3, totalPages)).map { PageItem.Number(it) })
            if (totalPages > 4) items.add(PageItem.Ellipsis)
            if (totalPages > 3) items.add(PageItem.Number(totalPages))
        }
        currentPage >= totalPages - 2 -> {
            items.add(PageItem.Number(1))
            if (totalPages > 4) items.add(PageItem.Ellipsis)
            items.addAll((maxOf(1, totalPages - 2)..totalPages).map { PageItem.Number(it) })
        }
        else -> {
            items.add(PageItem.Number(1))
            items.add(PageItem.Ellipsis)
            items.add(PageItem.Number(currentPage))
            items.add(PageItem.Ellipsis)
            items.add(PageItem.Number(totalPages))
        }
    }

    return items
}