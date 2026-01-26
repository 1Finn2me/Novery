package com.emptycastle.novery.domain.model

/**
 * Represents a novel in search results or catalog listings.
 */
data class Novel(
    val name: String,
    val url: String,
    val posterUrl: String? = null,
    val rating: Int? = null,        // 0-1000 scale
    val latestChapter: String? = null,
    val apiName: String = ""
)