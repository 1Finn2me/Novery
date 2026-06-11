# Plan de Integración del Reader de QuickNovel en Novery

Este documento detalla el plan para clonar la lógica del lector de QuickNovel e integrarla en Novery, enfocándose exclusivamente en el código (lógica de negocio, procesamiento de datos, TTS y traducciones) sin incluir la interfaz gráfica (GUI).

## 1. Análisis de Arquitectura

QuickNovel utiliza una abstracción llamada `AbstractBook` para manejar diferentes fuentes de contenido (EPUB locales o Stream de Providers online). Su ViewModel (`ReadActivityViewModel`) maneja el pre-procesamiento de texto, segmentación para TTS y una lógica de caché de traducción basada en hashes MD5.

Novery actualmente utiliza `ChapterLoader` y `MainProvider`. La integración buscará unificar estas arquitecturas para permitir que Novery herede las capacidades de QuickNovel, especialmente el soporte robusto de EPUB y su lógica de segmentación de texto.

---

## 2. Librerías y Dependencias Requeridas

Se deben añadir las siguientes dependencias a `app/build.gradle.kts`:

```kotlin
dependencies {
    // Renderizado de Markdown/HTML (QuickNovel usa Markwon)
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    
    // Soporte para EPUB (Portado de QuickNovel)
    implementation("org.slf4j:slf4j-android:1.7.36")
    
    // Ya presentes en Novery, pero cruciales:
    // implementation("com.google.mlkit:translate:17.0.3")
    // implementation("org.jsoup:jsoup:1.18.1")
}
```

---

## 3. Archivos a Clonar y Crear

### A. Librerías Internas (Source Port)
Debemos copiar el código fuente de las librerías que QuickNovel tiene embebidas:
- `me.ag2s.epublib`: Core para manejo de EPUBs.
- `me.ag2s.umdlib`: Soporte para formato UMD.
- `me.ag2s.base`: Utilidades base para las librerías anteriores.

### B. Abstracción de Contenido (`com.emptycastle.novery.data.reader`)
Crearemos una capa de compatibilidad basada en `AbstractBook` de QuickNovel:
1. `AbstractBook.kt`: Clase base abstracta.
2. `QuickBook.kt`: Implementación para el sistema de Providers de Novery (adaptado de `QuickStreamData`).
3. `RegularBook.kt`: Implementación para archivos EPUB usando `epublib`.

### C. Lógica de Procesamiento (`com.emptycastle.novery.util.reader`)
1. `QNTSHelper.kt`: Port de la lógica de segmentación de texto de QuickNovel (`ttsParseText`) que es más granular que la actual de Novery.
2. `QNTranslationHelper.kt`: Port del sistema de caché de traducción por hash y el integrador de Google Translate Online.

---

## 4. Modificaciones en Novery

### A. `NovelRepository.kt` y `OfflineRepository.kt`
- Añadir soporte para "Novelas Locales" (EPUBs).
- Implementar la detección de archivos en el almacenamiento local para tratarlos como entradas en la biblioteca.

### B. `ReaderViewModel.kt`
- Integrar `AbstractBook` como la fuente de verdad del contenido.
- Reemplazar (o complementar) la lógica de carga de capítulos para que utilice las estrategias de `QuickBook` y `RegularBook`.
- Integrar la segmentación de `QNTSHelper` para mejorar la experiencia de TTS.

### C. `ChapterLoader.kt`
- Adaptar para que pueda devolver objetos `Spanned` procesados por Markwon, permitiendo un renderizado más fiel al de QuickNovel.

---

## 5. Código de Referencia a Crear (Esquema)

### Port de `AbstractBook` (Adaptado)
```kotlin
abstract class AbstractBook {
    abstract fun size(): Int
    abstract fun title(): String
    abstract fun getChapterTitle(index: Int): String
    abstract suspend fun getChapterData(index: Int, reload: Boolean): String
    // ... otros métodos de utilidad para imágenes y autor
}
```

### Port de `QNTSHelper.ttsParseText` (Crucial para el Reader)
Esta función divide el texto en segmentos lógicos para el TTS evitando cortes bruscos en abreviaciones (Mr., Dr.) y manejando correctamente la puntuación.

---

## 6. Pasos para la Ejecución

1. **Fase 1**: Copiar los paquetes `me.ag2s.*` al directorio de fuentes de Novery.
2. **Fase 2**: Crear las clases de abstracción `AbstractBook`, `QuickBook` y `RegularBook`.
3. **Fase 3**: Implementar `QNTSHelper` y `QNTranslationHelper`.
4. **Fase 4**: Refactorizar `ReaderViewModel` para usar la nueva lógica de carga.
5. **Fase 5**: Sincronizar Gradle y verificar compilación.

¿Deseas que proceda con la creación de los archivos de librería y las abstracciones base?
