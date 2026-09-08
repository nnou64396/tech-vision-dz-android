package com.techvisiondz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleShareTextTest {

    private val url = "https://techvisiondz.com/article/example-slug"

    @Test
    fun `share text contains title and canonical url`() {
        assertEquals(
            "Example title\n$url",
            articleShareText(title = "Example title", url = url),
        )
    }

    @Test
    fun `share text keeps only the url when title is blank`() {
        assertEquals(url, articleShareText(title = "   ", url = url))
    }

    @Test
    fun `share text keeps only the url when title is null`() {
        assertEquals(url, articleShareText(title = null, url = url))
    }

    @Test
    fun `share text trims title whitespace`() {
        assertEquals("Title\n$url", articleShareText(title = "  Title  ", url = url))
    }
}