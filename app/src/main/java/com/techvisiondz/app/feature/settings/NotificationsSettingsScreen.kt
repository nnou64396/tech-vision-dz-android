package com.techvisiondz.app.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.SectionHeader
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settingsErrorMessage = stringResource(R.string.notifications_settings_unavailable)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasNotificationPermission = isGranted
        val msg = if (isGranted) "Notification permission granted" else "Notification permission denied"
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val openNotificationSettings = {
        runCatching {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar(settingsErrorMessage) }
        }
        Unit
    }

    BackTopBarScreen(
        title = stringResource(R.string.notifications_title),
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
                .testTag("notifications_content"),
            verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Lg),
        ) {
            Text(
                text = stringResource(R.string.notifications_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Permission status card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (hasNotificationPermission) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasNotificationPermission) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error,
                ),
            ) {
                Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasNotificationPermission) Icons.Filled.CheckCircle else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.width(TechVisionSpacing.Sm))
                        Text(
                            text = if (hasNotificationPermission) {
                                stringResource(R.string.notifications_permission_granted)
                            } else {
                                stringResource(R.string.notifications_permission_denied)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (!hasNotificationPermission) {
                        Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
                        Text(
                            text = stringResource(R.string.notifications_permission_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openNotificationSettings()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_notification_permission_button"),
                        ) {
                            Text(text = stringResource(R.string.notifications_request_permission_button))
                        }
                    }
                }
            }

            // Master notification toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TechVisionSpacing.Lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notifications_master_toggle),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.notifications_master_toggle_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                        modifier = Modifier.testTag("notifications_master_switch"),
                    )
                }
            }

            // Channels Section
            if (state.notificationsEnabled) {
                SectionHeader(title = stringResource(R.string.notifications_categories_header))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column {
                        NotificationChannelRow(
                            title = stringResource(R.string.notif_channel_news_name),
                            description = stringResource(R.string.notif_channel_news_desc),
                            checked = state.newsNotificationsEnabled,
                            onCheckedChange = viewModel::setNewsNotificationsEnabled,
                            testTag = "channel_news_switch",
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        NotificationChannelRow(
                            title = stringResource(R.string.notif_channel_announcements_name),
                            description = stringResource(R.string.notif_channel_announcements_desc),
                            checked = state.announcementsNotificationsEnabled,
                            onCheckedChange = viewModel::setAnnouncementsNotificationsEnabled,
                            testTag = "channel_announcements_switch",
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        NotificationChannelRow(
                            title = stringResource(R.string.notif_channel_alerts_name),
                            description = stringResource(R.string.notif_channel_alerts_desc),
                            checked = state.alertsNotificationsEnabled,
                            onCheckedChange = viewModel::setAlertsNotificationsEnabled,
                            testTag = "channel_alerts_switch",
                        )
                    }
                }
            }

            // FCM Token Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
                    Text(
                        text = stringResource(R.string.notifications_fcm_token_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(TechVisionSpacing.Xs))
                    Text(
                        text = if (!state.fcmToken.isNullOrBlank()) {
                            stringResource(R.string.notifications_fcm_token_registered)
                        } else {
                            stringResource(R.string.notifications_fcm_token_pending)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = openNotificationSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_notification_settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(TechVisionSpacing.Sm))
                Text(text = stringResource(R.string.notifications_open_system_settings))
            }
        }
    }
}

@Composable
private fun NotificationChannelRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TechVisionSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
