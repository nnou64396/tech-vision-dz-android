package com.techvisiondz.app.feature.tag

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
class TagArticlesViewModelTest {

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
    fun `loads tag articles into success state`() = runTest(dispatcher) {
        val cards = listOf(sampleArticleCard(id = "a1"))
        val repository = FakeArticleRepository(tagArticles = mapOf("ai" to cards))
        val viewModel = TagArticlesViewModel(repository = repository, slug = "ai")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(cards, (state as UiState.Success).data)
        assertEquals("ai", repository.lastArticleSlug)
        assertEquals("ar", repository.lastLanguageCode)
    }

    @Test
    fun `emits empty state when tag has no articles`() = runTest(dispatcher) {
        val viewModel = TagArticlesViewModel(
            repository = FakeArticleRepository(tagArticles = emptyMap()),
            slug = "ai",
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `emits error state on failure`() = runTest(dispatcher) {
        val viewModel = TagArticlesViewModel(
            repository = FakeArticleRepository(error = DataException.Server("oops")),
            slug = "ai",
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}