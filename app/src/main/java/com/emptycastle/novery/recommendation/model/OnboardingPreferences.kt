package com.emptycastle.novery.recommendation.model

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory

/**
 * Represents user preferences collected during onboarding.
 * Used to configure initial recommendation seeding.
 */
data class OnboardingPreferences(
    /** Providers to seed from (empty = all enabled) */
    val selectedProviders: Set<String> = emptySet(),

    /** Genres the user likes */
    val likedGenres: Set<TagCategory> = emptySet(),

    /** Genres the user dislikes */
    val dislikedGenres: Set<TagCategory> = emptySet(),

    /** Whether to include mature/adult content */
    val includeMatureContent: Boolean = false,

    /** Whether to include BL (Boys Love) content */
    val includeBLContent: Boolean = true,

    /** Whether to include GL (Girls Love) content */
    val includeGLContent: Boolean = true,

    /** Preferred reading style (if any) */
    val preferredStyles: Set<TagCategory> = emptySet(),

    /** Keywords the user is interested in (free text) */
    val interestKeywords: List<String> = emptyList(),

    /** Whether onboarding was completed */
    val completed: Boolean = false,

    /** When onboarding was completed */
    val completedAt: Long = 0
) {
    /**
     * Get all blocked tags based on content preferences
     */
    fun getBlockedTags(): Set<TagCategory> {
        val blocked = dislikedGenres.toMutableSet()

        if (!includeMatureContent) {
            blocked.addAll(listOf(
                TagCategory.MATURE,
                TagCategory.ADULT,
                TagCategory.SMUT,
                TagCategory.ECCHI,
                TagCategory.GORE
            ))
        }

        if (!includeBLContent) {
            blocked.add(TagCategory.BL)
        }

        if (!includeGLContent) {
            blocked.add(TagCategory.GL)
        }

        return blocked
    }

    /**
     * Get boosted tags based on preferences
     */
    fun getBoostedTags(): Set<TagCategory> {
        return likedGenres + preferredStyles
    }

    companion object {
        val EMPTY = OnboardingPreferences()
    }
}

/**
 * Genre category for onboarding display
 */
data class GenreOption(
    val category: TagCategory,
    val displayName: String,
    val description: String,
    val icon: String? = null // Emoji or icon name
)

/**
 * Pre-defined genre options for onboarding
 */
object OnboardingGenres {

    val mainGenres = listOf(
        GenreOption(TagCategory.FANTASY, "Fantasy", "Magic, mythical creatures, other worlds", "🧙"),
        GenreOption(TagCategory.SCI_FI, "Sci-Fi", "Technology, space, future", "🚀"),
        GenreOption(TagCategory.ROMANCE, "Romance", "Love stories, relationships", "💕"),
        GenreOption(TagCategory.ACTION, "Action", "Fighting, battles, excitement", "⚔️"),
        GenreOption(TagCategory.ADVENTURE, "Adventure", "Journeys, exploration, quests", "🗺️"),
        GenreOption(TagCategory.MYSTERY, "Mystery", "Detective, crime, suspense", "🔍"),
        GenreOption(TagCategory.HORROR, "Horror", "Scary, supernatural threats", "👻"),
        GenreOption(TagCategory.COMEDY, "Comedy", "Funny, lighthearted", "😂"),
        GenreOption(TagCategory.DRAMA, "Drama", "Emotional, serious themes", "🎭"),
        GenreOption(TagCategory.SLICE_OF_LIFE, "Slice of Life", "Daily life, realistic", "☕")
    )

    val subGenres = listOf(
        GenreOption(TagCategory.CULTIVATION, "Cultivation", "Eastern martial arts & power growth", "🏔️"),
        GenreOption(TagCategory.LITRPG, "LitRPG", "Game-like systems, stats, levels", "🎮"),
        GenreOption(TagCategory.ISEKAI, "Isekai", "Transported to another world", "🌀"),
        GenreOption(TagCategory.REINCARNATION, "Reincarnation", "Rebirth with past memories", "♻️"),
        GenreOption(TagCategory.PROGRESSION, "Progression", "Power growth over time", "📈"),
        GenreOption(TagCategory.DUNGEON, "Dungeon", "Dungeon diving, exploration", "🏰"),
        GenreOption(TagCategory.HAREM, "Harem", "Multiple love interests", "👥"),
        GenreOption(TagCategory.KINGDOM_BUILDING, "Kingdom Building", "Building empires, ruling", "👑"),
        GenreOption(TagCategory.XIANXIA, "Xianxia", "Chinese immortal cultivation", "☯️"),
        GenreOption(TagCategory.WUXIA, "Wuxia", "Chinese martial arts", "🥋")
    )

    val tones = listOf(
        GenreOption(TagCategory.DARK, "Dark", "Serious, gritty themes", "🌑"),
        GenreOption(TagCategory.LIGHTHEARTED, "Lighthearted", "Fun, casual reading", "☀️"),
        GenreOption(TagCategory.WHOLESOME, "Wholesome", "Heartwarming, feel-good", "💖"),
        GenreOption(TagCategory.GRIMDARK, "Grimdark", "Very dark, brutal", "⚫")
    )

    val protagonistTypes = listOf(
        GenreOption(TagCategory.OP_MC, "OP MC", "Overpowered main character", "💪"),
        GenreOption(TagCategory.WEAK_TO_STRONG, "Weak to Strong", "Growth from nothing", "📊"),
        GenreOption(TagCategory.VILLAIN_PROTAGONIST, "Villain MC", "Evil or morally grey lead", "😈"),
        GenreOption(TagCategory.FEMALE_LEAD, "Female Lead", "Female protagonist", "👩"),
        GenreOption(TagCategory.ANTI_HERO, "Anti-Hero", "Morally ambiguous hero", "🦹")
    )
}