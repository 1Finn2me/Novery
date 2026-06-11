package com.emptycastle.novery.util.reader

import android.content.Context
import android.util.Log
import com.emptycastle.novery.translation.TranslationManager
import java.io.File
import java.security.MessageDigest

object QNTranslationHelper {
    private const val TAG = "QNTranslationHelper"

    /**
     * Generates an MD5 hash of the given text to use as a cache key.
     */
    fun hashString(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Advanced translation with disk caching.
     * 1. Checks if a translated version exists in cache.
     * 2. If not, translates and saves to disk.
     */
    suspend fun translateWithCache(
        context: Context,
        paragraphs: List<String>,
        sourceLang: String,
        targetLang: String,
        useOnline: Boolean,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): List<String> {
        if (paragraphs.isEmpty()) return emptyList()

        // Create a unique hash for this set of paragraphs
        val combinedText = paragraphs.joinToString("")
        val textHash = hashString(combinedText)
        
        val mode = if (useOnline) "online" else "offline"
        val fileName = "qn_ml_${textHash}_${sourceLang}_to_${targetLang}_$mode.txt"
        val cacheFile = File(context.cacheDir, fileName)

        // 1. Try to read from cache
        if (cacheFile.exists()) {
            try {
                Log.d(TAG, "Cache hit for translation: $fileName")
                return cacheFile.readLines()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading translation cache", e)
            }
        }

        // 2. Translate using existing TranslationManager
        Log.d(TAG, "Cache miss. Translating ${paragraphs.size} paragraphs...")
        
        var effectiveSource = sourceLang
        if (effectiveSource == "auto" || effectiveSource.isBlank()) {
            val sample = paragraphs.take(3).joinToString("\n")
            effectiveSource = TranslationManager.identifyLanguage(sample) ?: "en"
            Log.d(TAG, "Auto-detected source language: $effectiveSource")
        }

        // Ensure model is ready if offline
        if (!useOnline) {
            val ready = TranslationManager.prepareModel(effectiveSource, targetLang) { msg ->
                Log.d(TAG, "ML Model Prepare: $msg")
            }
            if (ready.isFailure) {
                Log.e(TAG, "Failed to prepare ML model for translation")
                return paragraphs
            }
        }

        val translated = if (useOnline) {
            TranslationManager.onlineTranslate(paragraphs, effectiveSource, targetLang, onProgress)
        } else {
            TranslationManager.translate(paragraphs, effectiveSource, targetLang, useOnline, onProgress)
        }

        // 3. Save to cache asynchronously (best effort)
        try {
            val tempFile = File(context.cacheDir, "$fileName.tmp")
            tempFile.writeText(translated.joinToString("\n"))
            tempFile.renameTo(cacheFile)
            Log.d(TAG, "Saved translation to cache: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving translation to cache", e)
        }

        return translated
    }

    /**
     * Clears old translation cache files.
     */
    fun clearCache(context: Context) {
        val files = context.cacheDir.listFiles { _, name -> name.startsWith("qn_ml_") }
        files?.forEach { it.delete() }
    }
}
