package com.techvisiondz.app.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic mapping tests for the discovery DTOs
 * ([CatalogModels], [ArticleCardRpcDoc]).
 */
class CatalogModelsTest {

    @Test
    fun `category row maps with matching translation and translation slug`() {
        val row = CategoryRow(
            id = "c1",
            translations = listOf(
                CategoryTranslationRow(languageCode = "ar", name = "الصحة", slug = "alssha", description = "وصف"),
            ),
        )

        val category = row.toCategory("ar")

        assertEquals("c1", category?.id)
        assertEquals("alssha", category?.slug)
        assertEquals("الصحة", category?.name)
        assertEquals("وصف", category?.description)
    }

    @Test
    fun `category row falls back to first translation when language missing`() {
        val row = CategoryRow(
            id = "c1",
            translations = listOf(
                CategoryTranslationRow(languageCode = "ar", name = "الصحة", slug = "alssha"),
            ),
        )

        assertEquals("alssha", row.toCategory("fr")?.slug)
    }

    @Test
    fun `category row with no usable translation maps to null`() {
        assertEquals(null, CategoryRow(id = "c1", translations = emptyList()).toCategory("ar"))
        assertEquals(
            null,
            CategoryRow(
                id = "c1",
                translations = listOf(CategoryTranslationRow(languageCode = "ar", name = "", slug = "x")),
            ).toCategory("ar"),
        )
    }

    @Test
    fun `software and apps category is excluded while other categories remain available`() {
        val softwareRow = CategoryRow(
            id = "c_soft",
            translations = listOf(
                CategoryTranslationRow(languageCode = "en", name = "Software & Apps", slug = "software-applications"),
            ),
        )
        val techRow = CategoryRow(
            id = "c_tech",
            translations = listOf(
                CategoryTranslationRow(languageCode = "en", name = "Technology", slug = "tech"),
            ),
        )

        assertNull(softwareRow.toCategory("en"))
        assertEquals("tech", techRow.toCategory("en")?.slug)
    }

    @Test
    fun `apps slug is excluded for the default darija locale`() {
        val row = CategoryRow(
            id = "c_apps",
            translations = listOf(
                CategoryTranslationRow(
                    languageCode = "arq",
                    name = "البرامج والتطبيقات",
                    slug = "apps",
                ),
            ),
        )

        assertNull(row.toCategory("arq"))
    }

    @Test
    fun `localized slug that differs is still excluded by its localized name`() {
        val row = CategoryRow(
            id = "c_apps",
            translations = listOf(
                CategoryTranslationRow(
                    languageCode = "arq",
                    name = "البرامج والتطبيقات",
                    slug = "albaramij-waltatbiqat",
                ),
            ),
        )

        assertNull(row.toCategory("arq"))
    }

    @Test
    fun `french and russian localized names are excluded`() {
        val french = CategoryRow(
            id = "c_fr",
            translations = listOf(
                CategoryTranslationRow(
                    languageCode = "fr",
                    name = "Logiciels et Applications",
                    slug = "logiciels-et-applications",
                ),
            ),
        )
        val russian = CategoryRow(
            id = "c_ru",
            translations = listOf(
                CategoryTranslationRow(
                    languageCode = "ru",
                    name = "Программное обеспечение и приложения",
                    slug = "programmnoe-obespechenie-i-prilozheniya",
                ),
            ),
        )

        assertNull(french.toCategory("fr"))
        assertNull(russian.toCategory("ru"))
    }

    @Test
    fun `exclusion uses every translation not just the requested one`() {
        val row = CategoryRow(
            id = "c_apps",
            translations = listOf(
                CategoryTranslationRow(
                    languageCode = "fr",
                    name = "Applications mobiles",
                    slug = "applications-mobiles",
                ),
                CategoryTranslationRow(languageCode = "arq", name = "البرامج والتطبيقات", slug = "apps"),
            ),
        )

        assertNull(row.toCategory("fr"))
    }

    @Test
    fun `unrelated categories remain available in every language`() {
        val rows = listOf(
            CategoryRow(id = "c1", translations = listOf(CategoryTranslationRow("arq", "الصحة", "alssha"))),
            CategoryRow(id = "c2", translations = listOf(CategoryTranslationRow("fr", "Technologie", "technologie"))),
            CategoryRow(id = "c3", translations = listOf(CategoryTranslationRow("ru", "Новости", "novosti"))),
            CategoryRow(id = "c4", translations = listOf(CategoryTranslationRow("en", "Science", "science"))),
        )

        assertEquals("الصحة", rows[0].toCategory("arq")?.name)
        assertEquals("Technologie", rows[1].toCategory("fr")?.name)
        assertEquals("Новости", rows[2].toCategory("ru")?.name)
        assertEquals("Science", rows[3].toCategory("en")?.name)
    }

    @Test
    fun `excluded slug matching ignores case separators and padding`() {
        assertTrue(CategoryExclusions.isExcluded(" APPS "))
        assertTrue(CategoryExclusions.isExcluded("Software & Apps"))
        assertTrue(CategoryExclusions.isExcluded("software_apps"))
        assertTrue(CategoryExclusions.isExcluded("البرامج والتطبيقات"))
        assertFalse(CategoryExclusions.isExcluded("technology"))
        assertFalse(CategoryExclusions.isExcluded("الصحة"))
        assertFalse(CategoryExclusions.isExcluded(null))
    }

    @Test
    fun `author row maps name slug bio and resolved avatar`() {
        val row = AuthorFeedRow(
            id = "a1",
            slug = "tech-vision-dz",
            avatarUrl = "a.png",
            authorTranslations = listOf(
                AuthorTranslationRow(languageCode = "ar", displayName = "تيك فيجن", bio = "بوابة تقنية", slug = null),
            ),
        )

        val author = row.toAuthor("ar") { bucket, path -> "https://cdn.example/$bucket/$path" }

        assertEquals("a1", author?.id)
        assertEquals("tech-vision-dz", author?.slug)
        assertEquals("تيك فيجن", author?.name)
        assertEquals("بوابة تقنية", author?.bio)
        assertEquals("https://cdn.example/avatars/a.png", author?.avatarUrl)
    }

    @Test
    fun `author row without avatar keeps avatarUrl null`() {
        val row = AuthorFeedRow(
            id = "a1",
            slug = "tech-vision-dz",
            avatarUrl = null,
            authorTranslations = listOf(AuthorTranslationRow(languageCode = "ar", displayName = "تيك فيجن")),
        )

        assertNull(row.toAuthor("ar") { _, _ -> "unused" }?.avatarUrl)
    }

    @Test
    fun `tag row maps matching label`() {
        val row = TagRow(
            id = "t1",
            slug = "ai",
            translations = listOf(TagTranslationRow(languageCode = "ar", label = "ذكاء اصطناعي")),
        )

        val tag = row.toTag("ar")

        assertEquals("t1", tag?.id)
        assertEquals("ai", tag?.slug)
        assertEquals("ذكاء اصطناعي", tag?.name)
    }

    @Test
    fun `tag row with blank label maps to null`() {
        val row = TagRow(
            id = "t1",
            slug = "ai",
            translations = listOf(TagTranslationRow(languageCode = "ar", label = "")),
        )

        assertNull(row.toTag("ar"))
    }

    @Test
    fun `rpc card doc maps to article card resolving cover`() {
        val doc = ArticleCardRpcDoc(
            id = "article-1",
            slug = "sample-a1",
            title = "عنوان",
            excerpt = "مقتطف",
            featured = true,
            viewsCount = 10L,
            publishedAt = "2026-08-01T09:00:00Z",
            readingTimeMinutes = 5,
            author = ArticleCardRpcAuthor(name = "مؤلف"),
            category = ArticleCardRpcCategory(name = "تقنية"),
            cover = ArticleCardRpcCover(bucket = "media", storagePath = "covers/x.png"),
        )

        val card = doc.toArticleCard { bucket, path -> "https://cdn.example/$bucket/$path" }

        assertEquals("article-1", card?.id)
        assertEquals("sample-a1", card?.slug)
        assertEquals("عنوان", card?.title)
        assertEquals("مؤلف", card?.authorName)
        assertEquals("تقنية", card?.categoryName)
        assertEquals("https://cdn.example/media/covers/x.png", card?.coverUrl)
    }

    @Test
    fun `rpc card doc without slug or title maps to null`() {
        assertNull(ArticleCardRpcDoc(id = "x").toArticleCard { _, _ -> "" })
        assertNull(
            ArticleCardRpcDoc(id = "x", slug = "s", title = "").toArticleCard { _, _ -> "" },
        )
    }
}