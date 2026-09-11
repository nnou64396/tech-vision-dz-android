package com.techvisiondz.app.feature.tag

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleTag
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
class TagViewModelTest {

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
    fun `loads tags into success state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(tags = listOf(sampleTag()))
        val viewModel = TagViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(sampleTag()), (state as UiState.Success).data)
        assertEquals(AppConfig.DEFAULT_LANGUAGE_CODE, repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when no tags`() = runTest(dispatcher) {
        val viewModel = TagViewModel(FakeArticleRepository(tags = emptyList()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state on failure`() = runTest(dispatcher) {
        val viewModel = TagViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `reload on retry returns fresh tags`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(tags = emptyList())
        val viewModel = TagViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Empty)

        repository.tags = listOf(sampleTag(name = "ذكاء اصطناعي"))
        viewModel.loadTags()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("ذكاء اصطناعي", (state as UiState.Success).data.single().name)
    }
}