package com.techvisiondz.app.core.data.model

import kotlinx.serialization.Serializable

/**
 * Article-card item returned by the discovery RPCs
 * (`get_published_article_cards_by_category` / `_by_author` / `_by_tag`).
 *
 * Unlike the home-feed PostgREST query (snake_case columns), these functions
 * return a camelCase JSON document per card (verified against production):
 * title/slug/excerpt flat, author/category as {name}, cover as {bucket,
 * storagePath}. Only the fields needed for a card are modeled; the default
 * supabase-kt decoder ignores unknown keys.
 */
@Serializable
data class ArticleCardRpcDoc(
    val id: String = "",
    val slug: String? = null,
    val title: String? = null,
    val excerpt: String? = null,
    val featured: Boolean? = null,
    val viewsCount: Long? = null,
    val publishedAt: String? = null,
    val readingTimeMinutes: Int? = null,
    val author: ArticleCardRpcAuthor? = null,
    val category: ArticleCardRpcCategory? = null,
    val cover: ArticleCardRpcCover? = null,
)

@Serializable
data class ArticleCardRpcAuthor(
    val name: String? = null,
)

@Serializable
data class ArticleCardRpcCategory(
    val name: String? = null,
)

@Serializable
data class ArticleCardRpcCover(
    val bucket: String? = null,
    val storagePath: String? = null,
)

/** Maps a discovery-RPC document to a compact [ArticleCard], never throwing on gaps. */
fun ArticleCardRpcDoc.toArticleCard(
    resolvePublicUrl: (bucket: String, storagePath: String) -> String,
): ArticleCard? {
    val cardSlug = slug?.takeIf { it.isNotBlank() } ?: return null
    val cardTitle = title?.takeIf { it.isNotBlank() } ?: return null
    val coverUrl = cover?.let { doc ->
        val bucket = doc.bucket
        val path = doc.storagePath
        if (bucket.isNullOrBlank() || path.isNullOrBlank()) null else resolvePublicUrl(bucket, path)
    }
    return ArticleCard(
        id = id,
        slug = cardSlug,
        title = cardTitle,
        excerpt = excerpt,
        featured = featured ?: false,
        publishedAt = publishedAt ?: "",
        readingTimeMinutes = readingTimeMinutes,
        viewsCount = viewsCount ?: 0L,
        categoryName = category?.name,
        authorName = author?.name,
        coverUrl = coverUrl,
    )
}