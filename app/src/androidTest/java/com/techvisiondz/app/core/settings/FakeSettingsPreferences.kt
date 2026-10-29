package com.techvisiondz.app.core.settings

/**
 * In-memory [SettingsPreferences] for deterministic androidTest renders and
 * navigation tests (no SharedPreferences file is touched).
 */
class FakeSettingsPreferences(
    private var theme: ThemeMode? = null,
    private var language: AppLanguage? = null,
) : SettingsPreferences {
    override fun themeMode(): ThemeMode = theme ?: ThemeMode.SYSTEM
    override fun setThemeMode(mode: ThemeMode) {
        theme = mode
    }

    override fun language(): AppLanguage? = language
    override fun setLanguage(language: AppLanguage) {
        this.language = language
    }

    override fun isNotificationsEnabled(): Boolean = true
    override fun setNotificationsEnabled(enabled: Boolean) {}

    override fun isNewsNotificationsEnabled(): Boolean = true
    override fun setNewsNotificationsEnabled(enabled: Boolean) {}

    override fun isAnnouncementsNotificationsEnabled(): Boolean = true
    override fun setAnnouncementsNotificationsEnabled(enabled: Boolean) {}

    override fun isAlertsNotificationsEnabled(): Boolean = true
    override fun setAlertsNotificationsEnabled(enabled: Boolean) {}

    override fun fcmToken(): String? = null
    override fun setFcmToken(token: String?) {}

}