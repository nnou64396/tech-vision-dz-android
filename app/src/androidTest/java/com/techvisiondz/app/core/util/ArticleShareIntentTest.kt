package com.techvisiondz.app.core.util

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleShareIntentTest {

    private val canonicalUrl = "https://techvisiondz.com/article/example-slug"

    @Test
    fun intentUsesActionSendWithPlainText() {
        val intent = createArticleShareIntent(url = canonicalUrl, title = "Example title")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
    }

    @Test
    fun intentContainsCanonicalUrlAndTitleInTextAndTitleExtras() {
        val intent = createArticleShareIntent(url = canonicalUrl, title = "Example title")
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        assertEquals("Example title\n$canonicalUrl", text)
        assertEquals("Example title", intent.getStringExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun intentDoesNotLeakSupabaseUrlsOrInternalIds() {
        val intent = createArticleShareIntent(url = canonicalUrl, title = "Example title")
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        assertTrue(text.orEmpty().contains("https://techvisiondz.com/article/example-slug"))
        assertFalse(text.orEmpty().contains("supabase.co"))
        assertFalse(text.orEmpty().contains("rest/v1"))
        assertFalse(text.orEmpty().contains("rpc"))
        assertFalse(text.orEmpty().contains("article-1"))
    }

    @Test
    fun intentOmitsTitleAlso() {
        val intent = createArticleShareIntent(url = canonicalUrl, title = "   ")
        assertEquals(canonicalUrl, intent.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(intent.getStringExtra(Intent.EXTRA_TITLE))
    }
}