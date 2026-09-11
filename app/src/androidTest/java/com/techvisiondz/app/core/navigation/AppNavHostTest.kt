package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic navigation tests using a fake [ArticleRepository].
 *
 * Proves the full Home → ArticleDetail → Home round trip works without any
 * network, Supabase, or device timing dependency.
 */
class AppNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeCardClickNavigatesToDetailAndBackReturnsHome() {
        val repository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "عنوان في القائمة")),
        )
        repository.detailArticle = sampleArticle(slug = "sample-a1", title = "عنوان المقال الكامل")

        composeRule.setContent { TechVisionDzTheme { AppNavHost(repository = repository, authRepository = FakeAuthRepository.authenticated()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("عنوان في القائمة").assertIsDisplayed()

        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("عنوان المقال الكامل")[0].assertIsDisplayed()
        assertEquals("sample-a1", repository.lastArticleSlug)

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("عنوان في القائمة").assertIsDisplayed()
    }
}