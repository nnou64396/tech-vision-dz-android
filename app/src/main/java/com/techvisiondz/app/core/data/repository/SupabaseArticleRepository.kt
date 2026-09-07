package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.toDataException
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.ArticleFeedRow
import com.techvisiondz.app.core.data.model.ArticleRpcDoc
import com.techvisiondz.app.core.data.model.toArticle
import com.techvisiondz.app.core.data.model.toArticleCard
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
    }
}

/**
 * Maps the raw `get_published_article_by_slug` RPC response body to an
 * [ArticleRpcDoc].
 *
 * PostgREST returns the JSON literal `null` (SQL NULL) when no published
 * article matches the requested slug + language, so that body must map to
 * `null` instead of a malformed-document error. A valid body decodes normally,
 * and decode faults are NOT swallowed: they propagate so the caller (the
 * repository's `runTranslated`) can convert them to a [DataException].
 *
 * `internal` and pure on purpose: the null/empty contract is unit-tested with
 * synthetic bodies, without any Supabase client or network access.
 */
internal fun decodeArticleRpcBody(body: String, decodeDoc: () -> ArticleRpcDoc): ArticleRpcDoc? {
    val trimmed = body.trim()
    if (trimmed.isEmpty() || trimmed == "null") return null
    return decodeDoc()
}