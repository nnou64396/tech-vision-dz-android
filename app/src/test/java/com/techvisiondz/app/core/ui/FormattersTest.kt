package com.techvisiondz.app.core.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `views formats zero as zero`() {
        assertEquals("0", formatViewsCount(0L, Locale.US))
    }

    @Test
    fun `views formats a small number`() {
        assertEquals("42", formatViewsCount(42L, Locale.US))
    }

    @Test
    fun `views formats a larger number with grouping`() {
        assertEquals("1,234", formatViewsCount(1234L, Locale.US))
    }

    @Test
    fun `views uses the requested locale grouping`() {
        assertEquals("1.234", formatViewsCount(1234L, Locale.GERMANY))
    }
}