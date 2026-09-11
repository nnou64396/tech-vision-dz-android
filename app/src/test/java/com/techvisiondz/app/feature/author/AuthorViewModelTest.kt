package com.techvisiondz.app.feature.author

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleAuthor
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
class AuthorViewModelTest {

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
    fun `loads authors into success state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(authors = listOf(sampleAuthor()))
        val viewModel = AuthorViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(sampleAuthor()), (state as UiState.Success).data)
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when no authors`() = runTest(dispatcher) {
        val viewModel = AuthorViewModel(FakeArticleRepository(authors = emptyList()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state on failure`() = runTest(dispatcher) {
        val viewModel = AuthorViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `reload on retry returns fresh authors`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(authors = emptyList())
        val viewModel = AuthorViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Empty)

        repository.authors = listOf(sampleAuthor(name = "كاتب"))
        viewModel.loadAuthors()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("كاتب", (state as UiState.Success).data.single().name)
    }
}