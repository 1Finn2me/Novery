// com/emptycastle/novery/ui/screens/home/tabs/profile/ProfileTab.kt

package com.emptycastle.novery.ui.screens.home.tabs.profile

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.emptycastle.novery.ui.components.NoveryPullToRefreshBox
import com.emptycastle.novery.ui.theme.NoveryTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime

// ============================================================================
// Color Constants
// ============================================================================

private object ProfileColors {
    val StreakOrange = Color(0xFFFF6B35)
    val StreakYellow = Color(0xFFFFB800)
    val GoalPrimary = Color(0xFF6366F1)
    val GoalSecondary = Color(0xFF8B5CF6)
    val ChapterBlue = Color(0xFF3B82F6)
    val TimeGreen = Color(0xFF10B981)
    val DaysAmber = Color(0xFFF59E0B)
    val AchievementGold = Color(0xFFFFD700)
    val LevelPurple = Color(0xFF9333EA)
    val InsightCyan = Color(0xFF06B6D4)

    fun getLevelColor(level: Int): Color = when (level) {
        1 -> Color(0xFF94A3B8)
        2 -> Color(0xFF22C55E)
        3 -> Color(0xFF3B82F6)
        4 -> Color(0xFF8B5CF6)
        5 -> Color(0xFFF59E0B)
        6 -> Color(0xFFEF4444)
        7 -> AchievementGold
        else -> Color(0xFFE879F9)
    }
}

// ============================================================================
// Main Profile Tab
// ============================================================================

@Composable
fun ProfileTab(
    onNavigateToSettings: () -> Unit,
    onNovelClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val context = LocalContext.current

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.loadStats()

        viewModel.events.collectLatest { event ->
            when (event) {
                is ProfileEvent.ShareStats -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share your reading stats")
                    context.startActivity(shareIntent)
                }
                is ProfileEvent.NavigateToNovel -> {
                    onNovelClick(event.novelUrl, event.sourceName)
                }
                is ProfileEvent.ShowError -> {
                    // Could show a snackbar here
                }
            }
        }
    }

    NoveryPullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        if (!uiState.hasAnyStats && !uiState.isLoading) {
            ProfileEmptyState(
                onNavigateToSettings = onNavigateToSettings,
                statusBarPadding = statusBarPadding,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ProfileContent(
                uiState = uiState,
                onNovelClick = { novel -> viewModel.onNovelClick(novel) },
                onShareStats = { viewModel.onShareStats() },
                onNavigateToSettings = onNavigateToSettings,
                statusBarPadding = statusBarPadding
            )
        }
    }
}

// ============================================================================
// Profile Content
// ============================================================================

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNovelClick: (NovelReadingStats) -> Unit,
    onShareStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    statusBarPadding: PaddingValues
) {
    val dimensions = NoveryTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarPadding.calculateTopPadding(),
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Hero Section
        item(key = "hero") {
            ProfileHeroSection(
                uiState = uiState,
                onShareStats = onShareStats,
                onNavigateToSettings = onNavigateToSettings
            )
        }

        // Quick Stats Row
        item(key = "quick_stats") {
            QuickStatsRow(
                currentStreak = uiState.currentStreak,
                totalChapters = uiState.totalChaptersRead,
                totalHours = uiState.totalHours,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Streak Card (if active or has streak)
        if (uiState.isStreakActive || uiState.currentStreak > 0) {
            item(key = "streak") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -it / 2 }
                ) {
                    StreakCard(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak,
                        isStreakActive = uiState.isStreakActive,
                        modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                    )
                }
            }
        }

        // Goals Progress
        item(key = "goals") {
            GoalsSection(
                todayMinutes = uiState.todayMinutes.toInt(),
                weekMinutes = uiState.weekMinutes.toInt(),
                dailyGoal = uiState.dailyGoalMinutes,
                weeklyGoal = uiState.weeklyGoalMinutes,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Reading Insights
        item(key = "insights") {
            ReadingInsightsSection(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Weekly Activity
        item(key = "weekly_activity") {
            WeeklyActivitySection(
                dailyMinutes = uiState.weeklyActivity,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Reading Time Breakdown
        item(key = "reading_time") {
            ReadingTimeSection(
                todayMinutes = uiState.todayMinutes,
                weekMinutes = uiState.weekMinutes,
                monthMinutes = uiState.monthMinutes,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }

        // Most Read Novels
        if (uiState.mostReadNovels.isNotEmpty()) {
            item(key = "most_read") {
                MostReadSection(
                    novels = uiState.mostReadNovels,
                    onNovelClick = onNovelClick,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }

        // Achievements
        if (uiState.achievements.isNotEmpty()) {
            item(key = "achievements") {
                AchievementsSection(
                    achievements = uiState.achievements,
                    modifier = Modifier.padding(horizontal = dimensions.gridPadding)
                )
            }
        }

        // All Time Stats
        item(key = "all_time") {
            AllTimeStatsSection(
                totalChapters = uiState.totalChaptersRead,
                totalDays = uiState.totalDaysRead,
                totalReadingTime = uiState.totalReadingTime,
                modifier = Modifier.padding(horizontal = dimensions.gridPadding)
            )
        }
    }
}

// ============================================================================
// Profile Hero Section
// ============================================================================

@Composable
private fun ProfileHeroSection(
    uiState: ProfileUiState,
    onShareStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val levelColor = ProfileColors.getLevelColor(uiState.readerLevel)

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }

    val greetingIcon = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> Icons.Rounded.WbSunny
            in 12..16 -> Icons.Rounded.WbSunny
            in 17..20 -> Icons.Rounded.WbTwilight
            else -> Icons.Rounded.Nightlight
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        levelColor.copy(alpha = 0.15f),
                        levelColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            // Top Row: Greeting + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = greetingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$greeting, Reader",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onShareStats,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share stats",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Card with Avatar and Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with Level Ring
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Level progress ring
                    Canvas(
                        modifier = Modifier.size(88.dp)
                    ) {
                        val strokeWidth = 4.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Background ring
                        drawCircle(
                            color = levelColor.copy(alpha = 0.2f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Progress arc
                        drawArc(
                            color = levelColor,
                            startAngle = -90f,
                            sweepAngle = 360f * uiState.levelProgress,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Avatar circle
                    Surface(
                        shape = CircleShape,
                        color = levelColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = levelColor
                            )
                        }
                    }

                    // Level badge
                    Surface(
                        shape = CircleShape,
                        color = levelColor,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "${uiState.readerLevel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Level Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.readerLevelName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )

                    Text(
                        text = "Level ${uiState.readerLevel} Reader",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // XP Progress bar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${uiState.totalHours}h read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.hoursToNextLevel > 0) {
                                Text(
                                    text = "${uiState.hoursToNextLevel}h to next",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(levelColor.copy(alpha = 0.2f))
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = uiState.levelProgress,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "level_progress"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                levelColor,
                                                levelColor.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Quick Stats Row
// ============================================================================

@Composable
private fun QuickStatsRow(
    currentStreak: Int,
    totalChapters: Int,
    totalHours: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatPill(
            icon = Icons.Rounded.LocalFireDepartment,
            value = "$currentStreak",
            label = "streak",
            color = ProfileColors.StreakOrange,
            modifier = Modifier.weight(1f)
        )
        QuickStatPill(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            value = "$totalChapters",
            label = "chapters",
            color = ProfileColors.ChapterBlue,
            modifier = Modifier.weight(1f)
        )
        QuickStatPill(
            icon = Icons.Rounded.Schedule,
            value = "${totalHours}h",
            label = "total",
            color = ProfileColors.TimeGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatPill(
    icon: ImageVector,
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
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ============================================================================
// Streak Card
// ============================================================================

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    isStreakActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isStreakActive) {
                ProfileColors.StreakOrange.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$currentStreak",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isStreakActive) ProfileColors.StreakOrange
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isStreakActive) "🔥 Keep it going!"
                        else "Start reading today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Best: $longestStreak days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (isStreakActive && currentStreak > 0) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = fireScale
                            scaleY = fireScale
                        },
                    tint = ProfileColors.StreakOrange
                )
            }
        }
    }
}

// ============================================================================
// Reading Insights Section
// ============================================================================

@Composable
private fun ReadingInsightsSection(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    val bestDay = remember(uiState.weeklyActivity) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val maxIndex = uiState.weeklyActivity.indexOf(uiState.weeklyActivity.maxOrNull() ?: 0L)
        if (maxIndex >= 0 && uiState.weeklyActivity[maxIndex] > 0) days[maxIndex] else null
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Insights",
            icon = Icons.Rounded.Insights
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightCard(
                title = "Avg Session",
                value = "${uiState.averageSessionMinutes}m",
                subtitle = "per day",
                color = ProfileColors.InsightCyan,
                modifier = Modifier.weight(1f)
            )

            InsightCard(
                title = "Reading Pace",
                value = String.format("%.1f", uiState.chaptersPerDay),
                subtitle = "ch/day",
                color = ProfileColors.GoalSecondary,
                modifier = Modifier.weight(1f)
            )

            InsightCard(
                title = "Best Day",
                value = bestDay ?: "—",
                subtitle = "this week",
                color = ProfileColors.DaysAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================================
// Goals Section
// ============================================================================

@Composable
private fun GoalsSection(
    todayMinutes: Int,
    weekMinutes: Int,
    dailyGoal: Int,
    weeklyGoal: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Goals",
            icon = Icons.Rounded.TrendingUp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularGoalCard(
                title = "Daily",
                current = todayMinutes,
                target = dailyGoal,
                color = ProfileColors.GoalPrimary,
                modifier = Modifier.weight(1f)
            )
            CircularGoalCard(
                title = "Weekly",
                current = weekMinutes,
                target = weeklyGoal,
                color = ProfileColors.GoalSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CircularGoalCard(
    title: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    val isCompleted = current >= target

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )
                } else {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$title Goal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$current / $target min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// Reading Time Section
// ============================================================================

@Composable
private fun ReadingTimeSection(
    todayMinutes: Long,
    weekMinutes: Long,
    monthMinutes: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Reading Time",
            icon = Icons.Rounded.Schedule
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeStatCard(
                label = "Today",
                minutes = todayMinutes,
                color = ProfileColors.TimeGreen,
                modifier = Modifier.weight(1f)
            )
            TimeStatCard(
                label = "This Week",
                minutes = weekMinutes,
                color = ProfileColors.ChapterBlue,
                modifier = Modifier.weight(1f)
            )
            TimeStatCard(
                label = "This Month",
                minutes = monthMinutes,
                color = ProfileColors.GoalSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeStatCard(
    label: String,
    minutes: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    val displayTime = remember(minutes) {
        when {
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
            else -> "${minutes / 60}h"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// Weekly Activity Section
// ============================================================================

@Composable
private fun WeeklyActivitySection(
    dailyMinutes: List<Long>,
    modifier: Modifier = Modifier
) {
    val dayLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val maxMinutes = remember(dailyMinutes) { dailyMinutes.maxOrNull()?.coerceAtLeast(1L) ?: 1L }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "This Week",
            icon = Icons.Rounded.CalendarMonth
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyMinutes.forEachIndexed { index, minutes ->
                    val heightFraction = if (maxMinutes > 0) (minutes.toFloat() / maxMinutes) else 0f
                    val animatedHeight by animateFloatAsState(
                        targetValue = heightFraction.coerceIn(0.08f, 1f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height_$index"
                    )

                    val isToday = index == dailyMinutes.lastIndex
                    val barColor = when {
                        isToday -> ProfileColors.ChapterBlue
                        minutes > 0 -> ProfileColors.ChapterBlue.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(60.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((60 * animatedHeight).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }

                        Text(
                            text = dayLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// All Time Stats Section
// ============================================================================

@Composable
private fun AllTimeStatsSection(
    totalChapters: Int,
    totalDays: Int,
    totalReadingTime: Long,
    modifier: Modifier = Modifier
) {
    val totalHours = remember(totalReadingTime) { totalReadingTime / 3600 }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "All Time",
            icon = Icons.Rounded.Star
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                value = totalChapters.toString(),
                label = "Chapters",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                color = ProfileColors.ChapterBlue,
                modifier = Modifier.weight(1f)
            )
            StatisticCard(
                value = totalDays.toString(),
                label = "Days",
                icon = Icons.Rounded.CalendarMonth,
                color = ProfileColors.DaysAmber,
                modifier = Modifier.weight(1f)
            )
            StatisticCard(
                value = "${totalHours}h",
                label = "Total",
                icon = Icons.Rounded.Schedule,
                color = ProfileColors.TimeGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatisticCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// Most Read Section
// ============================================================================

@Composable
private fun MostReadSection(
    novels: List<NovelReadingStats>,
    onNovelClick: (NovelReadingStats) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Most Read",
            icon = Icons.Rounded.AutoStories
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            novels.forEachIndexed { index, novel ->
                MostReadNovelCard(
                    rank = index + 1,
                    novel = novel,
                    onClick = { onNovelClick(novel) }
                )
            }
        }
    }
}

@Composable
private fun MostReadNovelCard(
    rank: Int,
    novel: NovelReadingStats,
    onClick: () -> Unit
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = rankColor.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            }

            if (novel.coverUrl != null) {
                AsyncImage(
                    model = novel.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = novel.novelName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeDisplay = remember(novel.readingTimeMinutes) {
                        when {
                            novel.readingTimeMinutes < 60 -> "${novel.readingTimeMinutes}m"
                            else -> "${novel.readingTimeMinutes / 60}h ${novel.readingTimeMinutes % 60}m"
                        }
                    }

                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfileColors.TimeGreen
                    )

                    if (novel.sourceName.isNotBlank()) {
                        Text(
                            text = "• ${novel.sourceName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Achievements Section
// ============================================================================

@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Achievements",
            icon = Icons.Rounded.EmojiEvents
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val icon = remember(achievement.iconName) { getAchievementIcon(achievement.iconName) }
    val animatedProgress by animateFloatAsState(
        targetValue = achievement.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "achievement_progress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                ProfileColors.AchievementGold.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (achievement.isUnlocked) {
                    ProfileColors.AchievementGold.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (achievement.isUnlocked) {
                            ProfileColors.AchievementGold
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }

            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (achievement.isUnlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (!achievement.isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(4.dp)
                            .background(ProfileColors.GoalPrimary)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Section Header
// ============================================================================

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ============================================================================
// Empty State
// ============================================================================

@Composable
private fun ProfileEmptyState(
    onNavigateToSettings: () -> Unit,
    statusBarPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val dimensions = NoveryTheme.dimensions

    Column(
        modifier = modifier
            .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Simplified hero for empty state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimensions.gridPadding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Start Your Journey",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Read your first chapter to unlock\nyour reader profile and stats",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Browse novels to get started",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

private fun getAchievementIcon(iconName: String): ImageVector {
    return when (iconName) {
        "book" -> Icons.Rounded.AutoStories
        "schedule" -> Icons.Rounded.Schedule
        "fire" -> Icons.Rounded.LocalFireDepartment
        "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
        "trending" -> Icons.Rounded.TrendingUp
        else -> Icons.Rounded.AutoStories
    }
}