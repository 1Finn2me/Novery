package com.emptycastle.novery.recommendation.model

/**
 * Different types of recommendations with their characteristics
 */
enum class RecommendationType {
    /** Based on user's overall taste profile */
    FOR_YOU,

    /** Similar to a specific novel */
    SIMILAR_TO,

    /** Because user read/liked a specific novel */
    BECAUSE_YOU_READ,

    /** Popular in genres the user likes */
    TRENDING_IN_YOUR_GENRES,

    /** Same author as novels user enjoyed */
    FROM_AUTHORS_YOU_LIKE,

    /** Highly rated in tags user prefers */
    TOP_RATED_FOR_YOU,

    /** New releases matching user preferences */
    NEW_FOR_YOU,

    /** Cross-provider discovery */
    DISCOVER_NEW_SOURCE
}