package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.core.data.repository.FakeProfileRepository
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.feature.saved.FakeSavedArticleRepository
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Saved-articles navigation tests using fakes for every repository so the
 * whole flow is deterministic: open Saved Articles from Account, confirm the
 * empty/list states, open a saved article's details, and confirm that losing
 * the session returns the user to the public Home.
 */
class SavedNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(
        authRepository: FakeAuthRepository = FakeAuthRepository.authenticated(),
        savedArticles: FakeSavedArticleRepository = FakeSavedArticleRepository(),
        articleRepository: FakeArticleRepository = FakeArticleRepository(articles = emptyList()),
        profileRepository: FakeProfileRepository = FakeProfileRepository(),
    ) {
        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    savedArticleRepository = savedArticles,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun openSavedArticlesFromAccount() {
        composeRule.onNodeWithTag("home_account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_saved_articles").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun authenticatedUserCanOpenSavedArticlesFromAccount() {
        setContent()

        openSavedArticlesFromAccount()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.saved_articles_title)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.saved_articles_empty)).assertIsDisplayed()
    }

    @Test
    fun savedArticleCardOpensTheArticleDetailScreen() {
        val articleRepository = FakeArticleRepository(articles = emptyList())
        articleRepository.detailArticle = sampleArticle(slug = "sample-a1", title = "Kitchen sink deep dive")
        setContent(
            savedArticles = FakeSavedArticleRepository(
                savedArticles = listOf(sampleArticleCard(id = "a1", title = "Kitchen sink deep dive")),
            ),
            articleRepository = articleRepository,
        )

        openSavedArticlesFromAccount()
        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Kitchen sink deep dive")[0].assertIsDisplayed()
        assertEquals("sample-a1", articleRepository.lastArticleSlug)
    }

    @Test
    fun signingOutWhileOnSavedArticlesReturnsToPublicHome() {
        val authRepository = FakeAuthRepository.authenticated()
        setContent(authRepository = authRepository)

        openSavedArticlesFromAccount()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.saved_articles_title)).assertIsDisplayed()

        authRepository.forceUnauthenticated()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_account").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.auth_sign_in_title)).assertDoesNotExist()
    }
}