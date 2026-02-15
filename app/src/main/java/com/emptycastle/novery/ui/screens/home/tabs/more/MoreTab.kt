package com.emptycastle.novery.ui.screens.home.tabs.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emptycastle.novery.ui.theme.NoveryTheme
import java.time.LocalTime

@Composable
fun MoreTab(
    onNavigateToProfile: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val dimensions = NoveryTheme.dimensions

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarPadding.calculateTopPadding(),
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item(key = "header") {
            MoreHeader(
                greeting = greeting,
                readerLevel = uiState.readerLevel,
                readerLevelName = uiState.readerLevelName,
                currentStreak = uiState.currentStreak,
                isStreakActive = uiState.isStreakActive
            )
        }

        // Quick Stats Summary
        item(key = "quick_stats") {
            QuickStatsSummary(
                totalChapters = uiState.totalChaptersRead,
                totalHours = uiState.totalHours,
                downloadCount = uiState.totalDownloads,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Menu Section
        item(key = "menu_header") {
            Text(
                text = "Menu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    horizontal = dimensions.gridPadding,
                    vertical = 8.dp
                )
            )
        }

        // Menu Items
        item(key = "profile_menu") {
            MoreMenuItem(
                icon = Icons.Rounded.Insights,
                title = "Reading Stats",
                subtitle = "View your reading progress and achievements",
                iconTint = Color(0xFF6366F1),
                onClick = onNavigateToProfile,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "downloads_menu") {
            MoreMenuItem(
                icon = Icons.Rounded.CloudDownload,
                title = "Downloads",
                subtitle = if (uiState.totalDownloads > 0) {
                    "${uiState.totalDownloads} chapters downloaded"
                } else {
                    "Manage offline content"
                },
                iconTint = Color(0xFF10B981),
                badge = if (uiState.activeDownloads > 0) "${uiState.activeDownloads}" else null,
                onClick = onNavigateToDownloads,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        item(key = "settings_menu") {
            MoreMenuItem(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                subtitle = "Theme, reading preferences, and more",
                iconTint = Color(0xFF8B5CF6),
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Spacer before About section
        item(key = "about_header") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    horizontal = dimensions.gridPadding,
                    vertical = 8.dp
                )
            )
        }

        item(key = "about_menu") {
            MoreMenuItem(
                icon = Icons.Rounded.Info,
                title = "About Novery",
                subtitle = "Version info, updates, and credits",
                iconTint = Color(0xFF3B82F6),
                onClick = onNavigateToAbout,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }
    }
}

@Composable
private fun MoreHeader(
    greeting: String,
    readerLevel: Int,
    readerLevelName: String,
    currentStreak: Int,
    isStreakActive: Boolean
) {
    val levelColor = getLevelColor(readerLevel)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        levelColor.copy(alpha = 0.12f),
                        levelColor.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$readerLevelName • Level $readerLevel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = levelColor
                    )
                }

                // Streak Badge
                if (currentStreak > 0 || isStreakActive) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isStreakActive) {
                            Color(0xFFFF6B35).copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isStreakActive) Color(0xFFFF6B35)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currentStreak",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isStreakActive) Color(0xFFFF6B35)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsSummary(
    totalChapters: Int,
    totalHours: Long,
    downloadCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatItem(
            value = "$totalChapters",
            label = "Chapters",
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        QuickStatItem(
            value = "${totalHours}h",
            label = "Reading",
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
        QuickStatItem(
            value = "$downloadCount",
            label = "Offline",
            color = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Badge for active downloads
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iconTint
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun getLevelColor(level: Int): Color = when (level) {
    1 -> Color(0xFF94A3B8)
    2 -> Color(0xFF22C55E)
    3 -> Color(0xFF3B82F6)
    4 -> Color(0xFF8B5CF6)
    5 -> Color(0xFFF59E0B)
    6 -> Color(0xFFEF4444)
    7 -> Color(0xFFFFD700)
    else -> Color(0xFFE879F9)
}