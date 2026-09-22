package com.techvisiondz.app.feature.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

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
 */
@Composable
fun UpdateDialogHost(
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val state by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        updateViewModel.onPermissionSettingsReturned()
    }

    UpdateDialog(
        state = state,
        onUpdateNow = updateViewModel::updateNow,
        onLater = updateViewModel::postpone,
        onCancelDownload = updateViewModel::cancelDownload,
        onInstall = updateViewModel::installUpdate,
        onOpenSettings = {
            updateViewModel.appSourceSettingsIntent()?.let { intent ->
                settingsLauncher.launch(intent)
            }
        },
        onDismiss = updateViewModel::dismiss,
        modifier = modifier,
    )
}
