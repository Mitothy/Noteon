package com.example.noteon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SearchState {
    data object Idle : SearchState()
    data class Filtering(val query: String) : SearchState()
    data class IntelligentSearching(val query: String) : SearchState()
    data class Error(val message: String) : SearchState()
    data class Results(val notes: List<Note>, val query: String) : SearchState()
    data class Empty(val query: String) : SearchState()
}

class SearchManager(private val preferencesManager: PreferencesManager) {
    private val intelligentSearchService = IntelligentSearchService.getInstance()
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    suspend fun performSearch(
        query: String,
        viewType: MainActivity.ViewType,
        folderId: Long,
        isIntelligentSearch: Boolean = false
    ) {
        if (query.isBlank()) {
            updateState(SearchState.Results(getInitialNotes(viewType, folderId), query))
            return
        }

        // Get base notes for the current view
        val baseNotes = getInitialNotes(viewType, folderId)

        if (isIntelligentSearch && preferencesManager.isIntelligentSearchEnabled()) {
            performIntelligentSearch(query, baseNotes)
        } else {
            performBasicSearch(query, baseNotes)
        }
    }

    private fun getInitialNotes(viewType: MainActivity.ViewType, folderId: Long): List<Note> {
        return when (viewType) {
            MainActivity.ViewType.ALL_NOTES -> DataHandler.getAllNotes().filter { it.isNormal() }
            MainActivity.ViewType.FAVORITES -> DataHandler.getAllNotes().filter { it.isFavorite() }
            MainActivity.ViewType.FOLDER -> DataHandler.getNotesInFolder(folderId)
            MainActivity.ViewType.TRASH -> DataHandler.getAllNotes().filter { it.isTrashed() }
        }
    }

    private suspend fun performIntelligentSearch(query: String, notes: List<Note>) {
        try {
            updateState(SearchState.IntelligentSearching(query))
            val searchResults = intelligentSearchService.searchNotes(query, notes)
            handleSearchResults(searchResults, query)
        } catch (e: Exception) {
            // Fallback to basic search on error
            performBasicSearch(query, notes)
        }
    }

    private fun performBasicSearch(query: String, notes: List<Note>) {
        updateState(SearchState.Filtering(query))
        val filteredNotes = notes.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
        }
        handleSearchResults(filteredNotes, query)
    }

    private fun handleSearchResults(results: List<Note>, query: String) {
        if (results.isEmpty()) {
            updateState(SearchState.Empty(query))
        } else {
            updateState(SearchState.Results(results, query))
        }
    }

    fun cancelSearch() {
        updateState(SearchState.Idle)
    }

    private fun updateState(newState: SearchState) {
        _searchState.value = newState
    }
}