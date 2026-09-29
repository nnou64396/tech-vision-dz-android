package com.techvisiondz.app.feature.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.ArticleLocales
import com.techvisiondz.app.core.data.model.ArticleTranslationRef
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
 *
 * Multilingual content is discovered per article rather than configured: after
 * the first successful load the screen reads which language versions are
 * actually published and exposes them through
 * [Article.availableLanguages] and [selectLanguage]. A single-language article
 * reports one entry and the UI shows no selector.
 */
class ArticleDetailViewModel(
    private val repository: ArticleRepository,
    private val slug: String,
    private val savedArticleRepository: SavedArticleRepository? = null,
    private val defaultLanguage: String = AppConfig.DEFAULT_LANGUAGE_CODE,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Article>>(UiState.Loading)
    val uiState: StateFlow<UiState<Article>> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow(ArticleSaveUiState())
    val saveState: StateFlow<ArticleSaveUiState> = _saveState.asStateFlow()

    private val _relatedArticles = MutableStateFlow<UiState<List<ArticleCard>>>(UiState.Empty())
    val relatedArticles: StateFlow<UiState<List<ArticleCard>>> = _relatedArticles.asStateFlow()

    private val _isSwitchingLanguage = MutableStateFlow(false)

    /**
     * True while a language switch is in flight. The current article stays
     * visible and readable throughout — this only dims the selector — so a slow
     * or failing switch never blanks the screen.
     */
    val isSwitchingLanguage: StateFlow<Boolean> = _isSwitchingLanguage.asStateFlow()

    private var currentArticleId: String? = null

    /**
     * Locale → slug for the article currently open, discovered from
     * `article_translations` on first load. Empty means "no other version is
     * published", which is the common single-language case and hides the
     * selector.
     */
    private var translationSlugs: Map<String, String> = emptyMap()

    init {
        loadArticle()
    }

    fun loadArticle() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val article = repository.getArticle(slug, defaultLanguage)
                if (article == null) {
                    _relatedArticles.value = UiState.Empty()
                    UiState.Empty()
                } else {
                    currentArticleId = article.id
                    translationSlugs = discoverTranslationSlugs(article)
                    refreshSaveState(article.id)
                    loadRelatedArticles(article)
                    UiState.Success(article.withAvailableLanguages())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _relatedArticles.value = UiState.Empty()
                UiState.Error(e.message ?: "Unable to load article")
            }
        }
    }

    /**
     * Reads which language versions of [article] are actually published.
     *
     * Any failure — network, RLS, malformed rows — resolves to an empty map
     * rather than propagating, because this is an enhancement to an article
     * that already loaded: the reader keeps their Darija article and simply
     * sees no selector. The article's own locale is always included so a
     * one-entry list stays truthful.
     */
    private suspend fun discoverTranslationSlugs(article: Article): Map<String, String> {
        val discovered = try {
            repository.getArticleTranslations(article.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        val slugs = discovered
            .filter { ArticleLocales.isSupported(it.languageCode) && it.slug.isNotBlank() }
            .associate { ArticleLocales.normalize(it.languageCode)!! to it.slug }
            .toMutableMap()
        // Guarantee the version actually on screen is always selectable, even
        // if the index read came back incomplete.
        slugs.putIfAbsent(article.languageCode, article.slug)
        return slugs
    }

    /**
     * Attaches the discovered locale list to [this] article.
     *
     * The list is the article's own locale plus every other published version,
     * in selector order, so a single-language article yields exactly one entry
     * and the UI's `size > 1` check suppresses the selector with no extra rule.
     */
    private fun Article.withAvailableLanguages(): Article = copy(
        availableLanguages = (ArticleLocales.SUPPORTED + languageCode)
            .distinct()
            .filter { translationSlugs.containsKey(it) }
            .map { language ->
                ArticleTranslationRef(languageCode = language, slug = translationSlugs.getValue(language))
            },
    )

    /**
     * Switches the open article to [languageCode], which must be one of the
     * article's [Article.availableLanguages].
     *
     * The target is fetched through the same [ArticleRepository.getArticle]
     * call the initial load uses, keyed by that locale's own slug, so this is
     * the same article rather than a lookup of a different one. Three guards
     * keep the reader on a valid article:
     *
     *  - an unknown/already-active locale is ignored (no request, no flicker);
     *  - a null result or any failure leaves the current article on screen with
     *    no error and no empty state;
     *  - a response whose `id` differs from the open article is discarded,
     *    so a stale or mismatched slug can never swap in unrelated content.
     *
     * Media, bookmark state and navigation are unaffected: `cover`, `video` and
     * `software` are article-level, and the save state already tracks
     * [currentArticleId].
     */
    fun selectLanguage(languageCode: String) {
        val language = ArticleLocales.normalize(languageCode) ?: return
        val current = _uiState.value as? UiState.Success ?: return
        if (language == current.data.languageCode) return
        if (_isSwitchingLanguage.value) return
        val targetSlug = translationSlugs[language] ?: return
        val expectedId = currentArticleId ?: return

        _isSwitchingLanguage.value = true
        viewModelScope.launch {
            try {
                val translated = repository.getArticle(targetSlug, language)
                if (translated != null && translated.id == expectedId) {
                    currentArticleId = translated.id
                    loadRelatedArticles(translated)
                    _uiState.value = UiState.Success(translated.withAvailableLanguages())
                }
                // A null result or an id mismatch is intentionally silent: the
                // previously loaded article stays exactly as it was.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Same reasoning — a failed switch is not a failed read.
            } finally {
                _isSwitchingLanguage.value = false
            }
        }
    }

    private fun loadRelatedArticles(article: Article) {
        viewModelScope.launch {
            _relatedArticles.value = UiState.Loading
            try {
                val candidates = when {
                    !article.category?.slug.isNullOrBlank() -> repository.getArticlesByCategory(
                        article.category!!.slug,
                        defaultLanguage,
                    )
                    !article.tags.isNullOrEmpty() -> {
                        val tag = article.tags.firstOrNull { !it.slug.isNullOrBlank() } ?: return@launch
                        repository.getArticlesByTag(tag.slug, defaultLanguage)
                    }
                    else -> emptyList()
                }
                val related = candidates
                    .filter { it.id != article.id }
                    .distinctBy { it.id }
                    .take(4)
                _relatedArticles.value = if (related.isEmpty()) UiState.Empty() else UiState.Success(related)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _relatedArticles.value = UiState.Error(e.message ?: "Unable to load related articles")
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