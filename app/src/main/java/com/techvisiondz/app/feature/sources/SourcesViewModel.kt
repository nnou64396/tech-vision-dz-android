package com.techvisiondz.app.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.Source
import com.techvisiondz.app.core.data.model.SourceCategory
import com.techvisiondz.app.core.data.repository.DefaultSourceRepository
import com.techvisiondz.app.core.data.repository.SourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SourcesUiState(
    val sources: List<Source> = emptyList(),
    val filteredSources: List<Source> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: SourceCategory = SourceCategory.ALL,
    val categories: List<SourceCategory> = listOf(
        SourceCategory.ALL,
        SourceCategory.SMARTPHONES_ANDROID,
        SourceCategory.TECHNOLOGY_NEWS,
        SourceCategory.INTERNATIONAL_NEWS,
        SourceCategory.COMPUTER_HARDWARE,
    ),
)

class SourcesViewModel(
    private val sourceRepository: SourceRepository = DefaultSourceRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        loadSources()
    }

    fun loadSources() {
        val allSources = sourceRepository.getSources().filter { it.category != SourceCategory.SOFTWARE_APPLICATIONS }
        _uiState.update { current ->
            val safeCategory = if (current.selectedCategory == SourceCategory.SOFTWARE_APPLICATIONS) SourceCategory.ALL else current.selectedCategory
            val filtered = filterSources(allSources, current.searchQuery, safeCategory)
            current.copy(sources = allSources, filteredSources = filtered, selectedCategory = safeCategory)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = filterSources(current.sources, query, current.selectedCategory)
            current.copy(searchQuery = query, filteredSources = filtered)
        }
    }

    fun onCategorySelected(category: SourceCategory) {
        val safeCategory = if (category == SourceCategory.SOFTWARE_APPLICATIONS) SourceCategory.ALL else category
        _uiState.update { current ->
            val filtered = filterSources(current.sources, current.searchQuery, safeCategory)
            current.copy(selectedCategory = safeCategory, filteredSources = filtered)
        }
    }

    private fun filterSources(
        sources: List<Source>,
        query: String,
        category: SourceCategory,
    ): List<Source> {
        val trimmedQuery = query.trim().lowercase()
        return sources.filter { source ->
            if (source.category == SourceCategory.SOFTWARE_APPLICATIONS) return@filter false
            val matchesCategory = category == SourceCategory.ALL || source.category == category
            val matchesQuery = trimmedQuery.isEmpty() ||
                source.name.lowercase().contains(trimmedQuery) ||
                source.descriptionAr.lowercase().contains(trimmedQuery) ||
                source.descriptionEn.lowercase().contains(trimmedQuery)
            matchesCategory && matchesQuery
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SourcesViewModel(DefaultSourceRepository())
            }
        }
    }
}
