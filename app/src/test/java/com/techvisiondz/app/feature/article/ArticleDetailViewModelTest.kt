package com.techvisiondz.app.feature.article

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        assertEquals("ar", repository.lastLanguageCode)
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
}