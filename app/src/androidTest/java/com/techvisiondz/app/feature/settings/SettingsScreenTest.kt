package com.techvisiondz.app.feature.settings

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.BuildConfig
import com.techvisiondz.app.R
import com.techvisiondz.app.core.settings.AppLanguage
import com.techvisiondz.app.core.settings.FakeSettingsPreferences
import com.techvisiondz.app.core.settings.ThemeMode
import com.techvisiondz.app.core.update.ApkVerificationResult
import com.techvisiondz.app.core.update.InstallLaunchResult
import com.techvisiondz.app.core.update.InstallPermissionState
import com.techvisiondz.app.core.update.InstallerResolution
import com.techvisiondz.app.core.update.UpdateApkDownloader
import com.techvisiondz.app.core.update.UpdateApkInstaller
import com.techvisiondz.app.core.update.UpdateApkVerifier
import com.techvisiondz.app.core.update.UpdateInfo
import com.techvisiondz.app.core.update.UpdateManifestFetcher
import com.techvisiondz.app.core.update.UpdatePreferences
import com.techvisiondz.app.core.update.UpdateRepository
import com.techvisiondz.app.feature.update.UpdateViewModel
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for [SettingsScreen]: sections render in both
 * languages, theme + language selection applies immediately and persists, the
 * update row reuses the check state, and the About link is only rendered for a
 * valid http(s) URL (never an unvalidated web address).
 */
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = composeRule.activity

    private lateinit var prefs: FakeSettingsPreferences
    private lateinit var viewModel: SettingsViewModel

    private fun setScreen(
        theme: ThemeMode? = null,
        language: AppLanguage? = null,
        websiteUrl: String? = "https://techvisiondz.com",
        onOpenWebsite: (() -> Unit)? = null,
    ) {
        prefs = FakeSettingsPreferences(theme = theme, language = language)
        viewModel = SettingsViewModel(prefs) { AppLanguage.ENGLISH }
        composeRule.setContent {
            TechVisionDzTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    updateViewModel = idleUpdateViewModel(),
                    onBack = {},
                    websiteUrl = websiteUrl,
                    onOpenWebsite = onOpenWebsite,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** An UpdateViewModel that stays Idle: no check is ever triggered. */
    private fun idleUpdateViewModel(): UpdateViewModel = UpdateViewModel(
        repository = UpdateRepository(
            manifestUrl = "https://example.invalid/update-manifest.json",
            currentVersionCode = BuildConfig.VERSION_CODE,
            enabled = false,
            fetcher = object : UpdateManifestFetcher {
                override suspend fun fetchManifest(rawUrl: String): String =
                    error("manifest must never be fetched in the Settings UI test")
            },
        ),
        downloader = object : UpdateApkDownloader {
            override suspend fun download(url: String, dest: File, onProgress: (Float?) -> Unit) = Unit
        },
        verifier = object : UpdateApkVerifier {
            override suspend fun verify(apk: File, expected: UpdateInfo): ApkVerificationResult =
                ApkVerificationResult.Success
        },
        installer = object : UpdateApkInstaller {
            override fun installPermissionState(): InstallPermissionState = InstallPermissionState.Allowed
            override fun resolveInstaller(apk: File): InstallerResolution = InstallerResolution.Ready(Intent())
            override fun launchInstaller(apk: File): InstallLaunchResult = InstallLaunchResult.Launched
            override fun unknownAppSourcesSettingsIntent(): Intent = Intent()
        },
        preferences = object : UpdatePreferences {
            override fun lastDeferredAtMillis(): Long? = null
            override fun markDeferred(timestampMillis: Long) = Unit
            override fun lastAutomaticCheckAtMillis(): Long? = null
            override fun markAutomaticCheck(timestampMillis: Long) = Unit
            override fun clear() = Unit
        },
        application = context.application as Application,
    )

    @Test
    fun allSectionsRender() {
        setScreen()

        composeRule.onNodeWithText(context.getString(R.string.settings_appearance)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_language)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_updates)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_about)).assertIsDisplayed()
    }

    @Test
    fun systemThemeIsSelectedByDefault() {
        setScreen()

        composeRule.onNodeWithTag("settings_theme_system").assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_light").assertIsNotSelected()
        composeRule.onNodeWithTag("settings_theme_dark").assertIsNotSelected()
    }

    @Test
    fun selectingDarkThemeAppliesImmediatelyAndPersists() {
        setScreen()

        composeRule.onNodeWithTag("settings_theme_dark").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_theme_dark").assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_light").assertIsNotSelected()
        composeRule.onNodeWithTag("settings_theme_system").assertIsNotSelected()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.DARK, prefs.themeMode())
    }

    @Test
    fun selectingLightThemeUpdatesStateAndSelection() {
        setScreen(theme = ThemeMode.SYSTEM)

        composeRule.onNodeWithTag("settings_theme_light").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_theme_light").assertIsSelected()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun storedThemeChoiceIsShownAsSelected() {
        setScreen(theme = ThemeMode.DARK)

        composeRule.onNodeWithTag("settings_theme_dark").assertIsSelected()
    }

    @Test
    fun englishLanguageIsSelectedByDefault() {
        setScreen()

        composeRule.onNodeWithTag("settings_language_english").assertIsSelected()
        composeRule.onNodeWithTag("settings_language_arabic").assertIsNotSelected()
    }

    @Test
    fun selectingArabicLanguageAppliesImmediatelyAndPersists() {
        setScreen()

        composeRule.onNodeWithTag("settings_language_arabic").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_language_arabic").assertIsSelected()
        composeRule.onNodeWithTag("settings_language_english").assertIsNotSelected()
        assertEquals(AppLanguage.ARABIC, viewModel.uiState.value.language)
        assertEquals(AppLanguage.ARABIC, prefs.language())
    }

    @Test
    fun updateSectionShowsCheckPromptAndInstalledVersion() {
        setScreen()

        composeRule.onNodeWithText(context.getString(R.string.update_check)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.update_version, BuildConfig.VERSION_NAME),
        ).assertIsDisplayed()
    }

    @Test
    fun aboutSectionShowsBrandIdentityVersionAndWebsiteLink() {
        setScreen()

        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.update_version, BuildConfig.VERSION_NAME),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_website)).assertIsDisplayed()
    }

    @Test
    fun websiteLinkHiddenWhenUrlIsNotSafe() {
        setScreen(websiteUrl = null)

        composeRule.onNodeWithTag("settings_about_website").assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.settings_website)).assertDoesNotExist()
    }

    @Test
    fun websiteLinkShownOnlyForSafeHttpUrl() {
        setScreen(websiteUrl = "ftp://techvisiondz.com")

        composeRule.onNodeWithTag("settings_about_website").assertDoesNotExist()

        setScreen(websiteUrl = "https://techvisiondz.com")

        composeRule.onNodeWithTag("settings_about_website").assertIsDisplayed()
    }

    @Test
    fun clickingWebsiteLinkInvokesOpenHandler() {
        var opens = 0
        setScreen(onOpenWebsite = { opens++ })

        composeRule.onNodeWithTag("settings_about_website").performClick()
        composeRule.waitForIdle()

        assertEquals(1, opens)
    }
}