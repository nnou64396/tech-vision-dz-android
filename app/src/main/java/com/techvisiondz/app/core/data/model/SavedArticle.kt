package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw PostgREST row shape for the saved-articles listing query.
 *
 * The query starts from `public.saved_articles` (migration 0040) and embeds the
 * related `articles` row using the exact feed shape, so each bookmark decodes
 * into an [ArticleFeedRow] that maps 1:1 to an [ArticleCard] via the existing
 * [ArticleFeedRow.toArticleCard] mapper.
 */
@Serializable
data class SavedArticleFeedRow(
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val articles: ArticleFeedRow? = null,
)

/**
 * Minimal row shape used to ask "does this bookmark already exist?" without
 * loading any article content.
 */
@Serializable
data class SavedArticleIdRow(
    @SerialName("article_id") val articleId: String,
)

/**
 * Body of a bookmark INSERT/UPSERT into `public.saved_articles`. Both columns
 * form the table's composite primary key, so the same (user, article) pair can
 * never be stored twice.
 */
@Serializable
data class SavedArticleInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("article_id") val articleId: String,
)