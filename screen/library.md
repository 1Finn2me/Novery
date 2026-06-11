# Documentación Completa: Sistema de Librerías de Bookmarks
> Documento unificado que consolida `librerias.md` (PR #23 + Parche Bookmark) y `libreriasII.md` (guía completa v6→v9).
---
# Documentación: Sistema de Librerías de Bookmarks

Este documento describe los cambios introducidos por el **PR #23** y su corrección posterior en el commit **"Parche Bookmark"** (`e7530e4`), que juntos implementan un sistema de librerías de lectura dinámico en reemplazo del enum `ReadType` hardcodeado.

---



### Archivos modificados

#### 1. `app/src/main/java/com/lagradost/quicknovel/DataStore.kt`

Se agrega la data class `DefaultLibrary`, la constante `LIBRARIES_KEY`, la lista `DEFAULT_LIBRARIES` y las funciones de extensión para CRUD de librerías.

```kotlin
const val LIBRARIES_KEY: String = "default_libraries"

data class DefaultLibrary(
    val id: Int,
    val key: String,
    val title: String,
    val editable: Boolean = true,
    val position: Int = 0
)

val DEFAULT_LIBRARIES: List<DefaultLibrary> = listOf(
    DefaultLibrary(5, "READING",       "Reading",      position = 1),
    DefaultLibrary(4, "ON_HOLD",       "On hold",      position = 2),
    DefaultLibrary(1, "PLAN_TO_READ",  "Plan to read", position = 3),
    DefaultLibrary(3, "COMPLETED",     "Completed",    editable = false, position = 4),
    DefaultLibrary(2, "DROPPED",       "Dropped",      position = 5),
)
```

Los `id` de cada `DefaultLibrary` coinciden con los valores `ReadType.prefValue` para mantener compatibilidad con bookmarks existentes.

Las **funciones de extensión de `Context`** son la API pública para interactuar con las librerías desde cualquier parte de la app. Se agregan para desarrollo futuro (gestión de librerías personalizadas desde la UI):

```kotlin
/**
 * Retorna la lista de librerías persistida, ordenada por [DefaultLibrary.position].
 * Si no hay lista guardada, devuelve [DEFAULT_LIBRARIES].
 */
fun Context.getLibraries(): List<DefaultLibrary> {
    val stored = with(DataStore) { this@getLibraries.getKey<List<DefaultLibrary>>(LIBRARIES_KEY) }
    return stored?.sortedBy { it.position } ?: DEFAULT_LIBRARIES
}

/**
 * Sobreescribe la lista de librerías persistida con [libs] (ordenada por position).
 * Lanza excepción si hay IDs duplicados.
 */
fun Context.saveLibraries(libs: List<DefaultLibrary>) {
    require(libs.map { it.id }.distinct().size == libs.size) {
        "Library list contains duplicate ids."
    }
    val sorted = libs.sortedBy { it.position }
    with(DataStore) { this@saveLibraries.setKey(LIBRARIES_KEY, sorted) }
}

/**
 * Agrega [newLib] a la lista persistida.
 * Lanza excepción si ya existe una librería con el mismo id.
 */
fun Context.addLibrary(newLib: DefaultLibrary) {
    val current = getLibraries().toMutableList()
    require(current.none { it.id == newLib.id }) {
        "A library with id ${newLib.id} already exists."
    }
    current.add(newLib)
    saveLibraries(current)
}

/**
 * Reemplaza la librería cuyo id coincide con [updated].
 * Respeta [DefaultLibrary.editable]: lanza excepción si la librería no es editable.
 */
fun Context.updateLibrary(updated: DefaultLibrary) {
    val current = getLibraries().toMutableList()
    val index = current.indexOfFirst { it.id == updated.id }
    require(index >= 0) { "No library with id ${updated.id} found." }
    val existing = current[index]
    require(existing.editable) { "Library '${existing.title}' is not editable." }
    current[index] = updated
    saveLibraries(current)
}

/**
 * Elimina la librería con el [id] dado.
 * Lanza excepción si no es editable (e.g. "Plan to read").
 */
fun Context.deleteLibrary(id: Int) {
    val current = getLibraries().toMutableList()
    val target = current.find { it.id == id }
    require(target != null) { "No library with id $id found." }
    require(target.editable) { "Library '${target.title}' is not editable." }
    current.removeAll { it.id == id }
    saveLibraries(current)
}

/**
 * Mueve la librería con [id] a [toPosition], desplazando el resto.
 * Los valores de posición se reasignan desde 1 para mantenerlos únicos y limpios.
 */
fun Context.moveLibrary(id: Int, toPosition: Int) {
    val current = getLibraries().sortedBy { it.position }.toMutableList()
    val fromIndex = current.indexOfFirst { it.id == id }
    require(fromIndex >= 0) { "No library with id $id found." }
    val item = current.removeAt(fromIndex)
    val insertAt = (toPosition - 1).coerceIn(0, current.size)
    current.add(insertAt, item)
    // Reassign positions 1..n to keep them clean and unique
    val reordered = current.mapIndexed { i, lib -> lib.copy(position = i + 1) }
    saveLibraries(reordered)
}
```

---

#### 2. `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadViewModel.kt`

Se reemplaza la lista estática `readList: ArrayList<ReadType>` por una función dinámica `libraries()`:

```kotlin
// Antes
val readList = arrayListOf(
    ReadType.READING,
    ReadType.ON_HOLD,
    ReadType.PLAN_TO_READ,
    ReadType.COMPLETED,
    ReadType.DROPPED,
)

// Después
fun libraries(): List<DefaultLibrary> =
    context?.getLibraries() ?: DEFAULT_LIBRARIES
```

El método `loadAllData` también se actualizó para construir el `mapping` y las `pages` dinámicamente desde `libraries()`:

```kotlin
// Antes — hardcodeado con ReadType
val mapping: HashMap<Int, ArrayList<ResultCached>> = hashMapOf(
    ReadType.PLAN_TO_READ.prefValue to arrayListOf(),
    ReadType.DROPPED.prefValue     to arrayListOf(),
    ...
)
for (read in readList) {
    pages.add(Page(read.name, unsortedItems = mapping[read.prefValue]!!, ...))
}

// Después — dinámico
val libs = libraries()
val mapping = hashMapOf<Int, ArrayList<ResultCached>>().apply {
    libs.forEach { lib -> put(lib.id, arrayListOf()) }
}
for (lib in libs) {
    val items = mapping[lib.id] ?: arrayListOf()
    pages.add(Page(lib.title, unsortedItems = items, items = sortNormalArray(items)))
}
```

---

#### 3. `app/src/main/java/com/lagradost/quicknovel/BookDownloader2.kt`

Se elimina la lista `readList` hardcodeada (que además tenía `READING` duplicado) y se usa `getLibraries()` para resolver la librería por posición de tab:

```kotlin
// Antes
suspend fun getOldDataReadingProgress(currentTabIndex: Int) {
    val keys = getKeys(RESULT_BOOKMARK_STATE) ?: return
    val readList = arrayListOf(
        ReadType.READING, ReadType.READING, // ← READING duplicado
        ReadType.ON_HOLD, ReadType.PLAN_TO_READ,
        ReadType.COMPLETED, ReadType.DROPPED,
    )
    ...
    if (state == readList[currentTabIndex].prefValue) { ... }
}

// Después
suspend fun getOldDataReadingProgress(currentTabIndex: Int) {
    if (currentTabIndex <= 0) return   // Tab 0 = Downloads, no necesita refresco
    val keys = getKeys(RESULT_BOOKMARK_STATE) ?: return
    val libraries = (context ?: return).getLibraries()
    val library = libraries.getOrNull(currentTabIndex - 1) ?: return
    ...
    if (state == library.id) { ... }
}
```

---

#### 4. `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadFragment.kt`

Se corrige el error de compilación: las referencias a `viewModel.readList` y `read.stringRes` se reemplazan por `viewModel.libraries()` y `lib.title`. Además, se arregla el uso incorrecto de `setId()` en el `TabLayoutMediator`:

```kotlin
// Antes — error de compilación: readList no existe, setId recibe un Int
val tabs = mutableListOf(R.string.tab_downloads)
for (read in viewModel.readList) {
    tabs.add(read.stringRes)
}
TabLayoutMediator(this, binding.viewpager) { tab, position ->
    tab.setId(tabs[position]).setText(tabs[position])
}.attach()

// Después — usa libraries(), texto plano como String
val tabLabels = mutableListOf(this@DownloadFragment.getString(R.string.tab_downloads))
for (lib in viewModel.libraries()) {
    tabLabels.add(lib.title)
}
TabLayoutMediator(this, binding.viewpager) { tab, position ->
    tab.setText(tabLabels[position])
}.attach()
```

---

## Commit `e7530e4` — "Parche Bookmark"

Correcciones aplicadas después del merge del PR #23 para ajustar los IDs de las librerías, agregar la categoría "Trash", y reemplazar el uso de `ReadType` en la pantalla de resultado y en `MainActivity` con el nuevo sistema de librerías dinámicas.

---

### Archivos modificados

#### 1. `app/src/main/java/com/lagradost/quicknovel/DataStore.kt`

Se reordenan los `id` de `DEFAULT_LIBRARIES` para que coincidan con los valores correctos de `ReadType.prefValue`, y se agrega la nueva librería `TRASH` (id = 6):

```kotlin
// Antes
val DEFAULT_LIBRARIES: List<DefaultLibrary> = listOf(
    DefaultLibrary(5, "READING",       "Reading",      position = 1),
    DefaultLibrary(4, "ON_HOLD",       "On hold",      position = 2),
    DefaultLibrary(1, "PLAN_TO_READ",  "Plan to read", position = 3),
    DefaultLibrary(3, "COMPLETED",     "Completed",    editable = false, position = 4),
    DefaultLibrary(2, "DROPPED",       "Dropped",      position = 5),
)

// Después
val DEFAULT_LIBRARIES: List<DefaultLibrary> = listOf(
    DefaultLibrary(1, "PLAN_TO_READ",  "Plan to read",  editable = false, position = 1),
    DefaultLibrary(2, "READING",       "Reading",    position = 2),
    DefaultLibrary(3, "COMPLETED",     "Completed",  position = 3),
    DefaultLibrary(4, "ON_HOLD",       "On hold",    position = 4),
    DefaultLibrary(5, "DROPPED",       "Dropped",    position = 5),
    DefaultLibrary(6, "TRASH",         "Trash",      position = 6),
)
```

También se agrega el salto de línea final al cierre del `object DataStore` (corrección de formato).

---

#### 2. `app/src/main/java/com/lagradost/quicknovel/MainActivity.kt`

Se reemplaza el uso de `viewModel.readState` (basado en `ReadType`) por `viewModel.libraryId` (Int con el id de la librería). El botón bookmark ahora abre un `showBottomDialog` con la lista de librerías en lugar de un `popupMenu` con entradas de `ReadType`:

```kotlin
// Nuevo import
import com.lagradost.quicknovel.util.SingleSelectionHelper.showBottomDialog

// Antes — observaba ReadType enum
observe(viewModel.readState) {
    bookmark.setIconResource(if (it == ReadType.NONE) R.drawable.ic_baseline_bookmark_border_24 else R.drawable.ic_baseline_bookmark_24)
    bookmark.setText(it.stringRes)
}

// Después — observa libraryId (Int)
observe(viewModel.libraryId) { libraryId ->
    bookmark.setIconResource(if (libraryId == 0) R.drawable.ic_baseline_bookmark_border_24 else R.drawable.ic_baseline_bookmark_24)
    val libraries = this@MainActivity.getLibraries()
    val selectedLibrary = libraries.firstOrNull { it.id == libraryId }
    val displayName = selectedLibrary?.title ?: getString(R.string.type_none)
    bookmark.text = displayName
}

// Antes — popup con ReadType entries
bookmark.setOnClickListener { view ->
    view.popupMenu(
        ReadType.entries.map { it.prefValue to it.stringRes },
        selectedItemId = viewModel.readState.value?.prefValue
    ) {
        viewModel.bookmark(itemId)
    }
}

// Después — bottom dialog con librerías dinámicas
bookmark.setOnClickListener { view ->
    val context = view.context ?: return@setOnClickListener
    val libraries = context.getLibraries()
    val allOptions = listOf(context.getString(R.string.type_none)) + libraries.map { it.title }
    val currentLibraryId = viewModel.libraryId.value ?: 0
    val selectedIndex = if (currentLibraryId == 0) 0 else libraries.indexOfFirst { it.id == currentLibraryId } + 1

    context.showBottomDialog(
        allOptions,
        selectedIndex = selectedIndex,
        context.getString(R.string.bookmark), false, {}
    ) { selected ->
        if (selected == 0) {
            viewModel.bookmark(0)
        } else {
            val selectedLibrary = libraries.getOrNull(selected - 1) ?: return@showBottomDialog
            viewModel.bookmark(selectedLibrary.id)
        }
    }
}
```

---

#### 3. `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultFragment.kt`

Se migra el botón de bookmark en `ResultFragment` del sistema `ReadType` al sistema de librerías dinámicas. Ahora observa `viewModel.libraryId` en lugar de `viewModel.readState`:

```kotlin
// Nuevo import
import com.lagradost.quicknovel.getLibraries

// Antes — lista fija con ReadType
resultBookmark.setOnClickListener { view ->
    context.showBottomDialog(
        ReadType.entries.map { context.getString(it.stringRes) },
        selectedIndex = ReadType.entries.map { it.prefValue }.indexOf(viewModel.readState.value?.prefValue),
        context.getString(R.string.bookmark), false, {}
    ) { selected ->
        viewModel.bookmark(ReadType.entries[selected].prefValue)
    }
}

// Después — lista dinámica de librerías con opción "Ninguno"
resultBookmark.setOnClickListener { view ->
    val context = view.context ?: return@setOnClickListener
    val libraries = context.getLibraries()
    val allOptions = listOf(context.getString(R.string.type_none)) + libraries.map { it.title }
    val currentLibraryId = viewModel.libraryId.value ?: 0
    val selectedIndex = if (currentLibraryId == 0) 0 else libraries.indexOfFirst { it.id == currentLibraryId } + 1

    context.showBottomDialog(
        allOptions,
        selectedIndex = selectedIndex,
        context.getString(R.string.bookmark), false, {}
    ) { selected ->
        if (selected == 0) {
            viewModel.bookmark(0)
        } else {
            val selectedLibrary = libraries.getOrNull(selected - 1) ?: return@showBottomDialog
            viewModel.bookmark(selectedLibrary.id)
        }
    }
}

// Antes — observaba ReadType
observe(viewModel.readState) { state ->
    binding.resultBookmark.setText(if (state == ReadType.NONE) R.string.bookmark else state.stringRes)
    binding.resultBookmark.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
        if (state == ReadType.NONE) R.drawable.ic_baseline_bookmark_border_24 else R.drawable.ic_baseline_bookmark_24)
}

// Después — observa libraryId (Int)
observe(viewModel.libraryId) { libraryId ->
    val context = binding.root.context
    val libraries = context.getLibraries()
    val selectedLibrary = libraries.firstOrNull { it.id == libraryId }
    val displayName = selectedLibrary?.title ?: context.getString(R.string.bookmark)
    binding.resultBookmark.text = displayName
    binding.resultBookmark.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
        if (libraryId == 0) R.drawable.ic_baseline_bookmark_border_24 else R.drawable.ic_baseline_bookmark_24)
}
```

---

#### 4. `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultViewModel.kt`

Se agrega `libraryId: MutableLiveData<Int>` como nueva fuente de verdad del estado del bookmark. Se actualiza `bookmark()` y `setState()` para usar el `id` de librería directamente en lugar del enum `ReadType`:

```kotlin
// Nuevo campo
var libraryId: MutableLiveData<Int> = MutableLiveData<Int>(0)

// Antes — postValue con ReadType
readState.postValue(ReadType.fromSpinner(state))

// Después — postValue con Int directo
libraryId.postValue(state)

// Antes — leía y convertía con ReadType
private fun setState(tid: Int) {
    readState.postValue(
        ReadType.fromSpinner(
            getKey(RESULT_BOOKMARK_STATE, tid.toString())
        )
    )
}

// Después — lee el id directamente (incluye TRASH = 6)
private fun setState(tid: Int) {
    val currentLibraryId = getKey<Int>(RESULT_BOOKMARK_STATE, tid.toString()) ?: 0
    libraryId.postValue(currentLibraryId)
}
```

---

## Resumen de archivos afectados

| Archivo | PR #23 | Commit "Parche Bookmark" |
|---------|--------|--------------------------|
| `app/src/main/java/com/lagradost/quicknovel/DataStore.kt` | ✅ Agrega `DefaultLibrary`, `DEFAULT_LIBRARIES`, helpers CRUD | ✅ Reordena IDs, agrega `TRASH` |
| `app/src/main/java/com/lagradost/quicknovel/BookDownloader2.kt` | ✅ Usa `getLibraries()` en lugar de lista hardcodeada | — |
| `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadViewModel.kt` | ✅ Reemplaza `readList` por `libraries()` | — |
| `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadFragment.kt` | ✅ Corrige error de compilación en tabs | — |
| `app/src/main/java/com/lagradost/quicknovel/MainActivity.kt` | — | ✅ Migra bookmark a `libraryId` + `showBottomDialog` |
| `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultFragment.kt` | — | ✅ Migra bookmark a `libraryId` + lista dinámica |
| `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultViewModel.kt` | — | ✅ Agrega `libraryId`, elimina uso de `ReadType` en estado |


---

# LibreriasII - Guia completa con codigo (recuperacion total)

Este documento esta pensado para reconstruir TODO el sistema de librerias/bookmark unificadas sin tener que volver a diseñar codigo desde cero.

## Cambios desde v3

- `LibrarySection` cambia de menu contextual a acciones directas por icono en cada fila.
- Librerias base (`READING`, `ON_HOLD`, `PLAN_TO_READ`, `COMPLETED`, `DROPPED`) muestran solo icono de lapiz (`Rename`).
- Librerias editables muestran 3 iconos: lapiz (`Rename`), merge y borrar (`Delete`).
- `getLibraryChapterCount(id)` queda disponible en `DataStore.kt` como helper opcional (no usado por `LibrarySectionFragment`).
- La UI de fila se actualiza en `item_library_section.xml`.

## Cambios desde v5

- Se elimina el badge/contador de novelas de la pantalla de gestion (`LibrarySectionFragment`).
- Se elimina `sectionCount` TextView del layout `item_library_section.xml`.
- Se elimina `chapterCounts`/`countsByLibraryId` del `LibrarySectionAdapter`.
- `submitList` acepta solo `List<DefaultLibrary>` (sin mapa de conteos).
- Se elimina calculo de `novelCounts` en `refresh()` de `LibrarySectionFragment`.
- `getLibraryBookmarkCount` sigue disponible en `DataStore.kt` y se sigue usando como guard de seguridad en `deleteLibrary` y `showDeleteDialog`.

## 1) Alcance funcional final

- Una sola instancia de gestion: `LibrarySectionFragment`.
- Las 5 librerias base son: `READING`, `ON_HOLD`, `PLAN_TO_READ`, `COMPLETED`, `DROPPED`.
- Esas 5 siempre deben existir.
- En esas 5, `editable = false`.
- En esas 5, solo se puede cambiar `title`.
- En esas 5, `key` no se toca porque queda para legacy.
- Selector de bookmark en:
  - `ResultFragment`
  - popup rapido de `MainActivity`
- En ambos selectores:
  - lista `None + librerias` (solo nombres, sin contadores)
  - icono `+` a la izquierda del titulo en la misma fila
  - click en `+` abre `navigation_library_section`
  - en popup rapido (`MainActivity`), el `+` cierra tambien el popup corto antes de navegar
- En `LibrarySectionFragment` (pantalla de gestion):
  - las acciones son iconos directos por fila (sin `PopupMenu`)
  - NO se muestra contador de novelas por libreria
- CRUD (crear/renombrar/unir/eliminar) solo en la pantalla unica de gestion.
- En las 5 librerias base (`editable = false`), solo se muestra icono `Rename` (lapiz).
- En librerias editables, se muestran 3 iconos: `Rename` (lapiz), `Merge`, `Delete`.
- Tocar la fila de una built-in ejecuta `Rename` directo.
- Todas las acciones CRUD en `LibrarySection` van envueltas en manejo de errores para evitar cierre de app.
- Create/Rename/Merge/Delete se ejecutan despues de cerrar el dialog (`post`) para evitar crashes de callback/re-render.
- Seguridad: una libreria solo se puede eliminar si esta vacia (sin bookmarks asignados).
- El campo de nombre no debe usar label flotante arriba; el hint debe quedarse dentro del campo.

## 2) Mapa de archivos

### Archivos NUEVOS

1. `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionFragment.kt`
2. `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionAdapter.kt`
3. `app/src/main/res/layout/bottom_selection_dialog_with_action.xml`
4. `app/src/main/res/layout/sort_bottom_single_choice_with_count.xml`
5. `app/src/main/res/layout/fragment_library_section.xml`
6. `app/src/main/res/layout/item_library_section.xml`
7. `app/src/main/res/layout/dialog_add_folder.xml`
8. `app/src/main/res/drawable/library_count_circle.xml`

### Archivos MODIFICADOS

1. `app/src/main/java/com/lagradost/quicknovel/DataStore.kt`
2. `app/src/main/java/com/lagradost/quicknovel/util/SingleSelectionHelper.kt`
3. `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultFragment.kt`
4. `app/src/main/java/com/lagradost/quicknovel/MainActivity.kt`
5. `app/src/main/res/navigation/mobile_navigation.xml`
6. `app/src/main/res/xml/settings.xml`
7. `app/src/main/java/com/lagradost/quicknovel/ui/settings/SettingsFragment.kt`
8. `app/src/main/res/values/strings.xml`
9. `app/src/main/res/values-es/strings.xml`

---

## 3) Codigo exacto por cada modificacion

## 3.1 `DataStore.kt` (modelo + CRUD + reasignacion segura)

> Asegura estos bloques dentro de `app/src/main/java/com/lagradost/quicknovel/DataStore.kt`.

```kotlin
data class DefaultLibrary(
    val id: Int,
    val key: String,
    val title: String,
    val editable: Boolean = true,
    val position: Int = 0
)

val DEFAULT_LIBRARIES: List<DefaultLibrary> = listOf(
    DefaultLibrary(5, "READING",       "Reading",      editable = false, position = 1),
    DefaultLibrary(4, "ON_HOLD",       "On hold",      editable = false, position = 2),
    DefaultLibrary(1, "PLAN_TO_READ",  "Plan to read", editable = false, position = 3),
    DefaultLibrary(3, "COMPLETED",     "Completed",    editable = false, position = 4),
    DefaultLibrary(2, "DROPPED",       "Dropped",      editable = false, position = 5),
)

private val BUILT_IN_LIBRARY_KEYS = DEFAULT_LIBRARIES.map { it.key }.toSet()
```

```kotlin
fun Context.getLibraries(): List<DefaultLibrary> {
    val stored = try {
        val json = with(DataStore) { this@getLibraries.getSharedPrefs().getString(LIBRARIES_KEY, null) }
        json?.let { DataStore.mapper.readerForListOf(DefaultLibrary::class.java).readValue<List<DefaultLibrary>>(it) }
    } catch (_: Exception) {
        null
    }

    if (stored == null) return DEFAULT_LIBRARIES.sortedBy { it.position }

    val builtIns = DEFAULT_LIBRARIES.map { default ->
        val persisted = stored.firstOrNull { it.key == default.key || it.id == default.id }
        default.copy(title = persisted?.title ?: default.title)
    }
    val custom = stored.filterNot { storedLib ->
        DEFAULT_LIBRARIES.any { it.key == storedLib.key || it.id == storedLib.id }
    }

    return (builtIns + custom).sortedBy { it.position }
}

fun Context.saveLibraries(libs: List<DefaultLibrary>) {
    require(libs.map { it.id }.distinct().size == libs.size) {
        "Library list contains duplicate ids."
    }
    val sorted = libs.sortedBy { it.position }
    with(DataStore) { this@saveLibraries.setKey(LIBRARIES_KEY, sorted) }
}

fun Context.addLibrary(newLib: DefaultLibrary) {
    val current = getLibraries().toMutableList()
    require(current.none { it.id == newLib.id }) {
        "A library with id ${newLib.id} already exists."
    }
    current.add(newLib)
    saveLibraries(current)
}

fun Context.updateLibrary(updated: DefaultLibrary) {
    val current = getLibraries().toMutableList()
    val index = current.indexOfFirst { it.id == updated.id }
    require(index >= 0) { "No library with id ${updated.id} found." }
    val existing = current[index]
    val isBuiltIn = existing.key in BUILT_IN_LIBRARY_KEYS
    if (isBuiltIn) {
        require(updated.key == existing.key) { "Library key cannot change." }
        current[index] = existing.copy(title = updated.title)
    } else {
        require(existing.editable) { "Library '${existing.title}' is not editable." }
        current[index] = updated
    }
    saveLibraries(current)
}

fun Context.deleteLibrary(id: Int) {
    val current = getLibraries().toMutableList()
    val target = current.find { it.id == id }
    require(target != null) { "No library with id $id found." }
    require(target.editable) { "Library '${target.title}' is not editable." }
    require(getLibraryBookmarkCount(id) == 0) {
        "Library '${target.title}' still has bookmarked novels. Move/remove them before deleting."
    }
    current.removeAll { it.id == id }
    saveLibraries(current)
}

fun Context.getLibraryBookmarkCount(id: Int): Int {
    val stateKeys = with(DataStore) { this@getLibraryBookmarkCount.getKeys(RESULT_BOOKMARK_STATE) }
    var count = 0
    stateKeys.forEach { key ->
        val state = with(DataStore) { this@getLibraryBookmarkCount.getKey<Int>(key) } ?: return@forEach
        if (state == id) count++
    }
    return count
}

fun Context.getLibraryChapterCount(id: Int): Int {
    val stateKeys = with(DataStore) { this@getLibraryChapterCount.getKeys(RESULT_BOOKMARK_STATE) }
    var totalChapters = 0
    stateKeys.forEach { key ->
        val state = with(DataStore) { this@getLibraryChapterCount.getKey<Int>(key) } ?: return@forEach
        if (state != id) return@forEach

        val bookKey = key.replaceFirst(RESULT_BOOKMARK_STATE, RESULT_BOOKMARK)
        val book = with(DataStore) { this@getLibraryChapterCount.getKey<ResultCached>(bookKey) } ?: return@forEach
        totalChapters += book.currentTotalChapters.coerceAtLeast(0)
    }
    return totalChapters
}

fun Context.reassignLibraryBookmarks(sourceId: Int, targetId: Int = 0) {
    require(sourceId != targetId) { "sourceId and targetId must be different." }
    if (targetId != 0) {
        require(getLibraries().any { it.id == targetId }) { "No target library with id $targetId found." }
    }

    val stateKeys = with(DataStore) { this@reassignLibraryBookmarks.getKeys(RESULT_BOOKMARK_STATE) }
    stateKeys.forEach { key ->
        val current = with(DataStore) { this@reassignLibraryBookmarks.getKey<Int>(key) } ?: return@forEach
        if (current == sourceId) {
            with(DataStore) { this@reassignLibraryBookmarks.setKey(key, targetId) }
        }
    }
}

fun Context.mergeLibraries(sourceId: Int, targetId: Int) {
    require(sourceId != targetId) { "sourceId and targetId must be different." }
    val source = getLibraries().firstOrNull { it.id == sourceId }
    require(source != null) { "No source library with id $sourceId found." }
    require(source.editable) { "Library '${source.title}' is not editable." }

    reassignLibraryBookmarks(sourceId, targetId)
    deleteLibrary(sourceId)
}
```

## 3.2 `SingleSelectionHelper.kt` (selector con accion header)

Archivo: `app/src/main/java/com/lagradost/quicknovel/util/SingleSelectionHelper.kt`

Import requerido:

```kotlin
import androidx.annotation.DrawableRes
```

Funcion exacta a agregar:

```kotlin
/** Bottom-sheet single-choice selector with an optional header action icon (left side). */
fun Context.showBottomDialogWithHeaderAction(
    items: List<String>,
    selectedIndex: Int,
    name: String,
    @DrawableRes actionIconRes: Int,
    actionContentDescription: String,
    showApply: Boolean,
    dismissCallback: () -> Unit,
    onHeaderActionClick: (Dialog) -> Unit,
    callback: (Int) -> Unit,
) {
    val dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
    dialog.setContentView(R.layout.bottom_selection_dialog_with_action)
    dialog.show()

    val actionView = dialog.findViewById<ImageButton>(R.id.header_action)
    actionView?.setImageResource(actionIconRes)
    actionView?.contentDescription = actionContentDescription
    actionView?.setOnClickListener {
        onHeaderActionClick(dialog)
    }

    showDialog(
        dialog,
        items,
        listOf(selectedIndex),
        name,
        showApply,
        false,
        { callback.invoke(it.first()) },
        dismissCallback
    )
}
```

## 3.3 `ResultFragment.kt` (selector bookmark con `+`)

Archivo: `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultFragment.kt`

Import:

```kotlin
import com.lagradost.quicknovel.util.SingleSelectionHelper.showBottomDialogWithHeaderAction
```

Reemplazar bloque del click de `resultBookmark` por:

```kotlin
resultBookmark.setOnClickListener { view ->
    val context = view.context ?: return@setOnClickListener
    val libraries = context.getLibraries()
    val allOptions = listOf(context.getString(R.string.type_none)) + libraries.map { it.title }
    val currentLibraryId = viewModel.libraryId.value ?: 0
    val selectedIndex = if (currentLibraryId == 0) 0 else libraries.indexOfFirst { it.id == currentLibraryId } + 1

    context.showBottomDialogWithHeaderAction(
        allOptions,
        selectedIndex = selectedIndex,
        name = context.getString(R.string.bookmark),
        actionIconRes = R.drawable.ic_baseline_add_24,
        actionContentDescription = context.getString(R.string.library_manager_title),
        showApply = false,
        dismissCallback = {},
        onHeaderActionClick = { dialog ->
            dialog.dismiss()
            activity.navigate(R.id.navigation_library_section)
        }
    ) { selected ->
        if (selected == 0) {
            viewModel.bookmark(0)
        } else {
            val selectedLibrary = libraries.getOrNull(selected - 1) ?: return@showBottomDialogWithHeaderAction
            viewModel.bookmark(selectedLibrary.id)
        }
    }
}
```

## 3.4 `MainActivity.kt` (popup rapido con mismo selector)

Archivo: `app/src/main/java/com/lagradost/quicknovel/MainActivity.kt`

Import:

```kotlin
import com.lagradost.quicknovel.util.SingleSelectionHelper.showBottomDialogWithHeaderAction
```

Dentro de `bookmark.setOnClickListener` del preview popup, usar:

```kotlin
context.showBottomDialogWithHeaderAction(
    allOptions,
    selectedIndex = selectedIndex,
    name = context.getString(R.string.bookmark),
    actionIconRes = R.drawable.ic_baseline_add_24,
    actionContentDescription = context.getString(R.string.library_manager_title),
    showApply = false,
    dismissCallback = {},
    onHeaderActionClick = { dialog ->
        dialog.dismiss()
        hidePreviewPopupDialog()
        this@MainActivity.navigate(R.id.navigation_library_section)
    }
) { selected ->
    if (selected == 0) {
        viewModel.bookmark(0)
    } else {
        val selectedLibrary = libraries.getOrNull(selected - 1)
            ?: return@showBottomDialogWithHeaderAction
        viewModel.bookmark(selectedLibrary.id)
    }
}
```

## 3.5 Navigation

Archivo: `app/src/main/res/navigation/mobile_navigation.xml`

Agregar este fragment:

```xml
<fragment
    android:id="@+id/navigation_library_section"
    android:name="com.lagradost.quicknovel.ui.library.LibrarySectionFragment"
    android:label="@string/library_manager_title"
    android:layout_height="match_parent"
    tools:layout="@layout/fragment_library_section">
</fragment>
```

## 3.6 Settings XML

Archivo: `app/src/main/res/xml/settings.xml`

Agregar este `Preference` en categoria general:

```xml
<Preference
    app:icon="@drawable/ic_baseline_collections_bookmark_24"
    android:key="@string/library_manager_key"
    android:title="@string/library_manager_title" />
```

## 3.7 SettingsFragment

Archivo: `app/src/main/java/com/lagradost/quicknovel/ui/settings/SettingsFragment.kt`

Agregar listener:

```kotlin
getPref(R.string.library_manager_key)?.setOnPreferenceClickListener {
    activity.navigate(R.id.navigation_library_section)
    true
}
```

## 3.8 Strings (EN)

Archivo: `app/src/main/res/values/strings.xml`

Agregar:

```xml
<string name="library_manager_key" translatable="false">library_manager_key</string>
<string name="library_manager_title">Manage libraries</string>
<string name="library_add">Add library</string>
<string name="library_create">Create library</string>
<string name="library_name_hint">Library name</string>
<string name="library_rename">Rename</string>
<string name="library_merge">Merge into</string>
<string name="library_delete">Delete</string>
<string name="library_delete_message">All books in this library will move to None.</string>
<string name="library_delete_empty_only_message">This library can be deleted only when it is empty.</string>
<string name="library_editable">Editable</string>
<string name="library_fixed">Fixed</string>
<string name="library_rename_only">Rename only</string>
```

## 3.9 Strings (ES)

Archivo: `app/src/main/res/values-es/strings.xml`

Agregar:

```xml
<string name="library_manager_key" translatable="false">library_manager_key</string>
<string name="library_manager_title">Gestionar bibliotecas</string>
<string name="library_add">Agregar biblioteca</string>
<string name="library_create">Crear biblioteca</string>
<string name="library_name_hint">Nombre de la biblioteca</string>
<string name="library_rename">Renombrar</string>
<string name="library_merge">Unir en</string>
<string name="library_delete">Eliminar</string>
<string name="library_delete_message">Todos los libros de esta biblioteca pasaran a Ninguno.</string>
<string name="library_delete_empty_only_message">Esta biblioteca solo se puede eliminar cuando este vacia.</string>
<string name="library_editable">Editable</string>
<string name="library_fixed">Fija</string>
<string name="library_rename_only">Solo renombrar</string>
```

---

## 4) Codigo completo de archivos NUEVOS (copiar/pegar)

## 4.1 `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionAdapter.kt`

```kotlin
package com.lagradost.quicknovel.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.quicknovel.DEFAULT_LIBRARIES
import com.lagradost.quicknovel.DefaultLibrary
import com.lagradost.quicknovel.databinding.ItemLibrarySectionBinding

class LibrarySectionAdapter(
    private val onRenameClick: (DefaultLibrary) -> Unit,
    private val onMergeClick: (DefaultLibrary) -> Unit,
    private val onDeleteClick: (DefaultLibrary) -> Unit,
) : RecyclerView.Adapter<LibrarySectionAdapter.ViewHolder>() {

    private val items = mutableListOf<DefaultLibrary>()
    private val builtInKeys = DEFAULT_LIBRARIES.map { it.key }.toSet()

    fun submitList(newItems: List<DefaultLibrary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLibrarySectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isBuiltIn = item.key in builtInKeys
        val canRename = isBuiltIn || item.editable
        val canMergeDelete = !isBuiltIn && item.editable
        holder.binding.sectionName.text = item.title

        holder.binding.actionRename.visibility = if (canRename) View.VISIBLE else View.GONE
        holder.binding.actionMerge.visibility = if (canMergeDelete) View.VISIBLE else View.GONE
        holder.binding.actionDelete.visibility = if (canMergeDelete) View.VISIBLE else View.GONE

        holder.binding.actionRename.setOnClickListener { onRenameClick(item) }
        holder.binding.actionMerge.setOnClickListener { onMergeClick(item) }
        holder.binding.actionDelete.setOnClickListener { onDeleteClick(item) }

        // Built-in rows are rename-only, so tapping the row acts as a direct rename shortcut.
        holder.itemView.setOnClickListener {
            if (isBuiltIn) onRenameClick(item)
        }

        holder.itemView.setOnLongClickListener { false }
    }

    inner class ViewHolder(val binding: ItemLibrarySectionBinding) : RecyclerView.ViewHolder(binding.root)
}
```

## 4.2 `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionFragment.kt`

```kotlin
package com.lagradost.quicknovel.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lagradost.quicknovel.CommonActivity.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lagradost.quicknovel.DefaultLibrary
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.addLibrary
import com.lagradost.quicknovel.databinding.FragmentLibrarySectionBinding
import com.lagradost.quicknovel.deleteLibrary
import com.lagradost.quicknovel.getLibraries
import com.lagradost.quicknovel.getLibraryBookmarkCount
import com.lagradost.quicknovel.mergeLibraries
import com.lagradost.quicknovel.updateLibrary

class LibrarySectionFragment : Fragment() {
    private var _binding: FragmentLibrarySectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LibrarySectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibrarySectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.libraryToolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        adapter = LibrarySectionAdapter(
            onRenameClick = ::showRenameDialog,
            onMergeClick = ::showMergeDialog,
            onDeleteClick = ::showDeleteDialog,
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        binding.fabAddFolder.setOnClickListener { showCreateDialog() }
        refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun refresh() {
        val ctx = requireContext()
        adapter.submitList(ctx.getLibraries())
    }

    private inline fun postLibraryAction(crossinline action: () -> Unit) {
        val root = _binding?.root ?: return
        root.post {
            runLibraryAction(action)
        }
    }

    private inline fun runLibraryAction(crossinline action: () -> Unit) {
        try {
            action()
            showToast(R.string.done)
        } catch (t: Throwable) {
            showToast(t.message ?: getString(R.string.error_loading))
        }
    }

    private fun showCreateDialog() {
        val inputView = layoutInflater.inflate(R.layout.dialog_add_folder, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_create)
            .setView(inputView)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val editText = inputView.findViewById<EditText>(R.id.editFolderName)
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    val context = requireContext()
                    dialog.dismiss()
                    postLibraryAction {
                        val current = context.getLibraries()
                        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
                        val nextPosition = (current.maxOfOrNull { it.position } ?: 0) + 1
                        context.addLibrary(
                            DefaultLibrary(
                                id = nextId,
                                key = "CUSTOM_$nextId",
                                title = title,
                                editable = true,
                                position = nextPosition
                            )
                        )
                        refresh()
                    }
                    return@setPositiveButton
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRenameDialog(item: DefaultLibrary) {
        val inputView = layoutInflater.inflate(R.layout.dialog_add_folder, null)
        inputView.findViewById<EditText>(R.id.editFolderName).setText(item.title)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_rename)
            .setView(inputView)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val editText = inputView.findViewById<EditText>(R.id.editFolderName)
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    dialog.dismiss()
                    postLibraryAction {
                        requireContext().updateLibrary(item.copy(title = title))
                        refresh()
                    }
                    return@setPositiveButton
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMergeDialog(item: DefaultLibrary) {
        val context = requireContext()
        val targetCandidates = context.getLibraries().filter { it.id != item.id }
        if (targetCandidates.isEmpty()) return

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.library_merge)
            .setItems(targetCandidates.map { it.title }.toTypedArray()) { dialog, which ->
                val target = targetCandidates.getOrNull(which) ?: return@setItems
                dialog.dismiss()
                postLibraryAction {
                    context.mergeLibraries(item.id, target.id)
                    refresh()
                }
            }
            .show()
    }

    private fun showDeleteDialog(item: DefaultLibrary) {
        val inUse = requireContext().getLibraryBookmarkCount(item.id)
        if (inUse > 0) {
            showToast("${item.title}: $inUse bookmark(s). Empty it before delete.")
            return
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(R.string.library_delete)
        builder.setMessage(R.string.library_delete_message)
        builder.setPositiveButton(R.string.delete) { dialog, _ ->
            dialog.dismiss()
            postLibraryAction {
                requireContext().deleteLibrary(item.id)
                refresh()
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

}
```

## 4.3 `app/src/main/res/layout/bottom_selection_dialog_with_action.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="?attr/colorSurfaceContainerLow">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginTop="14dp"
        android:layout_marginBottom="6dp"
        android:paddingStart="8dp"
        android:paddingEnd="8dp">

        <ImageButton
            android:id="@+id/header_action"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:padding="8dp"
            android:tint="?attr/colorOnSurface"
            tools:src="@drawable/ic_baseline_add_24"
            tools:contentDescription="Manage" />

        <TextView
            android:id="@+id/text1"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:paddingStart="4dp"
            android:paddingEnd="8dp"
            android:textStyle="bold"
            android:textSize="20sp"
            android:textColor="?attr/colorOnSurface"
            tools:text="Bookmark" />
    </LinearLayout>

    <ListView
        android:id="@+id/listview1"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="60dp"
        android:paddingTop="10dp"
        tools:listitem="@layout/sort_bottom_single_choice" />

    <LinearLayout
        android:id="@+id/apply_btt_holder"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:layout_gravity="bottom"
        android:layout_marginTop="-60dp"
        android:gravity="bottom|end"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/apply_btt"
            style="@style/WhiteButton"
            android:layout_width="wrap_content"
            android:layout_gravity="center_vertical|end"
            android:text="@string/sort_apply" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/cancel_btt"
            style="@style/BlackButton"
            android:layout_width="wrap_content"
            android:layout_gravity="center_vertical|end"
            android:text="@string/sort_cancel" />
    </LinearLayout>
</LinearLayout>
```

## 4.4 `app/src/main/res/layout/fragment_library_section.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/colorSurface">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/libraryToolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="?attr/colorSurface"
        android:minHeight="?attr/actionBarSize"
        android:title="@string/library_manager_title"
        android:titleTextColor="?attr/colorOnSurface"
        app:navigationIcon="@drawable/ic_baseline_arrow_back_24"
        app:navigationIconTint="?attr/colorOnSurface" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingStart="12dp"
        android:paddingTop="8dp"
        android:paddingEnd="12dp"
        android:paddingBottom="96dp"
        app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddFolder"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end|bottom"
        android:layout_margin="20dp"
        android:contentDescription="@string/library_add"
        app:srcCompat="@drawable/ic_baseline_add_24"
        app:backgroundTint="?attr/colorPrimary"
        app:tint="?attr/colorOnPrimary" />
</LinearLayout>
```

## 4.5 `app/src/main/res/layout/item_library_section.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="6dp"
    app:cardBackgroundColor="?attr/colorSurfaceContainer"
    app:cardElevation="0dp"
    app:strokeColor="?attr/colorOutlineVariant"
    app:strokeWidth="1dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="12dp"
        android:background="?attr/selectableItemBackground">

        <TextView
            android:id="@+id/sectionName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="?attr/colorOnSurface" />

        <ImageButton
            android:id="@+id/actionRename"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginEnd="2dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/library_rename"
            android:padding="6dp"
            android:src="@drawable/ic_baseline_edit_24"
            app:tint="?attr/colorOnSurface" />

        <ImageButton
            android:id="@+id/actionMerge"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginEnd="2dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/library_merge"
            android:padding="6dp"
            android:src="@drawable/ic_baseline_autorenew_24"
            app:tint="?attr/colorOnSurface" />

        <ImageButton
            android:id="@+id/actionDelete"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/library_delete"
            android:padding="6dp"
            android:src="@drawable/ic_baseline_delete_outline_24"
            app:tint="?attr/colorError" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## 4.6 `app/src/main/res/layout/dialog_add_folder.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:hintEnabled="false"
        app:placeholderText="@string/library_name_hint"
        app:boxBackgroundMode="filled">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editFolderName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/library_name_hint"
            android:inputType="textCapWords" />
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

## 4.7 `app/src/main/res/drawable/library_count_circle.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/colorPrimaryContainer" />
    <stroke
        android:width="1dp"
        android:color="?attr/colorOutlineVariant" />
</shape>
```

---

## 5) Secuencia de reconstruccion (orden recomendado)

1. Implementar `DataStore.kt` (modelo + CRUD + merge + reasignacion).
2. Crear selector con header action (`SingleSelectionHelper.kt` + `bottom_selection_dialog_with_action.xml`).
3. Integrar selector en `ResultFragment` y `MainActivity`.
4. Crear `LibrarySectionFragment` + `LibrarySectionAdapter` + layouts/drawable.
5. Registrar navigation (`mobile_navigation.xml`).
6. Conectar Settings (`settings.xml` + `SettingsFragment.kt`).
7. Agregar strings EN/ES.
8. QA manual.

## 6) QA manual obligatorio

1. `ResultFragment` -> bookmark -> `+` visible a la izquierda del titulo.
2. Popup rapido (`MainActivity`) -> bookmark -> mismo `+`.
3. `+` desde ambos -> abre `navigation_library_section`.
3.1. El selector de bookmark muestra solo los nombres, sin contadores.
3.2. desde popup rapido, no debe quedar superpuesto el popup corto al abrir la instancia de librerias.
4. Crear libreria y volver -> aparece en ambos selectores.
5. Renombrar -> nombre actualizado en ambos selectores.
5.1. En `READING`, `ON_HOLD`, `PLAN_TO_READ`, `COMPLETED` y `DROPPED` solo debe verse el icono lapiz.
5.2. En esas 5, los iconos `Merge` y `Delete` no deben aparecer.
6. En librerias editables, deben verse los 3 iconos (`Rename`, `Merge`, `Delete`) y ejecutar accion directa.
7. En `LibrarySectionFragment`, las acciones son iconos directos (lapiz, merge, delete). No se muestra contador de novelas.
8. Merge A->B -> items de A pasan a B.
8.1. Merge no debe cerrar la app; debe cerrar el dialog, aplicar cambios y mostrar toast de exito.
9. Eliminar custom con contenido -> pasa a `None` (id 0).
10. Entrar desde Settings -> misma pantalla de gestion.
11. Librerias no editables no exponen acciones de edicion fuera de `Rename`.
12. El campo de nombre no debe levantar label flotante arriba cuando ya tiene texto.

## 7) Checklist de regresion

- [ ] No hay CRUD dentro de popup de seleccion.
- [ ] Solo una instancia de gestion (LibrarySection).
- [ ] `RESULT_BOOKMARK_STATE` consistente tras delete/merge.
- [ ] `None` siempre disponible.
- [ ] UI de menu principal no alterada.
- [ ] Colores MD3 correctos (`colorOnSurface`, `colorOnSurfaceVariant`, `colorSurfaceContainer*`).

---

Version: `v6 - 2026-04-08`

---

## 8) Corrección: `loadBookmarkedNovels()` en Updates — novelas vacías y fuera de librería

### Fecha: 2026-05-11

### Problema encontrado

En la pantalla **Novel Updates** (feature de seguimiento de actualizaciones), al abrir el selector para agregar novelas a la lista de seguimiento, aparecían:

1. **Entradas vacías** — `ResultCached` con `name = ""` (datos incompletos/corruptos en SharedPrefs).
2. **Novelas que no existen en ninguna librería** — claves huérfanas en `RESULT_BOOKMARK_STATE` de novelas que fueron quitadas de la librería pero cuya clave en SharedPrefs quedó con `ReadType.NONE (0)`.

### Causa raíz

`loadBookmarkedNovels()` en `UpdatesViewModel.kt` **nunca leía el valor del estado** (`ReadType`). Solo usaba las claves de `RESULT_BOOKMARK_STATE` como índice para buscar el dato de `RESULT_BOOKMARK`, sin verificar si ese estado correspondía a una librería real:

```kotlin
// ❌ ANTES — buggy
val list = keys.mapNotNull { key ->
    val bookKey = key.replaceFirst(RESULT_BOOKMARK_STATE, RESULT_BOOKMARK)
    getKey<ResultCached>(bookKey)  // nunca verifica el ReadType
}.sortedBy { it.name }
```

`LibrarySection.setLibraryBooks()` (la pantalla de librería) hacía bien siempre:

```kotlin
// ✅ LibrarySection — correcto
val typeValue = getKey<Int>(key) ?: continue           // lee el ReadType
val type = ReadType.values().find { it.prefValue == typeValue } ?: continue  // filtra NONE(0)
```

### Tabla de IDs relevante

| ID | Significado |
|---|---|
| **0** | `ReadType.NONE` — UI informativo "sin librería", nunca es librería real |
| 1 | PLAN_TO_READ |
| 2 | DROPPED |
| 3 | COMPLETED |
| 4 | ON_HOLD |
| 5 | READING |
| **6+** | Librerías dinámicas del usuario (auto-incremento en `LibrarySectionFragment`) |

Las librerías dinámicas usan `nextId = (current.maxOfOrNull { it.id } ?: 0) + 1`, así que siempre son ≥ 6. El ID 0 (`NONE`) **nunca puede ser asignado** a ninguna librería real.

### Archivo modificado

`app/src/main/java/com/lagradost/quicknovel/ui/updates/UpdatesViewModel.kt`

### Import agregado

```kotlin
import com.lagradost.quicknovel.ui.ReadType
```

### Código corregido

```kotlin
fun loadBookmarkedNovels() {
    viewModelScope.launch(Dispatchers.IO) {
        val keys = getKeys(RESULT_BOOKMARK_STATE) ?: return@launch
        val list = keys.mapNotNull { key ->
            // Skip NONE(0) / orphan keys — any nonzero state means the novel is in a library
            // (works for both built-in ReadTypes 1-5 and custom dynamic libraries with IDs 6+)
            val stateValue = getKey<Int>(key) ?: return@mapNotNull null
            if (stateValue == ReadType.NONE.prefValue) return@mapNotNull null
            val bookKey = key.replaceFirst(RESULT_BOOKMARK_STATE, RESULT_BOOKMARK)
            val book = getKey<ResultCached>(bookKey) ?: return@mapNotNull null
            // Skip entries with blank name (incomplete/corrupted data)
            if (book.name.isBlank()) return@mapNotNull null
            book
        }.sortedBy { it.name }
        bookmarkedNovels.postValue(list)
    }
}
```

### Decisiones de diseño

- **No se usa `distinctBy { it.id }`**: una misma novela puede aparecer más de una vez si viene de distintos providers (URL diferente → mismo título pero ID de autor distinto). Eso es correcto y esperado.
- **No se hardcodean los valores 1-5**: se compara contra `ReadType.NONE.prefValue` (== 0) en lugar de `!in setOf(1,2,3,4,5)`, para que las librerías dinámicas (IDs 6+) pasen el filtro automáticamente.
- **Nombre en blanco**: `if (book.name.isBlank())` atrapa entradas con `name = ""` o solo espacios.

### Checklist de regresión adicional

- [ ] El selector de "agregar novela a updates" no muestra entradas sin nombre.
- [ ] Solo aparecen novelas que están en alguna librería (READING, PLAN_TO_READ, ON_HOLD, COMPLETED, DROPPED o librería dinámica).
- [ ] Novelas del mismo título de providers distintos aparecen como entradas separadas (correcto).
- [ ] Novelas con `ReadType.NONE` (quitadas recientemente de la librería) no aparecen.
- [ ] Librerías dinámicas (ID ≥ 6) siguen apareciendo en el selector.

Version: `v7 - 2026-05-11`

---

## Cambios desde v7 — Fix borrado de librería + crash en tabs (v8)

_(Ver `bug.md` en la raíz del proyecto para el detalle completo de estos 2 bugs.)_

### Resumen de cambios

| Archivo | Cambio |
|---|---|
| `DataStore.kt` | `deleteLibrary`: elimina `reassignLibraryBookmarks`, añade `require(count==0)` |
| `LibrarySectionFragment.kt` | `showDeleteDialog`: bloquea con diálogo-only-OK cuando `inUse>0` |
| `DownloadFragment.kt` | `tabLabels` dinámico desde `pages`, `currentItem` con `coerceIn` |
| `values/strings.xml` | Añade `library_delete_has_novels_message`, `library_delete_confirm_message` |
| `values-es/strings.xml` | Equivalentes en español + tildes corregidas |

Version: `v8 - 2026-05-12`

---

## Cambios desde v8 — Contador de novelas por librería (v9)

Muestra cuántas novelas tiene cada librería directamente en la pantalla de gestión (`LibrarySectionFragment`), entre el nombre y los botones de acción.

### Por qué cuenta correctamente ahora

`getLibraryBookmarkCount(id)` solo lee `RESULT_BOOKMARK_STATE` y compara `state == id` — no requiere que exista `ResultCached`. Esto resuelve el problema histórico donde novelas bookmarked sin cache completa no eran contadas.

| Caso | Antes (fallaba) | Ahora |
|---|---|---|
| Novela en librería legacy sin cache | No contaba ❌ | Cuenta ✓ |
| Novela en librería dinámica sin cache | No contaba ❌ | Cuenta ✓ |
| Novela con estado NONE (0) | Podía contar ❌ | Excluida ✓ |

### Archivo 1 — `app/src/main/res/layout/item_library_section.xml`

Añadir `TextView` con id `novelCount` entre el `sectionName` y el primer `ImageButton`:

```xml
<!-- Después de sectionName, antes de actionRename: -->
<TextView
    android:id="@+id/novelCount"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="8dp"
    android:textSize="13sp"
    android:textColor="?attr/colorOnSurfaceVariant"
    android:text="(0)" />
```

### Archivo 2 — `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionAdapter.kt`

```kotlin
// ANTES:
private val items = mutableListOf<DefaultLibrary>()
private val builtInKeys = DEFAULT_LIBRARIES.map { it.key }.toSet()

fun submitList(newItems: List<DefaultLibrary>) {
    items.clear()
    items.addAll(newItems)
    notifyDataSetChanged()
}
// En onBindViewHolder:
holder.binding.sectionName.text = item.title

// DESPUÉS:
private val items = mutableListOf<DefaultLibrary>()
private val counts = mutableMapOf<Int, Int>()   // libraryId → novel count
private val builtInKeys = DEFAULT_LIBRARIES.map { it.key }.toSet()

fun submitList(newItems: List<DefaultLibrary>, novelCounts: Map<Int, Int> = emptyMap()) {
    items.clear()
    items.addAll(newItems)
    counts.clear()
    counts.putAll(novelCounts)
    notifyDataSetChanged()
}
// En onBindViewHolder:
holder.binding.sectionName.text = item.title
holder.binding.novelCount.text = "(${counts[item.id] ?: 0})"
```

### Archivo 3 — `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionFragment.kt`

```kotlin
// ANTES:
private fun refresh() {
    val ctx = requireContext()
    adapter.submitList(ctx.getLibraries())
}

// DESPUÉS:
private fun refresh() {
    val ctx = requireContext()
    val libs = ctx.getLibraries()
    val counts = libs.associate { lib -> lib.id to ctx.getLibraryBookmarkCount(lib.id) }
    adapter.submitList(libs, counts)
}
```

### Resultado visual en pantalla

```
Reading         (12)   ✏️
Plan to read     (3)   ✏️
On hold          (0)   ✏️
Completed        (8)   ✏️
Dropped          (1)   ✏️
Mi librería      (7)   ✏️  🔀  🗑️
Vacía            (0)   ✏️  🔀  🗑️
```

Las librerías con `(0)` son las únicas que permiten borrado. Las demás muestran el diálogo de bloqueo.

Version: `v9 - 2026-05-12`

---

## Cambios desde v9 — Badge circular con colores del tema (v10)

El contador de novelas que antes se mostraba como `(N)` en texto plano ahora es un **círculo badge** con los colores del tema activo (Material You / MD3). Además se corrigieron imports faltantes que impedían compilar y se eliminó el modificador `inner` redundante en `ViewHolder`.

### Problema visual que se corrigió

A partir del ítem 3 Android empezaba a renderizar el texto `(N)` con un borde redondeado de sistema (bubble), lo que mezclaba ese estilo con los paréntesis. Solución: reemplazar el texto `(N)` por un `TextView` con fondo oval y colores del tema.

---

### Archivo 1 — `app/src/main/res/drawable/bg_novel_count_badge.xml` *(nuevo)*

Crear este archivo en `res/drawable/`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#FFFFFF" />
</shape>
```

> El color `#FFFFFF` es un placeholder. El color real se aplica en runtime con `ViewCompat.setBackgroundTintList()` usando el color del tema activo.

---

### Archivo 2 — `app/src/main/res/layout/item_library_section.xml`

Reemplazar el `TextView` de `novelCount`:

```xml
<!-- ANTES: -->
<TextView
    android:id="@+id/novelCount"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="8dp"
    android:textSize="13sp"
    android:textColor="?attr/colorOnSurfaceVariant"
    android:text="(0)" />

<!-- DESPUÉS: -->
<TextView
    android:id="@+id/novelCount"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="8dp"
    android:minWidth="24dp"
    android:minHeight="24dp"
    android:gravity="center"
    android:paddingStart="6dp"
    android:paddingEnd="6dp"
    android:paddingTop="3dp"
    android:paddingBottom="3dp"
    android:textSize="12sp"
    android:textStyle="bold"
    android:background="@drawable/bg_novel_count_badge"
    android:text="0" />
```

> `minWidth/Height = 24dp` garantiza forma circular con un solo dígito.
> `paddingStart/End = 6dp` da espacio para números de 2+ dígitos sin deformar el círculo.

---

### Archivo 3 — `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionAdapter.kt`

**Imports a añadir** (estaban faltando y causaban error de compilación):

```kotlin
import android.content.res.ColorStateList
import androidx.core.view.ViewCompat
import com.google.android.material.R as MatR
import com.google.android.material.color.MaterialColors
```

**Cambios en `onBindViewHolder`:**

```kotlin
// ANTES:
holder.binding.sectionName.text = item.title
holder.binding.novelCount.text = "(${counts[item.id] ?: 0})"

// DESPUÉS:
holder.binding.sectionName.text = item.title

val count = counts[item.id] ?: 0
holder.binding.novelCount.text = "$count"
val bgColor = MaterialColors.getColor(holder.itemView, MatR.attr.colorSecondaryContainer, 0)
val fgColor = MaterialColors.getColor(holder.itemView, MatR.attr.colorOnSecondaryContainer, 0)
ViewCompat.setBackgroundTintList(holder.binding.novelCount, ColorStateList.valueOf(bgColor))
holder.binding.novelCount.setTextColor(fgColor)
```

**Cambio en `ViewHolder`** — eliminar `inner` redundante:

```kotlin
// ANTES:
inner class ViewHolder(val binding: ItemLibrarySectionBinding) : RecyclerView.ViewHolder(binding.root)

// DESPUÉS:
class ViewHolder(val binding: ItemLibrarySectionBinding) : RecyclerView.ViewHolder(binding.root)
```

**Archivo completo después de los cambios:**

```kotlin
package com.lagradost.quicknovel.ui.library

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MatR
import com.google.android.material.color.MaterialColors
import com.lagradost.quicknovel.DEFAULT_LIBRARIES
import com.lagradost.quicknovel.DefaultLibrary
import com.lagradost.quicknovel.databinding.ItemLibrarySectionBinding

class LibrarySectionAdapter(
    private val onRenameClick: (DefaultLibrary) -> Unit,
    private val onMergeClick: (DefaultLibrary) -> Unit,
    private val onDeleteClick: (DefaultLibrary) -> Unit,
) : RecyclerView.Adapter<LibrarySectionAdapter.ViewHolder>() {

    private val items = mutableListOf<DefaultLibrary>()
    private val counts = mutableMapOf<Int, Int>()   // libraryId → novel count
    private val builtInKeys = DEFAULT_LIBRARIES.map { it.key }.toSet()

    fun submitList(newItems: List<DefaultLibrary>, novelCounts: Map<Int, Int> = emptyMap()) {
        items.clear()
        items.addAll(newItems)
        counts.clear()
        counts.putAll(novelCounts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLibrarySectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isBuiltIn = item.key in builtInKeys
        val canRename = isBuiltIn || item.editable
        val canMergeDelete = !isBuiltIn && item.editable
        holder.binding.sectionName.text = item.title

        val count = counts[item.id] ?: 0
        holder.binding.novelCount.text = "$count"
        val bgColor = MaterialColors.getColor(holder.itemView, MatR.attr.colorSecondaryContainer, 0)
        val fgColor = MaterialColors.getColor(holder.itemView, MatR.attr.colorOnSecondaryContainer, 0)
        ViewCompat.setBackgroundTintList(holder.binding.novelCount, ColorStateList.valueOf(bgColor))
        holder.binding.novelCount.setTextColor(fgColor)

        holder.binding.actionRename.visibility = if (canRename) View.VISIBLE else View.GONE
        holder.binding.actionMerge.visibility = if (canMergeDelete) View.VISIBLE else View.GONE
        holder.binding.actionDelete.visibility = if (canMergeDelete) View.VISIBLE else View.GONE

        holder.binding.actionRename.setOnClickListener { onRenameClick(item) }
        holder.binding.actionMerge.setOnClickListener { onMergeClick(item) }
        holder.binding.actionDelete.setOnClickListener { onDeleteClick(item) }

        holder.itemView.setOnClickListener {
            if (isBuiltIn) onRenameClick(item)
        }
        holder.itemView.setOnLongClickListener { false }
    }

    class ViewHolder(val binding: ItemLibrarySectionBinding) : RecyclerView.ViewHolder(binding.root)
}
```

---

### Resultado visual en pantalla

```
Reading         12   ✏️          ← badge circular, color colorSecondaryContainer
Plan to read     3   ✏️
On hold          0   ✏️
Completed        8   ✏️
Dropped          1   ✏️
Mi librería      7   ✏️  🔀  🗑️
Vacía            0   ✏️  🔀  🗑️
```

El badge muestra el número **sin paréntesis** sobre un círculo/píldora con:
- Fondo → `colorSecondaryContainer` del tema activo
- Texto → `colorOnSecondaryContainer` del tema activo
- Forma → oval (circular para 1–2 dígitos, píldora para ≥ 3)

### Correcciones adicionales en esta versión

| Problema | Causa | Solución |
|---|---|---|
| `Unresolved reference 'MaterialColors'` | Imports faltantes | Añadidos 4 imports |
| `Unresolved reference 'ViewCompat'` | Import faltante | Añadido `androidx.core.view.ViewCompat` |
| `Redundant 'inner' modifier` | Warning del IDE | Eliminado `inner` de `ViewHolder` |
| Badge visual incorrecto con `(N)` | Android renderizaba bubble propio | Reemplazado por oval drawable + tint |

Version: `v10 - 2026-05-12`

---

## Cambios desde v10 — Badge "Library" en tarjetas de búsqueda y provider (v11)

Muestra un badge `"Library"` sobre la portada de cada novela en las pantallas de **búsqueda global**, **Home del provider** y **página principal del provider**, indicando visualmente que esa novela ya está en alguna librería del usuario.

### Arquitectura del sistema de badges

```
Arranque app (MainActivity)
  └─► LibraryHelper.setLibraryBooks()
        ├─ Lee RESULT_BOOKMARK_STATE → filtra state != 0 (NONE)
        └─ Construye libraryTitles (Set<String>)
              ↓
        Caché en memoria (O(n) lookup, sin I/O por tarjeta)

Usuario ve tarjeta en Search / Provider Home / Main Page
  └─► Adapter.onBindContent()
        └─► FavoritesHelper.bindBadge(favoriteBadge, item.name)
              └─► LibraryHelper.getBookmarkForBook(title)
                    └─► libraryTitles.any { equals(ignoreCase=true) }
                          ↓
                    badge.isVisible = true/false

Usuario cambia bookmark en ResultFragment
  └─► ResultViewModel.bookmark(state: Int)
        ├─ setKey(RESULT_BOOKMARK_STATE, id, state)
        ├─ updateBookmarkData()
        ├─ libraryId.postValue(state)
        └─► LibraryHelper.setLibraryBooks()  ← refresca caché → badges actualizados
```

---

### Decisiones de diseño (v2 — simplificado)

- **`Set<String>` en lugar de `Map<ReadType, List<ResultCached>>`**: el badge solo necesita saber si el título existe en *alguna* librería, no en cuál.
- **History excluido**: un libro en historial no es necesariamente un bookmark deliberado.
- **Downloads excluidos**: si está descargado Y bookmarked, ya aparece vía `RESULT_BOOKMARK_STATE`.
- **Solo filtra `state != 0`**: cubre automáticamente librerías built-in (IDs 1–5) y librerías dinámicas (IDs ≥ 6).

---

### Archivos NUEVOS

#### 1. `app/src/main/java/com/lagradost/quicknovel/util/FavoritesHelper.kt`

Helper que expone el badge de librería a los adapters. Utiliza el caché en memoria de `LibraryHelper` — sin I/O por tarjeta.

```kotlin
package com.lagradost.quicknovel.util

import android.widget.TextView
import androidx.core.view.isVisible
import com.lagradost.quicknovel.LibraryHelper

object FavoritesHelper {

    /**
     * Returns "Library" if the novel title is in any real library, null otherwise.
     */
    fun getStatusForTitle(title: String): String? {
        return LibraryHelper.getBookmarkForBook(title)
    }

    /**
     * Convenience: bind a badge [TextView] that already exists in the layout.
     * Shows the badge ("Library") when the novel is bookmarked, hides it otherwise.
     */
    fun bindBadge(badgeView: TextView, novelTitle: String) {
        val status = getStatusForTitle(novelTitle)
        if (status != null) {
            badgeView.text = status
            badgeView.isVisible = true
        } else {
            badgeView.isVisible = false
        }
    }
}
```

---

### Archivos MODIFICADOS

#### 2. `app/src/main/java/com/lagradost/quicknovel/mvvm/LibrarySection.kt`

Reemplaza la lógica de `Map<ReadType, List<ResultCached>>` por un `Set<String>` plano. Elimina History y Downloads del caché.

```kotlin
package com.lagradost.quicknovel

import android.content.Context
import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.DataStore.getKey
import com.lagradost.quicknovel.DataStore.getKeys
import com.lagradost.quicknovel.util.ResultCached

object LibraryHelper {

    /**
     * Flat set of novel names assigned to ANY real library
     * (RESULT_BOOKMARK_STATE value != 0/NONE).
     */
    private var libraryTitles: Set<String> = emptySet()

    fun Context.setLibraryBooks() {
        val keys = getKeys(RESULT_BOOKMARK_STATE)
        val titles = mutableSetOf<String>()

        for (key in keys) {
            val state = getKey<Int>(key) ?: continue
            if (state == 0) continue           // NONE — not assigned to any library
            val bookKey = key.replaceFirst(RESULT_BOOKMARK_STATE, RESULT_BOOKMARK)
            val book = getKey<ResultCached>(bookKey) ?: continue
            if (book.name.isBlank()) continue
            titles.add(book.name)
        }

        libraryTitles = titles
    }

    /**
     * Returns "Library" if the title is in any library, null otherwise.
     * Used by [com.lagradost.quicknovel.util.FavoritesHelper] to show badges.
     */
    fun getBookmarkForBook(title: String): String? {
        return if (libraryTitles.any { it.equals(title, ignoreCase = true) }) "Library" else null
    }

    fun getLastReadChapterIndex(bookName: String): Int {
        val k = getKey<Int>(EPUB_CURRENT_POSITION, bookName) ?: 0
        return k + 1
    }
}
```

#### 3. `app/src/main/res/layout/search_result_grid.xml`

Agregar `TextView` badge con id `favoriteBadge` dentro del `FrameLayout`, entre `imageView` e `imageText`:

```xml
<!-- Library status badge -->
<TextView
    android:id="@+id/favoriteBadge"
    android:visibility="gone"
    tools:visibility="visible"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="top|start"
    android:layout_margin="6dp"
    android:paddingStart="6dp"
    android:paddingEnd="6dp"
    android:paddingTop="2dp"
    android:paddingBottom="2dp"
    android:background="@drawable/download_chapterprogress_bg"
    android:textColor="@android:color/white"
    tools:text="Library" />
```

#### 4. `app/src/main/res/layout/home_result_grid.xml`

Mismo `TextView` badge con id `favoriteBadge` dentro del `FrameLayout`, entre `imageView` e `imageText`.

#### 5. `app/src/main/java/com/lagradost/quicknovel/ui/search/SearchAdapter.kt`

```kotlin
// Import:
import com.lagradost.quicknovel.util.FavoritesHelper

// En onBindContent, tras: imageText.text = item.name
FavoritesHelper.bindBadge(favoriteBadge, item.name)
```

#### 6. `app/src/main/java/com/lagradost/quicknovel/ui/search/HomeChildItemAdapter.kt`

```kotlin
import com.lagradost.quicknovel.util.FavoritesHelper

// En onBindContent, tras: imageText.text = item.name
FavoritesHelper.bindBadge(favoriteBadge, item.name)
```

#### 7. `app/src/main/java/com/lagradost/quicknovel/ui/mainpage/MainAdapter.kt`

```kotlin
import com.lagradost.quicknovel.util.FavoritesHelper

// En onBindContent, tras: imageText.text = item.name
FavoritesHelper.bindBadge(favoriteBadge, item.name)
```

#### 8. `app/src/main/java/com/lagradost/quicknovel/ui/result/ResultViewModel.kt`

Refrescar el caché de librería tras cada cambio de bookmark (al final de `bookmark()`):

```kotlin
import com.lagradost.quicknovel.LibraryHelper

// Al final de fun bookmark(state: Int), fuera del loadMutex.withLock:
context?.let { ctx ->
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        with(LibraryHelper) { ctx.setLibraryBooks() }
    }
}
```

#### 9. `app/src/main/java/com/lagradost/quicknovel/MainActivity.kt`

Carga inicial del caché al arrancar la app (en `onCreate` o `onStart`):

```kotlin
// Import:
import com.lagradost.quicknovel.LibraryHelper

// En onCreate/onStart, dentro del scope de LibraryHelper:
with(LibraryHelper) { setLibraryBooks() }
```

---

### Mapa de archivos afectados

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `util/FavoritesHelper.kt` | NUEVO | Helper de badge |
| `mvvm/LibrarySection.kt` | MODIFICADO | Usa `Set<String>` en lugar de Map+ReadType |
| `res/layout/search_result_grid.xml` | MODIFICADO | Agrega `favoriteBadge` |
| `res/layout/home_result_grid.xml` | MODIFICADO | Agrega `favoriteBadge` |
| `ui/search/SearchAdapter.kt` | MODIFICADO | Llama a `bindBadge` |
| `ui/search/HomeChildItemAdapter.kt` | MODIFICADO | Llama a `bindBadge` |
| `ui/mainpage/MainAdapter.kt` | MODIFICADO | Llama a `bindBadge` |
| `ui/result/ResultViewModel.kt` | MODIFICADO | Refresca caché post-bookmark |
| `MainActivity.kt` | MODIFICADO | Carga inicial del caché |

### Resultado visual

El badge `"Library"` aparece en la esquina superior izquierda de cada portada en búsqueda/provider cuando la novela está en alguna librería. Se refresca automáticamente cada vez que el usuario cambia el estado de bookmark en `ResultFragment`.

### Checklist de regresión

- [ ] Al abrir la app, las novelas bookmarkeadas muestran el badge en búsqueda.
- [ ] Al agregar una novela a una librería y volver a búsqueda, el badge aparece.
- [ ] Al quitar una novela de la librería, el badge desaparece al volver a búsqueda.
- [ ] Novelas con `ReadType.NONE` (id = 0) NO muestran badge.
- [ ] Librerías dinámicas (ID >= 6) también muestran badge correctamente.
- [ ] History y Downloads NO producen badge.

Version: `v11 - 2026-05-13`

---

## Cambios desde v11 — Drag-to-reorder en Library Manager (v12)

Permite al usuario reordenar las librería arrastrando un ícono de manija (`≡`) en cada fila del gestor de librería (`LibrarySectionFragment`). El nuevo orden se persiste automáticamente en SharedPreferences al soltar el ítem.

### Arquitectura del sistema de reordenamiento

```
Usuario toca dragHandle (ACTION_DOWN)
  └─► holder.itemTouchHelper?.startDrag(holder)
        └─► LibraryItemTouchCallback.onMove(from, to)
              └─► adapter.moveItem(from, to)
                    ├─ items.removeAt(from)
                    ├─ items.add(to, moved)
                    └─ notifyItemMoved(from, to)   ← animación suave

Usuario suelta el ítem
  └─► LibraryItemTouchCallback.clearView()
        └─► onDragFinished()  →  persistNewOrder()
              └─► adapter.getCurrentList().mapIndexed { i, lib →
                    lib.copy(position = i + 1)
                  }
              └─► ctx.saveLibraries(reordered)     ← SharedPreferences ~1ms
```

### Decisiones de diseño

- **`isLongPressDragEnabled = false`**: el drag solo arranca desde el handle explícito, no por long-press en toda la fila.
- **Solo UP/DOWN**: `makeMovementFlags(UP or DOWN, 0)` — sin swipe.
- **`runLibraryAction` (no `postLibraryAction`)**: `clearView` ya se ejecuta en el hilo principal con la vista visible; no se necesita `.post {}`.
- **`saveLibraries` síncrono**: escritura a SharedPreferences (~1 ms), seguro en hilo principal desde el callback de drag.

---

### Archivos NUEVOS

#### 1. `app/src/main/java/com/lagradost/quicknovel/ui/library/LibraryItemTouchCallback.kt`

Callback `ItemTouchHelper.Callback` que habilita mover ítems hacia arriba/abajo. Llama a `adapter.moveItem()` durante el drag y dispara `onDragFinished()` al soltar.

```kotlin
package com.lagradost.quicknovel.ui.library

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class LibraryItemTouchCallback(
    private val adapter: LibrarySectionAdapter,
    private val onDragFinished: () -> Unit,
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swipe deshabilitado — no-op.
    }

    override fun isLongPressDragEnabled(): Boolean = false

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onDragFinished()
    }
}
```

#### 2. `app/src/main/res/drawable/ic_baseline_drag_handle_24.xml`

Ícono de manija de arrastre (`≡`): dos barras horizontales.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M20,9H4v2h16V9zM4,15h16v-2H4v2z"/>
</vector>
```

---

### Archivos MODIFICADOS

#### 3. `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionAdapter.kt`

**Imports agregados:**
```kotlin
import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
```

**Campos y métodos nuevos:**
```kotlin
private var itemTouchHelper: ItemTouchHelper? = null

fun attachTouchHelper(helper: ItemTouchHelper) {
    itemTouchHelper = helper
}

fun moveItem(from: Int, to: Int) {
    val moved = items.removeAt(from)
    items.add(to, moved)
    notifyItemMoved(from, to)
}

fun getCurrentList(): List<DefaultLibrary> = items.toList()
```

**En `onBindViewHolder` — touch listener en el drag handle:**
```kotlin
@SuppressLint("ClickableViewAccessibility")
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    // ...existing binding code...
    holder.binding.dragHandle.setOnTouchListener { _, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            itemTouchHelper?.startDrag(holder)
        }
        false
    }
}
```

#### 4. `app/src/main/java/com/lagradost/quicknovel/ui/library/LibrarySectionFragment.kt`

**Imports agregados:**
```kotlin
import androidx.recyclerview.widget.ItemTouchHelper
import com.lagradost.quicknovel.saveLibraries
```

**En `onViewCreated`, tras configurar el adapter:**
```kotlin
val touchCallback = LibraryItemTouchCallback(adapter) { persistNewOrder() }
val itemTouchHelper = ItemTouchHelper(touchCallback)
itemTouchHelper.attachToRecyclerView(binding.recyclerView)
adapter.attachTouchHelper(itemTouchHelper)
```

**Función nueva:**
```kotlin
private fun persistNewOrder() {
    val ctx = requireContext()
    val reordered = adapter.getCurrentList().mapIndexed { index, lib ->
        lib.copy(position = index + 1)
    }
    runLibraryAction {
        ctx.saveLibraries(reordered)
    }
}
```

#### 5. `app/src/main/res/layout/item_library_section.xml`

Agregar `ImageView` drag handle al inicio del `LinearLayout` (antes de `sectionName`):

```xml
<ImageView
    android:id="@+id/dragHandle"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:layout_marginEnd="4dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/drag_handle"
    android:padding="6dp"
    android:src="@drawable/ic_baseline_drag_handle_24"
    app:tint="?attr/colorOnSurfaceVariant" />
```

#### 6. `app/src/main/res/values/strings.xml`

```xml
<string name="drag_handle">Drag to reorder</string>
```

#### 7. `app/src/main/res/values-es/strings.xml`

```xml
<string name="drag_handle">Arrastrar para reordenar</string>
```

---

### Mapa de archivos afectados

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `ui/library/LibraryItemTouchCallback.kt` | NUEVO | Callback drag-to-reorder |
| `res/drawable/ic_baseline_drag_handle_24.xml` | NUEVO | Ícono manija `≡` |
| `ui/library/LibrarySectionAdapter.kt` | MODIFICADO | `moveItem`, `getCurrentList`, `attachTouchHelper`, touch listener |
| `ui/library/LibrarySectionFragment.kt` | MODIFICADO | Setup `ItemTouchHelper`, `persistNewOrder()` |
| `res/layout/item_library_section.xml` | MODIFICADO | Agrega `dragHandle` ImageView |
| `res/values/strings.xml` | MODIFICADO | `drag_handle` string EN |
| `res/values-es/strings.xml` | MODIFICADO | `drag_handle` string ES |

### Resultado visual

Cada fila del gestor de librería muestra un ícono `≡` a la izquierda. Al tocarlo y arrastrar, la fila se mueve con animación suave. Al soltar, el nuevo orden se guarda automáticamente.

### Checklist de regresión

- [ ] El drag handle aparece en cada fila del Library Manager.
- [ ] Arrastrar una librería la reordena visualmente con animación suave.
- [ ] Al cerrar y reabrir el manager, el orden persiste.
- [ ] Long-press en la fila NO inicia drag (solo el handle).
- [ ] El swipe lateral no tiene efecto.
- [ ] El orden se refleja en `position = index + 1` en SharedPreferences.

Version: `v12 - 2026-05-13`

---

## Fix v12 — `Unresolved reference 'dragHandle'` (v12.1)

### Problema

Al compilar tras aplicar v12, Android Studio reportaba:

```
e: file:///...LibrarySectionAdapter.kt:86:24 Unresolved reference 'dragHandle'.
```

**Causa**: el binding generado (`ItemLibrarySectionBinding`) no contenía la propiedad `dragHandle` porque el `ImageView` con ese id **nunca se había agregado** al XML del layout. El v12 documentó el cambio pero no lo aplicó al archivo físico.

### Archivos corregidos

#### 1. `app/src/main/res/layout/item_library_section.xml`

Se agregó el `ImageView` con `android:id="@+id/dragHandle"` al inicio del `LinearLayout`, antes del `sectionName`:

```xml
<ImageView
    android:id="@+id/dragHandle"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:layout_marginEnd="4dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/drag_handle"
    android:padding="6dp"
    android:src="@drawable/ic_baseline_drag_handle_24"
    app:tint="?attr/colorOnSurfaceVariant" />

<TextView
    android:id="@+id/sectionName"
    ...
```

#### 2. `app/src/main/res/values/strings.xml`

```xml
<string name="drag_handle">Drag to reorder</string>
```

#### 3. `app/src/main/res/values-es/strings.xml`

```xml
<string name="drag_handle">Arrastrar para reordenar</string>
```

> **Nota**: `ic_baseline_drag_handle_24.xml` ya existía en `res/drawable/` — no requirió recrearse.

### Por qué falló la primera vez

En la sesión anterior, los archivos XML se documentaron pero no se escribieron al disco. Solo se crearon los archivos Kotlin (`LibraryItemTouchCallback.kt`) y se modificó el adapter/fragment. El layout y los strings quedaron pendientes hasta este fix.

### Estado tras el fix

| Error | Estado |
|-------|--------|
| `Unresolved reference 'dragHandle'` | ✅ Resuelto — view existe en XML → binding generado correctamente |
| `drag_handle` string faltante | ✅ Resuelto — agregado a EN y ES |

Version: `v12.1 - 2026-05-13`

---

## Fix v12.2 — Reorden no persiste para librerías built-in (2026-05-13)

### Problema

Al arrastrar y reordenar librerías en el manager, la animación funcionaba correctamente y el toast de "Done" aparecía, pero al salir y volver el orden volvía al original. Las librerías en los tabs tampoco cambiaban.

**Causa raíz — `getLibraries()` en `DataStore.kt`:**

```kotlin
// ❌ ANTES — solo restauraba el title, ignoraba position
val builtIns = DEFAULT_LIBRARIES.map { default ->
    val persisted = stored.firstOrNull { it.key == default.key || it.id == default.id }
    default.copy(title = persisted?.title ?: default.title)
}
```

Las librerías built-in (Reading, Plan to read, On hold, Completed, Dropped) se reconstruyen siempre desde `DEFAULT_LIBRARIES` con posiciones hardcodeadas (1–5). Aunque `saveLibraries()` guardaba correctamente la nueva `position`, al leer con `getLibraries()` el campo `position` se descartaba y se usaba el valor por defecto. El `sortedBy { it.position }` al final siempre devolvía el orden original.

Las librerías **custom** no tenían este problema porque se leen tal cual desde SharedPreferences.

### Archivo corregido

#### `app/src/main/java/com/lagradost/quicknovel/DataStore.kt`

```kotlin
// ✅ DESPUÉS — restaura title Y position desde lo guardado
val builtIns = DEFAULT_LIBRARIES.map { default ->
    val persisted = stored.firstOrNull { it.key == default.key || it.id == default.id }
    default.copy(
        title    = persisted?.title    ?: default.title,
        position = persisted?.position ?: default.position,
    )
}
```

### Flujo completo tras el fix

```
Drag → moveItem() reordena adapter en memoria (animación suave)
Soltar → clearView() → persistNewOrder()
  └─► adapter.getCurrentList().mapIndexed { i, lib → lib.copy(position = i + 1) }
  └─► ctx.saveLibraries(reordered)  → SharedPreferences ✅

Salir y entrar al manager → getLibraries()
  ├─ built-ins: default.copy(title = ..., position = ...)  ← position restaurada ✅
  ├─ custom: leídas tal cual desde SharedPreferences       ✅
  └─► sortedBy { it.position }  → orden correcto ✅

Tabs de librería → también llaman getLibraries() → mismo orden ✅
```

### Estado tras el fix

| Síntoma | Causa | Estado |
|---------|-------|--------|
| Reorden visual funciona pero no persiste | `getLibraries()` ignoraba `position` de built-ins | ✅ Resuelto |
| Tabs mantienen orden original tras reordenar | Mismo bug — lecturas siempre volvían a posición 1–5 | ✅ Resuelto |

Version: `v12.2 - 2026-05-13`

---

## Cambios desde v12.2 — Ícono acceso directo al Library Manager (v13)

Agrega un botón de acceso directo al **Library Manager** en la barra superior de la pantalla de Downloads/Library, entre el ícono de sort y la campana de notificaciones. Un toque navega directamente a `LibrarySectionFragment` sin pasar por Settings.

### Barra resultante

```
[ 🔍 Search              ] [ ↕ ] [ 📚 ] [ 🔔 ]
                             sort  mgr   bell
```

### Por qué aquí

- La pantalla de Downloads ya muestra los tabs de librería → es el contexto natural.
- Acceso en **1 toque** vs el flujo anterior de Settings → Manage Libraries (2 pasos + navegación fuera de contexto).
- No requiere nuevos fragmentos, navegación ni archivos extra — solo un botón y un click listener.

### Ícono elegido

`ic_baseline_collections_bookmark_24` — ya existía en el proyecto. Representa colecciones/librería sin ambigüedad.

---

### Archivos MODIFICADOS

#### 1. `app/src/main/res/layout/fragment_downloads.xml`

Nuevo `MaterialButton` con id `download_library_manager_icon` insertado **antes** del botón de la campana (`download_updates_icon`):

```xml
<!-- ANTES del download_updates_icon existente -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/download_library_manager_icon"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_marginStart="4dp"
    android:contentDescription="@string/library_manager_title"
    app:icon="@drawable/ic_baseline_collections_bookmark_24" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/download_updates_icon"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_marginStart="4dp"
    android:contentDescription="@string/updates_open_screen"
    app:icon="@drawable/ic_baseline_notifications_24" />
```

#### 2. `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadFragment.kt`

Click listener agregado junto al resto de los listeners de íconos:

```kotlin
binding.downloadLibraryManagerIcon.setOnClickListener {
    activity.navigate(R.id.navigation_library_section)
}

binding.downloadUpdatesIcon.setOnClickListener {
    activity.navigate(R.id.navigation_updates)
}
```

---

### Mapa de archivos afectados

| Archivo | Tipo | Cambio |
|---------|------|--------|
| `res/layout/fragment_downloads.xml` | MODIFICADO | Agrega `download_library_manager_icon` antes de la campana |
| `ui/download/DownloadFragment.kt` | MODIFICADO | Click listener navega a `navigation_library_section` |

### Recursos reutilizados (sin cambios)

| Recurso | Descripción |
|---------|-------------|
| `ic_baseline_collections_bookmark_24` | Ya existía en `res/drawable/` |
| `library_manager_title` | Ya existía en `res/values/strings.xml` → `"Manage libraries"` |
| `R.id.navigation_library_section` | Destino ya registrado en el nav graph |

Version: `v13 - 2026-05-13`

---

## Fix v13.1 — Click listener del ícono era decorativo (2026-05-13)

### Problema

El ícono `📚` aparecía en la UI pero **no hacía nada** al tocarlo. El click listener estaba documentado en v13 pero nunca se escribió al archivo fuente `DownloadFragment.kt`.

### Archivo corregido

#### `app/src/main/java/com/lagradost/quicknovel/ui/download/DownloadFragment.kt`

Se agregó el click listener entre el de sort y el de la campana (línea ~234):

```kotlin
binding.downloadSortIcon.setOnClickListener(showSortSheet)

binding.downloadLibraryManagerIcon.setOnClickListener {
    activity.navigate(R.id.navigation_library_section)
}

binding.downloadUpdatesIcon.setOnClickListener {
    activity.navigate(R.id.navigation_updates)
}
```

### Estado

| Síntoma | Causa | Estado |
|---------|-------|--------|
| Ícono `📚` visible pero sin acción | Listener solo documentado, nunca aplicado al archivo | ✅ Resuelto |

Version: `v13.1 - 2026-05-13`

---

## Fix v13.2 — Ícono no respetaba el tema activo (2026-05-13)

### Problema

El ícono `📚` aparecía siempre **blanco** sin importar el tema (claro u oscuro). Los otros iconos del toolbar (sort, campana) sí se adaptaban al tema.

### Causa raíz

`ic_baseline_collections_bookmark_24.xml` tiene `android:tint="?attr/white"` hardcodeado en el vector. Sin `app:iconTint` explícito en el view, el tint nativo del drawable gana sobre el que aplicaría el estilo `Widget.Material3.Button.IconButton`.

### Archivo corregido

#### `app/src/main/res/layout/fragment_downloads.xml`

```xml
<!-- ANTES: -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/download_library_manager_icon"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_marginStart="4dp"
    android:contentDescription="@string/library_manager_title"
    app:icon="@drawable/ic_baseline_collections_bookmark_24" />

<!-- DESPUÉS: -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/download_library_manager_icon"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_marginStart="4dp"
    android:contentDescription="@string/library_manager_title"
    app:icon="@drawable/ic_baseline_collections_bookmark_24"
    app:iconTint="?attr/colorOnSurfaceVariant" />
```

`?attr/colorOnSurfaceVariant` es el color estándar M3 para iconos de botones de solo-ícono. No es el color primario — es un tono neutro que cambia automáticamente entre tema claro y oscuro según lo definido en `styles.xml` del proyecto.

### Estado

| Síntoma | Causa | Estado |
|---------|-------|--------|
| Ícono siempre blanco, ignora tema | `android:tint="?attr/white"` en el drawable | ✅ Resuelto |

Version: `v13.2 - 2026-05-13`

