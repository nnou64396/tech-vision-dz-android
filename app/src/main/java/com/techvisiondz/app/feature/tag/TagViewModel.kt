package com.techvisiondz.app.feature.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.Tag
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tags discovery state holder. Loads the localized tags from the existing
 * backend through an [ArticleRepository] and exposes the standard
 * loading/success/empty/error contract with retry.
 */
class TagViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = "ar",
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Tag>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Tag>>> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val tags = repository.getTags(defaultLanguage)
                if (tags.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(tags)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load tags")
            }
        }
    }

    companion object {
        fun factory(repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TagViewModel(repository) }
        }
    }
}