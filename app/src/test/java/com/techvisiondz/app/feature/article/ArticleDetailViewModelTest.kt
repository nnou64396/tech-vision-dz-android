package com.techvisiondz.app.feature.article

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
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
        assertEquals("hello-world", repository.lastArticleSlug)
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
}