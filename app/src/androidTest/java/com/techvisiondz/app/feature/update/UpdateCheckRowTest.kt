package com.techvisiondz.app.feature.update

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for [UpdateCheckRow]. Pure state rendering;
 * no network, no ViewModel.
 */
class UpdateCheckRowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = composeRule.activity

    private fun setRow(
        state: UpdateUiState,
        onCheckForUpdate: () -> Unit = {},
        onRetry: () -> Unit = {},
        onUpdate: () -> Unit = {},
    ) {
        composeRule.setContent {
            TechVisionDzTheme {
                UpdateCheckRow(
                    state = state,
                    onCheckForUpdate = onCheckForUpdate,
                    onRetry = onRetry,
                    onUpdate = onUpdate,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun idleShowsPromptAndCheckNowButton() {
        setRow(state = UpdateUiState.Idle)

        composeRule.onNodeWithText(context.getString(R.string.update_check)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_check_now)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertDoesNotExist()
    }

    @Test
    fun checkingShowsProgressAndNoButtons() {
        setRow(state = UpdateUiState.Checking)

        composeRule.onNodeWithText(context.getString(R.string.update_checking)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_check_now)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertDoesNotExist()
    }

    @Test
    fun noUpdateShowsUpToDateAndCheckAgain() {
        setRow(state = UpdateUiState.NoUpdate)

        composeRule.onNodeWithText(context.getString(R.string.update_up_to_date)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_check_again)).assertIsDisplayed()
    }

    @Test
    fun updateAvailableShowsVersionAndUpdateAction() {
        setRow(state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null))

        composeRule.onNodeWithText(
            context.getString(R.string.update_available_row, "1.1.0"),
        ).assertIsDisplayed()
        // The row label and the action button can both read "Update"; the action
        // is the node that is clickable.
        composeRule.onNode(hasClickAction() and hasText(context.getString(R.string.update)))
            .assertIsDisplayed()
    }

    @Test
    fun readyToInstallShowsInstallAction() {
        setRow(state = UpdateUiState.ReadyToInstall)

        composeRule.onNode(hasClickAction() and hasText(context.getString(R.string.update_install)))
            .assertIsDisplayed()
    }

    @Test
    fun errorShowsLocalizedMessageAndRetry() {
        setRow(state = UpdateUiState.Error(R.string.update_error_network))

        composeRule.onNodeWithText(context.getString(R.string.update_error_network)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertIsDisplayed()
        composeRule.onNodeWithText("Exception", substring = true).assertDoesNotExist()
    }

    @Test
    fun clickingCheckNowAndRetryAndUpdateInvokeCallbacks() {
        var checkClicks = 0
        var retryClicks = 0
        var updateClicks = 0

        setRow(
            state = UpdateUiState.Idle,
            onCheckForUpdate = { checkClicks++ },
            onRetry = { retryClicks++ },
            onUpdate = { updateClicks++ },
        )
        composeRule.onNodeWithText(context.getString(R.string.update_check_now)).performClick()

        setRow(
            state = UpdateUiState.Error(R.string.update_error_network),
            onCheckForUpdate = { checkClicks++ },
            onRetry = { retryClicks++ },
            onUpdate = { updateClicks++ },
        )
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()

        setRow(
            state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null),
            onCheckForUpdate = { checkClicks++ },
            onRetry = { retryClicks++ },
            onUpdate = { updateClicks++ },
        )
        composeRule.onNode(hasClickAction() and hasText(context.getString(R.string.update))).performClick()
        composeRule.waitForIdle()

        assertTrue(checkClicks == 1)
        assertTrue(retryClicks == 1)
        assertTrue(updateClicks == 1)
    }
}