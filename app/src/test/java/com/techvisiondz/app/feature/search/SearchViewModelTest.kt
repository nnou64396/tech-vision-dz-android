package com.techvisiondz.app.feature.search

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.repository.ARTICLE_PAGE_SIZE
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticleCard
import kotlinx.coroutines.CompletableDeferred
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
        assertEquals("نتيجة البحث", (state as UiState.Success).data.articles.single().title)
        assertEquals("android", repository.lastSearchQuery)
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastSearchLanguage)
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
        assertEquals("Retried", (state as UiState.Success).data.articles.single().title)
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
        assertEquals("محدث", (state as UiState.Success).data.articles.single().title)
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

    @Test
    fun `search requests the first page with the shared page size`() = runTest(dispatcher) {
        val results = (1..ARTICLE_PAGE_SIZE).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(ARTICLE_PAGE_SIZE, (state as UiState.Success).data.articles.size)
        assertTrue((state as UiState.Success).data.hasMore)
        assertEquals(0, repository.lastSearchOffset)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastSearchLimit)
    }

    @Test
    fun `loadMore appends the next page of the active query`() = runTest(dispatcher) {
        val results = (1..35).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertEquals(ARTICLE_PAGE_SIZE, (viewModel.uiState.value as UiState.Success).data.articles.size)

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(35, (state as UiState.Success).data.articles.size)
        assertEquals(35, (state as UiState.Success).data.articles.distinctBy { it.id }.size)
        assertFalse((state as UiState.Success).data.hasMore)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastSearchOffset)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastSearchLimit)
    }

    @Test
    fun `loadMore ignores a request when the typed query differs from the active one`() = runTest(dispatcher) {
        val results = (1..35).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertEquals(1, repository.searchCalls)

        viewModel.onQueryChange("ios")
        runCurrent()
        viewModel.loadMore()
        runCurrent()

        assertEquals(1, repository.searchCalls)
        assertFalse((viewModel.uiState.value as UiState.Success).data.isLoadingMore)
    }

    @Test
    fun `loadMore failure keeps the results and exposes the error`() = runTest(dispatcher) {
        val results = (1..35).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()

        repository.searchError = DataException.Network("Could not reach the server")
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(ARTICLE_PAGE_SIZE, data.articles.size)
        assertTrue(data.hasMore)
        assertFalse(data.isLoadingMore)
        assertEquals("Could not reach the server", data.loadMoreError)

        repository.searchError = null
        viewModel.loadMore()
        advanceUntilIdle()
        assertNull((viewModel.uiState.value as UiState.Success).data.loadMoreError)
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)
    }

    @Test
    fun `refresh reloads the first page of the active query`() = runTest(dispatcher) {
        val results = (1..35).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)

        repository.searchResults = (1..19).map { sampleArticleCard(id = "b$it") }
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(19, (viewModel.uiState.value as UiState.Success).data.articles.size)
        assertFalse((viewModel.uiState.value as UiState.Success).data.hasMore)
        assertEquals(0, repository.lastSearchOffset)
    }

    @Test
    fun `refresh with no active query is a no-op`() = runTest(dispatcher) {
        val repository = FakeArticleRepository()
        val viewModel = SearchViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(0, repository.searchCalls)
    }

    @Test
    fun `refresh failure keeps results and records the refresh error`() = runTest(dispatcher) {
        val results = (1..ARTICLE_PAGE_SIZE).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(searchResults = results)
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.searchError = DataException.Network("Could not reach the server")
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(ARTICLE_PAGE_SIZE, (viewModel.uiState.value as UiState.Success).data.articles.size)
        assertEquals("Could not reach the server", viewModel.refreshError.value)

        viewModel.consumeRefreshError()
        assertNull(viewModel.refreshError.value)
    }

    @Test
    fun `a new query resets accumulated pagination state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(searchResults = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)

        repository.searchResults = (1..25).map { sampleArticleCard(id = "b$it") }
        viewModel.onQueryChange("ios")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(20, (state as UiState.Success).data.articles.size)
        assertTrue((state as UiState.Success).data.hasMore)
        assertEquals(0, repository.lastSearchOffset)
        assertEquals("ios", repository.lastSearchQuery)
    }

    @Test
    fun `loadMore ignores a request while a page is already in flight`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(searchResults = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = SearchViewModel(repository)

        viewModel.onQueryChange("android")
        advanceUntilIdle()
        assertEquals(1, repository.searchCalls)

        val gate = CompletableDeferred<Unit>()
        repository.searchGate = gate
        viewModel.loadMore()
        runCurrent()
        assertEquals(2, repository.searchCalls)
        assertTrue((viewModel.uiState.value as UiState.Success).data.isLoadingMore)

        viewModel.loadMore()
        runCurrent()
        assertEquals(2, repository.searchCalls)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse((viewModel.uiState.value as UiState.Success).data.isLoadingMore)
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)
    }
}