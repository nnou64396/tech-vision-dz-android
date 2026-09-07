package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.AuthorSummary
import com.techvisiondz.app.core.data.model.CategorySummary
import com.techvisiondz.app.core.data.model.TagSummary
import com.techvisiondz.app.core.data.repository.ArticleRepository

/** Deterministic repository for instrumented UI tests. No network involved. */
class FakeArticleRepository(
    articles: List<ArticleCard> = emptyList(),
    error: Exception? = null,
) : ArticleRepository {

    var articles: List<ArticleCard> = articles

    /** Reassignable so tests can simulate failures on subsequent calls. */
    var error: Exception? = error

    var lastLanguageCode: String? = null
        private set

    var lastArticleSlug: String? = null
        private set

    /** Article returned by [getArticle]; null means "not found". */
    var detailArticle: Article? = null

    override suspend fun getHomeFeed(languageCode: String): List<ArticleCard> {
        lastLanguageCode = languageCode
        error?.let { throw it }
        return articles
    }

    override suspend fun getArticle(slug: String, languageCode: String): Article? {
        lastArticleSlug = slug
        lastLanguageCode = languageCode
        error?.let { throw it }
        return detailArticle
    }
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

fun sampleArticle(
    slug: String = "sample-article",
    title: String = "Sample article title",
    excerpt: String? = "A short excerpt for the sample article.",
    body: String? = "<p>Full article body.</p>",
    category: CategorySummary = CategorySummary(slug = "tech", name = "Tech", description = null),
    author: AuthorSummary = AuthorSummary(name = "TECH VISION DZ", bio = null, avatarUrl = null),
    tags: List<TagSummary> = listOf(TagSummary(slug = "ai", label = "AI")),
    coverUrl: String? = null,
) = Article(
    id = "article-$slug",
    slug = slug,
    title = title,
    excerpt = excerpt,
    body = body,
    featured = false,
    publishedAt = "2026-08-01T09:00:00Z",
    readingTimeMinutes = 5,
    viewsCount = 42L,
    category = category,
    author = author,
    tags = tags,
    coverUrl = coverUrl,
    coverAlt = null,
    video = null,
    software = null,
)