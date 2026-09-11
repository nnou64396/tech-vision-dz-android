package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

/**
 * Auth-navigation tests for the public-by-default graph: public content is
 * always visible (even while [com.techvisiondz.app.core.data.AuthState.Loading]
 * restores the session), guests are never evicted from public screens, and
 * authentication is only required for protected actions such as saving an
 * article.
 */
class AuthNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingStateDoesNotBlockPublicHome() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        val authRepository = FakeAuthRepository()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("المقال الأول").assertIsDisplayed()
    }

    @Test
    fun unauthenticatedStateShowsPublicHomeFeed() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        val authRepository = FakeAuthRepository.unauthenticated()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("المقال الأول").assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_email").assertDoesNotExist()
    }

    @Test
    fun authenticatedStateShowsHomeFeed() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        val authRepository = FakeAuthRepository.authenticated()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("المقال الأول").assertIsDisplayed()
    }

    @Test
    fun guestCanBrowsePublicArticleDetail() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        articleRepository.detailArticle = sampleArticle(slug = "sample-a1", title = "تفاصيل المقال")
        val authRepository = FakeAuthRepository.unauthenticated()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("تفاصيل المقال")[0].assertIsDisplayed()
    }

    @Test
    fun guestBookmarkRequiresSignInAndReturnsToArticle() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        articleRepository.detailArticle = sampleArticle(slug = "sample-a1", title = "تفاصيل المقال")
        val authRepository = FakeAuthRepository.unauthenticated()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("تفاصيل المقال")[0].assertIsDisplayed()

        composeRule.onNodeWithTag("article_bookmark").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sign_in_email").assertIsDisplayed()

        composeRule.onNodeWithTag("sign_in_email").performTextInput("reader@example.com")
        composeRule.onNodeWithTag("sign_in_password").performTextInput("password123")
        composeRule.onNodeWithTag("sign_in_submit").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("تفاصيل المقال")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("article_bookmark").assertIsDisplayed()
    }

    @Test
    fun unauthenticatedStateDoesNotEvictPublicScreens() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "المقال الأول")),
        )
        articleRepository.detailArticle = sampleArticle(slug = "sample-a1", title = "تفاصيل المقال")
        val authRepository = FakeAuthRepository.authenticated()

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = articleRepository,
                    authRepository = authRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("تفاصيل المقال")[0].assertIsDisplayed()

        authRepository.forceUnauthenticated()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("تفاصيل المقال")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("article_bookmark").assertIsDisplayed()
    }
}