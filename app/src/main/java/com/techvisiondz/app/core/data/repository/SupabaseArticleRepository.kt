package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.toDataException
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.ArticleCardRpcDoc
import com.techvisiondz.app.core.data.model.ArticleFeedRow
import com.techvisiondz.app.core.data.model.ArticleRpcDoc
import com.techvisiondz.app.core.data.model.Author
import com.techvisiondz.app.core.data.model.AuthorFeedRow
import com.techvisiondz.app.core.data.model.Category
import com.techvisiondz.app.core.data.model.CategoryRow
import com.techvisiondz.app.core.data.model.Tag
import com.techvisiondz.app.core.data.model.TagRow
import com.techvisiondz.app.core.data.model.toArticle
import com.techvisiondz.app.core.data.model.toArticleCard
import com.techvisiondz.app.core.data.model.toAuthor
import com.techvisiondz.app.core.data.model.toCategory
import com.techvisiondz.app.core.data.model.toTag
import com.techvisiondz.app.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase-backed [ArticleRepository] for the existing TECH VISION DZ project.
 *
 * - Feed: relational PostgREST query against `articles` (published only,
 *   `published_at` desc), exactly like the website's `getPublishedArticles`.
 * - Detail: `get_published_article_by_slug` RPC (migrations 0037/0039).
 *
 * All network/backend errors are converted to [com.techvisiondz.app.core.data.DataException].
 */
class SupabaseArticleRepository(
    private val postgrest: Postgrest = SupabaseClientProvider.postgrest,
    private val storage: Storage = SupabaseClientProvider.storage,
) : ArticleRepository {

    override suspend fun getHomeFeed(languageCode: String): List<ArticleCard> = runTranslated {
        postgrest.from("articles")
            .select(Columns.raw(HOME_FEED_SELECT)) {
                filter { eq("status", "published") }
                filter { eq("article_translations.language_code", languageCode) }
                order("published_at", Order.DESCENDING, nullsFirst = true)
                limit(HOME_FEED_LIMIT)
            }
            .decodeList<ArticleFeedRow>()
            .mapNotNull { it.toArticleCard(languageCode, ::resolvePublicUrl) }
    }

    override suspend fun getArticle(slug: String, languageCode: String): Article? = runTranslated {
        val result = postgrest.rpc(
            function = GET_PUBLISHED_ARTICLE_BY_SLUG,
            parameters = buildJsonObject {
                put("p_slug", slug)
                put("p_lang", languageCode)
            },
        )
        val doc = decodeArticleRpcBody(result.data) { result.decodeAs<ArticleRpcDoc>() }
            ?: return@runTranslated null
        doc.toArticle(languageCode, ::resolvePublicUrl)
    }

    override suspend fun getCategories(languageCode: String): List<Category> = runTranslated {
        postgrest.from("categories")
            .select(Columns.raw(CATEGORY_LIST_SELECT)) {
                filter { eq("is_active", true) }
                filter { eq("category_translations.language_code", languageCode) }
            }
            .decodeList<CategoryRow>()
            .mapNotNull { it.toCategory(languageCode) }
            .sortedBy { it.name }
    }

    override suspend fun getAuthors(languageCode: String): List<Author> = runTranslated {
        postgrest.from("authors")
            .select(Columns.raw(AUTHOR_LIST_SELECT)) {
                filter { eq("is_active", true) }
                filter { eq("author_translations.language_code", languageCode) }
            }
            .decodeList<AuthorFeedRow>()
            .mapNotNull { it.toAuthor(languageCode, ::resolvePublicUrl) }
            .sortedBy { it.name }
    }

    override suspend fun getTags(languageCode: String): List<Tag> = runTranslated {
        postgrest.from("tags")
            .select(Columns.raw(TAG_LIST_SELECT)) {
                filter { eq("tag_translations.language_code", languageCode) }
            }
            .decodeList<TagRow>()
            .mapNotNull { it.toTag(languageCode) }
            .sortedBy { it.name }
    }

    override suspend fun getArticlesByCategory(slug: String, languageCode: String): List<ArticleCard> =
        runTranslated { rpcArticleCards(GET_PUBLISHED_ARTICLE_CARDS_BY_CATEGORY, slug, languageCode) }

    override suspend fun getArticlesByAuthor(slug: String, languageCode: String): List<ArticleCard> =
        runTranslated { rpcArticleCards(GET_PUBLISHED_ARTICLE_CARDS_BY_AUTHOR, slug, languageCode) }

    override suspend fun getArticlesByTag(slug: String, languageCode: String): List<ArticleCard> =
        runTranslated { rpcArticleCards(GET_PUBLISHED_ARTICLE_CARDS_BY_TAG, slug, languageCode) }

    override suspend fun searchArticles(query: String, languageCode: String, limit: Int): List<ArticleCard> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        val pattern = "*$normalized*"
        return runTranslated {
            postgrest.from("articles")
                .select(Columns.raw(SEARCH_SELECT)) {
                    filter { eq("status", "published") }
                    filter { eq("article_translations.language_code", languageCode) }
                    filter {
                        or(referencedTable = "article_translations") {
                            ilike("title", pattern)
                            ilike("excerpt", pattern)
                        }
                    }
                    order("published_at", Order.DESCENDING, nullsFirst = true)
                    limit(limit.toLong())
                }
                .decodeList<ArticleFeedRow>()
                .mapNotNull { it.toArticleCard(languageCode, ::resolvePublicUrl) }
        }
    }

    private suspend fun rpcArticleCards(function: String, slug: String, languageCode: String): List<ArticleCard> {
        val result = postgrest.rpc(
            function = function,
            parameters = buildJsonObject {
                put("p_slug", slug)
                put("p_lang", languageCode)
            },
        )
        val cards = decodeRpcJsonBody(result.data) { result.decodeList<ArticleCardRpcDoc>() }
            ?: return emptyList()
        return cards.mapNotNull { it.toArticleCard(::resolvePublicUrl) }
    }

    private fun resolvePublicUrl(bucket: String, storagePath: String): String =
        storage.from(bucket).publicUrl(storagePath)

    private suspend fun <T> runTranslated(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDataException()
    }

    private companion object {
        const val HOME_FEED_LIMIT = 30L
        const val GET_PUBLISHED_ARTICLE_BY_SLUG = "get_published_article_by_slug"
        const val GET_PUBLISHED_ARTICLE_CARDS_BY_CATEGORY = "get_published_article_cards_by_category"
        const val GET_PUBLISHED_ARTICLE_CARDS_BY_AUTHOR = "get_published_article_cards_by_author"
        const val GET_PUBLISHED_ARTICLE_CARDS_BY_TAG = "get_published_article_cards_by_tag"

        const val CATEGORY_LIST_SELECT = """
            id,
            is_active,
            category_translations(
                language_code,
                name,
                slug,
                description
            )
        """

        const val AUTHOR_LIST_SELECT = """
            id,
            slug,
            avatar_url,
            is_active,
            author_translations(
                language_code,
                display_name,
                bio,
                slug
            )
        """

        const val TAG_LIST_SELECT = """
            id,
            slug,
            tag_translations(
                language_code,
                label
            )
        """

        // Mirrors the website's `articleSelect` const (src/lib/supabase/queries/articles.ts),
        // trimmed to the fields needed for feed cards.
        const val HOME_FEED_SELECT = """
            id,
            status,
            featured,
            published_at,
            reading_time_minutes,
            views_count,
            article_translations(
                language_code,
                title,
                slug,
                excerpt
            ),
            authors(
                id,
                slug,
                avatar_url,
                is_active,
                author_translations(
                    language_code,
                    display_name,
                    bio,
                    slug
                )
            ),
            categories(
                id,
                slug,
                is_active,
                category_translations(
                    language_code,
                    name,
                    slug,
                    description
                )
            ),
            article_media(
                role,
                sort_order,
                caption,
                media(
                    id,
                    type,
                    storage_path,
                    bucket,
                    alt_text
                )
            )
        """

        // Same shape as the home feed but `article_translations` is embedded with
        // `!inner` so PostgREST drops articles that have no matching translation,
        // and the title/excerpt ILIKE filter is applied via an `or(...)` logical
        // expression scoped to that embed.
        const val SEARCH_SELECT = """
            id,
            status,
            featured,
            published_at,
            reading_time_minutes,
            views_count,
            article_translations!inner(
                language_code,
                title,
                slug,
                excerpt
            ),
            authors(
                id,
                slug,
                avatar_url,
                is_active,
                author_translations(
                    language_code,
                    display_name,
                    bio,
                    slug
                )
            ),
            categories(
                id,
                slug,
                is_active,
                category_translations(
                    language_code,
                    name,
                    slug,
                    description
                )
            ),
            article_media(
                role,
                sort_order,
                caption,
                media(
                    id,
                    type,
                    storage_path,
                    bucket,
                    alt_text
                )
            )
        """
    }
}

/**
 * Maps a raw RPC response body to a value, or null when the body is the JSON
 * literal `null` (SQL NULL) or empty.
 *
 * PostgREST returns the JSON literal `null` for SQL NULL results, so such a
 * body must map to `null` instead of a malformed-document error. A valid body
 * decodes normally, and decode faults are NOT swallowed: they propagate so the
 * caller (the repository's `runTranslated`) can convert them to a
 * [DataException].
 *
 * `internal` and pure on purpose: the null/empty contract is unit-tested with
 * synthetic bodies, without any Supabase client or network access.
 */
internal fun <T> decodeRpcJsonBody(body: String, decode: () -> T): T? {
    val trimmed = body.trim()
    if (trimmed.isEmpty() || trimmed == "null") return null
    return decode()
}

/** Typed convenience wrapper kept for the article-detail contract and its tests. */
internal fun decodeArticleRpcBody(body: String, decodeDoc: () -> ArticleRpcDoc): ArticleRpcDoc? =
    decodeRpcJsonBody(body, decodeDoc)