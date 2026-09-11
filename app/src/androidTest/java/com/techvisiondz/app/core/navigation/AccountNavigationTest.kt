package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.core.data.repository.FakeProfileRepository
import com.techvisiondz.app.core.data.repository.sampleUserProfile
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Account-profile navigation tests using a fake [ProfileRepository] and a fake
 * [com.techvisiondz.app.core.data.repository.AuthRepository] so the whole flow
 * is deterministic: open Account from Home, sign out, and confirm the user is
 * returned to the auth graph.
 */
class AccountNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun authenticatedUserCanOpenAccountProfileFromHome() {
        val profileRepository = FakeProfileRepository(
            profile = sampleUserProfile(
                id = "test-user",
                email = "reader@example.com",
                displayName = "Yasmine",
            ),
        )

        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = FakeArticleRepository(articles = emptyList()),
                    authRepository = FakeAuthRepository.authenticated(),
                    profileRepository = profileRepository,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_account").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.account_title)).assertIsDisplayed()
        composeRule.onNodeWithText("reader@example.com").assertIsDisplayed()
        assertEquals(1, profileRepository.loadCalls)
    }

    @Test
    fun signOutReturnsUserToSignInScreen() {
        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = FakeArticleRepository(articles = emptyList()),
                    authRepository = FakeAuthRepository.authenticated(),
                    profileRepository = FakeProfileRepository(),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_sign_out").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.auth_sign_in_title)).assertIsDisplayed()
    }
}