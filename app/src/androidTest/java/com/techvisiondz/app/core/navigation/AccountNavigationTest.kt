package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.core.data.repository.FakeProfileRepository
import com.techvisiondz.app.core.data.repository.sampleUserProfile
import com.techvisiondz.app.core.settings.AppLanguage
import com.techvisiondz.app.core.settings.FakeSettingsPreferences
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.settings.SettingsViewModel
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Account-profile navigation tests using a fake [ProfileRepository] and a fake
 * [com.techvisiondz.app.core.data.repository.AuthRepository] so the whole flow
 * is deterministic: Account stays authenticated-only (guests are sent to Sign
 * In and land on Account after signing in), and signing out returns to the
 * public Home instead of the auth flow.
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
    fun guestAccountClickRequiresSignInThenOpensAccount() {
        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = FakeArticleRepository(articles = emptyList()),
                    authRepository = FakeAuthRepository.unauthenticated(),
                    profileRepository = FakeProfileRepository(
                        profile = sampleUserProfile(
                            id = "test-user",
                            email = "reader@example.com",
                            displayName = "Yasmine",
                        ),
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sign_in_email").assertIsDisplayed()

        composeRule.onNodeWithTag("sign_in_email").performTextInput("reader@example.com")
        composeRule.onNodeWithTag("sign_in_password").performTextInput("password123")
        composeRule.onNodeWithTag("sign_in_submit").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.account_title)).assertIsDisplayed()
        composeRule.onNodeWithText("reader@example.com").assertIsDisplayed()
    }

    @Test
    fun signingOutOnAccountReturnsToPublicHome() {
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

        composeRule.onNodeWithTag("home_account").assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_email").assertDoesNotExist()
    }

    @Test
    fun settingsRowOnAccountOpensSettingsScreen() {
        composeRule.setContent {
            TechVisionDzTheme {
                AppNavHost(
                    repository = FakeArticleRepository(articles = emptyList()),
                    authRepository = FakeAuthRepository.authenticated(),
                    profileRepository = FakeProfileRepository(),
                    settingsViewModel = SettingsViewModel(
                        FakeSettingsPreferences(),
                    ) { AppLanguage.ENGLISH },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_account").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("account_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.settings))
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.settings_language),
        ).assertIsDisplayed()
    }
}