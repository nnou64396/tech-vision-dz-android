package com.techvisiondz.app.feature.article

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.model.ArticleTranslationRef
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.feature.saved.FakeSavedArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads article by slug into success state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val viewModel = ArticleDetailViewModel(repository = repository, slug = "hello-world")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("hello-world", (state as UiState.Success).data.slug)
        assertEquals("hello-world", repository.lastArticleDetailSlug)
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when the article is not found`() = runTest(dispatcher) {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(),
            slug = "missing",
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state with the repository message on failure`() = runTest(dispatcher) {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(error = DataException.Network("Could not reach the server")),
            slug = "hello-world",
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `starts in loading state then settles`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = "hello-world")

        assertTrue(viewModel.uiState.value is UiState.Loading)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `reload on retry returns the article after a failure`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(error = DataException.Network("Could not reach the server"))
        val viewModel = ArticleDetailViewModel(repository = repository, slug = "hello-world")

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Error)

        repository.error = null
        repository.detailArticle = sampleArticle()
        viewModel.loadArticle()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `save state stays at the default when no saved repository is wired`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val viewModel = ArticleDetailViewModel(repository = repository, slug = "hello-world")

        advanceUntilIdle()

        assertEquals(ArticleSaveUiState(), viewModel.saveState.value)
    }

    @Test
    fun `isArticleSaved is checked once the article has loaded`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val savedRepository = FakeSavedArticleRepository(isArticleSavedOverride = { true })
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = "hello-world",
            savedArticleRepository = savedRepository,
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Success)
        assertEquals(listOf("article-hello-world"), savedRepository.isArticleSavedCalls)
        assertTrue(viewModel.saveState.value.saved)
        assertFalse(viewModel.saveState.value.isSaving)
        assertNull(viewModel.saveState.value.error)
    }

    @Test
    fun `toggleSave bookmarks a currently unsaved article`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val savedRepository = FakeSavedArticleRepository()
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = "hello-world",
            savedArticleRepository = savedRepository,
        )

        advanceUntilIdle()
        assertFalse(viewModel.saveState.value.saved)

        viewModel.toggleSave()
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state.saved)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals(listOf("article-hello-world"), savedRepository.saveArticleCalls)
        assertTrue(savedRepository.unsaveArticleCalls.isEmpty())
    }

    @Test
    fun `toggleSave unbookmarks a currently saved article`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val savedRepository = FakeSavedArticleRepository(isArticleSavedOverride = { true })
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = "hello-world",
            savedArticleRepository = savedRepository,
        )

        advanceUntilIdle()
        assertTrue(viewModel.saveState.value.saved)

        viewModel.toggleSave()
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertFalse(state.saved)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertEquals(listOf("article-hello-world"), savedRepository.unsaveArticleCalls)
        assertTrue(savedRepository.saveArticleCalls.isEmpty())
    }

    @Test
    fun `a second toggle while one is in flight is ignored`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val savedRepository = FakeSavedArticleRepository()
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = "hello-world",
            savedArticleRepository = savedRepository,
        )

        advanceUntilIdle()

        viewModel.toggleSave()
        runCurrent()
        assertTrue(viewModel.saveState.value.isSaving)

        viewModel.toggleSave()
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertEquals(1, savedRepository.saveArticleCalls.size)
        assertTrue(state.saved)
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    @Test
    fun `failed toggle surfaces the mapped error and keeps the article readable`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        repository.detailArticle = sampleArticle(slug = "hello-world")
        val savedRepository = FakeSavedArticleRepository().apply {
            error = DataException.Authentication("Not signed in")
        }
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = "hello-world",
            savedArticleRepository = savedRepository,
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        viewModel.toggleSave()
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertFalse(state.saved)
        assertFalse(state.isSaving)
        assertEquals(AuthError.Unknown, state.error)
        // The article itself is untouched by a failed bookmark request.
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `loads related articles by category and ignores the current article`() = runTest(dispatcher) {
        val current = sampleArticle(
            slug = "feature-story",
            category = com.techvisiondz.app.core.data.model.CategorySummary(
                slug = "tech",
                name = "Tech",
                description = null,
            ),
        )
        val repository = FakeArticleRepository(
            categoryArticles = mapOf(
                "tech" to listOf(
                    ArticleCard(
                        id = "related-one",
                        slug = "related-one",
                        title = "Related one",
                        excerpt = "First related",
                        featured = false,
                        publishedAt = "2026-08-02T00:00:00Z",
                        readingTimeMinutes = 3,
                        viewsCount = 11L,
                        categoryName = "Tech",
                        authorName = "TECH VISION DZ",
                        coverUrl = null,
                    ),
                    ArticleCard(
                        id = "article-feature-story",
                        slug = "feature-story",
                        title = "Duplicate",
                        excerpt = "Current article duplicate",
                        featured = false,
                        publishedAt = "2026-08-02T00:00:00Z",
                        readingTimeMinutes = 3,
                        viewsCount = 11L,
                        categoryName = "Tech",
                        authorName = "TECH VISION DZ",
                        coverUrl = null,
                    ),
                    ArticleCard(
                        id = "related-three",
                        slug = "related-three",
                        title = "Related three",
                        excerpt = "Third related",
                        featured = false,
                        publishedAt = "2026-08-02T00:00:00Z",
                        readingTimeMinutes = 3,
                        viewsCount = 11L,
                        categoryName = "Tech",
                        authorName = "TECH VISION DZ",
                        coverUrl = null,
                    ),
                ),
            ),
        )
        repository.detailArticle = current

        val viewModel = ArticleDetailViewModel(repository = repository, slug = "feature-story")

        advanceUntilIdle()

        val state = viewModel.relatedArticles.value
        assertTrue(state is UiState.Success)
        assertEquals(
            listOf("related-one", "related-three"),
            (state as UiState.Success).data.map { it.slug },
        )
    }

    // ---------------------------------------------------------------------
    // Multilingual articles
    //
    // The backend stores one row per (article, locale) and each row has its own
    // slug, so these tests model an article whose versions are reached through
    // different slugs but share one id.
    // ---------------------------------------------------------------------

    @Test
    fun `article with four translations offers all four and keeps darija first`() = runTest(dispatcher) {
        val repository = multilingualRepository()

        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

        advanceUntilIdle()

        val article = (viewModel.uiState.value as UiState.Success).data
        assertEquals(
            listOf("arq", "ar", "fr", "en"),
            article.availableLanguages.map { it.languageCode },
        )
        // Default behavior preserved: the app still opens on Darija.
        assertEquals("arq", article.languageCode)
        assertEquals(SLUG_ARQ, article.slug)
    }

    @Test
    fun `article with only two translations offers exactly those two`() = runTest(dispatcher) {
        val repository = FakeArticleRepository().apply {
            detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
            articleTranslations = listOf(
                ArticleTranslationRef("arq", SLUG_ARQ),
                ArticleTranslationRef("ar", SLUG_AR),
            )
        }

        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

        advanceUntilIdle()

        val article = (viewModel.uiState.value as UiState.Success).data
        assertEquals(listOf("arq", "ar"), article.availableLanguages.map { it.languageCode })
    }

    @Test
    fun `a single translation article reports one language and no selector source`() = runTest(dispatcher) {
        val repository = FakeArticleRepository().apply {
            detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
            articleTranslations = listOf(ArticleTranslationRef("arq", SLUG_ARQ))
        }

        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

        advanceUntilIdle()

        val article = (viewModel.uiState.value as UiState.Success).data
        assertEquals(1, article.availableLanguages.size)
        assertEquals("arq", article.availableLanguages.single().languageCode)
    }

    @Test
    fun `an article with no multilingual metadata still renders and offers no selector`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                articleTranslations = emptyList()
            }

            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            val article = (state as UiState.Success).data
            // Even with no index rows at all, the version on screen stays
            // selectable so the list is never empty and the UI stays silent.
            assertEquals(listOf("arq"), article.availableLanguages.map { it.languageCode })
            assertEquals(TITLE_ARQ, article.title)
        }

    @Test
    fun `a failing translation index keeps the article readable and hides the selector`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                translationsError = DataException.Network("Could not reach the server")
            }

            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(listOf("arq"), (state as UiState.Success).data.availableLanguages.map { it.languageCode })
        }

    @Test
    fun `the translation index is requested for the loaded article id`() = runTest(dispatcher) {
        val repository = multilingualRepository()

        ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)

        advanceUntilIdle()

        assertEquals("article-mediatek", repository.lastTranslationsArticleId)
    }

    @Test
    fun `switching darija to fusha then french then english swaps the version each time`() =
        runTest(dispatcher) {
            val repository = multilingualRepository()
            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
            advanceUntilIdle()

            listOf(
                Triple("ar", SLUG_AR, TITLE_AR),
                Triple("fr", SLUG_FR, TITLE_FR),
                Triple("en", SLUG_EN, TITLE_EN),
            ).forEach { (language, slug, title) ->
                viewModel.selectLanguage(language)
                advanceUntilIdle()

                val article = (viewModel.uiState.value as UiState.Success).data
                assertEquals(title, article.title)
                assertEquals(slug, article.slug)
                assertEquals(language, article.languageCode)
                // Identity is preserved across every switch.
                assertEquals("article-mediatek", article.id)
            }
        }

    @Test
    fun `each switch uses the slug of the target language, not the current one`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        viewModel.selectLanguage("fr")
        advanceUntilIdle()

        assertEquals(SLUG_FR, repository.lastArticleDetailSlug)
        assertEquals("fr", repository.lastDetailLanguageCode)
    }

    @Test
    fun `switching keeps the article available in every language after the change`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        viewModel.selectLanguage("en")
        advanceUntilIdle()

        val article = (viewModel.uiState.value as UiState.Success).data
        assertEquals(listOf("arq", "ar", "fr", "en"), article.availableLanguages.map { it.languageCode })
        assertEquals("en", article.languageCode)
    }

    @Test
    fun `switching preserves the article cover`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        viewModel.selectLanguage("fr")
        advanceUntilIdle()

        // Media is article-level on the backend, so it survives the switch.
        assertEquals(COVER, (viewModel.uiState.value as UiState.Success).data.coverUrl)
    }

    @Test
    fun `selecting the active language is a no-op`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()
        val callsBefore = repository.lastArticleDetailSlug

        viewModel.selectLanguage("arq")
        advanceUntilIdle()

        assertEquals(callsBefore, repository.lastArticleDetailSlug)
        assertEquals("arq", (viewModel.uiState.value as UiState.Success).data.languageCode)
    }

    @Test
    fun `selecting an unavailable language is ignored and keeps the article on screen`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                articleTranslations = listOf(ArticleTranslationRef("arq", SLUG_ARQ))
            }
            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
            advanceUntilIdle()

            viewModel.selectLanguage("fr")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(TITLE_ARQ, (state as UiState.Success).data.title)
        }

    @Test
    fun `a translation that resolves to null keeps the current article and shows no error`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                articleTranslations = listOf(
                    ArticleTranslationRef("arq", SLUG_ARQ),
                    ArticleTranslationRef("fr", SLUG_FR),
                )
                // The French slug is known to exist but the article never loads:
                // getArticle falls back to detailArticle, so force a null by
                // clearing it for the second call via a slug map miss.
                articlesBySlug = emptyMap()
            }
            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
            advanceUntilIdle()

            repository.detailArticle = null
            viewModel.selectLanguage("fr")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("a missing translation must not blank the article", state is UiState.Success)
            assertEquals(TITLE_ARQ, (state as UiState.Success).data.title)
            assertEquals("arq", state.data.languageCode)
        }

    @Test
    fun `a failed translation request keeps the current article and shows no error`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        repository.error = DataException.Network("Could not reach the server")
        viewModel.selectLanguage("fr")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(TITLE_ARQ, (state as UiState.Success).data.title)
    }

    @Test
    fun `a response for a different article id is discarded`() = runTest(dispatcher) {
        val repository = FakeArticleRepository().apply {
            detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
            articleTranslations = listOf(
                ArticleTranslationRef("arq", SLUG_ARQ),
                ArticleTranslationRef("fr", SLUG_FR),
            )
            // The French slug resolves, but to a different article entirely.
            articlesBySlug = mapOf(
                SLUG_ARQ to articleIn("arq", SLUG_ARQ, TITLE_ARQ),
                SLUG_FR to sampleArticle(
                    slug = SLUG_FR,
                    title = "An unrelated article",
                    languageCode = "fr",
                ).copy(id = "article-somewhere-else"),
            )
        }
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        viewModel.selectLanguage("fr")
        advanceUntilIdle()

        val article = (viewModel.uiState.value as UiState.Success).data
        assertEquals(TITLE_ARQ, article.title)
        assertEquals("article-mediatek", article.id)
    }

    @Test
    fun `the switch flag is cleared once a switch settles`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
        advanceUntilIdle()

        assertFalse(viewModel.isSwitchingLanguage.value)
        viewModel.selectLanguage("fr")
        advanceUntilIdle()
        assertFalse(viewModel.isSwitchingLanguage.value)

        repository.error = DataException.Network("Could not reach the server")
        viewModel.selectLanguage("en")
        advanceUntilIdle()
        // Cleared even on failure, so the selector never stays permanently dimmed.
        assertFalse(viewModel.isSwitchingLanguage.value)
    }

    @Test
    fun `switching language does not disturb the bookmark state`() = runTest(dispatcher) {
        val repository = multilingualRepository()
        val savedRepository = FakeSavedArticleRepository(isArticleSavedOverride = { true })
        val viewModel = ArticleDetailViewModel(
            repository = repository,
            slug = SLUG_ARQ,
            savedArticleRepository = savedRepository,
        )
        advanceUntilIdle()
        assertTrue(viewModel.saveState.value.saved)

        viewModel.selectLanguage("fr")
        advanceUntilIdle()

        assertTrue(viewModel.saveState.value.saved)
        assertEquals(listOf("article-mediatek"), savedRepository.isArticleSavedCalls)
    }

    @Test
    fun `the current article is filtered out of related articles by id after a switch`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                articleTranslations = listOf(
                    ArticleTranslationRef("arq", SLUG_ARQ),
                    ArticleTranslationRef("fr", SLUG_FR),
                )
                // Related cards come back in the default language, so their
                // slugs differ from the French slug currently on screen.
                categoryArticles = mapOf(
                    "tech" to listOf(
                        sampleArticleCard(id = "article-mediatek", title = "Self in arq"),
                        sampleArticleCard(id = "article-other", title = "Genuinely related"),
                    ),
                )
                articlesBySlug = mapOf(
                    SLUG_FR to articleIn("fr", SLUG_FR, TITLE_FR),
                )
            }
            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
            advanceUntilIdle()

            viewModel.selectLanguage("fr")
            advanceUntilIdle()

            val related = (viewModel.relatedArticles.value as UiState.Success).data
            assertEquals(listOf("article-other"), related.map { it.id })
        }

    @Test
    fun `an unsupported locale is never offered by the mapper fed to the selector`() =
        runTest(dispatcher) {
            val repository = FakeArticleRepository().apply {
                detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
                // A locale the app has no label for, plus a usable pair.
                articleTranslations = listOf(
                    ArticleTranslationRef("arq", SLUG_ARQ),
                    ArticleTranslationRef("es", "slug-espanol"),
                    ArticleTranslationRef("fr", SLUG_FR),
                )
                articlesBySlug = mapOf(
                    SLUG_ARQ to articleIn("arq", SLUG_ARQ, TITLE_ARQ),
                    SLUG_FR to articleIn("fr", SLUG_FR, TITLE_FR),
                )
            }
            val viewModel = ArticleDetailViewModel(repository = repository, slug = SLUG_ARQ)
            advanceUntilIdle()

            val article = (viewModel.uiState.value as UiState.Success).data
            assertEquals(listOf("arq", "fr"), article.availableLanguages.map { it.languageCode })

            // And tapping it is impossible even if it somehow reaches the VM.
            viewModel.selectLanguage("es")
            advanceUntilIdle()
            assertEquals("arq", (viewModel.uiState.value as UiState.Success).data.languageCode)
        }

    /** Repository modelling one article published in all four locales. */
    private fun multilingualRepository(): FakeArticleRepository = FakeArticleRepository().apply {
        articleTranslations = listOf(
            ArticleTranslationRef("arq", SLUG_ARQ),
            ArticleTranslationRef("ar", SLUG_AR),
            ArticleTranslationRef("fr", SLUG_FR),
            ArticleTranslationRef("en", SLUG_EN),
        )
        articlesBySlug = mapOf(
            SLUG_ARQ to articleIn("arq", SLUG_ARQ, TITLE_ARQ),
            SLUG_AR to articleIn("ar", SLUG_AR, TITLE_AR),
            SLUG_FR to articleIn("fr", SLUG_FR, TITLE_FR),
            SLUG_EN to articleIn("en", SLUG_EN, TITLE_EN),
        )
        detailArticle = articleIn("arq", SLUG_ARQ, TITLE_ARQ)
    }

    /**
     * One locale of the shared multilingual article. Every version keeps the
     * same id and cover, exactly as the backend does.
     */
    private fun articleIn(language: String, slug: String, title: String) = sampleArticle(
        slug = slug,
        title = title,
        coverUrl = COVER,
        languageCode = language,
    ).copy(id = "article-mediatek")

    private companion object {
        const val SLUG_ARQ = "mediatek-darija-slug"
        const val SLUG_AR = "mediatek-fusha-slug"
        const val SLUG_FR = "mediatek-french-slug"
        const val SLUG_EN = "mediatek-english-slug"
        const val TITLE_ARQ = "MediaTek تكشف معالج Dimensity"
        const val TITLE_AR = "MediaTek تكشف معالج Dimensity formally"
        const val TITLE_FR = "MediaTek dévoile le processeur Dimensity"
        const val TITLE_EN = "MediaTek unveils the Dimensity processor"
        const val COVER = "https://cdn.example/media/cover.jpg"
    }
}