El Problema (Por qué Novery se "traba" y QuickNovel no):
El problema está en que estamos traduciendo dos veces y en el lugar equivocado.
1.
Doble Trabajo: Tu ContentLoadingStrategy ya traduce el capítulo y lo guarda en la caché de memoria (memoryCache). Eso está perfecto y es lo que hace que sea rápido.
2.
El Cuello de Botella: En el ReaderViewModel.kt, después de cargar el capítulo, el código vuelve a llamar a una función llamada performTranslation. Esta función recorre todos los párrafos uno a uno e intenta traducirlos otra vez antes de mostrarlos.
3.
Resultado: Aunque el capítulo ya esté traducido en la caché, el ReaderViewModel detiene todo para procesar performTranslation, lo que causa el lag o el cierre de la app.
Solución Parcial 1: Limpiar el ReaderViewModel
Debemos eliminar la traducción redundante. El ViewModel solo debe pedir el capítulo y confiar en que la ContentLoadingStrategy ya lo trae traducido.
Cambio en ReaderViewModel.kt: Voy a eliminar el método performTranslation y hacer que loadChapterContent sea directo.
Kotlin
// --- ARCHIVO: ReaderViewModel.kt ---
// 1. Eliminar por completo la función performTranslation() (línea ~1686)
// 2. Simplificar loadChapterContent (línea ~1767)

private suspend fun loadChapterContent(chapterIndex: Int, ...) {
    // ... código de carga ...
    val result = chapterLoader.loadChapter(
        chapter = chapter,
        chapterIndex = chapterIndex,
        // Pasamos los ajustes para que la estrategia sepa qué hacer
        sourceLang = settings.sourceLang,
        targetLang = settings.targetLang,
        translationEnabled = settings.translationEnabled,
        useOnline = settings.useOnlineTranslation
    )
    when (result) {
        is ChapterLoadResult.Success -> {
            // ¡YA NO LLAMAMOS A performTranslation aquí! 
            // El resultado ya viene traducido de la estrategia.
            stateMutex.withLock {
                _uiState.update {
                    it.copy(loadedChapters = it.loadedChapters + (chapterIndex to result.loadedChapter))
                }
            }
        }
    }
}
Solución Parcial 2: Mejorar TranslationManager según tus notas
He visto tus notas en translation.md. Para que sea fluido como QN, el traductor debe ignorar textos muy cortos y reutilizar el cliente.
Cambio en TranslationManager.kt:
Kotlin
// En la función translate:
textList.map { text ->
    // Nota de performance: saltar textos insignificantes
    if (text.isBlank() || text.length < 3) return@map text 
    try {
        Tasks.await(client.translate(text))
    } catch (e: Exception) {
        text
    }
}
¿Qué ganes con esto?
Al quitar performTranslation del ViewModel, el capítulo se muestra al instante si ya fue precargado por el NovelRepository. La fluidez volverá a ser la misma que en el idioma original porque el "trabajo pesado" se queda exclusivamente en la capa de datos (segundo plano).
¿Quieres que aplique esta limpieza en el ViewModel ahora mismo para que veas la mejora de velocidad? Solo borraré lo que sobra y conectaré bien las piezas.
