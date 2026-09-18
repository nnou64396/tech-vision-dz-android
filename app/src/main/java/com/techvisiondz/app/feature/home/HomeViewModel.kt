package com.techvisiondz.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.repository.ARTICLE_PAGE_SIZE
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
 *
 * The feed is browsed incrementally: [loadHome] loads the first page only,
 * [loadMore] appends the next page (guarded against concurrent/duplicate
 * requests) and [refresh] reloads the first page without discarding the
 * currently shown content. Results are deduplicated by article id so an
 * overlapping page never renders a repeated card.
 */
class HomeViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeContent>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** One-shot message for a failed [refresh] while content was already shown. */
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    private var loadMoreJob: Job? = null

    /** Synchronous guard so two triggers cannot start a second page request. */
    private var loadMoreInFlight = false

    init {
        loadHome()
    }

    fun loadHome() {
        loadMoreJob?.cancel()
        loadMoreInFlight = false
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val articles = repository.getHomeFeed(defaultLanguage, offset = 0, limit = ARTICLE_PAGE_SIZE)
                if (articles.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(HomeContent(articles = articles, hasMore = articles.size >= ARTICLE_PAGE_SIZE))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to load articles")
            }
        }
    }

    /**
     * Appends the next page of the published feed. Safe to call repeatedly: a
     * pending "load more" is never duplicated, and when there are no more
     * results nothing further is requested.
     */
    fun loadMore() {
        val snapshot = (_uiState.value as? UiState.Success)?.data ?: return
        if (!snapshot.hasMore || snapshot.isLoadingMore || loadMoreInFlight) return

        loadMoreInFlight = true
        loadMoreJob = viewModelScope.launch {
            val loadingState = UiState.Success(snapshot.copy(isLoadingMore = true, loadMoreError = null))
            _uiState.value = loadingState
            try {
                val next = repository.getHomeFeed(
                    defaultLanguage,
                    offset = snapshot.articles.size,
                    limit = ARTICLE_PAGE_SIZE,
                )
                if (next.isEmpty()) {
                    _uiState.value = UiState.Success(snapshot.copy(isLoadingMore = false, hasMore = false))
                } else {
                    val merged = (snapshot.articles + next).distinctBy { it.id }
                    _uiState.value = UiState.Success(
                        HomeContent(
                            articles = merged,
                            hasMore = next.size >= ARTICLE_PAGE_SIZE,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Success(
                    snapshot.copy(isLoadingMore = false, loadMoreError = e.message ?: "Unable to load more articles"),
                )
            } finally {
                loadMoreInFlight = false
                // If this job was cancelled mid-flight (e.g. by a refresh), make
                // sure the loading-more flag is not left stuck. Only restore the
                // state we ourselves set so a newer state is never overwritten.
                val current = _uiState.value
                if (current is UiState.Success && current.data.isLoadingMore) {
                    _uiState.value = UiState.Success(current.data.copy(isLoadingMore = false))
                }
            }
        }
    }

    /**
     * Reloads the first page and discards any accumulated pagination state.
     * Whether content is already shown determines the visual result: with
     * content the list is replaced in place behind the pull-to-refresh
     * indicator; without content a full load is performed instead.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        loadMoreJob?.cancel()
        loadMoreInFlight = false
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val articles = repository.getHomeFeed(defaultLanguage, offset = 0, limit = ARTICLE_PAGE_SIZE)
                _uiState.value = if (articles.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(HomeContent(articles = articles, hasMore = articles.size >= ARTICLE_PAGE_SIZE))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the currently shown feed; surface the failure as a message.
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    repository = SupabaseArticleRepository(),
                )
            }
        }

        fun factory(repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}