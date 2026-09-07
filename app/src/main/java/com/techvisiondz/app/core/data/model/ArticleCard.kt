package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A compact, locale-aware article summary used to render list items such as the
 * home feed. Mapped from a published article row fetched from the existing
 * TECH VISION DZ Supabase project.
 */
data class ArticleCard(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val featured: Boolean,
    val publishedAt: String,
    val readingTimeMinutes: Int?,
    val viewsCount: Long,
    val categoryName: String?,
    val authorName: String?,
    val coverUrl: String?,
)

/**
 * Raw PostgREST row shape for the home-feed query. Field names map 1:1 to the
 * DB columns (snake_case); nested relations follow the exact select used by the
 * website so the Android feed shows identical content.
 */
@Serializable
data class ArticleFeedRow(
    val id: String,
    val status: String? = null,
    val featured: Boolean? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("reading_time_minutes") val readingTimeMinutes: Int? = null,
    @SerialName("views_count") val viewsCount: Long? = null,
    @SerialName("article_translations") val articleTranslations: List<ArticleTranslationRow> = emptyList(),
    val authors: AuthorFeedRow? = null,
    val categories: CategoryFeedRow? = null,
    @SerialName("article_media") val articleMedia: List<ArticleMediaLinkRow> = emptyList(),
)

@Serializable
data class ArticleTranslationRow(
    @SerialName("language_code") val languageCode: String,
    val title: String,
    val slug: String,
    val excerpt: String? = null,
    val body: String? = null,
)

@Serializable
data class AuthorFeedRow(
    val id: String,
    val slug: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("author_translations") val authorTranslations: List<AuthorTranslationRow> = emptyList(),
)

@Serializable
data class AuthorTranslationRow(
    @SerialName("language_code") val languageCode: String,
    @SerialName("display_name") val displayName: String,
    val bio: String? = null,
    val slug: String? = null,
)

@Serializable
data class CategoryFeedRow(
    val id: String,
    val slug: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("category_translations") val categoryTranslations: List<CategoryTranslationRow> = emptyList(),
)

@Serializable
data class CategoryTranslationRow(
    @SerialName("language_code") val languageCode: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
)

@Serializable
data class ArticleMediaLinkRow(
    val role: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val caption: String? = null,
    val media: MediaRow? = null,
)

@Serializable
data class MediaRow(
    val id: String,
    val type: String? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    val bucket: String? = null,
    @SerialName("alt_text") val altText: String? = null,
)

/** Maps a raw feed row to a locale-aware [ArticleCard], never throwing on gaps. */
fun ArticleFeedRow.toArticleCard(
    languageCode: String,
    resolvePublicUrl: (bucket: String, storagePath: String) -> String,
): ArticleCard? {
    val translation = articleTranslations.firstOrNull { it.languageCode == languageCode }
        ?: articleTranslations.firstOrNull()
        ?: return null

    return ArticleCard(
        id = id,
        slug = translation.slug,
        title = translation.title,
        excerpt = translation.excerpt,
        featured = featured ?: false,
        publishedAt = publishedAt ?: "",
        readingTimeMinutes = readingTimeMinutes,
        viewsCount = viewsCount ?: 0L,
        categoryName = categories?.pickName(languageCode),
        authorName = authors?.pickName(languageCode),
        coverUrl = articleMedia.firstOrNull { it.role == "cover" }?.media?.resolveUrl(resolvePublicUrl),
    )
}

private fun CategoryFeedRow.pickName(languageCode: String): String? =
    categoryTranslations.firstOrNull { it.languageCode == languageCode }?.name
        ?: categoryTranslations.firstOrNull()?.name

private fun AuthorFeedRow.pickName(languageCode: String): String? =
    authorTranslations.firstOrNull { it.languageCode == languageCode }?.displayName
        ?: authorTranslations.firstOrNull()?.displayName

private fun MediaRow.resolveUrl(resolvePublicUrl: (bucket: String, storagePath: String) -> String): String? {
    val b = bucket
    val p = storagePath
    if (b.isNullOrBlank() || p.isNullOrBlank()) return null
    return resolvePublicUrl(b, p)
}