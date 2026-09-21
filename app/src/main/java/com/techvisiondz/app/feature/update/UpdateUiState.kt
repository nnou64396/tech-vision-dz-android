package com.techvisiondz.app.feature.update

/**
 * UI-facing state of the update flow, deliberately free of repository-level
 * types so Compose never couples to the updater internals.
 *
 * The flow is: [Idle] → [Checking] → ([NoUpdate] | [UpdateAvailable] |
 * [Error]) → [Downloading] → [Verifying] → [ReadyToInstall] →
 * ([InstallationPermissionRequired] | [InstallerLaunched] | [Error] |
 * [InstallerError]).
 * [Cancelled] is reached only via an explicit user cancel of a download.
 */
sealed interface UpdateUiState {

    /** No check has completed and no update prompt is outstanding. */
    data object Idle : UpdateUiState

    /** A version check is in flight. */
    data object Checking : UpdateUiState

    /** Checked; remote is not strictly newer than the installed version. */
    data object NoUpdate : UpdateUiState

    /** Checked; a newer version exists. Carries only user-safe display data. */
    data class UpdateAvailable(
        val versionName: String,
        val releaseNotes: String?,
    ) : UpdateUiState

    /** Download in flight; [progress] is 0..1, or null when unknown. */
    data class Downloading(val progress: Float?) : UpdateUiState

    /** Downloaded; the staged APK is being verified. */
    data object Verifying : UpdateUiState

    /** Downloaded and verified; waiting for the user to start the installer. */
    data object ReadyToInstall : UpdateUiState

    /** Android denied install-from-this-source; a settings deep link is offered. */
    data object InstallationPermissionRequired : UpdateUiState

    /** The system package installer has been handed the verified APK. */
    data object InstallerLaunched : UpdateUiState

    /** The user cancelled an in-progress download. */
    data object Cancelled : UpdateUiState

    /**
     * A failure surfaced by [messageRes] — a resource id of a user-safe message;
     * no URLs, paths, versions, or exception details are ever exposed.
     */
    data class Error(val messageRes: Int) : UpdateUiState

    /**
     * An installer-stage failure. Unlike check-stage [Error] (shown in the
     * update row), this remains a visible dialog state so a failure after the
     * user pressed Install can never disappear silently.
     */
    data class InstallerError(val messageRes: Int) : UpdateUiState
}

/**
 * True when the update dialog should be visible for [this] state. Check-time
 * outcomes ([Idle], [Checking], [NoUpdate], [Error]) belong in the check row;
 * everything from "update available" onward is driven by the dialog.
 */
fun UpdateUiState.shouldShowDialog(): Boolean = when (this) {
    is UpdateUiState.UpdateAvailable,
    is UpdateUiState.Downloading,
    UpdateUiState.Verifying,
    UpdateUiState.ReadyToInstall,
    UpdateUiState.InstallationPermissionRequired,
    UpdateUiState.InstallerLaunched,
    UpdateUiState.Cancelled,
    is UpdateUiState.InstallerError -> true

    UpdateUiState.Idle,
    UpdateUiState.Checking,
    UpdateUiState.NoUpdate,
    is UpdateUiState.Error -> false
}