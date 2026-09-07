package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.repository.ArticleRepository

/** Deterministic repository for unit / UI tests. No network involved. */
class FakeArticleRepository(
    articles: List<ArticleCard> = emptyList(),
    private val error: Exception? = null,
) : ArticleRepository {

    /** Reassignable so tests can simulate updated content on a subsequent load. */
    var articles: List<ArticleCard> = articles
        set(value) {
            field = value
        }

    var lastLanguageCode: String? = null
        private set

    override suspend fun getHomeFeed(languageCode: String): List<ArticleCard> {
        lastLanguageCode = languageCode
        error?.let { throw it }
        return articles
    }

    override suspend fun getArticle(slug: String, languageCode: String): Article? = null
}

fun sampleArticleCard(
    id: String = "article-1",
    title: String = "Sample article title",
) = ArticleCard(
    id = id,
    slug = "sample-$id",
    title = title,
    excerpt = "A short excerpt for the sample article.",
    featured = false,
    publishedAt = "2026-08-01T09:00:00Z",
    readingTimeMinutes = 5,
    viewsCount = 42L,
    categoryName = "Tech",
    authorName = "TECH VISION DZ",
    coverUrl = null,
)