package com.emptycastle.novery.domain.model

/**
 * Detailed information about a novel including chapter list.
 */
data class NovelDetails(
    val url: String,
    val name: String,
    val chapters: List<Chapter>,
    val author: String? = null,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val tags: List<String>? = null,
    val rating: Int? = null,         // 0-1000 scale
    val peopleVoted: Int? = null,
    val status: String? = null,
    val views: Int? = null,
    val relatedNovels: List<Novel>? = null
)