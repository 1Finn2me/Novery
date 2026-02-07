package com.emptycastle.novery.ui.screens.onboarding

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.OnboardingPreferences

/**
 * UI state for the onboarding flow
 */
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val preferences: OnboardingPreferences = OnboardingPreferences.EMPTY,

    // Provider selection
    val availableProviders: List<ProviderInfo> = emptyList(),
    val selectedProviders: Set<String> = emptySet(),

    // Genre selection
    val likedGenres: Set<TagCategory> = emptySet(),
    val dislikedGenres: Set<TagCategory> = emptySet(),

    // Content settings
    val includeMatureContent: Boolean = false,
    val includeBLContent: Boolean = true,
    val includeGLContent: Boolean = true,

    // Seeding state
    val isSeeding: Boolean = false,
    val seedingProgress: SeedingProgressInfo? = null,

    // Error handling
    val error: String? = null
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.PROVIDERS -> selectedProviders.isNotEmpty()
            OnboardingStep.GENRES -> true // Optional
            OnboardingStep.CONTENT -> true
            OnboardingStep.READY -> true
            OnboardingStep.SEEDING -> false
            OnboardingStep.COMPLETE -> true
        }

    val progress: Float
        get() = (currentStep.ordinal.toFloat() / (OnboardingStep.entries.size - 1))

    fun toOnboardingPreferences(): OnboardingPreferences {
        return OnboardingPreferences(
            selectedProviders = selectedProviders,
            likedGenres = likedGenres,
            dislikedGenres = dislikedGenres,
            includeMatureContent = includeMatureContent,
            includeBLContent = includeBLContent,
            includeGLContent = includeGLContent,
            completed = true,
            completedAt = System.currentTimeMillis()
        )
    }
}

enum class OnboardingStep {
    WELCOME,
    PROVIDERS,
    GENRES,
    CONTENT,
    READY,
    SEEDING,
    COMPLETE
}

data class ProviderInfo(
    val name: String,
    val description: String,
    val novelCount: String, // e.g., "10,000+ novels"
    val genres: List<String>, // Main genres available
    val isEnabled: Boolean = true
)

data class SeedingProgressInfo(
    val currentProvider: String,
    val currentIndex: Int,
    val totalProviders: Int,
    val novelsDiscovered: Int
)