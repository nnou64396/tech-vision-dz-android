package com.techvisiondz.app.feature.update

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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
 * LocalActivityResultRegistryOwner` on the install-source permission path.
 * Settings returns no result data, so the host now rechecks permission when
 * the app resumes rather than registering an Activity Result launcher.
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

    private class FakeUpdateInstaller(var permission: InstallPermissionState) :
        UpdateApkInstaller {
        override fun installPermissionState(): InstallPermissionState = permission
        override fun resolveInstaller(apk: File): InstallerResolution = InstallerResolution.PermissionRequired
        override fun launchInstaller(apk: File): InstallLaunchResult =
            if (permission == InstallPermissionState.Allowed) {
                InstallLaunchResult.Launched
            } else {
                InstallLaunchResult.PermissionRequired
            }
        override fun unknownAppSourcesSettingsIntent(): Intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse(
                    "package:${ApplicationProvider.getApplicationContext<Application>().packageName}",
                ),
            )
    }

    private val manifestJson: String =
        """{"versionCode":9,"versionName":"1.1.6","downloadUrl":"https://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.6/app-release.apk","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","releaseNotes":"Test release.","minimumVersionCode":8}"""

    private class TestLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)

        fun simulateSettingsRoundTrip() {
            lifecycle.currentState = Lifecycle.State.CREATED
            lifecycle.currentState = Lifecycle.State.RESUMED
        }
    }

    private fun viewModel(
        stagedApk: File,
        installer: FakeUpdateInstaller = FakeUpdateInstaller(InstallPermissionState.Denied),
    ): UpdateViewModel {
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
            installer = installer,
            preferences = FakeUpdatePreferences(),
            application = application,
            stagedApkProvider = { stagedApk },
            deleteStagedApk = { stagedApk.delete() },
        )
    }

    // --- Regression tests --------------------------------------------------

    @Test
    fun permissionSettingsRoundTripWorksWithoutAnActivityResultRegistryOwner() {
        val staged = stagedApk()
        val installer = FakeUpdateInstaller(InstallPermissionState.Denied)
        val viewModel = viewModel(staged, installer)
        val lifecycleOwner = TestLifecycleOwner()
        composeRule.runOnUiThread {
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        val launchedIntents = mutableListOf<Intent>()
        val applicationContext: Context = ApplicationProvider.getApplicationContext()
        val contextWithoutActivityResultOwner = object : ContextWrapper(applicationContext) {
            override fun startActivity(intent: Intent) {
                launchedIntents += intent
            }
        }
        try {
            composeRule.runOnUiThread {
                viewModel.checkForUpdate(manual = true)
                viewModel.updateNow()
                viewModel.installUpdate()
            }
            composeRule.waitForIdle()

            assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)

            composeRule.setContent {
                CompositionLocalProvider(
                    LocalContext provides contextWithoutActivityResultOwner,
                    LocalLifecycleOwner provides lifecycleOwner,
                ) {
                    TechVisionDzTheme {
                        UpdateDialogHost(updateViewModel = viewModel)
                    }
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithText(strings.getString(R.string.update_open_settings))
                .assertIsDisplayed()
            composeRule.onNodeWithText(strings.getString(R.string.update_install))
                .assertIsDisplayed()

            composeRule.onNodeWithText(strings.getString(R.string.update_open_settings))
                .performClick()
            composeRule.waitForIdle()

            assertEquals(1, launchedIntents.size)
            assertEquals(
                "android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                launchedIntents.single().action,
            )
            assertEquals(
                "package:${strings.packageName}",
                launchedIntents.single().dataString,
            )

            installer.permission = InstallPermissionState.Allowed
            composeRule.runOnUiThread { lifecycleOwner.simulateSettingsRoundTrip() }
            composeRule.waitForIdle()

            assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)
            composeRule.onNodeWithText(strings.getString(R.string.update_ready_to_install))
                .assertIsDisplayed()
        } catch (t: Throwable) {
            android.util.Log.e("UpdateDialogHostTest", "assert failed", t)
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