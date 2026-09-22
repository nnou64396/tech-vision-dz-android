package com.techvisiondz.app.feature.update

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for [UpdateDialog]. Pure state rendering
 * tests: no network, no ViewModel, no device timing dependency.
 */
class UpdateDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = composeRule.activity

    private fun setDialog(state: UpdateUiState, onUpdateNow: () -> Unit = {}, onLater: () -> Unit = {}) {
        composeRule.setContent {
            TechVisionDzTheme {
                UpdateDialog(
                    state = state,
                    onUpdateNow = onUpdateNow,
                    onLater = onLater,
                    onCancelDownload = {},
                    onInstall = {},
                    onOpenSettings = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun updateAvailableShowsActionButtonsAndReleaseNotes() {
        setDialog(
            state = UpdateUiState.UpdateAvailable(
                versionName = "1.1.0",
                releaseNotes = "Bug fixes and improvements.",
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.update_now)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_later)).assertIsDisplayed()
        composeRule.onNodeWithText("Bug fixes and improvements.").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertDoesNotExist()
    }

    @Test
    fun updateAvailableNeverShowsRawUrls() {
        setDialog(state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null))

        composeRule.onNodeWithText("https://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("github.com", substring = true).assertDoesNotExist()
    }

    @Test
    fun downloadingShowsProgressAndCancel() {
        setDialog(state = UpdateUiState.Downloading(progress = 0.42f))

        composeRule.onNodeWithText(
            context.getString(R.string.update_downloading_percent, 42),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_cancel)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_now)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_later)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertDoesNotExist()
    }

    @Test
    fun downloadingWithoutProgressShowsIndeterminateText() {
        setDialog(state = UpdateUiState.Downloading(progress = null))

        composeRule.onNodeWithText(context.getString(R.string.update_downloading)).assertIsDisplayed()
    }

    @Test
    fun verifyingShowsVerificationTextAndNoActions() {
        setDialog(state = UpdateUiState.Verifying)

        composeRule.onNodeWithText(context.getString(R.string.update_verifying)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_cancel)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_now)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertDoesNotExist()
    }

    @Test
    fun readyToInstallShowsInstallAndLater() {
        setDialog(state = UpdateUiState.ReadyToInstall)

        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_later)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_now)).assertDoesNotExist()
    }

    @Test
    fun permissionRequiredShowsOpenSettingsAndInstall() {
        setDialog(state = UpdateUiState.InstallationPermissionRequired)

        composeRule.onNodeWithText(context.getString(R.string.update_open_settings)).assertIsDisplayed()
        // The secondary action is Install: it re-checks the permission after the
        // settings detour and resumes, instead of forcing "Not now"/postpone.
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertIsDisplayed()
    }

    @Test
    fun installedStateShowsDoneAndNeverClaimsAUrl() {
        setDialog(state = UpdateUiState.InstallerLaunched)

        composeRule.onNodeWithText(context.getString(R.string.update_done)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_installing_body)).assertIsDisplayed()
        composeRule.onNodeWithText("https://", substring = true).assertDoesNotExist()
    }

    @Test
    fun errorStateRendersNothingAndNeverExposesStackTraces() {
        setDialog(state = UpdateUiState.Error(R.string.update_error_network))

        composeRule.onNodeWithText(
            context.getString(R.string.update_error_network),
        ).assertDoesNotExist()
        composeRule.onNodeWithText("Exception", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("at com.techvisiondz", substring = true).assertDoesNotExist()
        // No action buttons leak out of the dialog for check-time failures.
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertDoesNotExist()
    }

    @Test
    fun installerErrorShowsMessageAndDismissInsteadOfClosingSilently() {
        setDialog(state = UpdateUiState.InstallerError(R.string.update_error_launch))

        composeRule.onNodeWithText(context.getString(R.string.update_error_launch)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_dismiss)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertDoesNotExist()
        composeRule.onNodeWithText("Exception", substring = true).assertDoesNotExist()
    }

    @Test
    fun installerErrorWithUndeclaredPermissionShowsManualInstallGuidance() {
        setDialog(
            state = UpdateUiState.InstallerError(R.string.update_error_permission_not_declared),
        )

        composeRule.onNodeWithText(
            context.getString(R.string.update_error_permission_not_declared),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_dismiss)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.update_install)).assertDoesNotExist()
    }

    @Test
    fun clickingUpdateNowInvokesCallback() {
        var clicked = false
        setDialog(
            state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null),
            onUpdateNow = { clicked = true },
        )

        composeRule.onNodeWithText(context.getString(R.string.update_now)).performClick()
        composeRule.waitForIdle()

        assert(clicked)
    }

    @Test
    fun clickingLaterInvokesCallback() {
        var clicked = false
        setDialog(
            state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null),
            onLater = { clicked = true },
        )

        composeRule.onNodeWithText(context.getString(R.string.update_later)).performClick()
        composeRule.waitForIdle()

        assert(clicked)
    }
}