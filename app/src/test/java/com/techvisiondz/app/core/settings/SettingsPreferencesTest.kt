package com.techvisiondz.app.core.settings

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/** System-language mapping used before the user picks a UI language. */
class SettingsPreferencesTest {

    private lateinit var original: Locale

    @Before
    fun setUp() {
        original = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(original)
    }

    @Test
    fun `french and russian system locales map to their app languages`() {
        Locale.setDefault(Locale.FRENCH)
        assertEquals(AppLanguage.FRENCH, defaultSystemLanguage())

        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        assertEquals(AppLanguage.RUSSIAN, defaultSystemLanguage())
    }

    @Test
    fun `arabic system locale maps to arabic and others fall back to english`() {
        Locale.setDefault(Locale.forLanguageTag("ar-DZ"))
        assertEquals(AppLanguage.ARABIC, defaultSystemLanguage())

        Locale.setDefault(Locale.GERMAN)
        assertEquals(AppLanguage.ENGLISH, defaultSystemLanguage())
    }

    @Test
    fun `french and russian are left-to-right bcp47 languages`() {
        assertEquals("fr", AppLanguage.FRENCH.code)
        assertEquals("ru", AppLanguage.RUSSIAN.code)
        assertFalse(AppLanguage.FRENCH.isRtl)
        assertFalse(AppLanguage.RUSSIAN.isRtl)
    }
}
