package com.techvisiondz.app.feature.update

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.update.AndroidUpdateApkInstaller
import com.techvisiondz.app.core.update.AndroidUpdateApkVerifier
import com.techvisiondz.app.core.update.ApkVerificationResult
import com.techvisiondz.app.core.update.InstallLaunchResult
import com.techvisiondz.app.core.update.OkHttpUpdateApkDownloader
import com.techvisiondz.app.core.update.SharedPrefsUpdatePreferences
import com.techvisiondz.app.core.update.UpdateApkDownloader
import com.techvisiondz.app.core.update.UpdateApkInstaller
import com.techvisiondz.app.core.update.UpdateApkVerifier
import com.techvisiondz.app.core.update.UpdateCheckResult
import com.techvisiondz.app.core.update.UpdateError
import com.techvisiondz.app.core.update.UpdateInfo
import com.techvisiondz.app.core.update.UpdatePreferences
import com.techvisiondz.app.core.update.UpdateRepository
import com.techvisiondz.app.core.update.UpdateStash
import com.techvisiondz.app.R
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrates the update flow against the [com.techvisiondz.app.core.update]
 * data layer: check → download → verify → installer launch.
 *
 * Threading: every function runs from the main thread (Compose events);
 * network and verification work is delegated to the suspend repository /
 * downloader / verifier, which move to `Dispatchers.IO` internally. The
 * installer and preferences are called on the main thread only and are cheap.
 * Coroutines live in [viewModelScope] (killed with the ViewModel); no
 * GlobalScope or manually managed threads are used.
 *
 * Guard rails:
 *  - a single manifest check at a time,
 *  - a single download at a time (started only from [UpdateUiState.UpdateAvailable]),
 *  - a single installer launch (the state leaves [UpdateUiState.ReadyToInstall]
 *    synchronously, so a second tap cannot launch twice),
 *  - an APK is only ever passed to the installer after [UpdateApkVerifier]
 *    reports [ApkVerificationResult.Success] — or, on the permission detour,
 *    the same still-staged verified APK,
 *  - cancelled or failed downloads clean up the staged file via [UpdateStash].
 *
 * Deferral: "Later" records a timestamp in [UpdatePreferences]; automatic
 * checks ([checkForUpdate] with [manual] = false) are suppressed for the
 * configured cooldown. Manual checks always run.
 */
class UpdateViewModel(
    private val repository: UpdateRepository,
    private val downloader: UpdateApkDownloader,
    private val verifier: UpdateApkVerifier,
    private val installer: UpdateApkInstaller,
    private val preferences: UpdatePreferences,
    private val application: Application,
    private val deferralCooldownMillis: Long = DEFAULT_DEFERRAL_COOLDOWN_MILLIS,
    // Automatic checks are additionally throttled to at most one per
    // automaticCheckCooldownMillis, so a fresh launch does not hammer the update
    // channel. Manual checks always run and reset the throttle.
    private val automaticCheckCooldownMillis: Long = DEFAULT_AUTOMATIC_CHECK_COOLDOWN_MILLIS,
    // Test seam: lets JVM tests stage and clean up the APK without an Android
    // Context. Defaults keep production behavior on UpdateStash (cacheDir/updater).
    private val stagedApkProvider: () -> File = { UpdateStash.apk(application) },
    private val deleteStagedApk: () -> Boolean = { UpdateStash.deleteApk(application) },
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var pendingUpdate: UpdateInfo? = null
    private var checkInProgress = false
    private var downloadJob: Job? = null

    /**
     * Checks for an update. With [manual] = false (automatic checks) the
     * pending deferral cooldown is honoured; manual checks always run.
     */
    fun checkForUpdate(manual: Boolean = false) {
        if (checkInProgress || downloadJob?.isActive == true) return
        if (!manual && (isWithinDeferralCooldown() || isWithinAutomaticCheckCooldown())) return

        checkInProgress = true
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            try {
                when (val result = repository.checkForUpdate()) {
                    UpdateCheckResult.Disabled -> _uiState.value = UpdateUiState.Idle
                    UpdateCheckResult.NoUpdate -> {
                        pendingUpdate = null
                        _uiState.value = UpdateUiState.NoUpdate
                    }
                    is UpdateCheckResult.UpdateAvailable -> {
                        pendingUpdate = result.update
                        _uiState.value = UpdateUiState.UpdateAvailable(
                            versionName = result.update.versionName,
                            releaseNotes = result.update.releaseNotes,
                        )
                    }
                    is UpdateCheckResult.Failed -> {
                        pendingUpdate = null
                        _uiState.value = UpdateUiState.Error(updateErrorMessageRes(result.error))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pendingUpdate = null
                _uiState.value = UpdateUiState.Idle
            } finally {
                checkInProgress = false
                if (!manual) preferences.markAutomaticCheck(System.currentTimeMillis())
            }
        }
    }

    /** Starts the download for the update that is currently being offered. */
    fun updateNow() {
        if (_uiState.value !is UpdateUiState.UpdateAvailable) return
        val update = pendingUpdate ?: return
        if (downloadJob?.isActive == true) return

        // The user engaged with the update; stop suppressing the prompt.
        preferences.clear()
        startDownload(update)
    }

    /** Cancels the in-progress download (and any concurrent verification). */
    fun cancelDownload() {
        when (_uiState.value) {
            is UpdateUiState.Downloading,
            UpdateUiState.Verifying -> {
                _uiState.value = UpdateUiState.Cancelled
                downloadJob?.cancel()
            }
            else -> Unit
        }
    }

    /**
     * Hands the verified APK to the system installer. Reachable from
     * [UpdateUiState.ReadyToInstall] or — if the user just granted the
     * install-unknown-apps permission on the settings detour and the still
     * staged APK remains — from [UpdateUiState.InstallationPermissionRequired].
     */
    fun installUpdate() {
        when (_uiState.value) {
            is UpdateUiState.ReadyToInstall,
            is UpdateUiState.InstallationPermissionRequired -> Unit
            else -> return
        }

        val apk = stagedApkProvider()
        if (!apk.exists()) {
            // Staged file no longer present (e.g. cache cleared); ask again.
            pendingUpdate = null
            _uiState.value = UpdateUiState.Idle
            return
        }

        val result = try {
            installer.launchInstaller(apk)
        } catch (e: Exception) {
            // AndroidUpdateApkInstaller converts every framework outcome into an
            // InstallLaunchResult and never throws, but an unexpected failure must
            // still land on a visible state below instead of silently falling through.
            InstallLaunchResult.LaunchFailed
        }
        // Exhaustive: the compiler rejects a future InstallLaunchResult variant that
        // has no deliberate state transition here.
        _uiState.value = when (result) {
            InstallLaunchResult.Launched -> {
                preferences.clear()
                UpdateUiState.InstallerLaunched
            }
            InstallLaunchResult.PermissionRequired ->
                UpdateUiState.InstallationPermissionRequired
            InstallLaunchResult.PermissionNotDeclared ->
                UpdateUiState.InstallerError(updateErrorMessageRes(UpdateError.PermissionNotDeclared))
            InstallLaunchResult.LaunchFailed ->
                UpdateUiState.InstallerError(updateErrorMessageRes(UpdateError.InstallerLaunch))
        }
    }

    /** "Later": defers the update and suppresses automatic prompts for the cooldown. */
    fun postpone() {
        when (_uiState.value) {
            is UpdateUiState.UpdateAvailable,
            UpdateUiState.ReadyToInstall,
            UpdateUiState.InstallationPermissionRequired -> {
                if (_uiState.value is UpdateUiState.ReadyToInstall) {
                    deleteStagedApk()
                }
                preferences.markDeferred(System.currentTimeMillis())
                resetToIdle()
            }
            else -> Unit
        }
    }

    /** Closes the dialog back to idle. Ignored while a download is running. */
    fun dismiss() {
        when (_uiState.value) {
            is UpdateUiState.Downloading,
            UpdateUiState.Verifying -> Unit // must use the explicit Cancel action
            else -> resetToIdle()
        }
    }

    /** Deep link to the "install unknown apps" system settings, or null. */
    fun appSourceSettingsIntent(): Intent? =
        if (_uiState.value is UpdateUiState.InstallationPermissionRequired) {
            installer.unknownAppSourcesSettingsIntent()
        } else {
            null
        }

    private fun startDownload(update: UpdateInfo) {
        _uiState.value = UpdateUiState.Downloading(progress = null)
        downloadJob = viewModelScope.launch {
            val apk = stagedApkProvider()
            try {
                downloader.download(url = update.downloadUrl, dest = apk) { progress ->
                    _uiState.update { current ->
                        if (current is UpdateUiState.Downloading) {
                            current.copy(progress = progress)
                        } else {
                            current
                        }
                    }
                }
                _uiState.value = UpdateUiState.Verifying
                when (val result = verifier.verify(apk, update)) {
                    ApkVerificationResult.Success ->
                        _uiState.value = UpdateUiState.ReadyToInstall
                    else -> {
                        deleteStagedApk()
                        _uiState.value = UpdateUiState.Error(R.string.update_error_verify)
                    }
                }
            } catch (e: CancellationException) {
                deleteStagedApk()
                throw e
            } catch (e: Exception) {
                deleteStagedApk()
                _uiState.value = UpdateUiState.Error(R.string.update_failed)
            }
        }
    }

    private fun resetToIdle() {
        pendingUpdate = null
        _uiState.value = UpdateUiState.Idle
    }

    private fun isWithinDeferralCooldown(): Boolean {
        val lastDeferred = preferences.lastDeferredAtMillis() ?: return false
        return System.currentTimeMillis() - lastDeferred < deferralCooldownMillis
    }

    private fun isWithinAutomaticCheckCooldown(): Boolean {
        val lastAutomaticCheck = preferences.lastAutomaticCheckAtMillis() ?: return false
        return System.currentTimeMillis() - lastAutomaticCheck < automaticCheckCooldownMillis
    }

    private fun updateErrorMessageRes(error: UpdateError): Int = when (error) {
        UpdateError.Disabled -> R.string.update_error_disabled
        UpdateError.Network,
        UpdateError.Http -> R.string.update_error_network
        UpdateError.MalformedManifest,
        UpdateError.InvalidHash,
        UpdateError.UntrustedUrl -> R.string.update_error_cannot_check
        UpdateError.Download -> R.string.update_failed
        UpdateError.HashMismatch,
        UpdateError.InvalidApk,
        UpdateError.WrongPackage,
        UpdateError.WrongVersion -> R.string.update_error_verify
        UpdateError.InstallationPermissionRequired -> R.string.update_error_permission
        UpdateError.PermissionNotDeclared -> R.string.update_error_permission_not_declared
        UpdateError.InstallerLaunch -> R.string.update_error_launch
        UpdateError.Cancelled -> R.string.update_error_cancelled
    }

    companion object {
        /** Default window during which automatic checks respect a "Later" choice. */
        const val DEFAULT_DEFERRAL_COOLDOWN_MILLIS = 24L * 60 * 60 * 1000

        /** Default throttle between automatic (non-manual) update checks. */
        const val DEFAULT_AUTOMATIC_CHECK_COOLDOWN_MILLIS = 24L * 60 * 60 * 1000

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                UpdateViewModel(
                    repository = UpdateRepository(),
                    downloader = OkHttpUpdateApkDownloader(),
                    verifier = AndroidUpdateApkVerifier(application),
                    installer = AndroidUpdateApkInstaller(application),
                    preferences = SharedPrefsUpdatePreferences(application),
                    application = application,
                )
            }
        }
    }
}