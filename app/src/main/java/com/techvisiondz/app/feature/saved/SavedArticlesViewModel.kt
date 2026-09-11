package com.techvisiondz.app.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.SavedArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseSavedArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Saved Articles screen state holder.
 *
 * Loads the signed-in user's bookmarks through [SavedArticleRepository] and
 * exposes the same [UiState] contract as the other listing screens (loading /
 * empty / error / success). Unauthenticated sessions fail the load with an
 * authentication error; the navigation host guards the route, so this is a
 * defensive fallback rather than a normal path.
 */
class SavedArticlesViewModel(
    private val savedArticleRepository: SavedArticleRepository,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ArticleCard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ArticleCard>>> = _uiState.asStateFlow()

    /** True once an initial load has settled; resume-refresh only reloads after that. */
    var hasLoadedOnce: Boolean = false
        private set

    private var loadInFlight: Boolean = false

    init {
        loadSaved()
    }

    /**
     * Full load used for the initial entry and retry: shows the full-screen
     * loading state while the fetch is underway.
     */
    fun loadSaved() = loadInternal(showLoading = true)

    /**
     * Background refresh used when the screen resumes. The existing content
     * stays on screen while the fetch runs; the settled result replaces it.
     */
    fun refresh() = loadInternal(showLoading = false)

    private fun loadInternal(showLoading: Boolean) {
        if (loadInFlight) return
        loadInFlight = true
        if (showLoading) _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = fetchSaved()
            hasLoadedOnce = true
            loadInFlight = false
        }
    }

    private suspend fun fetchSaved(): UiState<List<ArticleCard>> = try {
        val articles = savedArticleRepository.getSavedArticles(defaultLanguage)
        if (articles.isEmpty()) {
            UiState.Empty()
        } else {
            UiState.Success(articles)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UiState.Error(e.message ?: "Unable to load saved articles")
    }

    companion object {
        fun factory(
            savedArticleRepository: SavedArticleRepository = SupabaseSavedArticleRepository(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SavedArticlesViewModel(savedArticleRepository) }
        }
    }
}