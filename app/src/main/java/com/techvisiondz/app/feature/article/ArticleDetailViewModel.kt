package com.techvisiondz.app.feature.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SavedArticleRepository
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.feature.auth.toAuthError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Save/bookmark state for the article details screen.
 *
 * @property saved True when the shown article is bookmarked by the current user.
 * @property isSaving True while a save/unsave request is in flight (prevents
 *   duplicate taps).
 * @property error The last save/unsave failure to surface, or null.
 */
data class ArticleSaveUiState(
    val saved: Boolean = false,
    val isSaving: Boolean = false,
    val error: AuthError? = null,
)

/**
 * Article details screen state holder.
 *
 * Loads a single published article by slug through the existing
 * `get_published_article_by_slug` RPC (via [ArticleRepository.getArticle]) and
 * exposes the same [UiState] loading/empty/error contract used by the home feed.
 * A null result from the repository (backend reports "not published / missing")
 * becomes the empty state.
 *
 * When a [SavedArticleRepository] is provided, the screen also tracks the
 * article's bookmark state ([ArticleSaveUiState]) and toggles it without ever
 * disturbing the article content. The repository is optional so the article
 * screen remains fully usable in tests and contexts without bookmarks.
 */
class ArticleDetailViewModel(
    private val repository: ArticleRepository,
    private val slug: String,
    private val savedArticleRepository: SavedArticleRepository? = null,
    private val defaultLanguage: String = "ar",
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Article>>(UiState.Loading)
    val uiState: StateFlow<UiState<Article>> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow(ArticleSaveUiState())
    val saveState: StateFlow<ArticleSaveUiState> = _saveState.asStateFlow()

    private var currentArticleId: String? = null

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
                    currentArticleId = article.id
                    refreshSaveState(article.id)
                    UiState.Success(article)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load article")
            }
        }
    }

    /**
     * Refreshes the saved state for the shown article. Failures (for example a
     * vanished session) leave the bookmark as "not saved" without breaking the
     * article — the requested action itself surfaces any error.
     */
    fun refreshSaveState(articleId: String) {
        if (savedArticleRepository == null) return
        viewModelScope.launch {
            val saved = try {
                savedArticleRepository.isArticleSaved(articleId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            _saveState.update { it.copy(saved = saved) }
        }
    }

    /** Saves or unsaves the shown article; duplicate taps while busy are ignored. */
    fun toggleSave() {
        val state = _saveState.value
        if (state.isSaving) return
        val articleId = currentArticleId ?: return
        val repository = savedArticleRepository ?: return
        val targetSaved = !state.saved

        _saveState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                if (targetSaved) {
                    repository.saveArticle(articleId)
                } else {
                    repository.unsaveArticle(articleId)
                }
                _saveState.value = _saveState.value.copy(saved = targetSaved, isSaving = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveState.value = _saveState.value.copy(isSaving = false, error = e.toAuthError())
            }
        }
    }

    companion object {
        fun factory(
            slug: String,
            repository: ArticleRepository,
            savedArticleRepository: SavedArticleRepository? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArticleDetailViewModel(repository = repository, slug = slug, savedArticleRepository = savedArticleRepository) }
        }
    }
}