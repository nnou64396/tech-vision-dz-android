package com.techvisiondz.app.feature.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import kotlin.math.roundToInt

/**
 * The update dialog. Shown for [UpdateUiState.shouldShowDialog] states only;
 * every other state renders nothing.
 *
 * It drives the whole non-trivial update flow:
 *  - offer (version + release notes, or the generic update prompt),
 *  - download progress with a Cancel action,
 *  - verification,
 *  - install confirmation (never implicit),
 *  - a permission explanation with a *manual* "Open settings" action — the
 *    dialog never opens system settings on its own,
 *  - a post-launch state that does not claim installation has completed;
 *    Android's own package installer owns the actual install.
 *
 * Dismissing via the scrim/back is ignored while a download or verification is
 * running so the operation cannot be interrupted accidentally; the explicit
 * Cancel action is the only way out.
 *
 * All copy is localized through [R.string].
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.shouldShowDialog()) return

    AlertDialog(
        onDismissRequest = {
            when (state) {
                is UpdateUiState.Downloading,
                UpdateUiState.Verifying -> Unit
                else -> onDismiss()
            }
        },
        modifier = modifier,
        title = {
            Text(
                text = when (state) {
                    is UpdateUiState.UpdateAvailable -> stringResource(R.string.update_new_available)
                    is UpdateUiState.Downloading -> stringResource(R.string.update_downloading)
                    UpdateUiState.Verifying -> stringResource(R.string.update_verifying)
                    UpdateUiState.ReadyToInstall -> stringResource(R.string.update_ready_to_install)
                    UpdateUiState.InstallationPermissionRequired ->
                        stringResource(R.string.update_permission_required)
                    UpdateUiState.InstallerLaunched -> stringResource(R.string.update_installing)
                    UpdateUiState.Cancelled -> stringResource(R.string.update_cancelled)
                    else -> ""
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when (state) {
                is UpdateUiState.UpdateAvailable -> UpdateAvailableBody(state)
                is UpdateUiState.Downloading -> DownloadingBody(state)
                UpdateUiState.Verifying -> Text(
                    text = stringResource(R.string.update_verifying),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UpdateUiState.ReadyToInstall -> Text(
                    text = stringResource(R.string.update_ready_to_install_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UpdateUiState.InstallationPermissionRequired -> Text(
                    text = stringResource(R.string.update_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UpdateUiState.InstallerLaunched -> Text(
                    text = stringResource(R.string.update_installing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UpdateUiState.Cancelled -> Text(
                    text = stringResource(R.string.update_cancelled_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Unit
            }
        },
        confirmButton = {
            when (state) {
                is UpdateUiState.UpdateAvailable -> TextButton(onClick = onUpdateNow) {
                    Text(stringResource(R.string.update_now))
                }
                UpdateUiState.ReadyToInstall -> TextButton(onClick = onInstall) {
                    Text(stringResource(R.string.update_install))
                }
                UpdateUiState.InstallationPermissionRequired -> TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.update_open_settings))
                }
                UpdateUiState.InstallerLaunched,
                UpdateUiState.Cancelled -> TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_done))
                }
                else -> Unit
            }
        },
        dismissButton = {
            when (state) {
                is UpdateUiState.UpdateAvailable -> TextButton(onClick = onLater) {
                    Text(stringResource(R.string.update_later))
                }
                is UpdateUiState.Downloading -> TextButton(onClick = onCancelDownload) {
                    Text(stringResource(R.string.update_cancel))
                }
                UpdateUiState.Verifying -> Unit
                UpdateUiState.ReadyToInstall -> TextButton(onClick = onLater) {
                    Text(stringResource(R.string.update_later))
                }
                UpdateUiState.InstallationPermissionRequired -> TextButton(onClick = onLater) {
                    Text(stringResource(R.string.update_not_now))
                }
                else -> Unit
            }
        },
    )
}

@Composable
private fun UpdateAvailableBody(state: UpdateUiState.UpdateAvailable) {
    Column {
        Text(
            text = stringResource(R.string.update_new_available_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.update_version, state.versionName),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        if (!state.releaseNotes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.update_release_notes_heading),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.releaseNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadingBody(state: UpdateUiState.Downloading) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (state.progress == null) {
                stringResource(R.string.update_downloading)
            } else {
                stringResource(R.string.update_downloading_percent, (state.progress * 100).roundToInt())
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.progress == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateDialogOfferPreview() {
    TechVisionDzTheme {
        UpdateDialog(
            state = UpdateUiState.UpdateAvailable(
                versionName = "1.1.0",
                releaseNotes = "Bug fixes and improvements.",
            ),
            onUpdateNow = {},
            onLater = {},
            onCancelDownload = {},
            onInstall = {},
            onOpenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateDialogDownloadPreview() {
    TechVisionDzTheme {
        UpdateDialog(
            state = UpdateUiState.Downloading(progress = 0.42f),
            onUpdateNow = {},
            onLater = {},
            onCancelDownload = {},
            onInstall = {},
            onOpenSettings = {},
            onDismiss = {},
        )
    }
}