package com.techvisiondz.app.feature.author

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.Author
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Authors discovery state holder. Loads the active localized authors from the
 * existing backend through an [ArticleRepository] and exposes the standard
 * loading/success/empty/error contract with retry.
 */
class AuthorViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Author>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Author>>> = _uiState.asStateFlow()

    init {
        loadAuthors()
    }

    fun loadAuthors() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val authors = repository.getAuthors(defaultLanguage)
                if (authors.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(authors)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load authors")
            }
        }
    }

    companion object {
        fun factory(repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthorViewModel(repository) }
        }
    }
}