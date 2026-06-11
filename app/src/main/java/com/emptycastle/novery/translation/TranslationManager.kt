package com.emptycastle.novery.translation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.emptycastle.novery.data.remote.NetworkClient
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.*
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode

object TranslationManager {
    private const val TAG = "TranslationManager"
    private var translator: Translator? = null
    private var currentSource: String? = null
    private var currentTarget: String? = null
    private val prepareMutex = Mutex()

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun isModelDownloaded(source: String, target: String): Boolean = withContext(Dispatchers.IO) {
        val modelManager = RemoteModelManager.getInstance()
        val sourceModel = TranslateRemoteModel.Builder(source).build()
        val targetModel = TranslateRemoteModel.Builder(target).build()
        val sDownloaded = if (source == "en") true else Tasks.await(modelManager.isModelDownloaded(sourceModel))
        val tDownloaded = if (target == "en") true else Tasks.await(modelManager.isModelDownloaded(targetModel))
        sDownloaded && tDownloaded
    }

    suspend fun identifyLanguage(text: String): String? = withContext(Dispatchers.IO) {
        try {
            val sample = text.take(200)
            if (sample.isBlank()) return@withContext null
            
            val languageIdentifier = LanguageIdentification.getClient()
            val languageCode = Tasks.await(languageIdentifier.identifyLanguage(sample))
            
            if (languageCode == "und") {
                Log.w(TAG, "Language identification failed (und)")
                null
            } else {
                Log.i(TAG, "Identified language: $languageCode")
                languageCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in language identification: ${e.message}")
            null
        }
    }

    suspend fun prepareModel(source: String, target: String, onProgress: (String) -> Unit): Result<Boolean> = prepareMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Preparing model: $source -> $target")
                if (translator != null && currentSource == source && currentTarget == target) {
                    return@withContext Result.success(true)
                }
                if (currentSource != source || currentTarget != target) {
                    translator?.close()
                }

                val sourceTag = TranslateLanguage.fromLanguageTag(source) ?: TranslateLanguage.ENGLISH
                val targetTag = TranslateLanguage.fromLanguageTag(target) ?: TranslateLanguage.SPANISH

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceTag)
                    .setTargetLanguage(targetTag)
                    .build()

                val client = Translation.getClient(options)

                if (!isModelDownloaded(source, target)) {
                    onProgress("Downloading offline model...")
                    Tasks.await(client.downloadModelIfNeeded(DownloadConditions.Builder().build()))
                }

                translator = client
                currentSource = source
                currentTarget = target
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error in prepareModel: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Translates an HTML string robustly by extracting text nodes.
     */
    suspend fun translate(
        html: String,
        source: String,
        target: String,
        useOnline: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        if (html.isBlank()) return@withContext html

        try {
            val doc = Jsoup.parseBodyFragment(html)
            val textNodes = mutableListOf<TextNode>()
            
            // Extract all text nodes that are not whitespace only
            doc.body().traverse { node, _ ->
                if (node is TextNode && node.text().isNotBlank()) {
                    textNodes.add(node)
                }
            }

            if (textNodes.isEmpty()) return@withContext html

            val textsToTranslate = textNodes.map { it.text() }
            val translatedTexts = translate(textsToTranslate, source, target, useOnline)

            // Re-insert translated text into nodes
            translatedTexts.forEachIndexed { index, translated ->
                textNodes.getOrNull(index)?.text(translated)
            }

            doc.body().html()
        } catch (e: Exception) {
            Log.e(TAG, "Error in HTML translation: ${e.message}")
            html
        }
    }

    suspend fun translate(
        textList: List<String>,
        source: String,
        target: String,
        useOnline: Boolean = false,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        if (textList.isEmpty()) return@withContext emptyList()

        if (useOnline) return@withContext onlineTranslate(textList, source, target, onProgress)

        val client = translator
        if (client == null) {
            Log.e(TAG, "Translator is null. Call prepareModel first.")
            return@withContext textList
        }

        textList.mapIndexed { i, text ->
            onProgress(i + 1, textList.size)
            if (text.isBlank() || text.length < 3) return@mapIndexed text
            try {
                Tasks.await(client.translate(text))
            } catch (e: Exception) {
                Log.e(TAG, "Offline translation error: ${e.message}")
                text
            }
        }
    }

    private const val PARAGRAPHS_SEPARATOR = " XQZX "
    private const val CHARS_LIMIT = 1500 // Safer limit
    private val SEPARATOR_REGEX = Regex("(?i)\\s?XQZX\\s?")

    suspend fun onlineTranslate(
        paragraphs: List<String>,
        from: String,
        to: String,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): List<String> {
        if (paragraphs.isEmpty()) return emptyList()
        val chunks = paragraphs.chunkByLimit()
        val translated = chunks.mapIndexed { i, chunk ->
            onProgress(i, chunks.size)
            try {
                translateChunk(chunk, from, to)
            } catch (e: Exception) {
                Log.e(TAG, "Online translation chunk failed: ${e.message}")
                chunk
            }
        }
        return translated.flatMap { it.split(SEPARATOR_REGEX).filter { s -> s.isNotBlank() } }
    }

    private suspend fun translateChunk(
        text: String,
        from: String,
        to: String,
        isRetry: Boolean = false
    ): String {
        var attempt = 0
        val maxAttempts = 3
        while (attempt < maxAttempts) {
            try {
                val encoded = Uri.encode(text)
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$from&tl=$to&dt=t&q=$encoded"
                val response = NetworkClient.getText(url)
                val json = JSONArray(response)
                val sentences = json.getJSONArray(0)
                val result = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val trans = sentences.getJSONArray(i).getString(0)
                    val orig = sentences.getJSONArray(i).getString(1)
                    val failed = trans == orig && orig.any { it.isLetter() } && orig.split(" ").size >= 3
                    if (failed && !isRetry) return translateChunk(orig, from, to, isRetry = true)
                    result.append(trans)
                }
                return result.toString()
            } catch (e: Exception) {
                if (e is java.net.UnknownHostException) throw e
                attempt++
                if (attempt >= maxAttempts) throw e
                delay(500L * (2.0.pow(attempt).toLong()))
            }
        }
        return text
    }

    private fun List<String>.chunkByLimit(): List<String> {
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        for (p in this) {
            val segment = p.trim().ifBlank { " " } + PARAGRAPHS_SEPARATOR
            if (segment.length > CHARS_LIMIT) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().removeSuffix(PARAGRAPHS_SEPARATOR))
                    current = StringBuilder()
                }
                chunks.add(segment.removeSuffix(PARAGRAPHS_SEPARATOR))
                continue
            }
            if (current.length + segment.length > CHARS_LIMIT) {
                chunks.add(current.toString().removeSuffix(PARAGRAPHS_SEPARATOR))
                current = StringBuilder()
            }
            current.append(segment)
        }
        if (current.isNotEmpty()) chunks.add(current.toString().removeSuffix(PARAGRAPHS_SEPARATOR))
        return chunks
    }

    fun release() {
        translator?.close()
        translator = null
        currentSource = null
        currentTarget = null
    }

    suspend fun deleteModel(language: String) = withContext(Dispatchers.IO) {
        val modelManager = RemoteModelManager.getInstance()
        val model = TranslateRemoteModel.Builder(language).build()
        Tasks.await(modelManager.deleteDownloadedModel(model))
    }
}
