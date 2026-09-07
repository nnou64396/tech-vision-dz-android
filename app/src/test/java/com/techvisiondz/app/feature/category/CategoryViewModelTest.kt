package com.techvisiondz.app.feature.category

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleCategory
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
class CategoryViewModelTest {

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
    fun `loads categories into success state`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(categories = listOf(sampleCategory()))
        val viewModel = CategoryViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(sampleCategory()), (state as UiState.Success).data)
        assertEquals("ar", repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when no categories`() = runTest(dispatcher) {
        val viewModel = CategoryViewModel(FakeArticleRepository(categories = emptyList()))

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state on failure`() = runTest(dispatcher) {
        val viewModel = CategoryViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Could not reach the server", (state as UiState.Error).message)
    }

    @Test
    fun `reload on retry returns fresh categories`() = runTest(dispatcher) {
        val repository = FakeArticleRepository(categories = emptyList())
        val viewModel = CategoryViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UiState.Empty)

        repository.categories = listOf(sampleCategory(name = "المزيد"))
        viewModel.loadCategories()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("المزيد", (state as UiState.Success).data.single().name)
    }
}