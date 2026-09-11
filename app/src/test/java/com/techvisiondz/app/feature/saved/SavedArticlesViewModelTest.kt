package com.techvisiondz.app.feature.saved

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.sampleArticleCard
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedArticlesViewModelTest {

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
    fun `loads saved articles into success state with the request language`() = runTest(dispatcher) {
        val saved = listOf(
            sampleArticleCard(id = "article-1", title = "First saved"),
            sampleArticleCard(id = "article-2", title = "Second saved"),
        )
        val repository = FakeSavedArticleRepository(savedArticles = saved)
        val viewModel = SavedArticlesViewModel(repository, defaultLanguage = "en")

        assertTrue(viewModel.uiState.value is UiState.Loading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf("article-1", "article-2"), (state as UiState.Success).data.map { it.id })
        assertEquals(1, repository.getSavedCalls)
        assertTrue(viewModel.hasLoadedOnce)
    }

    @Test
    fun `emits empty state when there are no saved articles`() = runTest(dispatcher) {
        val viewModel = SavedArticlesViewModel(FakeSavedArticleRepository())

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state with the repository message on failure`() = runTest(dispatcher) {
        val repository = FakeSavedArticleRepository(
            savedArticles = listOf(sampleArticleCard()),
        ).apply { error = DataException.Network("Could not reach the server") }
        val viewModel = SavedArticlesViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `stays loading until the query returns`() = runTest(dispatcher) {
        val viewModel = SavedArticlesViewModel(FakeSavedArticleRepository())

        runCurrent()

        assertTrue(viewModel.uiState.value is UiState.Loading)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `retry after a load failure loads the articles again`() = runTest(dispatcher) {
        val repository = FakeSavedArticleRepository(
            savedArticles = listOf(sampleArticleCard(id = "article-1")),
        ).apply { error = DataException.Network("Could not reach the server") }
        val viewModel = SavedArticlesViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Error)
        assertEquals(1, repository.getSavedCalls)

        repository.error = null
        viewModel.loadSaved()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(2, repository.getSavedCalls)
    }

    @Test
    fun `repeated reload while already loaded refreshes the list`() = runTest(dispatcher) {
        val repository = FakeSavedArticleRepository(savedArticles = listOf(sampleArticleCard(id = "article-1")))
        val viewModel = SavedArticlesViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.savedArticles = emptyList()
        viewModel.loadSaved()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
        assertEquals(2, repository.getSavedCalls)
        assertTrue(viewModel.hasLoadedOnce)
    }

    @Test
    fun `refresh keeps already-loaded content visible and settles to the new state`() = runTest(dispatcher) {
        val repository = FakeSavedArticleRepository(
            savedArticles = listOf(sampleArticleCard(id = "article-1")),
        )
        val viewModel = SavedArticlesViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.savedArticles = listOf(sampleArticleCard(id = "article-2"))
        viewModel.refresh()
        runCurrent()

        val inFlight = viewModel.uiState.value
        assertTrue(inFlight is UiState.Success)
        assertEquals(listOf("article-1"), (inFlight as UiState.Success).data.map { it.id })

        advanceUntilIdle()

        val settled = viewModel.uiState.value
        assertTrue(settled is UiState.Success)
        assertEquals(listOf("article-2"), (settled as UiState.Success).data.map { it.id })
        assertEquals(2, repository.getSavedCalls)
        assertTrue(viewModel.hasLoadedOnce)
    }

    @Test
    fun `refresh settles to empty when the saved articles were removed meanwhile`() = runTest(dispatcher) {
        val repository = FakeSavedArticleRepository(
            savedArticles = listOf(sampleArticleCard(id = "article-1")),
        )
        val viewModel = SavedArticlesViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.savedArticles = emptyList()
        viewModel.refresh()
        runCurrent()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }
}