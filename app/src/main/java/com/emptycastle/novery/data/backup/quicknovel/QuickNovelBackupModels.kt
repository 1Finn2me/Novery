package com.emptycastle.novery.data.backup.quicknovel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuickNovelBackup(
    val datastore: QuickNovelDatastore,
    val settings: QuickNovelSettings? = null
)

@Serializable
data class QuickNovelDatastore(
    val _Bool: Map<String, Boolean> = emptyMap(),
    val _Int: Map<String, Int> = emptyMap(),
    val _String: Map<String, String> = emptyMap(),
    val _Float: Map<String, Float> = emptyMap(),
    val _Long: Map<String, Long> = emptyMap(),
    val _StringSet: Map<String, List<String>> = emptyMap()
)

@Serializable
data class QuickNovelSettings(
    val _Bool: Map<String, Boolean> = emptyMap(),
    val _Int: Map<String, Int> = emptyMap(),
    val _String: Map<String, String> = emptyMap(),
    val _Float: Map<String, Float> = emptyMap(),
    val _Long: Map<String, Long> = emptyMap(),
    val _StringSet: Map<String, List<String>> = emptyMap()
)

@Serializable
data class QuickNovelResult(
    val source: String,
    val name: String,
    val apiName: String,
    val id: Long,
    val author: String? = null,
    val poster: String? = null,
    val tags: List<String> = emptyList(),
    val rating: Int? = null,
    val totalChapters: Int? = null,
    val cachedTime: Long? = null,
    val synopsis: String? = null,
    val image: QuickNovelImage? = null
)

@Serializable
data class QuickNovelImage(
    val url: String? = null,
    val headers: JsonElement? = null,
    val errorDrawable: JsonElement? = null
)

@Serializable
data class QuickNovelDownloadData(
    val source: String,
    val name: String,
    val author: String? = null,
    val posterUrl: String? = null,
    val rating: Int? = null,
    val peopleVoted: Int? = null,
    val views: Int? = null,
    val synopsis: String? = null,
    val tags: List<String> = emptyList(),
    val apiName: String,
    val lastUpdated: Long? = null,
    val lastDownloaded: Long? = null
)

@Serializable
data class QuickNovelMlSettings(
    val from: String? = null,
    val to: String? = null,
    val fromDisplay: String? = null,
    val toDisplay: String? = null,
    val invalid: Boolean? = null,
    val valid: Boolean? = null
)