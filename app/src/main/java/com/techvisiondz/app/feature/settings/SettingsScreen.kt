package com.techvisiondz.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.BuildConfig
import com.techvisiondz.app.R
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.settings.AppLanguage
import com.techvisiondz.app.core.settings.ThemeMode
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.SectionHeader
import com.techvisiondz.app.core.util.DownloadUrlPolicy
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

/**
 * Settings screen - the app's control center.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSourcesClick: () -> Unit = {},
    websiteUrl: String? = AppConfig.ARTICLE_BASE_URL,
    onOpenWebsite: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val websiteFailureMessage = stringResource(R.string.settings_website_error)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val safeWebsiteUrl = remember(websiteUrl) { DownloadUrlPolicy.normalize(websiteUrl) }
    val openWebsite = onOpenWebsite ?: {
        if (safeWebsiteUrl == null) {
            scope.launch { snackbarHostState.showSnackbar(websiteFailureMessage) }
        } else {
            runCatching { uriHandler.openUri(safeWebsiteUrl) }
                .onFailure {
                    scope.launch { snackbarHostState.showSnackbar(websiteFailureMessage) }
                }
        }
        Unit
    }

    BackTopBarScreen(
        title = stringResource(R.string.settings),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TechVisionSpacing.Lg)
                .padding(top = TechVisionSpacing.Xl, bottom = TechVisionSpacing.Xl),
        ) {
            // About Section
            SettingsSection(title = stringResource(R.string.settings_about_section)) {
                SettingsNavigationItem(
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.update_version, BuildConfig.VERSION_NAME),
                    testTag = "settings_nav_about",
                    onClick = onAboutClick,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Sources Section
            SettingsSection(title = stringResource(R.string.sources_title)) {
                SettingsNavigationItem(
                    title = stringResource(R.string.sources),
                    subtitle = stringResource(R.string.sources_subtitle),
                    testTag = "settings_nav_sources",
                    onClick = onSourcesClick,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Privacy Policy
            SettingsSection(title = stringResource(R.string.settings_privacy_policy)) {
                SettingsNavigationItem(
                    title = stringResource(R.string.privacy_policy_title),
                    subtitle = stringResource(R.string.privacy_policy_intro),
                    testTag = "settings_nav_privacy_policy",
                    onClick = onPrivacyPolicyClick,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Permissions
            SettingsSection(title = stringResource(R.string.settings_permissions_details)) {
                SettingsNavigationItem(
                    title = stringResource(R.string.permissions_title),
                    subtitle = stringResource(R.string.permissions_intro),
                    testTag = "settings_nav_permissions",
                    onClick = onPermissionsClick,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Notifications
            SettingsSection(title = stringResource(R.string.settings_notifications)) {
                SettingsNavigationItem(
                    title = stringResource(R.string.notifications_title),
                    subtitle = stringResource(R.string.notifications_intro),
                    testTag = "settings_nav_notifications",
                    onClick = onNotificationsClick,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Appearance
            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                ThemeSelector(
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Language
            SettingsSection(title = stringResource(R.string.settings_language)) {
                LanguageSelector(
                    current = state.language,
                    onSelect = viewModel::setLanguage,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Developer Credit
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = stringResource(R.string.developer_attribution),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(TechVisionSpacing.Lg),
                )
            }
        }
    }
}

/** A titled settings group: editorial section header + its content card(s). */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
        content()
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TechVisionSpacing.Lg,
                vertical = TechVisionSpacing.Md,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(TechVisionSpacing.Xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SelectionGroup(
        options = listOf(
            SelectionOption(
                label = stringResource(R.string.theme_system),
                selected = current == ThemeMode.SYSTEM,
                testTag = "settings_theme_system",
                onClick = { onSelect(ThemeMode.SYSTEM) },
            ),
            SelectionOption(
                label = stringResource(R.string.theme_light),
                selected = current == ThemeMode.LIGHT,
                testTag = "settings_theme_light",
                onClick = { onSelect(ThemeMode.LIGHT) },
            ),
            SelectionOption(
                label = stringResource(R.string.theme_dark),
                selected = current == ThemeMode.DARK,
                testTag = "settings_theme_dark",
                onClick = { onSelect(ThemeMode.DARK) },
            ),
        ),
    )
}

@Composable
private fun LanguageSelector(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    SelectionGroup(
        options = listOf(
            SelectionOption(
                label = stringResource(R.string.language_arabic),
                selected = current == AppLanguage.ARABIC,
                testTag = "settings_language_arabic",
                onClick = { onSelect(AppLanguage.ARABIC) },
            ),
            SelectionOption(
                label = stringResource(R.string.language_english),
                selected = current == AppLanguage.ENGLISH,
                testTag = "settings_language_english",
                onClick = { onSelect(AppLanguage.ENGLISH) },
            ),
            SelectionOption(
                label = stringResource(R.string.language_french),
                selected = current == AppLanguage.FRENCH,
                testTag = "settings_language_french",
                onClick = { onSelect(AppLanguage.FRENCH) },
            ),
            SelectionOption(
                label = stringResource(R.string.language_russian),
                selected = current == AppLanguage.RUSSIAN,
                testTag = "settings_language_russian",
                onClick = { onSelect(AppLanguage.RUSSIAN) },
            ),
        ),
    )
}

private data class SelectionOption(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

/** A single settings card containing radio-style options split by dividers. */
@Composable
private fun SelectionGroup(options: List<SelectionOption>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                SelectionRow(option)
            }
        }
    }
}

@Composable
private fun SelectionRow(option: SelectionOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = option.selected,
                role = Role.RadioButton,
                onClick = option.onClick,
            )
            .testTag(option.testTag)
            .padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (option.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        RadioButton(
            selected = option.selected,
            onClick = option.onClick,
        )
    }
}
