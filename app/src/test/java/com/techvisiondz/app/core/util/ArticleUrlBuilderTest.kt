package com.techvisiondz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArticleUrlBuilderTest {

    private val productionBase = "https://techvisiondz.com"

    @Test
    fun `valid slug builds canonical url`() {
        assertEquals(
            "https://techvisiondz.com/article/example-slug",
            ArticleUrlBuilder.buildArticleUrl(productionBase, "example-slug"),
        )
    }

    @Test
    fun `blank slug is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleUrlBuilder.buildArticleUrl(productionBase, "")
        }
    }

    @Test
    fun `whitespace around slug is trimmed`() {
        assertEquals(
            "https://techvisiondz.com/article/example-slug",
            ArticleUrlBuilder.buildArticleUrl(productionBase, "  example-slug  "),
        )
    }

    @Test
    fun `base url with trailing slash matches base without one`() {
        val withSlash = ArticleUrlBuilder.buildArticleUrl("https://techvisiondz.com/", "example-slug")
        val withoutSlash = ArticleUrlBuilder.buildArticleUrl("https://techvisiondz.com", "example-slug")
        assertEquals(withoutSlash, withSlash)
        assertEquals("https://techvisiondz.com/article/example-slug", withSlash)
    }

    @Test
    fun `leading slash on slug does not create double slashes`() {
        assertEquals(
            "https://techvisiondz.com/article/example-slug",
            ArticleUrlBuilder.buildArticleUrl("https://techvisiondz.com/", "/example-slug"),
        )
    }

    @Test
    fun `canonical path is always article`() {
        val url = ArticleUrlBuilder.buildArticleUrl(productionBase, "example-slug")
        assertEquals("/article/example-slug", url.substringAfter("https://techvisiondz.com"))
    }

    @Test
    fun `blank base url is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleUrlBuilder.buildArticleUrl("", "example-slug")
        }
    }

    @Test
    fun `blank base url with whitespace is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleUrlBuilder.buildArticleUrl("   ", "example-slug")
        }
    }

    @Test
    fun `non-http base url is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ArticleUrlBuilder.buildArticleUrl("ftp://example.com", "example-slug")
        }
    }

    @Test
    fun `production slug produces the exact production url`() {
        val slug = "shrka-qualcomm-almarwfa-balmaaljat-ally-alnt-fy-mard-computex-2026-ala-maalj-processor-jdyd-smawh-snapdragon-c"
        assertEquals(
            "https://techvisiondz.com/article/$slug",
            ArticleUrlBuilder.buildArticleUrl(productionBase, slug),
        )
    }
}