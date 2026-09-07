package com.techvisiondz.app.feature.category

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

class CategoryArticlesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successDisplaysArticles() {
        val viewModel = CategoryArticlesViewModel(
            repository = FakeArticleRepository(
                categoryArticles = mapOf("alssha" to listOf(sampleArticleCard(id = "a1", title = "مقال الفئة"))),
            ),
            slug = "alssha",
        )

        composeRule.setContent {
            TechVisionDzTheme { CategoryArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("مقال الفئة").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = CategoryArticlesViewModel(
            repository = FakeArticleRepository(categoryArticles = emptyMap()),
            slug = "alssha",
        )

        composeRule.setContent {
            TechVisionDzTheme { CategoryArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.home_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetry() {
        val viewModel = CategoryArticlesViewModel(
            repository = FakeArticleRepository(error = DataException.Network("Could not reach the server")),
            slug = "alssha",
        )

        composeRule.setContent {
            TechVisionDzTheme { CategoryArticlesScreen(viewModel = viewModel, onBack = {}, onArticleClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}