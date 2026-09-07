package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bucket storing author avatars (public TECH VISION DZ bucket). */
private const val AVATAR_BUCKET = "avatars"

/** A discoverable category, localized for the requested language. */
data class Category(
    val id: String,
    val slug: String,
    val name: String,
    val description: String?,
)

/** A discoverable author, localized for the requested language. */
data class Author(
    val id: String,
    val slug: String,
    val name: String,
    val bio: String?,
    val avatarUrl: String?,
)

/** A discoverable tag, localized for the requested language. */
data class Tag(
    val id: String,
    val slug: String,
    val name: String,
)

/**
 * Raw PostgREST row shape for the categories listing.
 *
 * The root `categories.slug` column is null in production; the canonical slug
 * lives on `category_translations.slug` and is what the card RPCs expect.
 */
@Serializable
data class CategoryRow(
    val id: String,
    @SerialName("category_translations") val translations: List<CategoryTranslationRow> = emptyList(),
)

/** Raw PostgREST row shape for the tags listing. */
@Serializable
data class TagRow(
    val id: String,
    val slug: String = "",
    @SerialName("tag_translations") val translations: List<TagTranslationRow> = emptyList(),
)

@Serializable
data class TagTranslationRow(
    @SerialName("language_code") val languageCode: String,
    val label: String,
)

/** Maps a raw category row to a locale-aware [Category]; mirrors the feed's translation fallback. */
fun CategoryRow.toCategory(languageCode: String): Category? {
    val translation = translations.firstOrNull { it.languageCode == languageCode }
        ?: translations.firstOrNull()
        ?: return null
    val name = translation.name.takeIf { it.isNotBlank() } ?: return null
    val slug = translation.slug?.takeIf { it.isNotBlank() } ?: return null
    return Category(
        id = id,
        slug = slug,
        name = name,
        description = translation.description,
    )
}

/** Maps a raw author row to a locale-aware [Author]; the avatar resolves through storage. */
fun AuthorFeedRow.toAuthor(
    languageCode: String,
    resolvePublicUrl: (bucket: String, storagePath: String) -> String,
): Author? {
    val translation = authorTranslations.firstOrNull { it.languageCode == languageCode }
        ?: authorTranslations.firstOrNull()
        ?: return null
    val name = translation.displayName.takeIf { it.isNotBlank() } ?: return null
    val slug = this.slug?.takeIf { it.isNotBlank() } ?: translation.slug?.takeIf { it.isNotBlank() } ?: return null
    val avatarUrl = avatarUrl?.takeIf { it.isNotBlank() }?.let { path ->
        resolvePublicUrl(AVATAR_BUCKET, path)
    }
    return Author(
        id = id,
        slug = slug,
        name = name,
        bio = translation.bio,
        avatarUrl = avatarUrl,
    )
}

/** Maps a raw tag row to a locale-aware [Tag]. */
fun TagRow.toTag(languageCode: String): Tag? {
    val label = translations.firstOrNull { it.languageCode == languageCode }?.label
        ?: translations.firstOrNull()?.label
    if (label.isNullOrBlank()) return null
    return Tag(id = id, slug = slug, name = label)
}