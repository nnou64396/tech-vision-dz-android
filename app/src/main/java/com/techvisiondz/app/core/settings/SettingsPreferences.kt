package com.techvisiondz.app.core.settings

import android.content.Context
import java.util.Locale

/** Appearance scheme chosen by the user; [SYSTEM] keeps the device default. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * UI language of the app. The app ships exactly two resource locales
 * (`values/` English and `values-ar/` Arabic), so the selection is bounded to
 * those two; [code] is the BCP-47 tag used to build the localized configuration
 * and [isRtl] drives the layout direction.
 */
enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false),
}

/**
 * The UI language used before the user makes a choice: Arabic on Arabic-locale
 * devices, English everywhere else — exactly the behavior of resolving the
 * default resources today, so first launch is unchanged for existing users.
 */
fun defaultSystemLanguage(): AppLanguage =
    if (Locale.getDefault().language.equals("ar", ignoreCase = true)) {
        AppLanguage.ARABIC
    } else {
        AppLanguage.ENGLISH
    }

/**
 * Persisted app-experience choices (appearance theme + UI language).
 *
 * Read through a plain [SharedPreferences] file, mirroring
 * [com.techvisiondz.app.core.update.SharedPrefsUpdatePreferences] — no Room /
 * DataStore dependency. Writes are fire-and-forget (`apply`), so a quick
 * selection never stalls the UI thread.
 */
interface SettingsPreferences {
    /** Stored theme, or [ThemeMode.SYSTEM] when the user has not chosen yet. */
    fun themeMode(): ThemeMode

    fun setThemeMode(mode: ThemeMode)

    /** Stored UI language, or null before the user makes a choice. */
    fun language(): AppLanguage?

    fun setLanguage(language: AppLanguage)
}

class SharedPrefsSettingsPreferences(context: Context) : SettingsPreferences {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override fun themeMode(): ThemeMode =
        prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    override fun language(): AppLanguage? =
        prefs.getString(KEY_LANGUAGE, null)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }

    override fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    companion object {
        const val PREFS_FILE = "techvision_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
    }
}