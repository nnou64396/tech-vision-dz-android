package com.techvisiondz.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.settings.AppLanguage
import com.techvisiondz.app.core.settings.SettingsPreferences
import com.techvisiondz.app.core.settings.SharedPrefsSettingsPreferences
import com.techvisiondz.app.core.settings.ThemeMode
import com.techvisiondz.app.core.settings.defaultSystemLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state of the Settings screen.
 *
 * @property themeMode The active appearance scheme ([ThemeMode.SYSTEM] until
 *   the user overrides it, keeping the device default as the app default).
 * @property language The active UI language; falls back to the device-derived
 *   [defaultSystemLanguage] until the user makes a choice.
 */
data class SettingsUiState(
    val themeMode: ThemeMode,
    val language: AppLanguage,
)

/**
 * App-experience state holder for the Settings screen (also consumed at the
 * app root, which derives the theme and the locale from [uiState]).
 *
 * Every change is applied to the app immediately (state first, then the
 * persisted store) so the user sees their selection right away without a
 * navigation-stack recreation. The persisted values are read again on startup,
 * so the choice survives app restarts.
 *
 * [systemDefaultLanguage] is a seam for deterministic JVM tests; production
 * uses the device-locale default.
 */
class SettingsViewModel(
    private val preferences: SettingsPreferences,
    systemDefaultLanguage: () -> AppLanguage = ::defaultSystemLanguage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = preferences.themeMode(),
            language = preferences.language() ?: systemDefaultLanguage(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLanguage(language: AppLanguage) {
        preferences.setLanguage(language)
        _uiState.update { it.copy(language = language) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                SettingsViewModel(SharedPrefsSettingsPreferences(application))
            }
        }
    }
}