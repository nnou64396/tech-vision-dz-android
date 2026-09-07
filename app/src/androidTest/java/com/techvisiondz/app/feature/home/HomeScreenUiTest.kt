package com.techvisiondz.app.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for the home screen using a fake repository.
 * Proves the home screen renders all meaningful UI states without network access.
 */
class HomeScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun feedDisplaysArticleTitle() {
        val viewModel = HomeViewModel(
            FakeArticleRepository(
                articles = listOf(sampleArticleCard(title = "أحدث الابتكارات التكنولوجية في الجزائر")),
            ),
        )

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("أحدث الابتكارات التكنولوجية في الجزائر").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = HomeViewModel(FakeArticleRepository(articles = emptyList()))

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.home_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsErrorMessage() {
        val errorMessage = "Could not reach the server"
        val viewModel = HomeViewModel(
            FakeArticleRepository(error = DataException.Network(errorMessage)),
        )

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryButton() {
        val viewModel = HomeViewModel(
            FakeArticleRepository(error = DataException.Network("No connection")),
        )

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }

    @Test
    fun feedDisplaysExcerptAndMetadata() {
        val viewModel = HomeViewModel(
            FakeArticleRepository(
                articles = listOf(
                    sampleArticleCard(title = "Title").copy(
                        excerpt = "This is an excerpt.",
                        authorName = "Author Name",
                        categoryName = "Category",
                    ),
                ),
            ),
        )

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Title").assertIsDisplayed()
        composeRule.onNodeWithText("This is an excerpt.").assertIsDisplayed()
        composeRule.onNodeWithText("Author Name").assertIsDisplayed()
    }
}