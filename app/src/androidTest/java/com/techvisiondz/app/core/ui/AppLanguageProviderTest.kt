package com.techvisiondz.app.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.techvisiondz.app.R
import com.techvisiondz.app.core.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic tests proving [ProvideAppLanguage] applies the selected
 * language to the whole subtree: string resources flip between the English
 * and Arabic (RTL) localisations, and the layout direction follows — all
 * without any activity or navigation-stack recreation.
 */
class AppLanguageProviderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun englishResolvesEnglishResourcesAndKeepsLtrLayout() {
        var label = ""
        var direction = LayoutDirection.Rtl

        composeRule.setContent {
            ProvideAppLanguage(AppLanguage.ENGLISH) {
                label = stringResource(R.string.settings)
                direction = LocalLayoutDirection.current
            }
        }
        composeRule.waitForIdle()

        assertEquals("Settings", label)
        assertEquals(LayoutDirection.Ltr, direction)
    }

    @Test
    fun arabicResolvesArabicResourcesAndFlipsToRtlLayout() {
        var label = ""
        var direction = LayoutDirection.Ltr

        composeRule.setContent {
            ProvideAppLanguage(AppLanguage.ARABIC) {
                label = stringResource(R.string.settings)
                direction = LocalLayoutDirection.current
            }
        }
        composeRule.waitForIdle()

        assertEquals("الإعدادات", label)
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun switchingLanguageTakesEffectInPlace() {
        var label = ""
        var direction = LayoutDirection.Ltr
        val languageState = mutableStateOf(AppLanguage.ENGLISH)

        composeRule.setContent {
            ProvideAppLanguage(languageState.value) {
                label = stringResource(R.string.settings)
                direction = LocalLayoutDirection.current
            }
        }
        composeRule.waitForIdle()
        assertEquals("Settings", label)

        composeRule.runOnUiThread { languageState.value = AppLanguage.ARABIC }
        composeRule.waitForIdle()

        assertEquals("الإعدادات", label)
        assertEquals(LayoutDirection.Rtl, direction)
    }
}