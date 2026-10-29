package com.techvisiondz.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

@Composable
fun PermissionsDetailsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settingsErrorMessage = stringResource(R.string.permissions_settings_unavailable)

    val openAppSettings = {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar(settingsErrorMessage) }
        }
        Unit
    }

    BackTopBarScreen(
        title = stringResource(R.string.permissions_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TechVisionSpacing.Lg)
                .padding(top = TechVisionSpacing.Xl, bottom = TechVisionSpacing.Xl)
                .testTag("permissions_content"),
        ) {
            Text(
                text = stringResource(R.string.permissions_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            PermissionCard(
                name = stringResource(R.string.permission_internet_name),
                identifier = "android.permission.INTERNET",
                purpose = stringResource(R.string.permission_internet_purpose),
                type = "Normal (Granted by OS)",
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            Button(
                onClick = openAppSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_app_settings_button"),
            ) {
                Text(text = stringResource(R.string.permissions_open_settings))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    name: String,
    identifier: String,
    purpose: String,
    type: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Xs))
            Text(
                text = identifier,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
            Text(
                text = purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
            Text(
                text = "Type: $type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
