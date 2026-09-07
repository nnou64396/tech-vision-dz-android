package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
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
class HomeViewModelTest {

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
    fun `loads feed with articles into success state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = listOf(sampleArticleCard()))
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(sampleArticleCard()), (state as UiState.Success).data.articles)
        assertEquals("ar", repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when feed has no published articles`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeArticleRepository(articles = emptyList()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state with the repository message on failure`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `starts in loading state then settles`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeArticleRepository(articles = listOf(sampleArticleCard())))

        assertTrue(viewModel.uiState.value is UiState.Loading)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `reload on retry returns fresh articles`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = emptyList())
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Empty)

        repository.articles = listOf(sampleArticleCard(title = "New article"))
        viewModel.loadHome()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("New article", (state as UiState.Success).data.articles.single().title)
    }
}