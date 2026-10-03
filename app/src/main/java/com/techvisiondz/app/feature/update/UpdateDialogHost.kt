package com.techvisiondz.app.feature.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

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
 * ViewModel reports [UpdateUiState.InstallationPermissionRequired]. Since
 * Settings returns no result data, the host observes the next lifecycle resume
 * and asks the ViewModel to re-probe permission. The verified staged APK is
 * kept, never re-downloaded, and installation remains a separate,
 * user-initiated action.
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
 */
@Composable
private fun rememberPermissionSettingsOpenAction(
    updateViewModel: UpdateViewModel,
    state: UpdateUiState,
): () -> Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var awaitingSettingsReturn by remember(updateViewModel) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, updateViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingSettingsReturn) {
                awaitingSettingsReturn = false
                updateViewModel.onPermissionSettingsReturned()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return if (state is UpdateUiState.InstallationPermissionRequired) {
        {
            updateViewModel.appSourceSettingsIntent()?.let { intent ->
                context.startActivity(intent)
                awaitingSettingsReturn = true
            }
        }
    } else {
        {}
    }
}
