package com.emptycastle.novery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emptycastle.novery.R
import com.emptycastle.novery.ui.theme.Error
import com.emptycastle.novery.ui.theme.Orange400
import com.emptycastle.novery.ui.theme.Orange500
import com.emptycastle.novery.ui.theme.Orange600
import com.emptycastle.novery.ui.theme.Orange900
import com.emptycastle.novery.ui.theme.Zinc400
import com.emptycastle.novery.ui.theme.Zinc500
import com.emptycastle.novery.ui.theme.Zinc700
import com.emptycastle.novery.ui.theme.Zinc800
import com.emptycastle.novery.ui.theme.Zinc900
import com.emptycastle.novery.ui.theme.Zinc950

// ============================================================================
// Splash Screen
// ============================================================================

/**
 * Splash/loading screen shown on app startup
 */
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Logo breathing animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Ring rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Background orb floating
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_offset"
    )

    // Dots animation
    val dotsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dots_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0C0E),
                        Zinc950,
                        Color(0xFF08080A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating background orbs
        Box(
            modifier = Modifier
                .offset(x = (-100).dp + orbOffset.dp, y = (-140).dp - (orbOffset / 2).dp)
                .size(320.dp)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Orange900.copy(alpha = 0.25f),
                            Orange600.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .offset(x = 80.dp - (orbOffset / 2).dp, y = 160.dp + orbOffset.dp)
                .size(250.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Orange500.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with animations
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(logoScale)
            ) {
                // Outer rotating ring
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(ringRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Orange500.copy(alpha = glowAlpha),
                                Color.Transparent,
                                Color.Transparent,
                                Orange400.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Glow behind logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .blur(25.dp)
                        .background(
                            Orange500.copy(alpha = glowAlpha * 0.6f),
                            CircleShape
                        )
                )

                // Logo container
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent,
                    shadowElevation = 20.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Orange400,
                                        Orange500,
                                        Orange600
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // App Logo - Replace with your actual logo resource
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Novery Logo",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Text(
                text = "Novery",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Ultimate Novel Reader",
                style = MaterialTheme.typography.bodyMedium,
                color = Zinc400,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator - Animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotDelay = index * 200
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = dotDelay, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = dotDelay, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_scale_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale)
                            .alpha(dotAlpha)
                            .background(Orange500, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LOADING",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold,
                color = Orange500.copy(alpha = 0.8f)
            )
        }

        // Version at bottom
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = Zinc700,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// ============================================================================
// Loading Overlay
// ============================================================================

/**
 * Loading overlay with progress
 */
@Composable
fun LoadingOverlay(
    message: String,
    progress: Float? = null,
    currentItem: String? = null,
    total: Int? = null,
    current: Int? = null,
    onCancel: (() -> Unit)? = null
) {
    val actualProgress = progress ?: (current?.toFloat()?.div(total ?: 1) ?: 0f)

    val animatedProgress by animateFloatAsState(
        targetValue = actualProgress,
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Zinc900),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated spinner with glow
                Box(contentAlignment = Alignment.Center) {
                    // Glow
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .blur(16.dp)
                            .background(Orange500.copy(alpha = 0.3f), CircleShape)
                    )

                    // Spinner
                    Canvas(
                        modifier = Modifier
                            .size(56.dp)
                            .rotate(spinnerRotation)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Orange500,
                                    Orange400,
                                    Orange500.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Center percentage (if progress available)
                    if (progress != null || current != null) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Orange500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(
                    visible = currentItem != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = currentItem ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Zinc400,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (progress != null || (current != null && total != null)) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress bar with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Zinc800)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Orange600, Orange500, Orange400)
                                    )
                                )
                        )
                    }

                    if (current != null && total != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$current / $total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Zinc500
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Orange500
                            )
                        }
                    }
                }

                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Loading Indicator
// ============================================================================

/**
 * Simple centered loading indicator
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    size: Dp = 44.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(size + 12.dp)
                    .blur(14.dp)
                    .background(Orange500.copy(alpha = 0.25f), CircleShape)
            )

            // Spinner
            Canvas(
                modifier = Modifier
                    .size(size)
                    .rotate(rotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Orange500,
                            Orange400,
                            Orange500.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Zinc400,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// Full Screen Loading
// ============================================================================

/**
 * Full screen loading state
 */
@Composable
fun FullScreenLoading(
    message: String = "Loading..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0C0E),
                        Zinc950,
                        Color(0xFF08080A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(message = message)
    }
}

// ============================================================================
// Pulsing Dots Loader (Alternative style)
// ============================================================================

/**
 * Animated pulsing dots loader
 */
@Composable
fun PulsingDotsLoader(
    modifier: Modifier = Modifier,
    color: Color = Orange500,
    dotSize: Dp = 12.dp,
    spacing: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = delay, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha_$index"
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .alpha(alpha)
                    .background(color, CircleShape)
            )
        }
    }
}

// ============================================================================
// Skeleton Loader (for content placeholders)
// ============================================================================

/**
 * Shimmer skeleton box for loading placeholders
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Zinc800.copy(alpha = 0.6f),
                        Zinc700.copy(alpha = 0.3f),
                        Zinc800.copy(alpha = 0.6f)
                    ),
                    start = Offset(shimmerOffset * 300f, 0f),
                    end = Offset((shimmerOffset + 1) * 300f, 0f)
                )
            )
    )
}