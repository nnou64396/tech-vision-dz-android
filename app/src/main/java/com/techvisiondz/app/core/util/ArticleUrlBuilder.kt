package com.techvisiondz.app.core.util

/**
 * Builds canonical public article URLs used when sharing an article.
 *
 * Pure and deterministic (no Android dependencies) so it can be unit-tested
 * on the JVM. Produces URLs of the form:
 *
 *     https://techvisiondz.com/article/{slug}
 *
 * [buildArticleUrl] rejects blank inputs with [IllegalArgumentException]
 * instead of producing a malformed link. It never creates `//article`,
 * `https:///article`, or a URL without the base.
 */
object ArticleUrlBuilder {

    /** Canonical web path segment for article detail pages. */
    const val ARTICLE_PATH: String = "article"

    /**
     * Returns `baseUrl/article/slug` with whitespace trimmed and duplicate
     * slashes avoided, or throws [IllegalArgumentException] when the base URL
     * or slug is blank/invalid.
     */
    fun buildArticleUrl(baseUrl: String, slug: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.isNotEmpty()) { "Article base URL must not be blank" }
        require(cleanBase.startsWith("https://") || cleanBase.startsWith("http://")) {
            "Article base URL must be an absolute http(s) URL"
        }
        val cleanSlug = slug.trim().trimStart('/')
        require(cleanSlug.isNotEmpty()) { "Article slug must not be blank" }
        return "$cleanBase/$ARTICLE_PATH/$cleanSlug"
    }
}