# Plan de Acción: Actualización del Provider NovelBin

Este plan detalla los pasos para actualizar el provider `NovelBin` en Novery utilizando la lógica clonada de QuickNovel.

## 1. Análisis de Diferencias
- **QuickNovel (QN)**: Utiliza una base genérica (`AllNovelProvider`). El endpoint de capítulos es AJAX (`ajax/chapter-archive`). El sistema de filtrado de posters (`fullPosterFix`) es directo.
- **Novery (NV)**: Implementación manual con múltiples fallbacks. Usa `MainProvider` directamente.

## 2. Cambios Propuestos

### Actualización de `NovelBinProvider.kt`
- **Filtros**: Actualizar `tags` y `orderBys` para que coincidan con la lista exhaustiva de QN.
- **Main Page**: Simplificar `loadMainPage` para usar la construcción de URL de QN:
  - Si hay tag (género): `$mainUrl/genre/$tag?page=$page`
  - Si hay sort: `$mainUrl/$sort?page=$page` (Default: `sort/top-hot-novel`)
- **Detalles de Novela (`load`)**:
  - Extraer ID desde `#rating[data-novel-id]`.
  - Cargar capítulos mediante el endpoint AJAX: `$mainUrl/ajax/chapter-archive?novelId=$id`.
  - Usar selectores simplificados para autor, géneros, estado y sinopsis.
- **Búsqueda**: Usar el endpoint `$mainUrl/search?keyword=$query`.
- **Limpieza de Contenido**: Mantener los patrones de limpieza de NV pero asegurar que los selectores de contenido coincidan con los de QN (`#chapter-content` o `#chr-content`).

## 3. Pasos para la Ejecución

1. **Paso 1**: Respaldar el archivo actual de Novery (opcional pero recomendado).
2. **Paso 2**: Modificar `NovelBinProvider.kt` inyectando la lógica de QN adaptada a los modelos de NV (`Novel`, `Chapter`, `NovelDetails`).
3. **Paso 3**: Verificar que los selectores de Jsoup sean compatibles.
4. **Paso 4**: Probar la carga de la página principal, búsqueda y lectura.

---

¿Deseas que proceda con la actualización de `NovelBinProvider.kt` siguiendo este plan?
