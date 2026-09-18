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
import androidx.compose.foundation.layout.width
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
import com.techvisiondz.app.core.ui.components.BrandMark
import com.techvisiondz.app.core.ui.components.SectionHeader
import com.techvisiondz.app.core.util.DownloadUrlPolicy
import com.techvisiondz.app.feature.update.UpdateStatusCard
import com.techvisiondz.app.feature.update.UpdateUiState
import com.techvisiondz.app.feature.update.UpdateViewModel
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

/**
 * Settings screen - the app's app-experience control center.
 *
 * Four sections, all presentational and downstream-owned:
 *  - Appearance: System / Light / Dark theme selection. System stays the
 *    default; the choice is persisted and applied immediately by the app root.
 *  - Language: Arabic / English. The choice is persisted and applied
 *    immediately (RTL / LTR) without recreating the navigation stack.
 *  - Updates: reuses the shared [UpdateStatusCard] (check row + installed
 *    version); the root update dialog continues to drive everything from
 *    "update available" onward.
 *  - About: brand identity, installed version and a link to the configured
 *    website. The link only leaves the app when it passes the existing
 *    http(s)-only URL gate; failures surface a snackbar instead of crashing.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit,
    websiteUrl: String? = AppConfig.ARTICLE_BASE_URL,
    onOpenWebsite: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val websiteFailureMessage = stringResource(R.string.settings_website_error)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    // The link is only ever offered when it survives the http(s)-only gate,
    // regardless of what produced the [websiteUrl] value.
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
            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                ThemeSelector(
                    current = state.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            SettingsSection(title = stringResource(R.string.settings_language)) {
                LanguageSelector(
                    current = state.language,
                    onSelect = viewModel::setLanguage,
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            SettingsSection(title = stringResource(R.string.settings_updates)) {
                UpdateStatusCard(
                    state = updateState,
                    onCheckForUpdate = { updateViewModel.checkForUpdate(manual = true) },
                    onRetry = { updateViewModel.checkForUpdate(manual = true) },
                    onUpdate = {
                        when (updateState) {
                            is UpdateUiState.UpdateAvailable -> updateViewModel.updateNow()
                            UpdateUiState.ReadyToInstall,
                            UpdateUiState.InstallationPermissionRequired ->
                                updateViewModel.installUpdate()
                            else -> Unit
                        }
                    },
                    testTag = "settings_update_row",
                )
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            SettingsSection(title = stringResource(R.string.settings_about)) {
                AboutCard(
                    versionName = BuildConfig.VERSION_NAME,
                    websiteUrl = safeWebsiteUrl,
                    onVisitWebsite = openWebsite,
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

/** Brand identity, installed version and a validated website link. */
@Composable
private fun AboutCard(
    versionName: String,
    websiteUrl: String?,
    onVisitWebsite: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_about_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 44.dp)
                Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
            Text(
                text = stringResource(R.string.update_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (websiteUrl != null) {
                Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
                Surface(
                    onClick = onVisitWebsite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_about_website"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = TechVisionSpacing.Md,
                            vertical = TechVisionSpacing.Md,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_website),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}