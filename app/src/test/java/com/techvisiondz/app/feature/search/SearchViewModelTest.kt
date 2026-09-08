package com.techvisiondz.app.feature.search

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

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
    fun `successful search emits results and normalizes query`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard(title = "نتيجة البحث")),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("  android  ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("نتيجة البحث", (state as UiState.Success).data.single().title)
        assertEquals("android", repository.lastSearchQuery)
        assertEquals("ar", repository.lastSearchLanguage)
        assertEquals(1, repository.searchCalls)
    }

    @Test
    fun `blank query performs no request and shows hint`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("    ")
        advanceUntilIdle()

        assertEquals(0, repository.searchCalls)
        assertNull(repository.lastSearchQuery)
        assertTrue(viewModel.uiState.value is UiState.Empty)
        assertNull(viewModel.activeQuery.value)
    }

    @Test
    fun `query shorter than the minimum length performs no request`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("a")
        advanceUntilIdle()

        assertEquals(0, repository.searchCalls)
        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `empty results emit empty state`() = runTest(dispatcher) {
        val viewModel = SearchViewModel(FakeArticleRepository(searchResults = emptyList()))

        viewModel.onQueryChange("android")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
        assertEquals("android", viewModel.activeQuery.value)
    }

    @Test
    fun `repository failure emits error state with the message`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchError = DataException.Network("Could not reach the server"),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `retry after failure re-runs the active query`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchError = DataException.Network("Could not reach the server"),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Error)

        repository.searchError = null
        repository.searchResults = listOf(sampleArticleCard(title = "Retried"))
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("Retried", (state as UiState.Success).data.single().title)
        assertEquals(2, repository.searchCalls)
    }

    @Test
    fun `query is trimmed before reaching the repository`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("   أحدث الابتكارات في الجزائر \n")
        advanceUntilIdle()

        assertEquals("احدث الابتكارات في الجزائر", repository.lastSearchQuery)
    }

    @Test
    fun `arabic alef variants are normalized to plain alef`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("أندرويد إصدار آي")
        advanceUntilIdle()

        assertEquals("اندرويد اصدار اي", repository.lastSearchQuery)
    }

    @Test
    fun `characters that would corrupt the or filter are stripped`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("an*dro(i,d),\"a\\b%")
        advanceUntilIdle()

        assertEquals("androidab", repository.lastSearchQuery)
    }

    @Test
    fun `search is debounced and not fired before the delay elapses`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        runCurrent()

        assertEquals(0, repository.searchCalls)

        for (i in 1..349) {
            runCurrent()
        }
        assertEquals(0, repository.searchCalls)

        advanceUntilIdle()
        assertEquals(1, repository.searchCalls)
        assertEquals("android", repository.lastSearchQuery)
    }

    @Test
    fun `newer query cancels the pending search`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard(title = "محدث")),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        viewModel.onQueryChange("androidx")
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls)
        assertEquals("androidx", repository.lastSearchQuery)
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("محدث", (state as UiState.Success).data.single().title)
    }

    @Test
    fun `ime search submit bypasses the debounce`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard()),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        runCurrent()
        assertEquals(0, repository.searchCalls)

        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls)
        assertEquals("android", repository.lastSearchQuery)
    }

    @Test
    fun `clear resets query, results and active query`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard()),
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        viewModel.onClear()

        assertEquals("", viewModel.query.value)
        assertNull(viewModel.activeQuery.value)
        assertTrue(viewModel.uiState.value is UiState.Empty)
        assertEquals(1, repository.searchCalls)
    }

    @Test
    fun `normalizeSearchQuery collapses interior whitespace`() {
        assertEquals("android studio", normalizeSearchQuery("   android   studio  "))
        assertEquals("", normalizeSearchQuery("   "))
        assertEquals("", normalizeSearchQuery(""))
    }
}