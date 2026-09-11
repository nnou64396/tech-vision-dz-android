package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.SavedArticleFeedRow
import com.techvisiondz.app.core.data.model.SavedArticleIdRow
import com.techvisiondz.app.core.data.model.SavedArticleInsert
import com.techvisiondz.app.core.data.model.toArticleCard
import com.techvisiondz.app.core.data.toDataException
import com.techvisiondz.app.core.network.SupabaseClientProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

/**
 * Supabase-backed [SavedArticleRepository] using the existing
 * `public.saved_articles` table (migration 0040) and its own-row RLS policies
 * (`saved_articles_insert/select/delete_own` — `user_id = auth.uid()`).
 *
 * Every request is scoped to the current session's auth user id; no user id is
 * ever accepted as input. Failures are converted to
 * [com.techvisiondz.app.core.data.DataException] for clean UI reporting, and no
 * Supabase SDK types leak past this layer.
 */
class SupabaseSavedArticleRepository(
    private val postgrest: Postgrest = SupabaseClientProvider.postgrest,
    private val auth: Auth = SupabaseClientProvider.auth,
    private val resolvePublicUrl: (bucket: String, storagePath: String) -> String =
        { bucket, path -> SupabaseClientProvider.publicMediaUrl(bucket, path) },
) : SavedArticleRepository {

    override suspend fun getSavedArticles(languageCode: String): List<ArticleCard> = runSafely {
        val userId = currentUserId()
        postgrest.from("saved_articles")
            .select(Columns.raw(SAVED_SELECT)) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING, nullsFirst = true)
            }
            .decodeList<SavedArticleFeedRow>()
            .mapNotNull { it.articles?.toArticleCard(languageCode, resolvePublicUrl) }
    }

    override suspend fun isArticleSaved(articleId: String): Boolean = runSafely {
        val userId = currentUserId()
        postgrest.from("saved_articles")
            .select(Columns.raw(SAVED_ID_COLUMNS)) {
                filter { eq("user_id", userId) }
                filter { eq("article_id", articleId) }
                limit(1)
            }
            .decodeList<SavedArticleIdRow>()
            .isNotEmpty()
    }

    override suspend fun saveArticle(articleId: String) {
        runSafely {
            val userId = currentUserId()
            postgrest.from("saved_articles").upsert(
                values = listOf(SavedArticleInsert(userId = userId, articleId = articleId)),
            ) {
                onConflict = "user_id,article_id"
                ignoreDuplicates = true
            }
        }
    }

    override suspend fun unsaveArticle(articleId: String) {
        runSafely {
            val userId = currentUserId()
            postgrest.from("saved_articles").delete {
                filter { eq("user_id", userId) }
                filter { eq("article_id", articleId) }
            }
        }
    }

    private fun currentUserId(): String =
        auth.currentUserOrNull()?.id
            ?: throw DataException.Authentication("Not signed in")

    private suspend fun <T> runSafely(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDataException()
    }

    private companion object {
        const val SAVED_ID_COLUMNS = "article_id"

        // Mirrors the home-feed article shape (see HOME_FEED_SELECT); the
        // bookmark row carries the article via the embedded `articles` link.
        const val SAVED_SELECT = """
            user_id,
            created_at,
            articles(
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
            )
        """
    }
}