package com.techvisiondz.app.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for the search screen using a fake repository.
 * Proves search input, result rendering, empty/error states, retry and clear all
 * behave without network access.
 */
class SearchScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun searchDisplaysResults() {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard(id = "a1", title = "نتيجة البحث")),
        )
        val viewModel = SearchViewModel(repository)

        composeRule.setContent {
            TechVisionDzTheme { SearchScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("اندرويد")
        composeRule.onNodeWithTag("search_field").performImeAction()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("نتيجة البحث").assertIsDisplayed()
        assertEquals("اندرويد", repository.lastSearchQuery)
    }

    @Test
    fun hintShownWhenNoSearchYet() {
        val viewModel = SearchViewModel(FakeArticleRepository())
        composeRule.setContent {
            TechVisionDzTheme { SearchScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.search_hint)).assertIsDisplayed()
    }

    @Test
    fun noResultsMessageShownForEmptyResultSet() {
        val viewModel = SearchViewModel(FakeArticleRepository())
        composeRule.setContent {
            TechVisionDzTheme { SearchScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("xy")
        composeRule.onNodeWithTag("search_field").performImeAction()
        composeRule.waitForIdle()

        val expected = composeRule.activity.getString(R.string.search_no_results, "xy")
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetryFetchesResults() {
        val repository = FakeArticleRepository(
            searchError = DataException.Network("Could not reach the server"),
        )
        val viewModel = SearchViewModel(repository)

        composeRule.setContent {
            TechVisionDzTheme { SearchScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("اندرويد")
        composeRule.onNodeWithTag("search_field").performImeAction()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()

        repository.searchError = null
        repository.searchResults = listOf(sampleArticleCard(id = "a2", title = "المحاولة الثانية"))
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("المحاولة الثانية").assertIsDisplayed()
    }

    @Test
    fun clearButtonResetsQuery() {
        val viewModel = SearchViewModel(FakeArticleRepository())
        composeRule.setContent {
            TechVisionDzTheme { SearchScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("اندرويد")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_clear").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.search_hint)).assertIsDisplayed()
    }

    @Test
    fun articleClickInvokesCallbackWithSlug() {
        var clickedSlug: String? = null
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard(id = "a3", title = "مقال قابل للنقر")),
        )
        val viewModel = SearchViewModel(repository)

        composeRule.setContent {
            TechVisionDzTheme {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onArticleClick = { clickedSlug = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("اندرويد")
        composeRule.onNodeWithTag("search_field").performImeAction()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("article_card_sample-a3").performClick()

        assertEquals("sample-a3", clickedSlug)
    }
}