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

/**
 * The website's "Software & Apps" category (Arabic: "البرامج والتطبيقات") is
 * intentionally hidden from Android discovery and browsing.
 *
 * The canonical slug lives on `category_translations`, so the slug that reaches
 * Android depends on the active language and may be a localized transliteration
 * (the website fixtures use `apps`). Matching therefore inspects every
 * translation's slug and name instead of assuming the English slug, and keeps a
 * token-pair fallback ("software" + "apps") for localized names in the shipped
 * languages. No category id is hard-coded.
 */
object CategoryExclusions {
    /** Known normalized slugs for the excluded category. */
    private val EXCLUDED_SLUGS = setOf(
        "app",
        "apps",
        "application",
        "applications",
        "software",
        "software-applications",
        "software-apps",
        "software-and-apps",
        "logiciel",
        "logiciels",
        "logiciels-applications",
        "programme",
        "programmes",
    )

    /** Tokens naming the "software" half of the category, per shipped language. */
    private val SOFTWARE_TOKENS = setOf(
        "software",
        "logiciel",
        "logiciels",
        "programme",
        "programmes",
        "برنامج",
        "برامج",
    )

    /** Tokens naming the "apps" half of the category, per shipped language. */
    private val APP_TOKENS = setOf(
        "app",
        "apps",
        "application",
        "applications",
        "تطبيق",
        "تطبيقات",
    )

    /** True when [slug] is the excluded category, matched across localizations. */
    fun isExcluded(slug: String?): Boolean {
        val tokens = slug.tokens()
        if (tokens.isEmpty()) return false
        if (tokens.joinToString("-") in EXCLUDED_SLUGS) return true
        return tokens.hasSoftwareToken() && tokens.hasAppToken()
    }

    /** True when any translation identifies the excluded category. */
    fun isExcluded(translations: List<CategoryTranslationRow>): Boolean =
        translations.any { translation ->
            isExcluded(translation.slug) || translation.name.hasSoftwareAndAppTokens()
        }

    private fun String.hasSoftwareAndAppTokens(): Boolean {
        val tokens = tokens()
        return tokens.hasSoftwareToken() && tokens.hasAppToken()
    }

    private fun List<String>.hasSoftwareToken(): Boolean = any { token ->
        token in SOFTWARE_TOKENS ||
            token.startsWith("logiciel") ||
            token.startsWith("programme") ||
            token.startsWith("программ")
    }

    private fun List<String>.hasAppToken(): Boolean = any { token ->
        token in APP_TOKENS ||
            token.startsWith("application") ||
            token.startsWith("приложени")
    }

    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    private val ARABIC_DIACRITICS = Regex("[\\u064B-\\u065F\\u0670]")

    private fun String?.tokens(): List<String> = this
        ?.lowercase()
        ?.replace(ARABIC_DIACRITICS, "")
        ?.replace('أ', 'ا')
        ?.replace('إ', 'ا')
        ?.replace('آ', 'ا')
        ?.replace('ى', 'ي')
        ?.replace('ة', 'ه')
        ?.split(SEPARATORS)
        ?.mapNotNull { it.normalizedToken() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    /**
     * Drops the Arabic conjunction "و" and definite article "ال" so
     * "البرامج والتطبيقات" normalizes to the tokens "برامج" and "تطبيقات".
     */
    private fun String.normalizedToken(): String? {
        var token = this
        if (token.length > 2 && token.first() == 'و') token = token.substring(1)
        if (token.length > 3 && token.startsWith("ال")) token = token.substring(2)
        return token.takeIf { it.isNotBlank() }
    }
}

/** Maps a raw category row to a locale-aware [Category]; mirrors the feed's translation fallback. */
fun CategoryRow.toCategory(languageCode: String): Category? {
    if (CategoryExclusions.isExcluded(translations)) return null
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