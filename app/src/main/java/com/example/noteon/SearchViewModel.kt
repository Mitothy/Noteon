package com.example.noteon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchManager: SearchManager
) : ViewModel() {

    val searchState: StateFlow<SearchState> = searchManager.searchState

    fun search(
        query: String,
        viewType: MainActivity.ViewType,
        folderId: Long,
        isIntelligentSearch: Boolean = false
    ) {
        viewModelScope.launch {
            searchManager.performSearch(query, viewType, folderId, isIntelligentSearch)
        }
    }

    fun cancelSearch() {
        searchManager.cancelSearch()
    }

    class Factory(
        private val preferencesManager: PreferencesManager,
        private val intelligentSearchService: IntelligentSearchService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                val searchManager = SearchManager(
                    preferencesManager,
                    intelligentSearchService
                )
                return SearchViewModel(searchManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}