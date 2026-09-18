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
}