package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bucket that stores user avatars (public TECH VISION DZ bucket, migration 0043). */
private const val AVATAR_BUCKET = "avatars"

/**
 * Full, locale-aware article returned by the `get_published_article_by_slug`
 * RPC (migrations 0037/0039). Kept deliberately close to the website's public
 * shapes so a future article-details screen maps 1:1.
 */
data class Article(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String?,
    val body: String?,
    val featured: Boolean,
    val publishedAt: String,
    val readingTimeMinutes: Int?,
    val viewsCount: Long,
    val category: CategorySummary?,
    val author: AuthorSummary?,
    val tags: List<TagSummary>,
    val coverUrl: String?,
    val coverAlt: String?,
    val video: VideoRef?,
    val software: SoftwareSummary?,
)

data class CategorySummary(
    val slug: String,
    val name: String,
    val description: String?,
)

data class AuthorSummary(
    val name: String,
    val bio: String?,
    val avatarUrl: String?,
)

data class TagSummary(
    val slug: String,
    val label: String,
)

/** Video attached to an article: an embed URL, or an upload resolved to a URL. */
data class VideoRef(
    val kind: String,
    val url: String?,
)

data class SoftwareSummary(
    val name: String,
    val version: String?,
    val downloadUrl: String?,
    val fileUrl: String? = null,
)

/**
 * JSONB document returned by `get_published_article_by_slug`. Snake_case keys
 * exactly as produced by the RPC.
 */
@Serializable
data class ArticleRpcDoc(
    val id: String,
    val status: String? = null,
    val featured: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("reading_time_minutes") val readingTimeMinutes: Int? = null,
    @SerialName("views_count") val viewsCount: Long? = null,
    val translation: ArticleTranslationDoc? = null,
    val author: AuthorDoc? = null,
    val category: CategoryDoc? = null,
    val tags: List<TagDoc> = emptyList(),
    val cover: MediaDoc? = null,
    val video: VideoDoc? = null,
    val software: SoftwareDoc? = null,
    val download: DownloadDoc? = null,
)

@Serializable
data class ArticleTranslationDoc(
    @SerialName("language_code") val languageCode: String,
    val title: String,
    val slug: String,
    val excerpt: String? = null,
    val body: String? = null,
)

@Serializable
data class AuthorDoc(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    val bio: String? = null,
)

@Serializable
data class CategoryDoc(
    val slug: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class TagDoc(
    val slug: String,
    val label: String,
)

@Serializable
data class MediaDoc(
    @SerialName("storage_path") val storagePath: String? = null,
    val bucket: String? = null,
    val alt: String? = null,
)

@Serializable
data class VideoDoc(
    val kind: String? = null,
    val url: String? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    val bucket: String? = null,
)

@Serializable
data class SoftwareDoc(
    val name: String? = null,
    val version: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class DownloadDoc(
    @SerialName("storage_path") val storagePath: String? = null,
    val bucket: String? = null,
    @SerialName("original_name") val originalName: String? = null,
)

/** Maps the RPC document into a locale-aware [Article], or null if not publishable. */
fun ArticleRpcDoc.toArticle(
    languageCode: String,
    resolvePublicUrl: (bucket: String, storagePath: String) -> String,
): Article? {
    val t = translation ?: return null
    if (t.languageCode != languageCode) return null

    return Article(
        id = id,
        slug = t.slug,
        title = t.title,
        excerpt = t.excerpt,
        body = t.body,
        featured = featured,
        publishedAt = publishedAt ?: "",
        readingTimeMinutes = readingTimeMinutes,
        viewsCount = viewsCount ?: 0L,
        category = category?.let { CategorySummary(slug = it.slug, name = it.name, description = it.description) },
        author = author?.let { a ->
            AuthorSummary(
                name = a.name.orEmpty(),
                bio = a.bio,
                avatarUrl = a.avatarUrl?.takeUnless { it.isBlank() }?.let { path ->
                    resolvePublicUrl(AVATAR_BUCKET, path)
                },
            )
        },
        tags = tags.map { TagSummary(slug = it.slug, label = it.label) },
        coverUrl = cover?.resolveUrl(resolvePublicUrl),
        coverAlt = cover?.alt,
        video = video?.toVideoRef(resolvePublicUrl),
        software = software?.takeIf { !it.name.isNullOrBlank() }?.let {
            SoftwareSummary(
                name = it.name!!,
                version = it.version,
                downloadUrl = it.downloadUrl,
                // The sibling `download` object describes the actual artifact
                // stored in Supabase Storage (bucket + storage_path). Resolving
                // it through the same public-URL helper used for covers and
                // avatars exposes the direct file target alongside the external
                // URL, without inventing any new backend field.
                fileUrl = download?.resolveDownloadUrl(resolvePublicUrl),
            )
        },
    )
}

/** Resolves a storage-backed download artifact to its public URL, or null when incomplete. */
private fun DownloadDoc.resolveDownloadUrl(resolvePublicUrl: (bucket: String, storagePath: String) -> String): String? {
    val b = bucket
    val p = storagePath
    if (b.isNullOrBlank() || p.isNullOrBlank()) return null
    return resolvePublicUrl(b, p)
}

private fun MediaDoc.resolveUrl(resolvePublicUrl: (bucket: String, storagePath: String) -> String): String? {
    val b = bucket
    val p = storagePath
    if (b.isNullOrBlank() || p.isNullOrBlank()) return null
    return resolvePublicUrl(b, p)
}

private fun VideoDoc.toVideoRef(resolvePublicUrl: (bucket: String, storagePath: String) -> String): VideoRef? =
    when (kind) {
        "embed" -> VideoRef(kind = "embed", url = url?.takeUnless { it.isBlank() })
        "upload" -> VideoRef(kind = "upload", url = resolveUrl(resolvePublicUrl))
        else -> null
    }

private fun VideoDoc.resolveUrl(resolvePublicUrl: (bucket: String, storagePath: String) -> String): String? {
    val b = bucket
    val p = storagePath
    if (b.isNullOrBlank() || p.isNullOrBlank()) return null
    return resolvePublicUrl(b, p)
}