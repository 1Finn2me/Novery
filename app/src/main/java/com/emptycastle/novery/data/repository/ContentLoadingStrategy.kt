package com.emptycastle.novery.data.repository

import android.util.Log
import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.data.local.entity.OfflineChapterEntity
import com.emptycastle.novery.provider.MainProvider
import com.emptycastle.novery.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class CacheEntry(
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val maxAgeMs: Long = 30 * 60 * 1000 // 30 mins
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > maxAgeMs
}

class ContentLoadingStrategy(
    private val offlineDao: OfflineDao
) {
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun loadChapter(
        url: String,
        novelUrl: String,
        chapterTitle: String,
        provider: MainProvider,
        mode: LoadingMode,
        sourceLanguage: String,
        targetLanguage: String,
        translationEnabled: Boolean,
        useOnline: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        
        // 1. Memory Cache Interception (Returns already processed/translated content)
        val cached = memoryCache[url]
        if (cached != null && !cached.isExpired) {
            return@withContext cached.content
        }

        // 2. Resolve Source (Offline/Network)
        val rawContent = when (mode) {
            LoadingMode.OFFLINE_FIRST -> {
                offlineDao.getChapter(url)?.content ?: provider.loadChapterContent(url)?.also {
                    offlineDao.saveChapter(OfflineChapterEntity(url, novelUrl, chapterTitle, it))
                }
            }
            LoadingMode.NETWORK_FIRST -> {
                try {
                    provider.loadChapterContent(url)?.also {
                        offlineDao.saveChapter(OfflineChapterEntity(url, novelUrl, chapterTitle, it))
                    } ?: offlineDao.getChapter(url)?.content
                } catch (e: Exception) {
                    offlineDao.getChapter(url)?.content
                }
            }
            LoadingMode.OFFLINE_ONLY -> offlineDao.getChapter(url)?.content
            LoadingMode.NETWORK_ONLY -> provider.loadChapterContent(url)
        } ?: return@withContext null

        // 3. Transparent Translation Hook (NATIVE PRELOADING HOOK)
        val finalContent = if (translationEnabled && rawContent.isNotBlank()) {
            Log.d("ContentLoadingStrategy", "Native Hook: Translating chapter $url")
            
            // Ensure model is ready for offline mode
            if (!useOnline) {
                val prepareResult = TranslationManager.prepareModel(sourceLanguage, targetLanguage) { }
                if (prepareResult.isFailure) {
                    Log.e("ContentLoadingStrategy", "MLKit prepare failed: ${prepareResult.exceptionOrNull()?.message}")
                    return@withContext rawContent // Fallback to original
                }
            }

            // Use the robust Jsoup-based translation
            val translated = TranslationManager.translate(
                rawContent, sourceLanguage, targetLanguage, useOnline
            )
            Log.d("ContentLoadingStrategy", "Native Hook: Translation complete for $url")
            translated
        } else {
            rawContent
        }

        // 4. Save processed result to memory cache
        memoryCache[url] = CacheEntry(content = finalContent)
        return@withContext finalContent
    }

    fun clearCache() {
        memoryCache.clear()
    }
}
