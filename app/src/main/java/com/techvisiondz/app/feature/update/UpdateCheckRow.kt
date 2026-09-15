package com.techvisiondz.app.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * A reusable "check for updates" row for a settings-style screen (used on the
 * Account screen). Renders check-time outcomes; the [UpdateDialog] handles
 * everything from "update available" onward.
 *
 * The row is purely presentational: it never performs repository operations.
 * Every action is forwarded up so the parent/ViewModel owns the behavior.
 *
 * All copy is localized through [R.string]; [icon] is an optional leading
 * glyph (e.g. [com.techvisiondz.app.core.ui.TechVisionIcons.Update]).
 */
@Composable
fun UpdateCheckRow(
    state: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onRetry: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val (subtitle, action) = when (state) {
        is UpdateUiState.Idle -> stringResource(R.string.update_check) to
            RowAction.Button(stringResource(R.string.update_check_now), onCheckForUpdate)
        UpdateUiState.Checking -> stringResource(R.string.update_checking) to RowAction.Progress
        UpdateUiState.NoUpdate -> stringResource(R.string.update_up_to_date) to
            RowAction.Button(stringResource(R.string.update_check_again), onCheckForUpdate)
        is UpdateUiState.UpdateAvailable ->
            stringResource(R.string.update_available_row, state.versionName) to
                RowAction.Button(stringResource(R.string.update), onUpdate)
        is UpdateUiState.Downloading,
        UpdateUiState.Verifying -> stringResource(R.string.update_downloading) to RowAction.Progress
        UpdateUiState.ReadyToInstall -> stringResource(R.string.update_ready_to_install) to
            RowAction.Button(stringResource(R.string.update_install), onUpdate)
        UpdateUiState.InstallationPermissionRequired ->
            stringResource(R.string.update_permission_needed) to
                RowAction.Button(stringResource(R.string.update_install), onUpdate)
        UpdateUiState.InstallerLaunched ->
            stringResource(R.string.update_installation_started) to RowAction.None
        UpdateUiState.Cancelled -> stringResource(R.string.update_cancelled) to RowAction.None
        is UpdateUiState.Error -> stringResource(state.messageRes) to
            RowAction.Button(stringResource(R.string.retry), onRetry)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = TechVisionSpacing.Lg,
                vertical = TechVisionSpacing.Md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.update),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
        when (action) {
            RowAction.None -> Unit
            RowAction.Progress -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
            is RowAction.Button -> TextButton(onClick = action.onClick) {
                Text(action.label)
            }
        }
    }
}

private sealed interface RowAction {
    data object None : RowAction
    data object Progress : RowAction
    data class Button(val label: String, val onClick: () -> Unit) : RowAction
}

@Preview(showBackground = true)
@Composable
private fun UpdateCheckRowIdlePreview() {
    TechVisionDzTheme {
        UpdateCheckRow(
            state = UpdateUiState.Idle,
            onCheckForUpdate = {},
            onRetry = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateCheckRowAvailablePreview() {
    TechVisionDzTheme {
        UpdateCheckRow(
            state = UpdateUiState.UpdateAvailable(versionName = "1.1.0", releaseNotes = null),
            onCheckForUpdate = {},
            onRetry = {},
            onUpdate = {},
        )
    }
}