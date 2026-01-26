// com/emptycastle/novery/data/local/Converters.kt
package com.emptycastle.novery.data.local

import androidx.room.TypeConverter
import com.emptycastle.novery.data.local.entity.ChapterEntity
import com.emptycastle.novery.data.local.entity.RelatedNovelEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // String List converters
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    // Chapter List converters
    @TypeConverter
    fun fromChapterList(value: List<ChapterEntity>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toChapterList(value: String?): List<ChapterEntity>? {
        if (value == null) return null
        val type = object : TypeToken<List<ChapterEntity>>() {}.type
        return gson.fromJson(value, type)
    }

    // Related Novel List converters
    @TypeConverter
    fun fromRelatedNovelList(value: List<RelatedNovelEntity>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRelatedNovelList(value: String?): List<RelatedNovelEntity>? {
        if (value == null) return null
        val type = object : TypeToken<List<RelatedNovelEntity>>() {}.type
        return gson.fromJson(value, type)
    }
}