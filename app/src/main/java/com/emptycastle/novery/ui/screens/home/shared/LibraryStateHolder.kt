package com.emptycastle.novery.ui.screens.home.shared

import com.emptycastle.novery.data.repository.LibraryItem
import com.emptycastle.novery.data.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared state holder for library data that multiple ViewModels need access to.
 * This avoids duplicating library observation logic across ViewModels.
 */
object LibraryStateHolder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val libraryRepository = RepositoryProvider.getLibraryRepository()

    private val _libraryUrls = MutableStateFlow<Set<String>>(emptySet())
    val libraryUrls: StateFlow<Set<String>> = _libraryUrls.asStateFlow()

    private val _libraryItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems.asStateFlow()

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            libraryRepository.observeLibrary().collect { items ->
                _libraryItems.value = items
                _libraryUrls.value = items.map { it.novel.url }.toSet()
            }
        }
    }

    fun isInLibrary(novelUrl: String): Boolean {
        return _libraryUrls.value.contains(novelUrl)
    }

    fun getLibraryItem(novelUrl: String): LibraryItem? {
        return _libraryItems.value.find { it.novel.url == novelUrl }
    }
}