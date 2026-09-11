package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

/**
 * Auth-gating navigation tests: verifies the correct screen is shown for each
 * [com.techvisiondz.app.core.data.AuthState] and that a successful sign-in
 * transitions to the Home screen.
 */
class AuthNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingStateShowsFullScreenLoading() {
        val articleRepository = FakeArticleRepository(articles = emptyList())
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

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.loading))
            .assertIsDisplayed()
    }

    @Test
    fun unauthenticatedStateShowsSignIn() {
        val articleRepository = FakeArticleRepository(articles = emptyList())
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

        composeRule.onNodeWithTag("sign_in_email").assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_password").assertIsDisplayed()
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
    fun signInSuccessNavigatesToHomeFeed() {
        val articleRepository = FakeArticleRepository(
            articles = listOf(sampleArticleCard(id = "a1", title = "مرحبا بالعالم")),
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

        composeRule.onNodeWithTag("sign_in_email").performTextInput("reader@example.com")
        composeRule.onNodeWithTag("sign_in_password").performTextInput("password123")
        composeRule.onNodeWithTag("sign_in_submit").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("مرحبا بالعالم").assertIsDisplayed()
    }
}