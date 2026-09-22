package com.techvisiondz.app.feature.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * App-level host for [UpdateDialog].
 *
 * Renders the update dialog whenever the shared [UpdateViewModel]'s state
 * requires it, and wires the dialog's actions to that ViewModel. It is mounted
 * once at the root of the app (see `TechVisionDzApp`) so a single instance
 * drives both the automatic startup check and the manual Account check, and so
 * dialog state survives navigation (the ViewModel outlives any single screen).
 *
 * Opening the "install unknown apps" system settings page happens here —
 * explicitly, from the dialog's Open settings button and only when the
 * ViewModel reports [UpdateUiState.InstallationPermissionRequired] — and never
 * inside the ViewModel or dialog. The launch goes through an Activity Result
 * contract so control returning from Settings invokes
 * [UpdateViewModel.onPermissionSettingsReturned], which re-probes the
 * permission and resumes the flow (the verified staged APK is kept, never
 * re-downloaded). The final install itself always remains a separate,
 * user-initiated Install tap.
 *
 * The Activity Result launcher is *not* registered at the root of this
 * composable's composition. It is only composed while the dialog is actually in
 * [UpdateUiState.InstallationPermissionRequired], so no `ActivityResultRegistryOwner`
 * is required during the app's initial composition — on some Android 16
 * (API 36) devices registering a launcher at startup throws
 * `IllegalStateException: No ActivityResultRegistryOwner was provided via
 * LocalActivityResultRegistryOwner`. Because the launcher remains composed for
 * the whole settings round-trip (nothing changes the state while the user is in
 * Settings), the Activity Result still reaches the ViewModel on return.
 */
@Composable
fun UpdateDialogHost(
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val state by updateViewModel.uiState.collectAsState()

    UpdateDialog(
        state = state,
        onUpdateNow = updateViewModel::updateNow,
        onLater = updateViewModel::postpone,
        onCancelDownload = updateViewModel::cancelDownload,
        onInstall = updateViewModel::installUpdate,
        onOpenSettings = rememberPermissionSettingsOpenAction(updateViewModel, state),
        onDismiss = updateViewModel::dismiss,
        modifier = modifier,
    )
}

/**
 * The "Open settings" action for the install-unknown-apps permission detour,
 * or a no-op when the dialog is not in [UpdateUiState.InstallationPermissionRequired].
 *
 * The [rememberLauncherForActivityResult] call lives only inside the
 * [UpdateUiState.InstallationPermissionRequired] branch of this conditional.
 * Compose scopes the remembered launcher to that conditional group: it is
 * registered when the branch is composed and unregistered (by the API's own
 * internal `DisposableEffect`) when the branch leaves composition. This is the
 * Compose-legal way to delay launcher registration — the launcher is registered
 * exactly for the setting's round-trip that can actually deliver a result, and
 * it never exists during startup composition when the owner may be absent.
 */
@Composable
private fun rememberPermissionSettingsOpenAction(
    updateViewModel: UpdateViewModel,
    state: UpdateUiState,
): () -> Unit {
    val onOpenSettings: () -> Unit
    if (state is UpdateUiState.InstallationPermissionRequired) {
        val settingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            updateViewModel.onPermissionSettingsReturned()
        }

        onOpenSettings = {
            updateViewModel.appSourceSettingsIntent()?.let { intent ->
                settingsLauncher.launch(intent)
            }
        }
    } else {
        onOpenSettings = {}
    }
    return onOpenSettings
}