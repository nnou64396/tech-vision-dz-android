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
 * @property themeMode Active appearance scheme ([ThemeMode.SYSTEM] until overridden).
 * @property language Active UI language (falls back to [defaultSystemLanguage]).
 * @property notificationsEnabled Master toggle for notifications.
 * @property newsNotificationsEnabled Category toggle: Tech News.
 * @property announcementsNotificationsEnabled Category toggle: App Announcements.
 * @property alertsNotificationsEnabled Category toggle: Breaking Tech Alerts.
 * @property fcmToken Registered FCM token, if available.
 */
data class SettingsUiState(
    val themeMode: ThemeMode,
    val language: AppLanguage,
    val notificationsEnabled: Boolean = true,
    val newsNotificationsEnabled: Boolean = true,
    val announcementsNotificationsEnabled: Boolean = true,
    val alertsNotificationsEnabled: Boolean = true,
    val fcmToken: String? = null,
)

/**
 * App-experience state holder for the Settings screen and root theme/language provider.
 */
class SettingsViewModel(
    private val preferences: SettingsPreferences,
    systemDefaultLanguage: () -> AppLanguage = ::defaultSystemLanguage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = preferences.themeMode(),
            language = preferences.language() ?: systemDefaultLanguage(),
            notificationsEnabled = preferences.isNotificationsEnabled(),
            newsNotificationsEnabled = preferences.isNewsNotificationsEnabled(),
            announcementsNotificationsEnabled = preferences.isAnnouncementsNotificationsEnabled(),
            alertsNotificationsEnabled = preferences.isAlertsNotificationsEnabled(),
            fcmToken = preferences.fcmToken(),
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

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setNewsNotificationsEnabled(enabled: Boolean) {
        preferences.setNewsNotificationsEnabled(enabled)
        _uiState.update { it.copy(newsNotificationsEnabled = enabled) }
    }

    fun setAnnouncementsNotificationsEnabled(enabled: Boolean) {
        preferences.setAnnouncementsNotificationsEnabled(enabled)
        _uiState.update { it.copy(announcementsNotificationsEnabled = enabled) }
    }

    fun setAlertsNotificationsEnabled(enabled: Boolean) {
        preferences.setAlertsNotificationsEnabled(enabled)
        _uiState.update { it.copy(alertsNotificationsEnabled = enabled) }
    }

    fun setFcmToken(token: String?) {
        preferences.setFcmToken(token)
        _uiState.update { it.copy(fcmToken = token) }
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