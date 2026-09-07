package com.techvisiondz.app.feature.category

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleCategory
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

class CategoryListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successDisplaysCategoryNames() {
        val viewModel = CategoryViewModel(
            FakeArticleRepository(categories = listOf(sampleCategory(name = "الصحة والتقنية"))),
        )

        composeRule.setContent {
            TechVisionDzTheme { CategoryListScreen(viewModel = viewModel, onBack = {}, onCategoryClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("الصحة والتقنية").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = CategoryViewModel(FakeArticleRepository(categories = emptyList()))

        composeRule.setContent {
            TechVisionDzTheme { CategoryListScreen(viewModel = viewModel, onBack = {}, onCategoryClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.categories_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetry() {
        val viewModel = CategoryViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        composeRule.setContent {
            TechVisionDzTheme { CategoryListScreen(viewModel = viewModel, onBack = {}, onCategoryClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}