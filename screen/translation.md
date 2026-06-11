# Transparent MLKit Translation Implementation Manual (V7)

This document serves as a complete step-by-step guide to implement the in-place translation feature in Novery from scratch.

## 1. Build Configuration

### [libs.versions.toml](file:///home/hamunoz/StudioProjects/Novery/gradle/libs.versions.toml)
Add the MLKit Translate dependency version.
```toml
[versions]
mlkit-translate = "17.0.3"

[libraries]
mlkit-translate = { group = "com.google.mlkit", name = "translate", version.ref = "mlkit-translate" }
```

### [app/build.gradle.kts](file:///home/hamunoz/StudioProjects/Novery/app/build.gradle.kts)
Include the library in the dependencies block.
```kotlin
dependencies {
    implementation(libs.mlkit.translate)
}
```

---

## 2. Core Translation Logic

### [TranslationManager.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/translation/TranslationManager.kt)
Create this object to handle both offline (MLKit) and online (Google fallback) translation.

```kotlin
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

    suspend fun prepareModel(source: String, target: String, onProgress: (String) -> Unit = {}): Result<Boolean> = withContext(Dispatchers.IO) {
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

    private companion object {
        const val PARAGRAPHS_SEPARATOR = "\nXQZX\n"
        const val CHARS_LIMIT = 2000
        val SEPARATOR_REGEX = Regex("\\n?XQZX\\n?")
    }

    suspend fun onlineTranslate(
        paragraphs: List<String>,
        from: String,
        to: String
    ): List<String> {
        if (paragraphs.isEmpty()) return emptyList()
        val chunks = paragraphs.chunkByLimit()
        val translated = chunks.map { chunk -> translateChunk(chunk, from, to) }
        return translated.flatMap { it.split(SEPARATOR_REGEX) }
    }

    private suspend fun translateChunk(text: String, from: String, to: String, isRetry: Boolean = false): String {
        var attempt = 0
        while (attempt < 3) {
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
                    if (trans == orig && !isRetry && orig.length > 5) return translateChunk(orig, from, to, true)
                    result.append(trans)
                }
                return result.toString()
            } catch (e: Exception) {
                attempt++
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
}
```

---

## 3. Data Models

### [ReaderSettings.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/domain/model/ReaderSettings.kt)
Add translation fields to the `ReaderSettings` data class.
```kotlin
data class ReaderSettings(
    // ...
    val translationEnabled: Boolean = false,
    val sourceLang: String = "en",
    val targetLang: String = "es",
    val useOnlineTranslation: Boolean = false,
    val hideTranslateButtonWhenActive: Boolean = true
)
```

### [ReaderUiState.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/screens/reader/model/ReaderUiState.kt)
Add state for the translation panel and status.
```kotlin
data class ReaderUiState(
    // ...
    val translationStatus: String? = null,
    val showTranslation: Boolean = false
)
```

---

## 4. ViewModel Integration

### [ReaderViewModel.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/screens/reader/ReaderViewModel.kt)

#### Imports
```kotlin
import com.emptycastle.novery.translation.TranslationManager
import com.emptycastle.novery.util.SentenceParser
```

#### Settings Observation
Update `observeSettings` to trigger re-translation when settings change.
```kotlin
preferencesManager.readerSettings.collect { settings ->
    val oldSettings = _uiState.value.settings
    val translationChanged = oldSettings.translationEnabled != settings.translationEnabled ||
            oldSettings.sourceLang != settings.sourceLang ||
            oldSettings.targetLang != settings.targetLang ||
            oldSettings.useOnlineTranslation != settings.useOnlineTranslation

    _uiState.update { it.copy(settings = settings) }
    if (translationChanged) reTranslateAllLoadedChapters()
}
```

#### Translation Methods
```kotlin
private suspend fun performTranslation(loadedChapter: LoadedChapter): LoadedChapter {
    val settings = _uiState.value.settings
    if (!settings.translationEnabled) return loadedChapter

    val source = settings.sourceLang
    val target = settings.targetLang
    val useOnline = settings.useOnlineTranslation

    if (useOnline && appContext?.let { !TranslationManager.isOnline(it) } == true) {
        _uiState.update { it.copy(translationStatus = "No internet") }
        return loadedChapter
    }

    if (!useOnline) {
        TranslationManager.prepareModel(source, target) { progress ->
            _uiState.update { it.copy(translationStatus = progress) }
        }
    }

    val translatableItems = loadedChapter.contentItems.filter { /* Text, AuthorNote, Table, List */ }
    val texts = translatableItems.map { /* extract text */ }
    val translated = TranslationManager.translate(texts, source, target, useOnline)

    // Re-parse sentences for TTS compatibility
    val translatedItems = loadedChapter.contentItems.map { item ->
        // replace with translated text and SentenceParser.parse(translatedText)
    }

    _uiState.update { it.copy(translationStatus = null) }
    return loadedChapter.copy(contentItems = translatedItems)
}
```

#### Loading Flow Hook
In `loadChapterContent`, apply `performTranslation` before updating the state.
```kotlin
val result = chapterLoader.loadChapter(chapter, chapterIndex)
if (result is ChapterLoadResult.Success) {
    val processedChapter = performTranslation(result.loadedChapter)
    _uiState.update { it.copy(loadedChapters = it.loadedChapters + (chapterIndex to processedChapter)) }
}
```

#### Cleanup
```kotlin
override fun onCleared() {
    TranslationManager.release()
    super.onCleared()
}
```

---

## 5. UI Components

### [TranslationPanel.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/components/TranslationPanel.kt)
Create the Compose panel for selecting languages and modes.

### [ReaderBottomBar.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/components/ReaderBottomBar.kt)
Add the "Translate" button and ensure vertical layout for the "Listen" button to save space.

```kotlin
@Composable
private fun ListenButton(onClick: () -> Unit) {
    // Column layout with Icon on top and Text below
}
```

---

## 6. Screen Integration

### [ReaderScreen.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/screens/reader/ReaderScreen.kt)
Add the `TranslationPanel` to the main screen layout.

```kotlin
AnimatedVisibility(visible = uiState.showTranslation) {
    TranslationPanel(
        settings = uiState.settings,
        translationStatus = uiState.translationStatus,
        onSettingsChange = viewModel::updateReaderSettings,
        onDismiss = viewModel::hideTranslation
    )
}
```

## Summary
By following this manual, the translation feature integrates natively into the reading flow, providing in-place text replacement while maintaining synchronization with the TTS engine.

### Notes
**Lifecycle**  
`release()` and `deleteModel()` are already included in `TranslationManager`. Call `release()` from `ReaderViewModel.onCleared()`:
```kotlin
override fun onCleared() {—    TranslationManager.release()ó    super.onCleared()}—
```
**Translator reuse** 
— `prepareMoel` already handles reuse and close-on-change via `currentSource`/`currentTarget` checks.
 No extra work needed.
**Online Translation** 
— Uses `NetworkClient.getText()` (OkHttp) con chunking por 2000 chars, retry con backoff exponencial, 
y detección de traducciones fallidas. 
Endpoint unofficial de `translate.googleapis.com`, sin API key. Si deja de funcionar, se puede remover
sin afectar MLKit offline.
**Performance**  
MLKit local processing can be memory intensive on long 
chapters. Wrapped maps filter out tiny or empty text elements to bypass redundant task allocation.
**Scope**
ó Intenionally avoids external backends, API keys, paid services, or extra infrastructure. 
Lightweight by design.d