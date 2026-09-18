package com.techvisiondz.app.feature.author

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticleCard
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
class AuthorArticlesViewModelTest {

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
    fun `loads author articles into success state`() = runTest(dispatcher) {
        val cards = listOf(sampleArticleCard(id = "a1"))
        val repository = FakeArticleRepository(authorArticles = mapOf("tech-vision-dz" to cards))
        val viewModel = AuthorArticlesViewModel(repository = repository, slug = "tech-vision-dz")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(cards, (state as UiState.Success).data)
        assertEquals("tech-vision-dz", repository.lastArticleSlug)
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when author has no articles`() = runTest(dispatcher) {
        val viewModel = AuthorArticlesViewModel(
            repository = FakeArticleRepository(authorArticles = emptyMap()),
            slug = "tech-vision-dz",
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state on failure`() = runTest(dispatcher) {
        val viewModel = AuthorArticlesViewModel(
            repository = FakeArticleRepository(error = DataException.Server("oops")),
            slug = "tech-vision-dz",
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Error)
    }

    @Test
    fun `refresh reloads the author articles in place`() = runTest(dispatcher) {
        val cards = listOf(sampleArticleCard(id = "a1"), sampleArticleCard(id = "a2"))
        val repository = FakeArticleRepository(authorArticles = mapOf("tech-vision-dz" to cards))
        val viewModel = AuthorArticlesViewModel(repository = repository, slug = "tech-vision-dz")

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        val updated = listOf(sampleArticleCard(id = "a3", title = "محدث"))
        repository.authorArticles = mapOf("tech-vision-dz" to updated)

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertTrue(!viewModel.isRefreshing.value)
        assertEquals(updated, (viewModel.uiState.value as UiState.Success).data)
    }

    @Test
    fun `refresh failure keeps content and records the refresh error`() = runTest(dispatcher) {
        val cards = listOf(sampleArticleCard(id = "a1"))
        val repository = FakeArticleRepository(authorArticles = mapOf("tech-vision-dz" to cards))
        val viewModel = AuthorArticlesViewModel(repository = repository, slug = "tech-vision-dz")

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.error = DataException.Network("Could not reach the server")
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(!viewModel.isRefreshing.value)
        assertEquals(cards, (viewModel.uiState.value as UiState.Success).data)
        assertEquals("Could not reach the server", viewModel.refreshError.value)

        viewModel.consumeRefreshError()
        assertEquals(null, viewModel.refreshError.value)
    }
}