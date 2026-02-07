package com.emptycastle.novery.ui.screens.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emptycastle.novery.data.local.entity.TagFilterType
import com.emptycastle.novery.data.repository.RepositoryProvider
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.recommendation.InteractionType  // ADD THIS IMPORT
import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory
import com.emptycastle.novery.recommendation.model.OnboardingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "OnboardingViewModel"

class OnboardingViewModel : ViewModel() {

    private val preferencesManager = RepositoryProvider.getPreferencesManager()
    private val discoveryManager = RepositoryProvider.getDiscoveryManager()
    private val tagEnhancementManager = RepositoryProvider.getTagEnhancementManager()
    private val userFilterManager = RepositoryProvider.getUserFilterManager()
    private val userPreferenceManager = RepositoryProvider.getUserPreferenceManager()

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        loadProviders()
    }

    // ================================================================
    // PROVIDER LOADING
    // ================================================================

    private fun loadProviders() {
        val providers = MainProvider.getProviders().map { provider ->
            ProviderInfo(
                name = provider.name,
                description = getProviderDescription(provider.name),
                novelCount = getProviderNovelCount(provider.name),
                genres = getProviderGenres(provider.name),
                isEnabled = true
            )
        }

        _state.update { state ->
            state.copy(
                availableProviders = providers,
                selectedProviders = providers.map { it.name }.toSet() // All selected by default
            )
        }
    }

    private fun getProviderDescription(name: String): String {
        return when (name) {
            "Royal Road" -> "Western web fiction with strong LitRPG & progression fantasy"
            "NovelFire" -> "Large collection of translated & original novels"
            "NovelBin" -> "Diverse selection of light novels & web novels"
            "Webnovel" -> "Premium platform with exclusive titles"
            "LibRead" -> "Free light novel & web novel library"
            "NovelsOnline" -> "Extensive catalog of translated novels"
            else -> "Novel provider"
        }
    }

    private fun getProviderNovelCount(name: String): String {
        return when (name) {
            "Royal Road" -> "15,000+ novels"
            "NovelFire" -> "50,000+ novels"
            "NovelBin" -> "30,000+ novels"
            "Webnovel" -> "100,000+ novels"
            "LibRead" -> "40,000+ novels"
            "NovelsOnline" -> "20,000+ novels"
            else -> "Many novels"
        }
    }

    private fun getProviderGenres(name: String): List<String> {
        return when (name) {
            "Royal Road" -> listOf("LitRPG", "Progression", "Fantasy")
            "NovelFire" -> listOf("Cultivation", "Romance", "Fantasy")
            "NovelBin" -> listOf("Light Novel", "Isekai", "Fantasy")
            "Webnovel" -> listOf("Romance", "Fantasy", "Urban")
            "LibRead" -> listOf("Fantasy", "Romance", "Isekai")
            "NovelsOnline" -> listOf("Cultivation", "Martial Arts", "Fantasy")
            else -> listOf("Various")
        }
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    fun nextStep() {
        val current = _state.value.currentStep
        val next = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.PROVIDERS
            OnboardingStep.PROVIDERS -> OnboardingStep.GENRES
            OnboardingStep.GENRES -> OnboardingStep.CONTENT
            OnboardingStep.CONTENT -> OnboardingStep.READY
            OnboardingStep.READY -> {
                startSeeding()
                OnboardingStep.SEEDING
            }
            OnboardingStep.SEEDING -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }

        _state.update { it.copy(currentStep = next) }
    }

    fun previousStep() {
        val current = _state.value.currentStep
        val previous = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.PROVIDERS -> OnboardingStep.WELCOME
            OnboardingStep.GENRES -> OnboardingStep.PROVIDERS
            OnboardingStep.CONTENT -> OnboardingStep.GENRES
            OnboardingStep.READY -> OnboardingStep.CONTENT
            OnboardingStep.SEEDING -> OnboardingStep.READY // Can't go back during seeding
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }

        _state.update { it.copy(currentStep = previous) }
    }

    fun skipOnboarding() {
        // Use all providers with default settings
        _state.update { it.copy(
            selectedProviders = it.availableProviders.map { p -> p.name }.toSet()
        )}
        startSeeding()
        _state.update { it.copy(currentStep = OnboardingStep.SEEDING) }
    }

    // ================================================================
    // PROVIDER SELECTION
    // ================================================================

    fun toggleProvider(providerName: String) {
        _state.update { state ->
            val current = state.selectedProviders.toMutableSet()
            if (providerName in current) {
                current.remove(providerName)
            } else {
                current.add(providerName)
            }
            state.copy(selectedProviders = current)
        }
    }

    fun selectAllProviders() {
        _state.update { state ->
            state.copy(selectedProviders = state.availableProviders.map { it.name }.toSet())
        }
    }

    fun deselectAllProviders() {
        _state.update { it.copy(selectedProviders = emptySet()) }
    }

    // ================================================================
    // GENRE SELECTION
    // ================================================================

    fun toggleLikedGenre(genre: TagCategory) {
        _state.update { state ->
            val liked = state.likedGenres.toMutableSet()
            val disliked = state.dislikedGenres.toMutableSet()

            when {
                genre in liked -> {
                    liked.remove(genre)
                }
                genre in disliked -> {
                    disliked.remove(genre)
                    liked.add(genre)
                }
                else -> {
                    liked.add(genre)
                }
            }

            state.copy(likedGenres = liked, dislikedGenres = disliked)
        }
    }

    fun toggleDislikedGenre(genre: TagCategory) {
        _state.update { state ->
            val liked = state.likedGenres.toMutableSet()
            val disliked = state.dislikedGenres.toMutableSet()

            when {
                genre in disliked -> {
                    disliked.remove(genre)
                }
                genre in liked -> {
                    liked.remove(genre)
                    disliked.add(genre)
                }
                else -> {
                    disliked.add(genre)
                }
            }

            state.copy(likedGenres = liked, dislikedGenres = disliked)
        }
    }

    fun setGenrePreference(genre: TagCategory, preference: GenrePreference) {
        _state.update { state ->
            val liked = state.likedGenres.toMutableSet()
            val disliked = state.dislikedGenres.toMutableSet()

            // Remove from both first
            liked.remove(genre)
            disliked.remove(genre)

            // Add to appropriate set
            when (preference) {
                GenrePreference.LIKED -> liked.add(genre)
                GenrePreference.DISLIKED -> disliked.add(genre)
                GenrePreference.NEUTRAL -> { /* Already removed */ }
            }

            state.copy(likedGenres = liked, dislikedGenres = disliked)
        }
    }

    fun getGenrePreference(genre: TagCategory): GenrePreference {
        val state = _state.value
        return when {
            genre in state.likedGenres -> GenrePreference.LIKED
            genre in state.dislikedGenres -> GenrePreference.DISLIKED
            else -> GenrePreference.NEUTRAL
        }
    }

    // ================================================================
    // CONTENT SETTINGS
    // ================================================================

    fun setMatureContent(include: Boolean) {
        _state.update { it.copy(includeMatureContent = include) }
    }

    fun setBLContent(include: Boolean) {
        _state.update { it.copy(includeBLContent = include) }
    }

    fun setGLContent(include: Boolean) {
        _state.update { it.copy(includeGLContent = include) }
    }

    // ================================================================
    // SEEDING
    // ================================================================

    private fun startSeeding() {
        viewModelScope.launch {
            _state.update { it.copy(isSeeding = true) }

            try {
                // 1. Save preferences first
                savePreferences()

                // 2. Apply content filters
                applyContentFilters()

                // 3. Apply initial genre preferences to user profile
                applyInitialGenrePreferences()

                // 4. Start seeding with selected providers
                val selectedProviders = _state.value.selectedProviders
                val allProviders = _state.value.availableProviders.map { it.name }.toSet()
                val disabledProviders = allProviders - selectedProviders

                // Update app settings with disabled providers
                val currentSettings = preferencesManager.appSettings.value
                preferencesManager.updateAppSettings(
                    currentSettings.copy(disabledProviders = disabledProviders)
                )

                // Run discovery
                val result = discoveryManager.seedDiscoveryPool(
                    disabledProviders = disabledProviders
                ) { provider, current, total ->
                    _state.update { state ->
                        state.copy(
                            seedingProgress = SeedingProgressInfo(
                                currentProvider = provider,
                                currentIndex = current,
                                totalProviders = total,
                                novelsDiscovered = 0
                            )
                        )
                    }
                }

                Log.d(TAG, "Seeding complete: ${result.totalDiscovered} novels discovered")

                // 5. Enhance tags
                val enhancementResult = tagEnhancementManager.enhanceNovelsWithSynopsis()
                Log.d(TAG, "Tag enhancement: ${enhancementResult.novelsEnhanced} novels enhanced")

                // 6. Mark onboarding complete
                preferencesManager.setOnboardingComplete()
                preferencesManager.setFirstRunComplete()

                _state.update { it.copy(
                    isSeeding = false,
                    currentStep = OnboardingStep.COMPLETE
                )}

            } catch (e: Exception) {
                Log.e(TAG, "Error during seeding", e)
                _state.update { it.copy(
                    isSeeding = false,
                    error = "Failed to initialize: ${e.message}"
                )}
            }
        }
    }

    private suspend fun savePreferences() {
        val prefs = _state.value.toOnboardingPreferences()

        // Save to SharedPreferences for persistence
        val json = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer<OnboardingPreferencesData>(),
            OnboardingPreferencesData.fromPreferences(prefs)
        )
        // Could save this JSON to PreferencesManager if needed
    }

    private suspend fun applyContentFilters() {
        val state = _state.value

        // Block mature content if not wanted
        if (!state.includeMatureContent) {
            listOf(
                TagCategory.MATURE,
                TagCategory.ADULT,
                TagCategory.SMUT,
                TagCategory.GORE
            ).forEach { tag ->
                userFilterManager.setTagFilter(tag, TagFilterType.BLOCKED)
            }
        }

        // Block BL if not wanted
        if (!state.includeBLContent) {
            userFilterManager.setTagFilter(TagCategory.BL, TagFilterType.BLOCKED)
        }

        // Block GL if not wanted
        if (!state.includeGLContent) {
            userFilterManager.setTagFilter(TagCategory.GL, TagFilterType.BLOCKED)
        }

        // REDUCED for explicitly disliked genres (show less, don't block completely)
        state.dislikedGenres.forEach { tag ->
            userFilterManager.setTagFilter(tag, TagFilterType.REDUCED)
        }

        // Boost liked genres
        state.likedGenres.forEach { tag ->
            userFilterManager.setTagFilter(tag, TagFilterType.BOOSTED)
        }
    }

    private suspend fun applyInitialGenrePreferences() {
        val state = _state.value

        // Add liked genres to user preference with high initial score
        state.likedGenres.forEach { tag ->
            userPreferenceManager.recordTagInteraction(
                tag = tag,
                interactionType = InteractionType.EXPLICIT_LIKE,  // Now properly imported
                weight = 1.5f
            )
        }

        // Add disliked genres with negative score
        state.dislikedGenres.forEach { tag ->
            userPreferenceManager.recordTagInteraction(
                tag = tag,
                interactionType = InteractionType.EXPLICIT_DISLIKE,  // Now properly imported
                weight = 1.5f
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

enum class GenrePreference {
    LIKED,
    NEUTRAL,
    DISLIKED
}

/**
 * Serializable version of OnboardingPreferences for storage
 */
@kotlinx.serialization.Serializable
data class OnboardingPreferencesData(
    val selectedProviders: List<String>,
    val likedGenres: List<String>,
    val dislikedGenres: List<String>,
    val includeMatureContent: Boolean,
    val includeBLContent: Boolean,
    val includeGLContent: Boolean,
    val completedAt: Long
) {
    companion object {
        fun fromPreferences(prefs: OnboardingPreferences): OnboardingPreferencesData {
            return OnboardingPreferencesData(
                selectedProviders = prefs.selectedProviders.toList(),
                likedGenres = prefs.likedGenres.map { it.name },
                dislikedGenres = prefs.dislikedGenres.map { it.name },
                includeMatureContent = prefs.includeMatureContent,
                includeBLContent = prefs.includeBLContent,
                includeGLContent = prefs.includeGLContent,
                completedAt = prefs.completedAt
            )
        }
    }
}