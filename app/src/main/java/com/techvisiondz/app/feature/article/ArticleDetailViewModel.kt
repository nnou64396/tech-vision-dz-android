package com.techvisiondz.app.feature.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Article details screen state holder.
 *
 * Loads a single published article by slug through the existing
 * `get_published_article_by_slug` RPC (via [ArticleRepository.getArticle]) and
 * exposes the same [UiState] loading/empty/error contract used by the home feed.
 * A null result from the repository (backend reports "not published / missing")
 * becomes the empty state.
 */
class ArticleDetailViewModel(
    private val repository: ArticleRepository,
    private val slug: String,
    private val defaultLanguage: String = "ar",
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Article>>(UiState.Loading)
    val uiState: StateFlow<UiState<Article>> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    fun loadArticle() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val article = repository.getArticle(slug, defaultLanguage)
                if (article == null) {
                    UiState.Empty()
                } else {
                    UiState.Success(article)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load article")
            }
        }
    }

    companion object {
        fun factory(slug: String, repository: ArticleRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ArticleDetailViewModel(repository = repository, slug = slug) }
            }
    }
}