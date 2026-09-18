package com.techvisiondz.app.feature.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.BuildConfig
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.TechVisionIcons
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Card hosting the reusable [UpdateCheckRow] plus the installed version label.
 * Shared by the Account and Settings screens; purely presentational - all
 * events are forwarded to the parent/ViewModel.
 */
@Composable
fun UpdateStatusCard(
    state: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onRetry: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "update_row",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            UpdateCheckRow(
                state = state,
                onCheckForUpdate = onCheckForUpdate,
                onRetry = onRetry,
                onUpdate = onUpdate,
                icon = TechVisionIcons.Update,
            )
            Text(
                text = stringResource(R.string.update_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = TechVisionSpacing.Xl,
                    end = TechVisionSpacing.Md,
                    bottom = TechVisionSpacing.Md,
                ),
            )
        }
    }
}