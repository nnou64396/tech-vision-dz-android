package com.techvisiondz.app.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit coverage for the article-locale rules. These are the rules that decide
 * whether a language appears in the selector at all, so they are pinned here
 * rather than only through the UI.
 */
class ArticleLocaleTest {

    // --- normalize ---------------------------------------------------------

    @Test
    fun `normalize lowercases and trims a language code`() {
        assertEquals("fr", ArticleLocales.normalize("  FR  "))
        assertEquals("arq", ArticleLocales.normalize("ARQ"))
    }

    @Test
    fun `normalize returns null for null and blank codes`() {
        assertNull(ArticleLocales.normalize(null))
        assertNull(ArticleLocales.normalize(""))
        assertNull(ArticleLocales.normalize("   "))
    }

    // --- supported / rtl ---------------------------------------------------

    @Test
    fun `the four website locales are supported and none others`() {
        assertEquals(listOf("arq", "ar", "fr", "en"), ArticleLocales.SUPPORTED)
        listOf("arq", "ar", "fr", "en").forEach {
            assertTrue("$it should be supported", ArticleLocales.isSupported(it))
        }
        listOf("es", "de", "ar-SA", "ber", "zxx").forEach {
            assertFalse("$it should not be supported", ArticleLocales.isSupported(it))
        }
    }

    @Test
    fun `darija and fusha are distinct RTL locales`() {
        assertTrue(ArticleLocales.isRtl(ArticleLocales.DARIJA))
        assertTrue(ArticleLocales.isRtl(ArticleLocales.ARABIC))
        assertFalse(ArticleLocales.isRtl(ArticleLocales.FRENCH))
        assertFalse(ArticleLocales.isRtl(ArticleLocales.ENGLISH))
        // The two Arabic locales must never collapse into one another.
        assertFalse(ArticleLocales.DARIJA == ArticleLocales.ARABIC)
    }

    @Test
    fun `isRtl and isSupported tolerate case, padding and null`() {
        assertTrue(ArticleLocales.isRtl(" AR "))
        assertTrue(ArticleLocales.isSupported("Fr"))
        assertFalse(ArticleLocales.isRtl(null))
        assertFalse(ArticleLocales.isSupported(null))
        assertFalse(ArticleLocales.isRtl("es"))
    }

    // --- row mapping -------------------------------------------------------

    @Test
    fun `rows are returned in selector order regardless of wire order`() {
        val rows = listOf(
            row("en", "english-slug"),
            row("fr", "french-slug"),
            row("ar", "fusha-slug"),
            row("arq", "darija-slug"),
        )

        assertEquals(
            listOf("arq", "ar", "fr", "en"),
            rows.toTranslationRefs().map { it.languageCode },
        )
    }

    @Test
    fun `each locale keeps its own slug`() {
        val rows = listOf(row("arq", "slug-arq"), row("ar", "slug-ar"), row("fr", "slug-fr"), row("en", "slug-en"))

        val refs = rows.toTranslationRefs()

        assertEquals("slug-arq", refs.first { it.languageCode == "arq" }.slug)
        assertEquals("slug-ar", refs.first { it.languageCode == "ar" }.slug)
        assertEquals("slug-fr", refs.first { it.languageCode == "fr" }.slug)
        assertEquals("slug-en", refs.first { it.languageCode == "en" }.slug)
    }

    @Test
    fun `an article with no other translation maps to a single entry`() {
        val rows = listOf(row("arq", "only-darija"))

        val refs = rows.toTranslationRefs()

        assertEquals(1, refs.size)
        assertEquals("arq", refs.single().languageCode)
    }

    @Test
    fun `missing and null locale fields are dropped instead of throwing`() {
        val rows = listOf(
            ArticleTranslationIndexRow(articleId = "a", languageCode = null, slug = "s"),
            ArticleTranslationIndexRow(articleId = "a", languageCode = "", slug = "s"),
            ArticleTranslationIndexRow(articleId = "a", languageCode = "   ", slug = "s"),
            row("fr", "kept"),
        )

        assertEquals(listOf("fr"), rows.toTranslationRefs().map { it.languageCode })
    }

    @Test
    fun `rows with a missing slug are dropped so the selector has no dead option`() {
        val rows = listOf(
            ArticleTranslationIndexRow(articleId = "a", languageCode = "fr", slug = null),
            ArticleTranslationIndexRow(articleId = "a", languageCode = "en", slug = "  "),
            row("arq", "kept"),
        )

        val refs = rows.toTranslationRefs()

        assertEquals(listOf("arq"), refs.map { it.languageCode })
    }

    @Test
    fun `an unsupported locale is never surfaced as an option`() {
        val rows = listOf(row("arq", "darija"), row("es", "spanish"), row("de", "german"))

        assertEquals(listOf("arq"), rows.toTranslationRefs().map { it.languageCode })
    }

    @Test
    fun `an empty or all-malformed row set yields no versions at all`() {
        assertTrue(emptyList<ArticleTranslationIndexRow>().toTranslationRefs().isEmpty())
        assertTrue(
            listOf(
                ArticleTranslationIndexRow(articleId = "a", languageCode = null, slug = null),
                ArticleTranslationIndexRow(articleId = "a", languageCode = "xx", slug = ""),
            ).toTranslationRefs().isEmpty(),
        )
    }

    @Test
    fun `a duplicated locale keeps the first slug and appears once`() {
        val rows = listOf(row("fr", "first-slug"), row("fr", "second-slug"), row("arq", "darija"))

        val refs = rows.toTranslationRefs()

        assertEquals(listOf("arq", "fr"), refs.map { it.languageCode })
        assertEquals("first-slug", refs.first { it.languageCode == "fr" }.slug)
    }

    @Test
    fun `locale codes are normalized while mapping so casing cannot split a version`() {
        val rows = listOf(row("ARQ", "darija"), row("Fr", "french"))

        val refs = rows.toTranslationRefs()

        assertEquals(listOf("arq", "fr"), refs.map { it.languageCode })
    }

    @Test
    fun `rows without an article id are still mapped since the filter guarantees it`() {
        val rows = listOf(ArticleTranslationIndexRow(languageCode = "arq", slug = "darija"))

        assertEquals(listOf("arq"), rows.toTranslationRefs().map { it.languageCode })
    }

    private fun row(language: String?, slug: String?) =
        ArticleTranslationIndexRow(articleId = "article-1", languageCode = language, slug = slug)
}
