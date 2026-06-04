package com.emptycastle.novery.translation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.emptycastle.novery.data.remote.NetworkClient
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray

object TranslationManager {
    private var translator: Translator? = null
    private var currentSource: String? = null
    private var currentTarget: String? = null

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
        val sDownloaded = Tasks.await(modelManager.isModelDownloaded(sourceModel))
        val tDownloaded = Tasks.await(modelManager.isModelDownloaded(targetModel))
        sDownloaded && tDownloaded
    }

    suspend fun prepareModel(source: String, target: String, onProgress: (String) -> Unit): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
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
            Result.failure(e)
        }
    }

    suspend fun translate(
        textList: List<String>,
        source: String,
        target: String,
        useOnline: Boolean = false
    ): List<String> = withContext(Dispatchers.IO) {
        if (textList.isEmpty()) return@withContext emptyList()

        if (useOnline) return@withContext onlineTranslate(textList, source, target)

        val client = translator ?: return@withContext textList

        textList.map { text ->
            if (text.isBlank() || text.length < 2) return@map text
            try {
                Tasks.await(client.translate(text))
            } catch (e: Exception) {
                text
            }
        }
    }

    private const val PARAGRAPHS_SEPARATOR = "\nXQZX\n"
    private const val CHARS_LIMIT = 2000
    private val SEPARATOR_REGEX = Regex("\\n?XQZX\\n?")

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
            translateChunk(chunk, from, to)
        }
        return translated.flatMap { it.split(SEPARATOR_REGEX) }
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
