package com.techvisiondz.app.feature.author

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

class AuthorArticlesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successDisplaysArticles() {
        val viewModel = AuthorArticlesViewModel(
            repository = FakeArticleRepository(
                authorArticles = mapOf("tech-vision-dz" to listOf(sampleArticleCard(id = "a1", title = "مقال المؤلف"))),
            ),
            slug = "tech-vision-dz",
        )

        composeRule.setContent {
            TechVisionDzTheme { AuthorArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("مقال المؤلف").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = AuthorArticlesViewModel(
            repository = FakeArticleRepository(authorArticles = emptyMap()),
            slug = "tech-vision-dz",
        )

        composeRule.setContent {
            TechVisionDzTheme { AuthorArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.author_articles_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetry() {
        val viewModel = AuthorArticlesViewModel(
            repository = FakeArticleRepository(error = DataException.Network("Could not reach the server")),
            slug = "tech-vision-dz",
        )

        composeRule.setContent {
            TechVisionDzTheme { AuthorArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}