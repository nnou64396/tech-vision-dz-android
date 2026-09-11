package com.techvisiondz.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.ArticleCard
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
 * an [ArticleRepository]. A newer query cancels the previous request and the
 * IME Search action ([onSearchSubmit]) bypasses the debounce.
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

    private val _uiState = MutableStateFlow<UiState<List<ArticleCard>>>(UiState.Empty())
    val uiState: StateFlow<UiState<List<ArticleCard>>> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var debounceJob: Job? = null

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

    fun onClear() {
        searchJob?.cancel()
        debounceJob?.cancel()
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
        searchJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                val results = repository.searchArticles(normalized, defaultLanguage)
                if (results.isEmpty()) {
                    UiState.Empty()
                } else {
                    UiState.Success(results)
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