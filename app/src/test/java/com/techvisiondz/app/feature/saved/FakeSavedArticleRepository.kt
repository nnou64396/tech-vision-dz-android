package com.techvisiondz.app.feature.saved

import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.SavedArticleRepository
import kotlinx.coroutines.delay

/**
 * Deterministic [SavedArticleRepository] for unit tests.
 *
 * No network or real Supabase session is involved. Tests drive the return
 * values through public fields and can verify call counts and arguments. Every
 * call suspends briefly so in-flight checks (isSaving etc.) observe the pending
 * state before the result resolves.
 */
class FakeSavedArticleRepository(
    savedArticles: List<ArticleCard> = emptyList(),
    private val isArticleSavedOverride: (String) -> Boolean? = { null },
) : SavedArticleRepository {

    /** Articles returned by [getSavedArticles]. */
    var savedArticles: List<ArticleCard> = savedArticles

    /** When set, all calls throw this instead of returning data. */
    var error: Exception? = null

    var getSavedCalls: Int = 0
        private set

    var isArticleSavedCalls: List<String> = emptyList()
        private set

    var saveArticleCalls: List<String> = emptyList()
        private set

    var unsaveArticleCalls: List<String> = emptyList()
        private set

    override suspend fun getSavedArticles(languageCode: String): List<ArticleCard> {
        delay(1)
        getSavedCalls++
        error?.let { throw it }
        return savedArticles
    }

    override suspend fun isArticleSaved(articleId: String): Boolean {
        delay(1)
        isArticleSavedCalls += articleId
        error?.let { throw it }
        return isArticleSavedOverride(articleId)
            ?: savedArticles.any { it.id == articleId }
    }

    override suspend fun saveArticle(articleId: String) {
        delay(1)
        saveArticleCalls += articleId
        error?.let { throw it }
    }

    override suspend fun unsaveArticle(articleId: String) {
        delay(1)
        unsaveArticleCalls += articleId
        error?.let { throw it }
    }
}
