package com.techvisiondz.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Category article listing state holder. Loads published article cards for a
 * category slug through the existing card RPC and exposes the standard
 * loading/success/empty/error contract.
 */
class CategoryArticlesViewModel(
    private val repository: ArticleRepository,
    private val slug: String,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ArticleCard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArticleCard>>> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val articles = repository.getArticlesByCategory(slug, defaultLanguage)
                if (articles.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(articles)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load articles")
            }
        }
    }

    companion object {
        fun factory(slug: String, repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CategoryArticlesViewModel(repository = repository, slug = slug) }
        }
    }
}