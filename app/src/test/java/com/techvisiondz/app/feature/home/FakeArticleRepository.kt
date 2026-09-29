package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.data.model.ArticleTranslationRef
import com.techvisiondz.app.core.data.model.Author
import com.techvisiondz.app.core.data.model.AuthorSummary
import com.techvisiondz.app.core.data.model.Category
import com.techvisiondz.app.core.data.model.CategorySummary
import com.techvisiondz.app.core.data.model.DEFAULT_CONTENT_LANGUAGE
import com.techvisiondz.app.core.data.model.SoftwareSummary
import com.techvisiondz.app.core.data.model.Tag
import com.techvisiondz.app.core.data.model.TagSummary
import com.techvisiondz.app.core.data.repository.ArticleRepository
import kotlinx.coroutines.CompletableDeferred

/** Deterministic repository for unit / UI tests. No network involved. */
class FakeArticleRepository(
    articles: List<ArticleCard> = emptyList(),
    error: Exception? = null,
    categories: List<Category> = emptyList(),
    authors: List<Author> = emptyList(),
    tags: List<Tag> = emptyList(),
    categoryArticles: Map<String, List<ArticleCard>> = emptyMap(),
    authorArticles: Map<String, List<ArticleCard>> = emptyMap(),
    tagArticles: Map<String, List<ArticleCard>> = emptyMap(),
    searchResults: List<ArticleCard> = emptyList(),
    searchError: Exception? = null,
) : ArticleRepository {

    /** Reassignable so tests can simulate updated content on a subsequent load. */
    var articles: List<ArticleCard> = articles
        set(value) {
            field = value
        }

    /** Reassignable so tests can simulate failures on subsequent calls. */
    var error: Exception? = error

    var lastLanguageCode: String? = null
        private set

    var lastArticleSlug: String? = null
        private set

    var lastArticleDetailSlug: String? = null
        private set

    /**
     * Language passed to [getArticle] specifically. Tracked separately from
     * [lastLanguageCode] because related-article and catalog calls also write
     * that field, which would otherwise mask the detail request's locale.
     */
    var lastDetailLanguageCode: String? = null
        private set

    /** Article returned by [getArticle]; null means "not found". */
    var detailArticle: Article? = null

    /**
     * Slug-keyed articles consulted by [getArticle] before [detailArticle].
     * Lets a test model an article that resolves to different content per
     * locale, the way the backend's per-translation slugs behave.
     */
    var articlesBySlug: Map<String, Article> = emptyMap()

    /**
     * Language versions returned by [getArticleTranslations]. Empty is the
     * single-language case and is the default, so existing tests keep exercising
     * the "no selector" path.
     */
    var articleTranslations: List<ArticleTranslationRef> = emptyList()

    /** When set, [getArticleTranslations] throws it instead of returning rows. */
    var translationsError: Exception? = null

    var lastTranslationsArticleId: String? = null
        private set

    var categories: List<Category> = categories

    var authors: List<Author> = authors

    var tags: List<Tag> = tags

    var categoryArticles: Map<String, List<ArticleCard>> = categoryArticles

    var authorArticles: Map<String, List<ArticleCard>> = authorArticles

    var tagArticles: Map<String, List<ArticleCard>> = tagArticles

    /** Results returned by [searchArticles]; empty by default. */
    var searchResults: List<ArticleCard> = searchResults

    /** When set, [searchArticles] throws it instead of returning results. */
    var searchError: Exception? = searchError

    /** Last query handed to [searchArticles], after the ViewModel normalizes it. */
    var lastSearchQuery: String? = null
        private set

    var lastSearchLanguage: String? = null
        private set

    var searchCalls: Int = 0
        private set

    /** Number of [getHomeFeed] calls. */
    var feedCalls: Int = 0
        private set

    /** Last paging offsets/limits passed to each paged call. */
    var lastFeedOffset: Int = 0
        private set
    var lastFeedLimit: Int = 0
        private set
    var lastSearchOffset: Int = 0
        private set
    var lastSearchLimit: Int = 0
        private set

    /**
     * When set, [getHomeFeed] suspends until this gate is completed, letting
     * tests hold a request in flight (for load-more / refresh races).
     */
    var feedGate: CompletableDeferred<Unit>? = null

    /** Same as [feedGate] but for [searchArticles]. */
    var searchGate: CompletableDeferred<Unit>? = null

    override suspend fun getHomeFeed(languageCode: String, offset: Int, limit: Int): List<ArticleCard> {
        lastLanguageCode = languageCode
        lastFeedOffset = offset
        lastFeedLimit = limit
        feedCalls++
        feedGate?.await()
        error?.let { throw it }
        return articles.drop(offset).take(limit)
    }

    override suspend fun getArticle(slug: String, languageCode: String): Article? {
        lastArticleSlug = slug
        lastArticleDetailSlug = slug
        lastDetailLanguageCode = languageCode
        lastLanguageCode = languageCode
        error?.let { throw it }
        return articlesBySlug[slug] ?: detailArticle
    }

    override suspend fun getArticleTranslations(articleId: String): List<ArticleTranslationRef> {
        lastTranslationsArticleId = articleId
        translationsError?.let { throw it }
        return articleTranslations
    }

    override suspend fun getCategories(languageCode: String): List<Category> {
        lastLanguageCode = languageCode
        error?.let { throw it }
        return categories
    }

    override suspend fun getAuthors(languageCode: String): List<Author> {
        lastLanguageCode = languageCode
        error?.let { throw it }
        return authors
    }

    override suspend fun getTags(languageCode: String): List<Tag> {
        lastLanguageCode = languageCode
        error?.let { throw it }
        return tags
    }

    override suspend fun getArticlesByCategory(slug: String, languageCode: String): List<ArticleCard> {
        lastArticleSlug = slug
        lastLanguageCode = languageCode
        error?.let { throw it }
        return categoryArticles[slug] ?: emptyList()
    }

    override suspend fun getArticlesByAuthor(slug: String, languageCode: String): List<ArticleCard> {
        lastArticleSlug = slug
        lastLanguageCode = languageCode
        error?.let { throw it }
        return authorArticles[slug] ?: emptyList()
    }

    override suspend fun getArticlesByTag(slug: String, languageCode: String): List<ArticleCard> {
        lastArticleSlug = slug
        lastLanguageCode = languageCode
        error?.let { throw it }
        return tagArticles[slug] ?: emptyList()
    }

    override suspend fun searchArticles(query: String, languageCode: String, offset: Int, limit: Int): List<ArticleCard> {
        lastSearchQuery = query
        lastSearchLanguage = languageCode
        lastSearchOffset = offset
        lastSearchLimit = limit
        searchCalls++
        searchGate?.await()
        searchError?.let { throw it }
        return searchResults.drop(offset).take(limit)
    }
}

fun sampleCategory(
    id: String = "category-1",
    slug: String = "tech",
    name: String = "Tech",
    description: String? = "Technology news and reviews.",
) = Category(id = id, slug = slug, name = name, description = description)

fun sampleAuthor(
    id: String = "author-1",
    slug: String = "tech-vision-dz",
    name: String = "TECH VISION DZ",
    bio: String? = "Algerian technology portal.",
    avatarUrl: String? = null,
) = Author(id = id, slug = slug, name = name, bio = bio, avatarUrl = avatarUrl)

fun sampleTag(
    id: String = "tag-1",
    slug: String = "ai",
    name: String = "AI",
) = Tag(id = id, slug = slug, name = name)

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
    software: SoftwareSummary? = null,
    languageCode: String = DEFAULT_CONTENT_LANGUAGE,
    availableLanguages: List<ArticleTranslationRef> = emptyList(),
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
    software = software,
    languageCode = languageCode,
    availableLanguages = availableLanguages,
)