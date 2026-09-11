package com.techvisiondz.app.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the public content language contract: source articles are authored in
 * Algerian Darija ('arq'), so the app default must always resolve to 'arq' and
 * never backslide to 'ar' (a machine-translation target with no source rows).
 */
class AppConfigTest {

    @Test
    fun `default language code is Algerian Darija arq`() {
        assertEquals("arq", AppConfig.DEFAULT_LANGUAGE_CODE)
    }
}