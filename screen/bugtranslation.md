​1. En ContentLoadingStrategy.kt
​Este archivo se encarga de interceptar el contenido capítulo a capítulo.
Es el encargado de administrar la caché en memoria (memoryCache) y aplicar 
tu hook de traducción al vuelo antes de devolver el texto

```kotlin 

// ContentLoadingStrategy.kt
package com.emptycastle.novery.data.repository

import com.emptycastle.novery.data.local.dao.OfflineDao
import com.emptycastle.novery.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class LoadingMode {
    OFFLINE_FIRST, NETWORK_FIRST, OFFLINE_ONLY, NETWORK_ONLY
}

data class CacheEntry(
    val content: String, // Guarda el contenido ya procesado/traducido
    val timestamp: Long = System.currentTimeMillis(),
    val maxAgeMs: Long = 30 * 60 * 1000 // 30 minutos de vida temporal
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > maxAgeMs
}

class ContentLoadingStrategy(
    private val offlineDao: OfflineDao,
    private val mlKitTranslator: TranslationManager
) {
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun loadChapter(
        url: String,
        provider: NovelProvider,
        mode: LoadingMode,
        targetLanguage: String,
        translationEnabled: Boolean
    ): String? = withContext(Dispatchers.IO) {
        
        // 1. Intercepción rápida en caché de memoria temporal (estilo QuickNovel)
        val cached = memoryCache[url]
        if (cached != null && !cached.isExpired) {
            return@withContext cached.content
        }

        // 2. Resolver origen según el LoadingMode seleccionado
        val rawContent = when (mode) {
            LoadingMode.OFFLINE_FIRST -> {
                val local = offlineDao.getChapterContent(url)
                if (local != null) local else {
                    val network = provider.loadChapterContent(url)
                    if (network != null) offlineDao.saveChapterContent(url, network)
                    network
                }
            }
            LoadingMode.NETWORK_FIRST -> {
                try {
                    val network = provider.loadChapterContent(url)
                    if (network != null) {
                        offlineDao.saveChapterContent(url, network)
                        network
                    } else offlineDao.getChapterContent(url)
                } catch (e: Exception) {
                    offlineDao.getChapterContent(url)
                }
            }
            LoadingMode.OFFLINE_ONLY -> offlineDao.getChapterContent(url)
            LoadingMode.NETWORK_ONLY -> provider.loadChapterContent(url)
        } ?: return@withContext null

        // 3. TU HOOK: Traducir al vuelo de forma transparente
        val finalContent = if (translationEnabled && rawContent.isNotBlank()) {
            mlKitTranslator.translate(rawContent, targetLanguage)
        } else {
            rawContent
        }

        // 4. Guardar resultado final temporalmente en la memoria
        memoryCache[url] = CacheEntry(content = finalContent)
        return@withContext finalContent
    }

    fun saveToMemoryCache(url: String, content: String) {
        memoryCache[url] = CacheEntry(content = content)
    }

    fun clearCache() {
        memoryCache.clear()
    }
}

```
2. En NovelRepository.kt
​Este archivo actúa a un nivel superior (Patrón Repositorio completo). 
Su trabajo ahora es controlar la búsqueda masiva concurrente de todos los
proveedores en paralelo usando streams y orquestar la precarga de capítulos 
en segundo plano invocando al método asíncrono que acabamos de estructurar 
arriba.

```kotlin 
// NovelRepository.kt
package com.emptycastle.novery.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NovelRepository(
    private val providers: List<NovelProvider>,          // Todos los proveedores para búsquedas concurrentes
    private val currentProvider: NovelProvider,          // El proveedor activo que está usando el usuario para leer
    private val contentStrategy: ContentLoadingStrategy  // Delegado de carga de contenido y traducción
) {

    /**
     * Búsqueda en paralelo tipo Streaming usando channelFlow.
     * Dispara la búsqueda en todos los providers simultáneamente y emite los resultados según van terminando.
     */
    fun searchAllStreaming(query: String): Flow<List<Novel>> = channelFlow {
        providers.forEach { provider ->
            launch(Dispatchers.IO) {
                try {
                    val results = provider.search(query)
                    if (results.isNotEmpty()) {
                        send(results) // Emisión inmediata a la interfaz de usuario
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Un proveedor caído no frena a los demás
                }
            }
        }
    }

    /**
     * Precarga asíncrona de capítulos en segundo plano (Preloading).
     * Evita la lentitud pidiendo y traduciendo con antelación los siguientes capítulos en bloques controlados.
     */
    suspend fun preloadChapters(
        urls: List<String>, 
        mode: LoadingMode, 
        targetLanguage: String, 
        translationEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        // Procesamos de 5 en 5 para evitar picos de uso de CPU (MLKit) o bloqueos de red (HTTP 429)
        urls.chunked(5).forEach { chunk ->
            chunk.map { url ->
                async {
                    contentStrategy.loadChapter(
                        url = url,
                        provider = currentProvider,
                        mode = mode,
                        targetLanguage = targetLanguage,
                        translationEnabled = translationEnabled
                    )
                }
            }.awaitAll() // Espera que termine el bloque actual antes de pasar al siguiente
        }
    }

    /**
     * Método estándar que llamará tu ReaderViewModel para traer un único capítulo.
     */
    suspend fun getChapter(
        url: String,
        mode: LoadingMode,
        targetLanguage: String,
        translationEnabled: Boolean
    ): String? {
        return contentStrategy.loadChapter(url, currentProvider, mode, targetLanguage, translationEnabled)
    }
}

```

Resumen de la interacción (Por qué funciona tan rápido):
​Cuando el usuario abre una novela, el NovelRepository ejecuta 
preloadChapters() en segundo plano para los capítulos siguientes 
(por ejemplo, del 2 al 6).
​Esas peticiones llaman a contentStrategy.loadChapter(). Como la traducción
está activa, el texto se descarga de internet (o se saca de Room si ya 
estaba guardado el original), pasa por MLKit y se queda flotando ya 
traducido en el memoryCache.
​En el momento exacto en que el usuario le da a "Siguiente capítulo", el 
lector pide ese contenido. ContentLoadingStrategy intercepta la llamada en
el paso 1 (memoryCache[url]) y le devuelve el string traducido
instantáneamente. El usuario no experimenta ninguna pausa ni retraso por
procesamiento.