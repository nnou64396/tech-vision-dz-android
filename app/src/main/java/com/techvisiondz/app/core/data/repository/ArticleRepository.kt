package com.techvisiondz.app.core.data.repository

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
     * Loads the published home feed. Exactly mirrors the site's feed: only
     * `status = published` articles, ordered by `published_at` descending, with
     * translations resolved for [languageCode] (falling back per article).
     */
    suspend fun getHomeFeed(languageCode: String = "ar"): List<ArticleCard>

    /**
     * Loads a single published article by slug via the backend RPC. Returns null
     * when no published article with that slug + language exists.
     */
    suspend fun getArticle(slug: String, languageCode: String = "ar"): Article?

    /** Loads the discoverable categories for [languageCode]. */
    suspend fun getCategories(languageCode: String = "ar"): List<Category>

    /** Loads the active authors for [languageCode]. */
    suspend fun getAuthors(languageCode: String = "ar"): List<Author>

    /** Loads the tags for [languageCode]. */
    suspend fun getTags(languageCode: String = "ar"): List<Tag>

    /** Loads published article cards for a category slug via the backend RPC. */
    suspend fun getArticlesByCategory(slug: String, languageCode: String = "ar"): List<ArticleCard>

    /** Loads published article cards for an author slug via the backend RPC. */
    suspend fun getArticlesByAuthor(slug: String, languageCode: String = "ar"): List<ArticleCard>

    /** Loads published article cards for a tag slug via the backend RPC. */
    suspend fun getArticlesByTag(slug: String, languageCode: String = "ar"): List<ArticleCard>
}