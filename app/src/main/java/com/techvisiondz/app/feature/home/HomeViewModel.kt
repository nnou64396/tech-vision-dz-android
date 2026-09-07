package com.techvisiondz.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Home screen state holder.
 *
 * Follows the MVVM pattern used across the app:
 *   - exposes an immutable [StateFlow] for the UI
 *   - owns the loading/success/error/empty decision via [UiState]
 *   - loads real published articles from the existing Supabase backend through
 *     an [ArticleRepository]
 */
class HomeViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = "ar",
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeContent>> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val articles = repository.getHomeFeed(defaultLanguage)
                if (articles.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(HomeContent(articles = articles))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load articles")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    repository = SupabaseArticleRepository(),
                )
            }
        }
    }
}