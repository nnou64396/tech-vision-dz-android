package com.techvisiondz.app.feature.tag

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleTag
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

class TagListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successDisplaysTagNames() {
        val viewModel = TagViewModel(
            FakeArticleRepository(tags = listOf(sampleTag(name = "ذكاء اصطناعي"))),
        )

        composeRule.setContent {
            TechVisionDzTheme { TagListScreen(viewModel = viewModel, onBack = {}, onTagClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("ذكاء اصطناعي").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        val viewModel = TagViewModel(FakeArticleRepository(tags = emptyList()))

        composeRule.setContent {
            TechVisionDzTheme { TagListScreen(viewModel = viewModel, onBack = {}, onTagClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tags_empty)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetry() {
        val viewModel = TagViewModel(
            FakeArticleRepository(error = DataException.Network("Could not reach the server")),
        )

        composeRule.setContent {
            TechVisionDzTheme { TagListScreen(viewModel = viewModel, onBack = {}, onTagClick = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }
}