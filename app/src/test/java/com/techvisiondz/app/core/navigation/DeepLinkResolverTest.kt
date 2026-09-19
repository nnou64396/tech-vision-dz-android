package com.techvisiondz.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deterministic JVM tests for [DeepLinkResolver] — the pure routing contract
 * that turns a VIEW intent URL into the article screen it should open, or
 * nothing when the link is unsupported or unsafe.
 */
class DeepLinkResolverTest {

    @Test
    fun `canonical article url resolves to Article target`() {
        assertEquals(
            DeepLinkTarget.Article("example-slug"),
            DeepLinkResolver.resolve("https://techvisiondz.com/article/example-slug"),
        )
    }

    @Test
    fun `article url with trailing slash still resolves`() {
        assertEquals(
            DeepLinkTarget.Article("example-slug"),
            DeepLinkResolver.resolve("https://techvisiondz.com/article/example-slug/"),
        )
    }

    @Test
    fun `article url with query and fragment still resolves`() {
        assertEquals(
            DeepLinkTarget.Article("example-slug"),
            DeepLinkResolver.resolve("https://techvisiondz.com/article/example-slug?utm_source=app#section"),
        )
    }

    @Test
    fun `percent-encoded slug is decoded`() {
        assertEquals(
            DeepLinkTarget.Article("an article"),
            DeepLinkResolver.resolve("https://techvisiondz.com/article/an%20article"),
        )
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(DeepLinkResolver.resolve(null))
        assertNull(DeepLinkResolver.resolve(""))
        assertNull(DeepLinkResolver.resolve("   "))
    }

    @Test
    fun `http scheme is rejected`() {
        assertNull(DeepLinkResolver.resolve("http://techvisiondz.com/article/example-slug"))
    }

    @Test
    fun `uppercase host is accepted`() {
        assertEquals(
            DeepLinkTarget.Article("example-slug"),
            DeepLinkResolver.resolve("https://TECHVISIONDZ.COM/article/example-slug"),
        )
    }

    @Test
    fun `unknown host is rejected`() {
        assertNull(DeepLinkResolver.resolve("https://example.com/article/example-slug"))
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com.evil.net/article/example-slug"))
    }

    @Test
    fun `non article path is rejected`() {
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/tag/example-slug"))
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/video/example-slug"))
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/home/example-slug"))
    }

    @Test
    fun `article with missing slug is rejected`() {
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/article"))
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/article/"))
    }

    @Test
    fun `article with extra path segments is rejected`() {
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/article/example-slug/extra"))
    }

    @Test
    fun `malformed url is rejected`() {
        assertNull(DeepLinkResolver.resolve("not a url at all"))
        assertNull(DeepLinkResolver.resolve("techvisiondz.com/article/example-slug"))
        assertNull(DeepLinkResolver.resolve("https://"))
    }

    @Test
    fun `anchor only or empty path is rejected`() {
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com/#fragment"))
        assertNull(DeepLinkResolver.resolve("https://techvisiondz.com"))
    }
}