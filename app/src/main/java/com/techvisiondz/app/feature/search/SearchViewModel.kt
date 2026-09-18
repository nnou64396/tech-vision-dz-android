package com.techvisiondz.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.ARTICLE_PAGE_SIZE
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Search screen state holder.
 *
 * Debounces [onQueryChange] input, normalizes the query (trim, Arabic
 * letter-alef unification, and removal of characters that would corrupt the
 * PostgREST `or(...)` logical filter), then searches published articles through
 * an [ArticleRepository]. A newer query cancels the previous request, the IME
 * Search action ([onSearchSubmit]) bypasses the debounce, and results are
 * browsed incrementally: [loadMore] appends the next page of the current query
 * (never for a stale query) and [refresh] reloads the first page in place.
 */
class SearchViewModel(
    private val repository: ArticleRepository,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
    private val debounceMillis: Long = 350L,
    private val minQueryLength: Int = 2,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Normalized query backing the current results; null until a valid search runs. */
    private val _activeQuery = MutableStateFlow<String?>(null)
    val activeQuery: StateFlow<String?> = _activeQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<SearchContent>>(UiState.Empty())
    val uiState: StateFlow<UiState<SearchContent>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** One-shot message for a failed [refresh] while results were already shown. */
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var debounceJob: Job? = null

    /** Synchronous guard so two triggers cannot start a second-page request. */
    private var loadMoreInFlight = false

    /** Guards against redundant identical searches (e.g. double IME submit). */
    private var lastSearched: String? = null

    fun onQueryChange(value: String) {
        _query.value = value
        scheduleSearch(value)
    }

    fun onSearchSubmit() {
        debounceJob?.cancel()
        launchSearch(_query.value, force = true)
    }

    fun retry() {
        val target = _activeQuery.value ?: _query.value.takeIf { it.isNotBlank() } ?: return
        launchSearch(target, force = true)
    }

    /** Appends the next page of the currently active query, if any. */
    fun loadMore() {
        val snapshot = (_uiState.value as? UiState.Success)?.data ?: return
        if (!snapshot.hasMore || snapshot.isLoadingMore || loadMoreInFlight) return

        val target = _activeQuery.value ?: return
        val currentQuery = _query.value
        if (normalizeSearchQuery(currentQuery) != target) return

        loadMoreInFlight = true
        loadMoreJob = viewModelScope.launch {
            val loadingState = UiState.Success(snapshot.copy(isLoadingMore = true, loadMoreError = null))
            _uiState.value = loadingState
            try {
                val next = repository.searchArticles(
                    query = target,
                    languageCode = defaultLanguage,
                    offset = snapshot.articles.size,
                    limit = ARTICLE_PAGE_SIZE,
                )
                if (next.isEmpty()) {
                    _uiState.value = UiState.Success(snapshot.copy(isLoadingMore = false, hasMore = false))
                } else {
                    val merged = (snapshot.articles + next).distinctBy { it.id }
                    _uiState.value = UiState.Success(
                        SearchContent(
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
                val current = _uiState.value
                if (current is UiState.Success && current.data.isLoadingMore) {
                    _uiState.value = UiState.Success(current.data.copy(isLoadingMore = false))
                }
            }
        }
    }

    /** Reloads the first page of the active query (no-op without results). */
    fun refresh() {
        if (_isRefreshing.value) return
        val target = _activeQuery.value ?: return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        loadMoreInFlight = false
        _isRefreshing.value = true
        searchJob = viewModelScope.launch {
            try {
                val results = repository.searchArticles(
                    query = target,
                    languageCode = defaultLanguage,
                    offset = 0,
                    limit = ARTICLE_PAGE_SIZE,
                )
                _uiState.value = if (results.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(SearchContent(articles = results, hasMore = results.size >= ARTICLE_PAGE_SIZE))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the currently shown results; surface the failure as a message.
                _refreshError.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun consumeRefreshError() {
        _refreshError.value = null
    }

    fun onClear() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        debounceJob?.cancel()
        loadMoreInFlight = false
        lastSearched = null
        _query.value = ""
        _activeQuery.value = null
        _uiState.value = UiState.Empty()
    }

    private fun scheduleSearch(value: String) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(debounceMillis)
            runSearch(value)
        }
    }

    private fun runSearch(value: String) {
        val normalized = normalizeSearchQuery(value)
        if (normalized.length < minQueryLength) {
            lastSearched = null
            _activeQuery.value = null
            _uiState.value = UiState.Empty()
            return
        }
        launchSearch(normalized, force = false)
    }

    private fun launchSearch(value: String, force: Boolean) {
        val normalized = normalizeSearchQuery(value)
        if (normalized.length < minQueryLength) {
            lastSearched = null
            _activeQuery.value = null
            _uiState.value = UiState.Empty()
            return
        }
        if (!force && normalized == lastSearched) return

        lastSearched = normalized
        _activeQuery.value = normalized
        searchJob?.cancel()
        loadMoreJob?.cancel()
        loadMoreInFlight = false
        searchJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                val results = repository.searchArticles(
                    query = normalized,
                    languageCode = defaultLanguage,
                    offset = 0,
                    limit = ARTICLE_PAGE_SIZE,
                )
                if (results.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(SearchContent(articles = results, hasMore = results.size >= ARTICLE_PAGE_SIZE))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unable to search articles")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    repository = SupabaseArticleRepository(),
                )
            }
        }

        fun factory(repository: ArticleRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}

private val unsafeFilterCharacters = Regex("""[*%,()"\\]""")
private val whitespaceSequence = Regex("""\s+""")

/**
 * Prepares raw search input for the PostgREST ILIKE filter:
 *  - trims surrounding whitespace and collapses interior runs of whitespace
 *  - unifies Arabic alef variants (أ/إ/آ → ا)
 *  - strips `* % , ( ) " \` which would corrupt the `or(...)` logical filter
 *    (supabase-kt 3.2.0 does not quote values inside logical expressions)
 */
internal fun normalizeSearchQuery(query: String): String = query
    .replace('\u0623', '\u0627') // أ -> ا
    .replace('\u0625', '\u0627') // إ -> ا
    .replace('\u0622', '\u0627') // آ -> ا
    .trim()
    .replace(unsafeFilterCharacters, "")
    .replace(whitespaceSequence, " ")