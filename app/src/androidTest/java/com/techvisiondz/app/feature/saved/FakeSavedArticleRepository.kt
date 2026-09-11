package com.techvisiondz.app.feature.saved

import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.SavedArticleRepository
import kotlinx.coroutines.delay

/**
 * Deterministic [SavedArticleRepository] for Compose navigation tests. Starts
 * with the provided [savedArticles] list and reacts immediately to public-field
 * changes between test steps.
 */
class FakeSavedArticleRepository(
    savedArticles: List<ArticleCard> = emptyList(),
) : SavedArticleRepository {

    var savedArticles: List<ArticleCard> = savedArticles
    var error: Exception? = null

    override suspend fun getSavedArticles(languageCode: String): List<ArticleCard> {
        delay(1)
        error?.let { throw it }
        return savedArticles
    }

    override suspend fun isArticleSaved(articleId: String): Boolean {
        delay(1)
        error?.let { throw it }
        return savedArticles.any { it.id == articleId }
    }

    override suspend fun saveArticle(articleId: String) {
        delay(1)
        error?.let { throw it }
    }

    override suspend fun unsaveArticle(articleId: String) {
        delay(1)
        error?.let { throw it }
    }
}
