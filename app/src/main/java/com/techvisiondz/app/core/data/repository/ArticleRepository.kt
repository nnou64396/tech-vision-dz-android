package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.Author
import com.techvisiondz.app.core.data.model.Category
import com.techvisiondz.app.core.data.model.Tag

/**
 * Read-only access to the published TECH VISION DZ article feed stored in the
 * existing Supabase project. Implementations must never expose credentials and
 * must map transport/backend failures for clean UI reporting.
 */
interface ArticleRepository {

    /**
     * Loads a page of the published home feed. Exactly mirrors the site's feed:
     * only `status = published` articles, ordered by `published_at` descending,
     * with translations resolved for [languageCode] (falling back per article).
     *
     * Pagination is offset-based ([offset] rows skipped, at most [limit] rows
     * returned). A page smaller than [limit] means no further pages exist.
     */
    suspend fun getHomeFeed(
        languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE,
        offset: Int = 0,
        limit: Int = 30,
    ): List<ArticleCard>

    /**
     * Loads a single published article by slug via the backend RPC. Returns null
     * when no published article with that slug + language exists.
     */
    suspend fun getArticle(slug: String, languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): Article?

    /** Loads the discoverable categories for [languageCode]. */
    suspend fun getCategories(languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<Category>

    /** Loads the active authors for [languageCode]. */
    suspend fun getAuthors(languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<Author>

    /** Loads the tags for [languageCode]. */
    suspend fun getTags(languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<Tag>

    /** Loads published article cards for a category slug via the backend RPC. */
    suspend fun getArticlesByCategory(slug: String, languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<ArticleCard>

    /** Loads published article cards for an author slug via the backend RPC. */
    suspend fun getArticlesByAuthor(slug: String, languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<ArticleCard>

    /** Loads published article cards for a tag slug via the backend RPC. */
    suspend fun getArticlesByTag(slug: String, languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE): List<ArticleCard>

    /**
     * Searches a page of published article cards whose [languageCode]
     * translation title or excerpt contains [query] (case-insensitive ILIKE),
     * newest first, skipping [offset] rows and returning at most [limit]
     * results. A page smaller than [limit] means no further pages exist.
     * Implementations trim [query]; a blank query yields an empty list without
     * any request.
     */
    suspend fun searchArticles(
        query: String,
        languageCode: String = AppConfig.DEFAULT_LANGUAGE_CODE,
        offset: Int = 0,
        limit: Int = 30,
    ): List<ArticleCard>
}

/**
 * Page size used by the discovery screens (home feed and search) for
 * incremental "load more" browsing. Kept deliberately small so the app never
 * fetches the whole catalog at once.
 */
const val ARTICLE_PAGE_SIZE = 20