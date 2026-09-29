package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content locales the TECH VISION DZ website may publish an article in.
 *
 * The backend stores one row per (article, locale) in `article_translations`
 * and each of those rows carries its **own slug**, so the arq/ar/fr/en
 * versions of one article are four distinct URLs pointing at one `articles.id`.
 * Algerian Darija (`arq`) and Modern Standard Arabic (`ar`) are separate
 * locales with separate slugs and separate content: they are never merged and
 * never substituted for one another.
 *
 * This object is pure and Android-free so the parsing rules are unit-tested on
 * the JVM.
 */
object ArticleLocales {

    /** Algerian Arabic / Darija — the site's authoring source language. */
    const val DARIJA: String = "arq"

    /** Modern Standard Arabic / Fusha. */
    const val ARABIC: String = "ar"

    const val FRENCH: String = "fr"

    const val ENGLISH: String = "en"

    /**
     * Locales the app can render, in the order the selector lists them.
     *
     * A locale absent from this list is intentionally ignored rather than
     * rendered: the app ships a label for these four only, and inventing a
     * label for an unrecognised backend locale would be worse than hiding it.
     * Adding a fifth locale later is a one-line change here plus a label.
     */
    val SUPPORTED: List<String> = listOf(DARIJA, ARABIC, FRENCH, ENGLISH)

    /** Locales written right-to-left. Both Arabic locales are RTL. */
    private val RTL: Set<String> = setOf(DARIJA, ARABIC)

    /**
     * Canonicalises a backend `language_code` for comparison, or returns null
     * when it is absent/blank. Case and surrounding whitespace are the only
     * variations tolerated; the value is otherwise passed through unchanged so
     * a genuinely unknown locale is never silently rewritten into a supported
     * one.
     */
    fun normalize(code: String?): String? = code
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }

    /** True when [code] is a locale the app can display. */
    fun isSupported(code: String?): Boolean = normalize(code) in SUPPORTED

    /** True when [code] is a right-to-left locale; unknown codes are LTR. */
    fun isRtl(code: String?): Boolean = normalize(code) in RTL
}

/**
 * One published translation of an article: which locale it is, and the slug
 * that locale's version of the article is published under.
 *
 * The slug is required to switch languages because
 * `get_published_article_by_slug` resolves by translation slug, not by
 * article id: asking for the arq slug with `p_lang = 'fr'` returns null.
 */
data class ArticleTranslationRef(
    val languageCode: String,
    val slug: String,
)

/**
 * Row of the `article_translations` index read that backs the article language
 * selector. Snake_case, matching the columns exactly.
 */
@Serializable
data class ArticleTranslationIndexRow(
    @SerialName("article_id") val articleId: String? = null,
    @SerialName("language_code") val languageCode: String? = null,
    val slug: String? = null,
)

/**
 * Maps raw `article_translations` rows to the locales that are actually
 * available for one article, in selector order.
 *
 * Defensive by construction — this is the boundary where an article without
 * any multilingual metadata (older content), a null/blank locale, a blank slug
 * or an unrecognised locale must all degrade quietly instead of crashing or
 * producing an unusable selector entry. Unsupported, blank and duplicated
 * entries are dropped; the result may legitimately be empty, which the caller
 * treats as "single-language article, show no selector".
 */
fun List<ArticleTranslationIndexRow>.toTranslationRefs(): List<ArticleTranslationRef> {
    val byLanguage = LinkedHashMap<String, String>()
    for (row in this) {
        val language = ArticleLocales.normalize(row.languageCode) ?: continue
        if (language !in ArticleLocales.SUPPORTED) continue
        val slug = row.slug?.trim().orEmpty()
        if (slug.isEmpty()) continue
        // First non-blank slug wins: the table is keyed (article_id, language)
        // so duplicates would be a backend anomaly, not a real variant.
        byLanguage.putIfAbsent(language, slug)
    }
    return byLanguage.entries
        .sortedBy { ArticleLocales.SUPPORTED.indexOf(it.key) }
        .map { (language, slug) -> ArticleTranslationRef(languageCode = language, slug = slug) }
}
