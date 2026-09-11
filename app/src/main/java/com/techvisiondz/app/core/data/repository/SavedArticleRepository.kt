package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.ArticleCard

/**
 * Bookmark access for the signed-in user's saved article list.
 *
 * Backed by the existing Supabase `public.saved_articles` table (migration
 * 0040): PK (user_id, article_id), RLS `saved_articles_insert/select/delete_own`
 * scoped to `auth.uid()` with no UPDATE policy. Every operation resolves the
 * current authenticated identity internally — arbitrary user ids are never
 * accepted.
 *
 * Implementations must not expose Supabase/SDK types to the UI and must map
 * transport/backend failures to [com.techvisiondz.app.core.data.DataException].
 */
interface SavedArticleRepository {

    /**
     * Loads the signed-in user's saved articles, newest bookmark first, mapped
     * into the shared [ArticleCard] shape (published articles only).
     */
    suspend fun getSavedArticles(languageCode: String): List<ArticleCard>

    /** True when the signed-in user has already bookmarked [articleId]. */
    suspend fun isArticleSaved(articleId: String): Boolean

    /** Bookmarks [articleId] for the signed-in user. Safe when already saved. */
    suspend fun saveArticle(articleId: String)

    /** Removes the bookmark for [articleId]. Safe when not saved. */
    suspend fun unsaveArticle(articleId: String)
}