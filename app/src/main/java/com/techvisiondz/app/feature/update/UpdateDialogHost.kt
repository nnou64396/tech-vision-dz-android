package com.techvisiondz.app.feature.update

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
 * inside the ViewModel or dialog.
 */
@Composable
fun UpdateDialogHost(
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val state by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    UpdateDialog(
        state = state,
        onUpdateNow = updateViewModel::updateNow,
        onLater = updateViewModel::postpone,
        onCancelDownload = updateViewModel::cancelDownload,
        onInstall = updateViewModel::installUpdate,
        onOpenSettings = {
            updateViewModel.appSourceSettingsIntent()?.let { intent ->
                context.startActivity(intent)
            }
        },
        onDismiss = updateViewModel::dismiss,
        modifier = modifier,
    )
}