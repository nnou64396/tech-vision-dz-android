package com.techvisiondz.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.navigation.AppNavHost
import com.techvisiondz.app.core.ui.ProvideAppLanguage
import com.techvisiondz.app.core.settings.ThemeMode
import com.techvisiondz.app.feature.settings.SettingsViewModel
import com.techvisiondz.app.feature.update.UpdateDialogHost
import com.techvisiondz.app.feature.update.UpdateViewModel
import com.techvisiondz.app.ui.theme.TechVisionDzTheme

/**
 * Root composable of the app.
 *
 * Derives the entire app's theme and UI language from the persisted
 * [SettingsViewModel] choices so the user always sees their preference from
 * the first frame of a restart. Selecting a theme or a language inside
 * Settings takes effect immediately: [ThemeMode] switches the dark/light
 * scheme without recreating the activity, and [ProvideAppLanguage] overrides
 * the string-resource locale + layout direction in place so the back stack,
 * the navigation graph and all live ViewModels (auth session, in-progress
 * updates) are preserved.
 *
 * The per-session automatic update check and the global update dialog remain
 * unchanged - they sit inside the theme wrapper and never interfere with
 * the language / theme plumbing.
 */
@Composable
fun TechVisionDzApp(
    settingsViewModel: SettingsViewModel? = null,
) {
    val effectiveSettingsViewModel =
        settingsViewModel ?: viewModel(factory = SettingsViewModel.Factory)
    val settingsState by effectiveSettingsViewModel.uiState.collectAsState()

    ProvideAppLanguage(language = settingsState.language) {
        TechVisionDzTheme(
            darkTheme = when (settingsState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            },
        ) {
            val updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory)

            LaunchedEffect(Unit) {
                if (AppConfig.UPDATER_ENABLED) {
                    updateViewModel.checkForUpdate()
                }
            }

            AppNavHost(
                updateViewModel = updateViewModel,
                settingsViewModel = effectiveSettingsViewModel,
            )

            UpdateDialogHost(updateViewModel = updateViewModel)
        }
    }
}
