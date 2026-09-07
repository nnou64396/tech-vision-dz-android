package com.techvisiondz.app.feature.author

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleAuthor
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

class AuthorListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successDisplaysAuthorNames() {
        val viewModel = AuthorViewModel(
            FakeArticleRepository(authors = listOf(sampleAuthor(name = "محرر TECH VISION DZ"))),
        )

        composeRule.setContent {
            TechVisionDzTheme { AuthorListScreen(viewModel = viewModel, onBack = {}, onAuthorClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("محرر TECH VISION DZ").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = AuthorViewModel(FakeArticleRepository(authors = emptyList()))

        composeRule.setContent {
            TechVisionDzTheme { AuthorListScreen(viewModel = viewModel, onBack = {}, onAuthorClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.authors_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetry() {
        val viewModel = AuthorViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        composeRule.setContent {
            TechVisionDzTheme { AuthorListScreen(viewModel = viewModel, onBack = {}, onAuthorClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}