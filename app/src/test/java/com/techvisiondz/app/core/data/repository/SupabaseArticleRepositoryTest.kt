package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.ArticleRpcDoc
import com.techvisiondz.app.core.data.model.toArticle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Deterministic tests for the `getArticle` null/empty RPC contract of
 * [SupabaseArticleRepository].
 *
 * The production `get_published_article_by_slug` RPC returns the JSON literal
 * `null` (SQL NULL) for an unknown slug + language. These tests feed synthetic
 * response bodies to the internal decode helper only — no Supabase client, no
 * network, no timing dependencies.
 */
class SupabaseArticleRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(body: String, decoded: () -> ArticleRpcDoc): ArticleRpcDoc? =
        decodeArticleRpcBody(body, decoded)

    @Test
    fun `null rpc body maps to null`() {
        assertNull(
            decode("null") { throw AssertionError("decode must not run for a null body") },
        )
    }

    @Test
    fun `empty rpc body maps to null`() {
        assertNull(
            decode("") { throw AssertionError("decode must not run for an empty body") },
        )
    }

    @Test
    fun `whitespace-padded null body maps to null`() {
        assertNull(
            decode("  null  ") { throw AssertionError("decode must not run for a null body") },
        )
    }

    @Test
    fun `valid rpc body decodes and maps to article`() {
        val body = VALID_RPC_BODY

        val doc = decode(body) { json.decodeFromString(ArticleRpcDoc.serializer(), body) }
        assertNotNull(doc)

        val article = doc!!.toArticle("ar") { _, path -> "https://cdn.example/object/$path" }
        assertNotNull(article)

        assertEquals("example-slug", article!!.slug)
        assertEquals("عنوان المقال", article.title)
        assertEquals("مؤلف", article.author?.name)
        assertEquals("https://cdn.example/object/avatars/a.png", article.author?.avatarUrl)
        assertEquals("https://cdn.example/object/media/c.png", article.coverUrl)
    }

    @Test
    fun `storage download object maps to software file url`() {
        val body = withDownloadBody("uploads/toolbar-v2.4.apk", "uploads")

        val doc = decode(body) { json.decodeFromString(ArticleRpcDoc.serializer(), body) }
        assertNotNull(doc)

        val article = doc!!.toArticle("ar") { _, path -> "https://cdn.example/object/$path" }
        assertNotNull(article)

        val software = checkNotNull(article!!.software)
        assertEquals("شريط أدوات", software.name)
        assertEquals("2.4", software.version)
        assertEquals("https://releases.example/toolbar-v2.4.apk", software.downloadUrl)
        assertEquals("https://cdn.example/object/uploads/toolbar-v2.4.apk", software.fileUrl)
    }

    @Test
    fun `storage download without bucket or path maps to null file url`() {
        val body = withDownloadBody(null, null)

        val doc = decode(body) { json.decodeFromString(ArticleRpcDoc.serializer(), body) }
        assertNotNull(doc)

        val article = doc!!.toArticle("ar") { _, path -> "https://cdn.example/object/$path" }
        assertNotNull(article)

        val software = checkNotNull(article!!.software)
        assertNull(software.fileUrl)
    }

    @Test
    fun `malformed rpc body is not swallowed`() {
        val body = "{not valid json"

        assertThrows(SerializationException::class.java) {
            decode(body) { json.decodeFromString(ArticleRpcDoc.serializer(), body) }
        }
    }

    private companion object {
        // Extra keys (author_id, category_id, seo_title, author.slug, tag.id)
        // mirror what the live RPC returns; ignoreUnknownKeys=true is how the
        // supabase-kt default serializer decodes them.
        const val VALID_RPC_BODY = """
            {
                "id": "article-1",
                "status": "published",
                "featured": true,
                "published_at": "2026-08-01T09:00:00Z",
                "reading_time_minutes": 5,
                "views_count": 123,
                "author_id": "author-1",
                "category_id": "category-1",
                "translation": {
                    "language_code": "ar",
                    "title": "عنوان المقال",
                    "slug": "example-slug",
                    "excerpt": "مقتطف قصير",
                    "body": "<p>body</p>",
                    "seo_title": "SEO title"
                },
                "author": {
                    "id": "author-1",
                    "slug": "author-slug",
                    "avatar_url": "avatars/a.png",
                    "name": "مؤلف",
                    "bio": null
                },
                "category": {
                    "id": "category-1",
                    "slug": "tech",
                    "name": "تقنية",
                    "description": null
                },
                "tags": [
                    { "id": "t1", "slug": "ai", "label": "AI" }
                ],
                "cover": {
                    "storage_path": "media/c.png",
                    "bucket": "media",
                    "alt": "cover alt"
                },
                "video": null,
                "software": null,
                "download": null
            }
        """

        /**
         * The valid RPC body with a software callout plus a storage-backed
         * `download` block describing the actual artifact.
         */
        private fun withDownloadBody(storagePath: String?, bucket: String?): String = """
            {
                "id": "article-1",
                "status": "published",
                "featured": true,
                "published_at": "2026-08-01T09:00:00Z",
                "reading_time_minutes": 5,
                "views_count": 123,
                "author_id": "author-1",
                "category_id": "category-1",
                "translation": {
                    "language_code": "ar",
                    "title": "عنوان المقال",
                    "slug": "example-slug",
                    "excerpt": "مقتطف قصير",
                    "body": "<p>body</p>"
                },
                "author": { "name": "مؤلف", "bio": null },
                "category": { "slug": "tech", "name": "تقنية" },
                "tags": [],
                "cover": null,
                "video": null,
                "software": {
                    "name": "شريط أدوات",
                    "version": "2.4",
                    "download_url": "https://releases.example/toolbar-v2.4.apk"
                },
                "download": {
                    "original_name": "toolbar-v2.4.apk",
                    "storage_path": ${jsonValue(storagePath)},
                    "bucket": ${jsonValue(bucket)}
                }
            }
        """.trimIndent()

        private fun jsonValue(value: String?): String =
            value?.let { "\"$it\"" } ?: "null"
    }
}