package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.techvisiondz.app.R
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Navigation test for the full search flow: Home → Search → ArticleDetail → back
 * to Search → back to Home, using a fake repository.
 */
class SearchNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeToSearchToArticleAndBack() {
        val repository = FakeArticleRepository(
            searchResults = listOf(sampleArticleCard(id = "s1", title = "مقال البحث")),
        )
        repository.detailArticle = sampleArticle(slug = "sample-s1", title = "تفاصيل البحث")

        composeRule.setContent { TechVisionDzTheme { AppNavHost(repository = repository) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_entry").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("search_field").performTextInput("اندرويد")
        composeRule.onNodeWithTag("search_field").performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال البحث").assertIsDisplayed()

        composeRule.onNodeWithTag("article_card_sample-s1").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("تفاصيل البحث")[0].assertIsDisplayed()
        assertEquals("sample-s1", repository.lastArticleSlug)

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال البحث").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_entry").assertIsDisplayed()
    }
}