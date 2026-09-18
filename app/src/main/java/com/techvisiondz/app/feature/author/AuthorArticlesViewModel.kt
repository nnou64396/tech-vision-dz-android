package com.techvisiondz.app.feature.author

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
 * Author article listing state holder. Loads published article cards for an
 * author slug through the existing card RPC (one request, no pagination) and
 * exposes the standard loading/success/empty/error contract. [refresh] reloads
 * the list behind the pull-to-refresh indicator without discarding shown
 * content on failure.
 */
class AuthorArticlesViewModel(
    private val repository: ArticleRepository,
    private val slug: String,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ArticleCard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArticleCard>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** One-shot message for a failed [refresh] while articles were already shown. */
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val articles = repository.getArticlesByAuthor(slug, defaultLanguage)
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

    /** Reloads the author's articles in place behind a refresh indicator. */
    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val articles = repository.getArticlesByAuthor(slug, defaultLanguage)
                _uiState.value = if (articles.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(articles)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the shown articles; surface the failure as a message.
                _refreshError.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun consumeRefreshError() {
        _refreshError.value = null
    }

    companion object {
        fun factory(slug: String, repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthorArticlesViewModel(repository = repository, slug = slug) }
        }
    }
}