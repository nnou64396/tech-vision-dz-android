package com.techvisiondz.app.feature.settings

import com.techvisiondz.app.core.settings.AppLanguage
import com.techvisiondz.app.core.settings.SettingsPreferences
import com.techvisiondz.app.core.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * State and persistence of the Settings ViewModel. The store is a fake; the
 * same instance is shared across ViewModels to prove selections survive an
 * "app restart" (a fresh ViewModel reading the persisted values).
 */
class SettingsViewModelTest {

    private class FakeSettingsPreferences(
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

    @Test
    fun freshStoreDefaultsToSystemThemeAndSystemDefaultLanguage() {
        val viewModel = SettingsViewModel(FakeSettingsPreferences()) { AppLanguage.ENGLISH }

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
    }

    @Test
    fun storedChoicesAreLoadedOnStartup() {
        val viewModel = SettingsViewModel(
            FakeSettingsPreferences(
                theme = ThemeMode.DARK,
                language = AppLanguage.ARABIC,
            ),
        ) { AppLanguage.ENGLISH }

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(AppLanguage.ARABIC, viewModel.uiState.value.language)
    }

    @Test
    fun unsetLanguageFallsBackToProvidedSystemDefault() {
        val viewModel = SettingsViewModel(FakeSettingsPreferences()) { AppLanguage.ARABIC }

        assertEquals(AppLanguage.ARABIC, viewModel.uiState.value.language)
    }

    @Test
    fun settingThemeModeAppliesImmediately() {
        val viewModel = SettingsViewModel(FakeSettingsPreferences()) { AppLanguage.ENGLISH }

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun settingLanguageAppliesImmediately() {
        val viewModel = SettingsViewModel(FakeSettingsPreferences()) { AppLanguage.ENGLISH }

        viewModel.setLanguage(AppLanguage.ARABIC)

        assertEquals(AppLanguage.ARABIC, viewModel.uiState.value.language)
    }

    @Test
    fun themeAndLanguageChoicesSurviveViewModelRecreation() {
        val preferences = FakeSettingsPreferences()
        val first = SettingsViewModel(preferences) { AppLanguage.ENGLISH }
        first.setThemeMode(ThemeMode.DARK)
        first.setLanguage(AppLanguage.ARABIC)

        val restarted = SettingsViewModel(preferences) { AppLanguage.ENGLISH }

        assertEquals(ThemeMode.DARK, restarted.uiState.value.themeMode)
        assertEquals(AppLanguage.ARABIC, restarted.uiState.value.language)
    }

    @Test
    fun themeDefaultsBackToSystemWhenUserReturnsToSystem() {
        val preferences = FakeSettingsPreferences()
        val viewModel = SettingsViewModel(preferences) { AppLanguage.ENGLISH }

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode())
    }

    @Test
    fun storeKeepsLanguageUnsetUntilUserChooses() {
        val preferences = FakeSettingsPreferences()

        assertNull(preferences.language())
    }
}