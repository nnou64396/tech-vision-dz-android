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
 * UI language of the app. The app ships resource locales (`values/` English,
 * `values-ar/` Arabic, `values-fr/` French, and `values-ru/` Russian);
 * [code] is the BCP-47 tag used to build the localized configuration and [isRtl]
 * drives the layout direction.
 */
enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false),
    FRENCH("fr", false),
    RUSSIAN("ru", false),
}

/**
 * The UI language used before the user makes a choice: matches device language
 * when supported (Arabic, French, Russian), or English everywhere else.
 */
fun defaultSystemLanguage(): AppLanguage =
    when (Locale.getDefault().language.lowercase()) {
        "ar" -> AppLanguage.ARABIC
        "fr" -> AppLanguage.FRENCH
        "ru" -> AppLanguage.RUSSIAN
        else -> AppLanguage.ENGLISH
    }

/**
 * Persisted app-experience choices (appearance theme, UI language, push notification preferences).
 *
 * Read through a plain [SharedPreferences] file — no Room / DataStore dependency.
 * Writes are fire-and-forget (`apply`), so a quick selection never stalls the UI thread.
 */
interface SettingsPreferences {
    /** Stored theme, or [ThemeMode.SYSTEM] when the user has not chosen yet. */
    fun themeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)

    /** Stored UI language, or null before the user makes a choice. */
    fun language(): AppLanguage?
    fun setLanguage(language: AppLanguage)

    /** Master switch for push notifications. Default true. */
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)

    /** Category toggle: Tech News & Articles. Default true. */
    fun isNewsNotificationsEnabled(): Boolean
    fun setNewsNotificationsEnabled(enabled: Boolean)

    /** Category toggle: App Announcements & Updates. Default true. */
    fun isAnnouncementsNotificationsEnabled(): Boolean
    fun setAnnouncementsNotificationsEnabled(enabled: Boolean)

    /** Category toggle: Breaking Tech Alerts. Default true. */
    fun isAlertsNotificationsEnabled(): Boolean
    fun setAlertsNotificationsEnabled(enabled: Boolean)

    /** Registered Firebase Cloud Messaging (FCM) registration token. */
    fun fcmToken(): String?
    fun setFcmToken(token: String?)
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

    override fun isNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    override fun isNewsNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIF_NEWS_ENABLED, true)

    override fun setNewsNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_NEWS_ENABLED, enabled).apply()
    }

    override fun isAnnouncementsNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIF_ANNOUNCEMENTS_ENABLED, true)

    override fun setAnnouncementsNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_ANNOUNCEMENTS_ENABLED, enabled).apply()
    }

    override fun isAlertsNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIF_ALERTS_ENABLED, true)

    override fun setAlertsNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_ALERTS_ENABLED, enabled).apply()
    }

    override fun fcmToken(): String? =
        prefs.getString(KEY_FCM_TOKEN, null)

    override fun setFcmToken(token: String?) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    companion object {
        const val PREFS_FILE = "techvision_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIF_NEWS_ENABLED = "notif_news_enabled"
        private const val KEY_NOTIF_ANNOUNCEMENTS_ENABLED = "notif_announcements_enabled"
        private const val KEY_NOTIF_ALERTS_ENABLED = "notif_alerts_enabled"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}