package com.techvisiondz.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.Category
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Categories discovery state holder. Loads the localized category list from the
 * existing backend through an [ArticleRepository] and exposes the standard
 * loading/success/empty/error contract with retry.
 */
class CategoryViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = "ar",
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val categories = repository.getCategories(defaultLanguage)
                if (categories.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(categories)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load categories")
            }
        }
    }

    companion object {
        fun factory(repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CategoryViewModel(repository) }
        }
    }
}