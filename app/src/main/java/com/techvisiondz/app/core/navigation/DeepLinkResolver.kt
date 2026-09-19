package com.techvisiondz.app.core.navigation

import com.techvisiondz.app.core.util.ArticleUrlBuilder
import java.net.URI
import java.net.URLDecoder

/**
 * A deep link the app understands how to route.
 *
 * Only destinations backed by an existing [Routes] entry are modeled. The
 * canonical web structure is reused as-is (no invented URL format), so today
 * this is exactly the article URL built by [ArticleUrlBuilder]. Tag and video
 * links are intentionally absent: the site/app have no such canonical URL and
 * no dedicated destination.
 */
sealed interface DeepLinkTarget {

    /** `https://techvisiondz.com/article/{slug}` → [Routes.ArticleDetail]. */
    data class Article(val slug: String) : DeepLinkTarget
}

/**
 * Pure, Android-free resolver mapping a VIEW intent URL to a [DeepLinkTarget].
 *
 * Only `https` links on the canonical web host are accepted, and only under the
 * canonical `/article/{slug}` path. Anything else — wrong scheme, wrong host,
 * unknown path, blank or malformed input — resolves to null so the activity can
 * fail safely (stay on the current screen) instead of crashing or inventing a
 * route.
 *
 * Pure on purpose (java.net only) so the routing contract is unit-testable on
 * the JVM, mirroring [ArticleUrlBuilder] and [com.techvisiondz.app.core.util.DownloadUrlPolicy].
 */
object DeepLinkResolver {

    /**
     * Canonical web host. Must stay in sync with the `android:host` declared by
     * the MainActivity VIEW intent filter in AndroidManifest.xml — only links on
     * this exact host reach the app, and only links on this exact host route.
     */
    const val SUPPORTED_HOST: String = "techvisiondz.com"

    /**
     * Returns the [DeepLinkTarget] described by [rawUrl], or null when the URL
     * is unsupported, unsafe or malformed. A trailing slash and any query /
     * fragment are tolerated; the fragment is never part of an article slug.
     */
    fun resolve(rawUrl: String?): DeepLinkTarget? {
        val text = rawUrl?.trim().orEmpty()
        if (text.isEmpty()) return null

        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        if (!uri.isAbsolute) return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (!uri.host.equals(SUPPORTED_HOST, ignoreCase = true)) return null

        val segments = uri.rawPath?.trim('/')?.split('/')?.filter { it.isNotEmpty() }.orEmpty()
        if (segments.size != 2) return null
        if (segments[0] != ArticleUrlBuilder.ARTICLE_PATH) return null

        val slug = runCatching { URLDecoder.decode(segments[1], Charsets.UTF_8.name()) }
            .getOrNull()
            ?.trim()
            .orEmpty()
        if (slug.isEmpty()) return null

        return DeepLinkTarget.Article(slug)
    }
}