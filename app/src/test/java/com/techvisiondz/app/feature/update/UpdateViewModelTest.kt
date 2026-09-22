package com.techvisiondz.app.feature.update

import android.app.Application
import android.content.Intent
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
import com.techvisiondz.app.core.update.UpdateTestFixtures
import com.techvisiondz.app.core.update.UpdateTransportException
import com.techvisiondz.app.core.update.installerResolutionFor
import com.techvisiondz.app.core.update.permissionOutcome
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * State transitions of the update flow. Repository/verifier/installer/downloader
 * are all fakes; nothing touches the network or the filesystem beyond a temp
 * staging file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var stagedFile: File
    private var deleteCalls = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stagedFile = tempFolder.newFile("update.apk")
        deleteCalls = 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeManifestFetcher(
        var manifest: String?,
        var error: Throwable? = null,
    ) : UpdateManifestFetcher {
        var fetchCount = 0
        override suspend fun fetchManifest(rawUrl: String): String {
            fetchCount++
            error?.let { throw it }
            return manifest ?: throw UpdateTransportException("no manifest configured")
        }
    }

    private class FakeUpdatePreferences : UpdatePreferences {
        private var deferredAt: Long? = null
        private var automaticCheckAt: Long? = null
        var markCount = 0
        var clearCount = 0
        var automaticMarkCount = 0
        override fun lastDeferredAtMillis(): Long? = deferredAt
        override fun markDeferred(timestampMillis: Long) {
            deferredAt = timestampMillis
            markCount++
        }
        override fun lastAutomaticCheckAtMillis(): Long? = automaticCheckAt
        override fun markAutomaticCheck(timestampMillis: Long) {
            automaticCheckAt = timestampMillis
            automaticMarkCount++
        }
        override fun clear() {
            deferredAt = null
            automaticCheckAt = null
            clearCount++
        }
    }

    private class FakeUpdateDownloader : UpdateApkDownloader {
        var body: suspend (url: String, dest: File, onProgress: (Float?) -> Unit) -> Unit = { _, _, _ -> }
        var startCount = 0
        override suspend fun download(url: String, dest: File, onProgress: (Float?) -> Unit) {
            startCount++
            body(url, dest, onProgress)
        }
    }

    private class FakeUpdateVerifier : UpdateApkVerifier {
        var result: ApkVerificationResult = ApkVerificationResult.Success
        var lastApk: File? = null
        override suspend fun verify(apk: File, expected: UpdateInfo): ApkVerificationResult {
            lastApk = apk
            return result
        }
    }

    private class FakeUpdateInstaller : UpdateApkInstaller {
        enum class PermissionState { Allowed, Denied, NotDeclared }

        var permission: PermissionState = PermissionState.Allowed

        /** Whether the fake sees an activity that can open the APK. */
        var resolveActivityResolvable = true

        var result: InstallLaunchResult = InstallLaunchResult.Launched

        /**
         * When set, [launchInstaller] throws this throwable before returning,
         * modeling an unexpectedly failing installer implementation. This is
         * how the historical v1.1.2 bug behaved: the real installer threw a
         * SecurityException instead of returning a result.
         */
        var throwFromLaunch: Throwable? = null
        var lastApk: File? = null

        // Faithful to AndroidUpdateApkInstaller: the probe maps a SecurityException
        // (undeclared permission) into the typed NotDeclared state, never throws.
        override fun installPermissionState(): InstallPermissionState = permissionOutcome {
            when (permission) {
                PermissionState.Allowed -> true
                PermissionState.Denied -> false
                PermissionState.NotDeclared -> throw SecurityException(
                    "Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api",
                )
            }
        }

        override fun resolveInstaller(apk: File): InstallerResolution = installerResolutionFor(
            permission = installPermissionState(),
            activityResolvable = resolveActivityResolvable,
            launchIntent = Intent(),
        )

        override fun launchInstaller(apk: File): InstallLaunchResult {
            lastApk = apk
            throwFromLaunch?.let { throw it }
            // Mirrors AndroidUpdateApkInstaller: the permission check short-circuits
            // before anything is ever handed to the OS installer. "Permission not
            // declared" is a distinct condition from "declared but denied", so it
            // must never be reported as PermissionRequired.
            return when (permission) {
                PermissionState.Allowed -> result
                PermissionState.Denied -> InstallLaunchResult.PermissionRequired
                PermissionState.NotDeclared -> InstallLaunchResult.PermissionNotDeclared
            }
        }

        override fun unknownAppSourcesSettingsIntent(): Intent = Intent()
    }

    private fun viewModel(
        fetcher: FakeManifestFetcher,
        enabled: Boolean = true,
        currentVersionCode: Int = 2,
        downloader: FakeUpdateDownloader = FakeUpdateDownloader(),
        verifier: FakeUpdateVerifier = FakeUpdateVerifier(),
        installer: FakeUpdateInstaller = FakeUpdateInstaller(),
        preferences: FakeUpdatePreferences = FakeUpdatePreferences(),
    ): UpdateViewModel = UpdateViewModel(
        repository = UpdateRepository(
            manifestUrl = UpdateTestFixtures.TRUSTED_MANIFEST_URL,
            currentVersionCode = currentVersionCode,
            enabled = enabled,
            fetcher = fetcher,
        ),
        downloader = downloader,
        verifier = verifier,
        installer = installer,
        preferences = preferences,
        application = Application(),
        stagedApkProvider = { stagedFile },
        deleteStagedApk = {
            deleteCalls++
            stagedFile.delete()
        },
    )

    private fun availableManifest(versionCode: Int = 3) =
        UpdateTestFixtures.manifest(versionCode = versionCode)

    /** A downloader that keeps the flow suspended at the download stage. */
    private fun pausingDownloader() = FakeUpdateDownloader().apply {
        body = { _, _, _ -> awaitCancellation() }
    }

    @Test
    fun `initial check shows checking then resolves to no update`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest(versionCode = 2))
        val viewModel = viewModel(fetcher, currentVersionCode = 2)

        viewModel.checkForUpdate()
        assertEquals(UpdateUiState.Checking, viewModel.uiState.value)

        advanceUntilIdle()
        assertEquals(UpdateUiState.NoUpdate, viewModel.uiState.value)
        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `check reports update available with safe display data`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest(versionCode = 3))
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UpdateUiState.UpdateAvailable)
        assertEquals("1.1.0", (state as UpdateUiState.UpdateAvailable).versionName)
        assertEquals("Bug fixes and improvements.", state.releaseNotes)
    }

    @Test
    fun `later defers the prompt and returns to idle`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences()
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UpdateUiState.UpdateAvailable)

        viewModel.postpone()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertEquals(1, preferences.markCount)
        assertTrue(preferences.lastDeferredAtMillis() != null)
    }

    @Test
    fun `automatic check is skipped during the deferral cooldown`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences().apply { markDeferred(System.currentTimeMillis()) }
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate()
        runCurrent()

        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun `manual check bypasses the deferral cooldown`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences().apply { markDeferred(System.currentTimeMillis()) }
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate(manual = true)
        assertEquals(UpdateUiState.Checking, viewModel.uiState.value)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UpdateUiState.UpdateAvailable)
        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `automatic check is skipped during the automatic check cooldown`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences().apply {
            markAutomaticCheck(System.currentTimeMillis())
        }
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate()
        runCurrent()

        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun `automatic check records the timestamp when it runs`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences()
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate()
        advanceUntilIdle()

        assertEquals(1, preferences.automaticMarkCount)
        assertTrue(preferences.lastAutomaticCheckAtMillis() != null)
    }

    @Test
    fun `manual check bypasses the automatic check cooldown`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences().apply {
            markAutomaticCheck(System.currentTimeMillis())
        }
        val viewModel = viewModel(fetcher, preferences = preferences)

        viewModel.checkForUpdate(manual = true)
        advanceUntilIdle()

        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `updater disabled resolves to idle and never fetches`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher, enabled = false)

        viewModel.checkForUpdate()
        runCurrent()

        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun `update now clears the deferral and starts the download`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val preferences = FakeUpdatePreferences().apply { markDeferred(System.currentTimeMillis()) }
        val downloader = pausingDownloader()
        val viewModel = viewModel(fetcher, preferences = preferences, downloader = downloader)

        viewModel.checkForUpdate(manual = true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is UpdateUiState.UpdateAvailable)

        viewModel.updateNow()
        runCurrent()

        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)
        assertEquals(1, preferences.clearCount)
        assertEquals(1, downloader.startCount)

        // The engagement must not be re-suppressed by the earlier deferral.
        viewModel.checkForUpdate()
        runCurrent()
        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)
    }

    @Test
    fun `update now is ignored unless an update is being offered`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest(versionCode = 2))
        val downloader = FakeUpdateDownloader()
        val viewModel = viewModel(fetcher, downloader = downloader)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        assertEquals(UpdateUiState.NoUpdate, viewModel.uiState.value)

        viewModel.updateNow()
        runCurrent()

        assertEquals(UpdateUiState.NoUpdate, viewModel.uiState.value)
        assertEquals(0, downloader.startCount)
    }

    @Test
    fun `download reports intermediate progress`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val firstChunk = CompletableDeferred<Unit>()
        val downloader = FakeUpdateDownloader().apply {
            body = { _, _, onProgress ->
                onProgress(0.5f)
                firstChunk.await()
                onProgress(1f)
            }
        }
        val viewModel = viewModel(fetcher, downloader = downloader)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        runCurrent()

        assertEquals(UpdateUiState.Downloading(0.5f), viewModel.uiState.value)

        firstChunk.complete(Unit)
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)
    }

    @Test
    fun `continuous download reaches ready to install after verification`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val verifier = FakeUpdateVerifier().apply { result = ApkVerificationResult.Success }
        val viewModel = viewModel(fetcher, verifier = verifier)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)
        assertEquals(stagedFile, verifier.lastApk)
    }

    @Test
    fun `cancel download stops the job and reports cancelled`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher, downloader = pausingDownloader())

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        runCurrent()
        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)

        viewModel.cancelDownload()
        runCurrent()

        assertEquals(UpdateUiState.Cancelled, viewModel.uiState.value)
        assertEquals(1, deleteCalls)
    }

    @Test
    fun `verification failure shows an error and cleans up the staged apk`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val verifier = FakeUpdateVerifier().apply { result = ApkVerificationResult.HashMismatch }
        val viewModel = viewModel(fetcher, verifier = verifier)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        assertEquals(UpdateUiState.Error(R.string.update_error_verify), viewModel.uiState.value)
        assertTrue(deleteCalls >= 1)
    }

    @Test
    fun `successful verification leads to ready to install`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)
    }

    @Test
    fun `install requires permission when the source is not allowed`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply { permission = FakeUpdateInstaller.PermissionState.Denied }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)
    }

    @Test
    fun `install launches the system installer when permitted`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Allowed
            result = InstallLaunchResult.Launched
        }
        val preferences = FakeUpdatePreferences()
        val viewModel = viewModel(fetcher, installer = installer, preferences = preferences)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        viewModel.installUpdate()

        assertEquals(UpdateUiState.InstallerLaunched, viewModel.uiState.value)
        assertEquals(stagedFile, installer.lastApk)
        assertTrue(preferences.clearCount >= 1)
    }

    @Test
    fun `install launch failure surfaces a visible installer error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            result = InstallLaunchResult.LaunchFailed
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallerError(R.string.update_error_launch), viewModel.uiState.value)
    }

    @Test
    fun `install with undeclared permission surfaces a distinct visible error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.NotDeclared
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        viewModel.installUpdate()

        val state = viewModel.uiState.value
        assertTrue(state is UpdateUiState.InstallerError)
        assertEquals(
            R.string.update_error_permission_not_declared,
            (state as UpdateUiState.InstallerError).messageRes,
        )
    }

    @Test
    fun `security exception from the installer never silently stays ready to install`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            throwFromLaunch = SecurityException(
                "Need to declare android.permission.REQUEST_INSTALL_PACKAGES to call this api",
            )
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        viewModel.installUpdate()

        // Regression: the historical bug converted this exception to an
        // UpdateError the result-when never matched, leaving the UI stuck in
        // ReadyToInstall with zero state change. Any installer exception must
        // now produce a deliberate, visible error state.
        assertTrue(viewModel.uiState.value is UpdateUiState.InstallerError)
        assertTrue(viewModel.uiState.value !is UpdateUiState.ReadyToInstall)
    }

    @Test
    fun `unexpected installer exception surfaces a visible installer error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            throwFromLaunch = IllegalStateException("boom")
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallerError(R.string.update_error_launch), viewModel.uiState.value)
    }

    @Test
    fun `installer unavailable surfaces a distinct visible error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Allowed
            result = InstallLaunchResult.InstallerUnavailable
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()

        viewModel.installUpdate()
        assertEquals(
            UpdateUiState.InstallerError(R.string.update_error_installer_unavailable),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `install with missing staged apk surfaces a visible error instead of silent idle`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        // Cache cleared while the update was ready.
        stagedFile.delete()
        viewModel.installUpdate()

        // Regression: this used to silently reset to Idle, losing the update
        // with no explanation. It must now be a visible, actionable error.
        assertEquals(UpdateUiState.InstallerError(R.string.update_error_apk_missing), viewModel.uiState.value)
    }

    @Test
    fun `check below the manifest minimum surfaces too old and never downloads`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(
            manifest = UpdateTestFixtures.manifest(versionCode = 5, minimumVersionCode = 4),
        )
        val downloader = FakeUpdateDownloader()
        val viewModel = viewModel(
            fetcher,
            currentVersionCode = 2,
            downloader = downloader,
        )

        viewModel.checkForUpdate()
        advanceUntilIdle()

        assertEquals(UpdateUiState.Error(R.string.update_error_too_old), viewModel.uiState.value)
        assertEquals(0, downloader.startCount)
    }

    @Test
    fun `settings return after grant resumes to ready to install without redownload`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Denied
        }
        val downloader = FakeUpdateDownloader()
        val viewModel = viewModel(fetcher, installer = installer, downloader = downloader)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)
        assertEquals(1, downloader.startCount)

        // User grants the permission in Settings and returns.
        installer.permission = FakeUpdateInstaller.PermissionState.Allowed
        viewModel.onPermissionSettingsReturned()

        // The already-verified staged APK is reused: back to ReadyToInstall,
        // and the downloader was never called again.
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)
        assertEquals(1, downloader.startCount)
    }

    @Test
    fun `settings return while still denied stays on permission required`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Denied
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)

        viewModel.onPermissionSettingsReturned()

        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)
    }

    @Test
    fun `settings return with missing staged apk surfaces a visible error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Denied
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)

        // The staged APK disappears while the user is in Settings.
        stagedFile.delete()
        installer.permission = FakeUpdateInstaller.PermissionState.Allowed
        viewModel.onPermissionSettingsReturned()

        assertEquals(UpdateUiState.InstallerError(R.string.update_error_apk_missing), viewModel.uiState.value)
    }

    @Test
    fun `settings return with undeclared permission surfaces a distinct visible error`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val installer = FakeUpdateInstaller().apply {
            permission = FakeUpdateInstaller.PermissionState.Denied
        }
        val viewModel = viewModel(fetcher, installer = installer)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        viewModel.installUpdate()
        assertEquals(UpdateUiState.InstallationPermissionRequired, viewModel.uiState.value)

        // Should be unreachable on v1.1.3+ binaries, but the live probe can
        // still report it; the flow must map it cleanly instead of crashing.
        installer.permission = FakeUpdateInstaller.PermissionState.NotDeclared
        viewModel.onPermissionSettingsReturned()

        assertEquals(
            UpdateUiState.InstallerError(R.string.update_error_permission_not_declared),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `retry after a failed check re-runs the check`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(
            manifest = availableManifest(),
            error = UpdateTransportException("down"),
        )
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        assertEquals(UpdateUiState.Error(R.string.update_error_network), viewModel.uiState.value)

        fetcher.error = null
        fetcher.manifest = availableManifest(versionCode = 2)
        viewModel.checkForUpdate(manual = true)
        advanceUntilIdle()

        assertEquals(UpdateUiState.NoUpdate, viewModel.uiState.value)
        assertEquals(2, fetcher.fetchCount)
    }

    @Test
    fun `check is ignored while a download is running`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher, downloader = pausingDownloader())

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        runCurrent()
        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)

        viewModel.checkForUpdate(manual = true)
        runCurrent()

        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)
        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `dismiss is ignored while a download is running`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher, downloader = pausingDownloader())

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        runCurrent()
        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)

        viewModel.dismiss()
        assertTrue(viewModel.uiState.value is UpdateUiState.Downloading)
    }

    @Test
    fun `dismiss after ready returns to idle`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = availableManifest())
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.updateNow()
        advanceUntilIdle()
        assertEquals(UpdateUiState.ReadyToInstall, viewModel.uiState.value)

        viewModel.dismiss()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `malformed manifest never leaks repository details into state`() = runTest(dispatcher) {
        val fetcher = FakeManifestFetcher(manifest = "{ not json")
        val viewModel = viewModel(fetcher)

        viewModel.checkForUpdate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UpdateUiState.Error)
        assertEquals(R.string.update_error_cannot_check, (state as UpdateUiState.Error).messageRes)
    }
}