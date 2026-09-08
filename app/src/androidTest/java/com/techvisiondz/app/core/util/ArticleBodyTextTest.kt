package com.techvisiondz.app.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleBodyTextTest {

    private val linkColor = Color.Blue

    @Test
    fun blankHtmlReturnsNull() {
        assertNull(htmlBodySpanned(null))
        assertNull(htmlBodySpanned("  "))
    }

    @Test
    fun httpLinkBecomesActionableAnnotation() {
        val spanned = htmlBodySpanned("<p>See <a href=\"https://example.com/x\">this</a>.</p>")
        val annotatedText = spannedToAnnotatedString(spanned!!, linkColor)
        val links = annotatedText.getLinkAnnotations(0, annotatedText.length)
        assertEquals(1, links.size)
        val link = links.single().item as LinkAnnotation.Url
        assertEquals("https://example.com/x", link.url)
    }

    @Test
    fun javascriptLinkIsIgnored() {
        val spanned = htmlBodySpanned("<p>Try <a href=\"javascript:alert(1)\">this</a>.</p>")
        val annotatedText = spannedToAnnotatedString(spanned!!, linkColor)
        assertTrue(annotatedText.getLinkAnnotations(0, annotatedText.length).isEmpty())
    }

    @Test
    fun bodyWithoutLinksHasNoAnnotations() {
        val spanned = htmlBodySpanned("<p>Just text.</p>")
        val annotatedText = spannedToAnnotatedString(spanned!!, linkColor)
        assertTrue(annotatedText.getLinkAnnotations(0, annotatedText.length).isEmpty())
    }

    @Test
    fun boldTextIsPreservedWithLinkStyle() {
        val spanned = htmlBodySpanned("<p><strong>Strong</strong> and <a href=\"https://example.com\">link</a>.</p>")
        val annotatedText = spannedToAnnotatedString(spanned!!, linkColor)
        assertEquals(1, annotatedText.getLinkAnnotations(0, annotatedText.length).size)
    }
}