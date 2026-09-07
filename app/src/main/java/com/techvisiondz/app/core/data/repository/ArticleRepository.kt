package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard

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
}