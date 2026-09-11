package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.SavedArticleFeedRow
import com.techvisiondz.app.core.data.model.toArticleCard
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic decode tests for the saved-articles listing query shape
 * returned by Supabase. No network or real Supabase client is involved.
 *
 * The production query uses `Columns.raw(SAVED_SELECT)` starting from
 * `public.saved_articles` (migration 0040), embedding the full feed-shape
 * `articles(...)` object via PostgREST. These tests validate the JSON contract:
 * a bookmark row carries `user_id`, `created_at`, and an embedded `articles`
 * object that decodes into [SavedArticleFeedRow] exactly as the home-feed query
 * decodes [com.techvisiondz.app.core.data.model.ArticleFeedRow].
 */
class SupabaseSavedArticleRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a complete bookmark row with an embedded article decodes correctly`() {
        val rows = json.decodeFromString<List<SavedArticleFeedRow>>(COMPLETE_ROW)

        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals("user-1", row.userId)
        assertEquals("2026-09-10T12:00:00Z", row.createdAt)

        val article = row.articles
        assertNotNull(article)
        assertEquals("article-1", article!!.id)
        assertEquals("published", article.status)

        val card = article.toArticleCard("ar") { _, _ -> "https://cdn.example/mock.png" }
        assertNotNull(card)
        assertEquals("article-1", card!!.id)
        assertEquals("article-slug", card.slug)
        assertEquals("عنوان المقال", card.title)
        assertEquals("مقتطف قصير", card.excerpt)
        assertEquals("مؤلف", card.authorName)
        assertEquals("تقنية", card.categoryName)
        assertEquals("https://cdn.example/mock.png", card.coverUrl)
    }

    @Test
    fun `empty bookmark list decodes to an empty list`() {
        val rows = json.decodeFromString<List<SavedArticleFeedRow>>("[]")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `missing articles nested object decodes gracefully`() {
        val rows = json.decodeFromString<List<SavedArticleFeedRow>>(NO_ARTICLES_ROW)

        assertEquals(1, rows.size)
        assertEquals("user-1", rows.first().userId)
        // The `articles` key is null/missing; the mapper returns null.
        val card = rows.first().articles?.toArticleCard("ar") { _, _ -> "" }
        assertTrue(card == null)
    }

    private companion object {
        private const val COMPLETE_ROW = """
            [
                {
                    "user_id": "user-1",
                    "created_at": "2026-09-10T12:00:00Z",
                    "articles": {
                        "id": "article-1",
                        "status": "published",
                        "featured": false,
                        "published_at": "2026-09-10T09:00:00Z",
                        "reading_time_minutes": 5,
                        "views_count": 100,
                        "article_translations": [
                            {
                                "language_code": "ar",
                                "title": "عنوان المقال",
                                "slug": "article-slug",
                                "excerpt": "مقتطف قصير"
                            }
                        ],
                        "authors": {
                            "id": "author-1",
                            "slug": "author-slug",
                            "avatar_url": "avatars/a.png",
                            "is_active": true,
                            "author_translations": [
                                {
                                    "language_code": "ar",
                                    "display_name": "مؤلف",
                                    "bio": "سيرة ذاتية",
                                    "slug": "author-slug"
                                }
                            ]
                        },
                        "categories": {
                            "id": "category-1",
                            "slug": "tech",
                            "is_active": true,
                            "category_translations": [
                                {
                                    "language_code": "ar",
                                    "name": "تقنية",
                                    "slug": "tech",
                                    "description": "أخبار التكنولوجيا"
                                }
                            ]
                        },
                        "article_media": [
                            {
                                "role": "cover",
                                "sort_order": 1,
                                "caption": "Cover",
                                "media": {
                                    "id": "media-1",
                                    "type": "image",
                                    "storage_path": "media/covers/cover.png",
                                    "bucket": "media",
                                    "alt_text": "cover alt"
                                }
                            }
                        ]
                    }
                }
            ]
        """

        private const val NO_ARTICLES_ROW = """
            [
                {
                    "user_id": "user-1",
                    "created_at": "2026-09-10T12:00:00Z"
                }
            ]
        """
    }
}