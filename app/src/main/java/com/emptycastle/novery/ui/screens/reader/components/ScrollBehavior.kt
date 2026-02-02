package com.emptycastle.novery.ui.screens.reader.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.sign

/**
 * Configuration for scroll behavior based on user preferences.
 */
data class ScrollConfiguration(
    /** Sensitivity multiplier (0.5 = slow, 1.0 = normal, 2.0 = fast) */
    val sensitivity: Float = 1.0f,

    /** Whether smooth scrolling is enabled */
    val smoothScroll: Boolean = true,

    /** Whether to reduce motion/animations */
    val reduceMotion: Boolean = false,

    /** Friction multiplier for fling deceleration (higher = stops faster) */
    val friction: Float = 1.0f
)

/**
 * Creates a custom FlingBehavior that respects the scroll sensitivity setting.
 *
 * @param sensitivity Multiplier for scroll speed (0.5-2.0)
 * @param smoothScroll Whether to use smooth animations
 * @param reduceMotion Whether to reduce motion effects
 */
@Composable
fun rememberCustomFlingBehavior(
    sensitivity: Float = 1.0f,
    smoothScroll: Boolean = true,
    reduceMotion: Boolean = false
): FlingBehavior {
    val density = LocalDensity.current

    // Use default spline-based decay as base
    val baseDecaySpec = rememberSplineBasedDecay<Float>()

    return remember(sensitivity, smoothScroll, reduceMotion, density) {
        if (reduceMotion) {
            // Minimal fling for reduced motion - stops almost immediately
            ReducedMotionFlingBehavior()
        } else {
            CustomSensitivityFlingBehavior(
                sensitivity = sensitivity,
                baseDecaySpec = baseDecaySpec,
                smoothScroll = smoothScroll
            )
        }
    }
}

/**
 * A FlingBehavior that adjusts scroll velocity based on sensitivity setting.
 */
private class CustomSensitivityFlingBehavior(
    private val sensitivity: Float,
    private val baseDecaySpec: DecayAnimationSpec<Float>,
    private val smoothScroll: Boolean
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // Adjust initial velocity based on sensitivity
        // Lower sensitivity = slower scrolling, higher = faster
        val adjustedVelocity = initialVelocity * sensitivity

        // If velocity is too low, don't fling
        if (abs(adjustedVelocity) < 50f) {
            return adjustedVelocity
        }

        // Calculate friction based on sensitivity
        // Higher sensitivity = less friction = scrolls further
        // Lower sensitivity = more friction = stops sooner
        val frictionMultiplier = when {
            sensitivity < 0.7f -> 2.5f  // High friction for slow scroll
            sensitivity < 0.9f -> 1.8f
            sensitivity < 1.1f -> 1.0f  // Normal friction
            sensitivity < 1.5f -> 0.7f
            else -> 0.5f  // Low friction for fast scroll
        }

        var velocityLeft = adjustedVelocity
        var lastValue = 0f

        // Custom decay animation
        val animationSpec = exponentialDecay<Float>(
            frictionMultiplier = frictionMultiplier,
            absVelocityThreshold = 0.5f
        )

        androidx.compose.animation.core.AnimationState(
            initialValue = 0f,
            initialVelocity = adjustedVelocity
        ).animateDecay(animationSpec) {
            val delta = value - lastValue
            lastValue = value

            // Scroll by the delta amount
            val consumed = scrollBy(delta)

            // If we hit the edge (consumed less than delta), stop
            if (abs(delta - consumed) > 0.5f) {
                cancelAnimation()
            }

            velocityLeft = velocity
        }

        return velocityLeft
    }
}

/**
 * Minimal fling behavior for users who prefer reduced motion.
 * Scrolls a small fixed amount and stops.
 */
private class ReducedMotionFlingBehavior : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // For reduced motion, just do a small immediate scroll
        if (abs(initialVelocity) > 100f) {
            val scrollAmount = sign(initialVelocity) * 200f // Fixed small scroll
            scrollBy(scrollAmount)
        }
        return 0f // No remaining velocity
    }
}

/**
 * Creates scroll configuration from reader settings.
 */
fun createScrollConfiguration(
    sensitivity: Float,
    smoothScroll: Boolean,
    reduceMotion: Boolean
): ScrollConfiguration {
    return ScrollConfiguration(
        sensitivity = sensitivity.coerceIn(0.5f, 2.0f),
        smoothScroll = smoothScroll,
        reduceMotion = reduceMotion,
        friction = calculateFriction(sensitivity)
    )
}

private fun calculateFriction(sensitivity: Float): Float {
    // Inverse relationship: lower sensitivity = higher friction
    return when {
        sensitivity <= 0.5f -> 2.0f
        sensitivity <= 0.75f -> 1.5f
        sensitivity <= 1.0f -> 1.0f
        sensitivity <= 1.5f -> 0.75f
        else -> 0.5f
    }
}

/**
 * Extension to create appropriate animation spec based on settings.
 */
@Composable
fun rememberScrollAnimationSpec(
    smoothScroll: Boolean,
    reduceMotion: Boolean
): AnimationSpec<Float> {
    return remember(smoothScroll, reduceMotion) {
        when {
            reduceMotion -> spring(
                stiffness = Spring.StiffnessHigh,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
            smoothScroll -> spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            )
            else -> spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        }
    }
}