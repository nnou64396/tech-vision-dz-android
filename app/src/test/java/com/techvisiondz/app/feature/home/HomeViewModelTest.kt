package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.repository.ARTICLE_PAGE_SIZE
import com.techvisiondz.app.core.ui.UiState
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
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastLanguageCode)
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

    @Test
    fun `loadHome requests the first page with the shared page size`() = runTest(dispatcher) {
        val articles = (1..ARTICLE_PAGE_SIZE).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(articles = articles)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(articles, (state as UiState.Success).data.articles)
        assertTrue((state as UiState.Success).data.hasMore)
        assertEquals(0, repository.lastFeedOffset)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastFeedLimit)
        assertEquals(1, repository.feedCalls)
    }

    @Test
    fun `a page smaller than the page size ends the feed`() = runTest(dispatcher) {
        val articles = (1..ARTICLE_PAGE_SIZE - 1).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(articles = articles)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val data = (viewModel.uiState.value as UiState.Success).data
        assertFalse(data.hasMore)
    }

    @Test
    fun `loadMore appends the next page using the loaded count as offset`() = runTest(dispatcher) {
        val articles = (1..35).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(articles = articles)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertEquals(ARTICLE_PAGE_SIZE, (viewModel.uiState.value as UiState.Success).data.articles.size)

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(35, (state as UiState.Success).data.articles.size)
        assertFalse((state as UiState.Success).data.hasMore)
        assertEquals(35, (state as UiState.Success).data.articles.distinctBy { it.id }.size)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastFeedOffset)
        assertEquals(ARTICLE_PAGE_SIZE, repository.lastFeedLimit)
    }

    @Test
    fun `loadMore stops when the backend reports no further page`() = runTest(dispatcher) {
        val articles = (1..25).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(articles = articles)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(25, (viewModel.uiState.value as UiState.Success).data.articles.size)
        assertFalse((viewModel.uiState.value as UiState.Success).data.hasMore)

        val callsBeforeIgnoredLoad = repository.feedCalls
        viewModel.loadMore()
        runCurrent()
        assertEquals(callsBeforeIgnoredLoad, repository.feedCalls)
    }

    @Test
    fun `loadMore ignores a request while a page is already in flight`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertEquals(1, repository.feedCalls)

        val gate = CompletableDeferred<Unit>()
        repository.feedGate = gate
        viewModel.loadMore()
        runCurrent()

        assertTrue((viewModel.uiState.value as UiState.Success).data.isLoadingMore)
        assertEquals(2, repository.feedCalls)

        viewModel.loadMore()
        runCurrent()
        assertEquals(2, repository.feedCalls)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse((viewModel.uiState.value as UiState.Success).data.isLoadingMore)
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)
    }

    @Test
    fun `loadMore failure keeps the loaded articles and exposes the error`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertEquals(ARTICLE_PAGE_SIZE, (viewModel.uiState.value as UiState.Success).data.articles.size)

        repository.error = DataException.Network("Could not reach the server")
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(ARTICLE_PAGE_SIZE, data.articles.size)
        assertTrue(data.hasMore)
        assertFalse(data.isLoadingMore)
        assertEquals("Could not reach the server", data.loadMoreError)
    }

    @Test
    fun `loadMore error is cleared and retried on a subsequent request`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        repository.error = DataException.Network("Could not reach the server")
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals("Could not reach the server", (viewModel.uiState.value as UiState.Success).data.loadMoreError)

        repository.error = null
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)
        assertNull((viewModel.uiState.value as UiState.Success).data.loadMoreError)
    }

    @Test
    fun `refresh reloads the first page and resets pagination state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(35, (viewModel.uiState.value as UiState.Success).data.articles.size)

        repository.articles = (1..19).map { sampleArticleCard(id = "b$it") }
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(19, (viewModel.uiState.value as UiState.Success).data.articles.size)
        assertFalse((viewModel.uiState.value as UiState.Success).data.hasMore)
        assertEquals(0, repository.lastFeedOffset)
    }

    @Test
    fun `refresh failure keeps content and records the refresh error`() = runTest(dispatcher) {
        val articles = (1..20).map { sampleArticleCard(id = "a$it") }
        val repository = FakeArticleRepository(articles = articles)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Success)

        repository.error = DataException.Network("Could not reach the server")
        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(articles, (viewModel.uiState.value as UiState.Success).data.articles)
        assertEquals("Could not reach the server", viewModel.refreshError.value)

        viewModel.consumeRefreshError()
        assertNull(viewModel.refreshError.value)
    }

    @Test
    fun `refresh cancels an in-flight load more without leaving stale content`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(articles = (1..35).map { sampleArticleCard(id = "a$it") })
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()
        assertEquals(1, repository.feedCalls)

        val gate = CompletableDeferred<Unit>()
        repository.feedGate = gate
        viewModel.loadMore()
        runCurrent()
        assertEquals(2, repository.feedCalls)

        repository.articles = (1..20).map { sampleArticleCard(id = "b$it") }
        viewModel.refresh()
        runCurrent()
        assertEquals(3, repository.feedCalls)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        val data = (viewModel.uiState.value as UiState.Success).data
        assertFalse(data.isLoadingMore)
        assertEquals(20, data.articles.size)
        assertEquals("b1", data.articles.first().slug.substringAfter("-"))
    }
}