package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
    fun `viewmodel emits success with empty content`() = runTest(dispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
    }

    @Test
    fun `home content has no articles while no data source is wired`() = runTest(dispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(emptyList<Any>(), state.data.articles)
    }
}