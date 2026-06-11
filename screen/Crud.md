# Implementation Plan: Dynamic Library System (Full Code Details)

This document outlines the specific code changes and new files required to implement the fully dynamic, English-only library system in Novery.

## User Review Required

> [!IMPORTANT]
> - **v10 -> v11 Migration**: Adds `library_status` table and maps existing novels to it.
> - **System Guards**: The category with `key = "READING"` is protected from deletion.
> - **Persistence**: Internal `keys` ensure system logic works even if titles are renamed.

## Proposed Changes

### 1. Database & Persistence Layer

#### [NEW] [LibraryStatusEntity.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/data/local/entity/LibraryStatusEntity.kt)
```kotlin
@Entity(tableName = "library_status")
data class LibraryStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String? = null,
    val title: String,
    val position: Int = 0,
    val canDelete: Boolean = true
)
```

#### [NEW] [LibraryStatusDao.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/data/local/dao/LibraryStatusDao.kt)
```kotlin
@Dao
interface LibraryStatusDao {
    @Query("SELECT * FROM library_status ORDER BY position ASC")
    fun getAllFlow(): Flow<List<LibraryStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: LibraryStatusEntity): Long

    @Query("DELETE FROM library_status WHERE id = :id")
    suspend fun delete(id: Int)
}
```

#### [NovelDatabase.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/data/local/NovelDatabase.kt)
Implement **Migration 10 -> 11**.
```kotlin
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `library_status` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT, `title` TEXT NOT NULL, `position` INTEGER NOT NULL, `canDelete` INTEGER NOT NULL)")

        // Seed Defaults
        val defaults = listOf("READING" to "Reading", "COMPLETED" to "Completed", "ON_HOLD" to "On Hold", "PLAN_TO_READ" to "Plan to Read", "DROPPED" to "Dropped", "SPICY" to "Spicy")
        defaults.forEachIndexed { index, (key, title) ->
            val canDelete = if (key == "READING") 0 else 1
            database.execSQL("INSERT INTO library_status (`key`, title, position, canDelete) VALUES ('$key', '$title', ${index + 1}, $canDelete)")
        }

        database.execSQL("ALTER TABLE library ADD COLUMN statusId INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE library SET statusId = (SELECT id FROM library_status WHERE `key` = readingStatus)")
    }
}
```

---

### 2. UI Layer (Compose)

#### [NEW] [LibraryStatusSelector.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/components/LibraryStatusSelector.kt)
Dynamic BottomSheet for selecting a novel's status.
```kotlin
@Composable
fun LibraryStatusSelector(
    currentStatusId: Int,
    statuses: List<LibraryStatusEntity>,
    onStatusSelected: (Int) -> Unit,
    onManageClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Set Status", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            statuses.forEach { status ->
                StatusItem(
                    title = status.title,
                    isSelected = status.id == currentStatusId,
                    onClick = {
                        onStatusSelected(status.id)
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            TextButton(onClick = { onManageClick(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Libraries")
            }
        }
    }
}
```

#### [NEW] [LibraryManagerScreen.kt](file:///home/hamunoz/StudioProjects/Novery/app/src/main/java/com/emptycastle/novery/ui/screens/library/manager/LibraryManagerScreen.kt)
The management interface with reordering support.
```kotlin
@Composable
fun LibraryManagerScreen(
    viewModel: LibraryManagerViewModel,
    onBack: () -> Unit
) {
    val statuses by viewModel.statuses.collectAsStateWithLifecycle(emptyList())
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.moveStatus(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Scaffold(
        topBar = { ManagerTopBar(onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, "Add Library")
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(statuses, key = { it.id }) { status ->
                ReorderableItem(reorderState, key = status.id) { isDragging ->
                    StatusManagerItem(
                        status = status,
                        isDragging = isDragging,
                        onRename = { viewModel.showRenameDialog(status) },
                        onMerge = { viewModel.showMergeDialog(status) },
                        onDelete = { viewModel.showDeleteDialog(status) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
                        )
                    )
                }
            }
        }
    }

    // Dialogs for CRUD
    if (viewModel.activeDialog == DialogType.ADD) AddLibraryDialog(...)
    if (viewModel.activeDialog == DialogType.RENAME) RenameLibraryDialog(...)
    if (viewModel.activeDialog == DialogType.MERGE) MergeLibraryDialog(...)
}
```

---

## Verification Plan

### Manual
1. **Initial Seed**: Verify all 6 categories appear in English.
2. **Rename**: Rename "Reading" to "Favorites" and verify it's still undeletable.
3. **Reorder**: Use the drag handles in `LibraryManagerScreen` to reorder tabs.
4. **Merge**: Merge categories and ensure novels move correctly.
