package com.techvisiondz.app.feature.update

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.techvisiondz.app.R
import com.techvisiondz.app.core.update.ApkVerificationResult
import com.techvisiondz.app.core.update.InstallLaunchResult
import com.techvisiondz.app.core.update.InstallPermissionState
import com.techvisiondz.app.core.update.InstallerResolution
import com.techvisiondz.app.core.update.UpdateApkDownloader
import com.techvisiondz.app.core.update.UpdateApkInstaller
import com.techvisiondz.app.core.update.UpdateApkVerifier
import com.techvisiondz.app.core.update.UpdateInfo
import com.techvisiondz.app.core.update.UpdateManifestFetcher
import com.techvisiondz.app.core.update.UpdatePreferences
import com.techvisiondz.app.core.update.UpdateRepository
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for [UpdateDialogHost].
 *
 * These exist because the host is mounted once at the app root ([TechVisionDzApp])
 * and must not crash that first composition. Regression of the v1.1.5 startup
 * crash on Android 16 (API 36) devices:
 * `IllegalStateException: No ActivityResultRegistryOwner was provided via
 * LocalActivityResultRegistryOwner` — thrown by the unconditional
 * `rememberLauncherForActivityResult` that used to live at the root of this
 * composable. The launcher is only ever needed while the dialog shows the
 * "install unknown apps" settings detour
 * ([UpdateUiState.InstallationPermissionRequired]), so the fix registers it
 * conditionally, only in that state.
 */
class UpdateDialogHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val strings get() = composeRule.activity

    // --- Fakes (device-faithful ports of the JVM test doubles) -------------

    private class FakeManifestFetcher(private val manifest: String) : UpdateManifestFetcher {
        var fetchCount = 0
        override suspend fun fetchManifest(rawUrl: String): String {
            fetchCount++
            return manifest
        }
    }

    private class FakeUpdatePreferences : UpdatePreferences {
        override fun lastDeferredAtMillis(): Long? = null
        override fun markDeferred(timestampMillis: Long) = Unit
        override fun lastAutomaticCheckAtMillis(): Long? = null
        override fun markAutomaticCheck(timestampMillis: Long) = Unit
        override fun clear() = Unit
    }

    private class FakeUpdateDownloader : UpdateApkDownloader {
        override suspend fun download(url: String, dest: File, onProgress: (Float?) -> Unit) = Unit
    }

    private class FakeUpdateVerifier : UpdateApkVerifier {
        override suspend fun verify(apk: File, expected: UpdateInfo): ApkVerificationResult =
            ApkVerificationResult.Success
    }

    private class FakeUpdateInstaller(private val permission: InstallPermissionState) :
        UpdateApkInstaller {
        override fun installPermissionState(): InstallPermissionState = permission
        override fun resolveInstaller(apk: File): InstallerResolution = InstallerResolution.PermissionRequired
        override fun launchInstaller(apk: File): InstallLaunchResult =
            if (permission == InstallPermissionState.Allowed) {
                InstallLaunchResult.Launched
            } else {
                InstallLaunchResult.PermissionRequired
            }
        override fun unknownAppSourcesSettingsIntent(): Intent = Intent()
    }

    private val manifestJson: String =
        """{"versionCode":9,"versionName":"1.1.6","downloadUrl":"https://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.6/app-release.apk","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","releaseNotes":"Test release.","minimumVersionCode":8}"""

    private fun viewModel(stagedApk: File): UpdateViewModel {
        val application: Application = ApplicationProvider.getApplicationContext()
        return UpdateViewModel(
            repository = UpdateRepository(
                manifestUrl = "https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json",
                currentVersionCode = 8,
                enabled = true,
                fetcher = FakeManifestFetcher(manifestJson),
            ),
            downloader = FakeUpdateDownloader(),
            verifier = FakeUpdateVerifier(),
            installer = FakeUpdateInstaller(InstallPermissionState.Denied),
            preferences = FakeUpdatePreferences(),
            application = application,
            stagedApkProvider = { stagedApk },
            deleteStagedApk = { stagedApk.delete() },
        )
    }

    // --- Regression tests --------------------------------------------------

    /**
     * The v1.1.5 startup crash. The launcher used to be registered
     * unconditionally at the root of the host's composition; on the Infinix
     * X6873 (Android 16) no `ActivityResultRegistryOwner` resolved at first
     * frame and `rememberLauncherForActivityResult` threw the exact
     * `IllegalStateException` seen in `adb logcat -b crash`. That condition is
     * reproduced here by providing a non-activity [LocalContext] (so
     * `LocalActivityResultRegistryOwner.current` resolves to null) while the
     * host is in its initial Idle state — the startup path. This test fails on
     * the pre-fix code and passes after the fix.
     */
    @Test
    fun idleHostComposesWithoutAnActivityResultRegistryOwner() {
        val staged = stagedApk()
        val viewModel = viewModel(staged)
        try {
            // Application context is not an ActivityResultRegistryOwner, and the
            // registry-owner composition local is unset here, so any launcher
            // registration attempt throws — as it did on the device at startup.
            val nonActivityContext: Context = ApplicationProvider.getApplicationContext()
            composeRule.setContent {
                CompositionLocalProvider(LocalContext provides nonActivityContext) {
                    TechVisionDzTheme {
                        UpdateDialogHost(updateViewModel = viewModel)
                    }
                }
            }
            composeRule.waitForIdle()

            // Idle renders no dialog and, critically, no crash.
            composeRule.onNodeWithText(strings.getString(R.string.update_now)).assertDoesNotExist()
        } finally {
            staged.delete()
        }
    }

    /**
     * The permission detour is the one place the settings launcher is required.
     * With a real `ComponentActivity` host (owner present) and the state driven
     * to [UpdateUiState.InstallationPermissionRequired], the Open settings
     * button must still be rendered — proving the conditional registration is
     * alive exactly when needed and dead at every other time.
     */
    @Test
    fun permissionRequiredDrivesTheConditionalLauncherAndShowsOpenSettings() {
        val staged = stagedApk()
        val viewModel = viewModel(staged)
        try {
            // Drive the full flow on the main thread; fakes don't suspend, so the
            // immediate dispatcher completes the whole chain in-line.
            composeRule.runOnUiThread {
                viewModel.checkForUpdate(manual = true)
                viewModel.updateNow()
                viewModel.installUpdate()
            }
            composeRule.waitForIdle()

            assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)

            composeRule.setContent {
                TechVisionDzTheme {
                    UpdateDialogHost(updateViewModel = viewModel)
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithText(strings.getString(R.string.update_open_settings))
                .assertIsDisplayed()
            composeRule.onNodeWithText(strings.getString(R.string.update_install))
                .assertIsDisplayed()
        } catch (t: Throwable) {
            // Diagnostics: dump the tree instead of failing blind on a layout mismatch.
            android.util.Log.e("UpdateDialogHostTest", "assert failed", t)
            android.util.Log.e(
                "UpdateDialogHostTest",
                composeRule.onRoot(useUnmergedTree = true).printToString(),
            )
            throw t
        } finally {
            staged.delete()
        }
    }

    /** A real, existing file at the staged-APK path `installUpdate()` checks. */
    private fun stagedApk(): File {
        val application: Application = ApplicationProvider.getApplicationContext()
        val file = File(application.cacheDir, "update-dialog-host-test.apk")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x50, 0x4B)) // PK zip header; never really installed
        return file
    }
}