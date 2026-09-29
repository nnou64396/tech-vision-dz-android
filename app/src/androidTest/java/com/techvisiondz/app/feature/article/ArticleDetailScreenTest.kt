package com.techvisiondz.app.feature.article

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.data.model.ArticleTranslationRef
import com.techvisiondz.app.core.data.model.SoftwareSummary
import com.techvisiondz.app.core.data.model.VideoRef
import com.techvisiondz.app.core.ui.formatViewsCount
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic Compose UI tests for the article details screen using a fake
 * repository, proving the screen renders all meaningful UI states without
 * network access.
 */
class ArticleDetailScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successShowsTitleMetaAndBody() {
        val article = sampleArticle(
            title = "عنوان المقال الكامل",
            excerpt = "وصف مختصر للمقال.",
            body = "<p>محتوى المقال التفصيلي</p>",
            category = com.techvisiondz.app.core.data.model.CategorySummary(
                slug = "tech",
                name = "تقنية",
                description = null,
            ),
            author = com.techvisiondz.app.core.data.model.AuthorSummary(
                name = "محرر TECH VISION DZ",
                bio = null,
                avatarUrl = null,
            ),
            tags = listOf(com.techvisiondz.app.core.data.model.TagSummary(slug = "ai", label = "ذكاء اصطناعي")),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent { TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) } }
        composeRule.waitForIdle()

        // The title is rendered twice (top bar + content), the top bar one is enough below.
        composeRule.onAllNodesWithText("عنوان المقال الكامل")[0].assertIsDisplayed()
        composeRule.onNodeWithText("محتوى المقال التفصيلي").assertIsDisplayed()
        composeRule.onNodeWithText("محرر TECH VISION DZ · تقنية").assertIsDisplayed()
        composeRule.onNodeWithText("ذكاء اصطناعي").assertIsDisplayed()
    }

    @Test
    fun notFoundShowsEmptyMessage() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(),
            slug = "missing",
        )

        composeRule.setContent { TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_not_found)).assertIsDisplayed()
    }

    @Test
    fun errorShowsMessageAndRetry() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository(error = DataException.Network("Could not reach the server")),
            slug = "hello-world",
        )

        composeRule.setContent { TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not reach the server").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retry)).assertIsDisplayed()
    }

    @Test
    fun successShowsReadingTimeAndViews() {
        val article = sampleArticle()
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}, onShareArticle = {}) }
        }
        composeRule.waitForIdle()

        val minutesText = composeRule.activity.resources.getQuantityString(R.plurals.reading_time_minutes, 5, 5)
        val viewsText = composeRule.activity.getString(R.string.article_views, formatViewsCount(42L))
        composeRule.onNodeWithText("$minutesText · $viewsText").assertIsDisplayed()
    }

    @Test
    fun viewsStillShownWhenReadingTimeUnknown() {
        val article = sampleArticle().copy(readingTimeMinutes = null)
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}, onShareArticle = {}) }
        }
        composeRule.waitForIdle()

        val viewsText = composeRule.activity.getString(R.string.article_views, formatViewsCount(42L))
        composeRule.onNodeWithText(viewsText).assertIsDisplayed()
    }

    @Test
    fun shareActionIsAccessibleAndInvokesCallbackWithArticle() {
        var sharedArticle: Article? = null
        val article = sampleArticle(slug = "share-me", title = "مشاركة")
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onShareArticle = { sharedArticle = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.share_article))
            .assertIsDisplayed()
            .performClick()
        assertEquals(article, sharedArticle)
    }

    @Test
    fun videoCardShowsForSafeVideoUrl() {
        val article = sampleArticle(
            title = "Article with video",
            video = VideoRef(kind = "embed", url = "https://example.com/watch?v=123"),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_video)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_video_watch)).assertIsDisplayed()
    }

    @Test
    fun softwareCardShowsNameVersionAndDownloadButton() {
        val article = sampleArticle(
            title = "Article with software",
            body = null,
            software = SoftwareSummary(
                name = "تطبيق TECH VISION DZ",
                version = "2.4.0",
                downloadUrl = "https://example.com/app-release.apk",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_software, "تطبيق TECH VISION DZ"))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.software_version, "2.4.0"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag("software_download").assertIsDisplayed()
    }

    @Test
    fun softwareCardRendersNameWithoutVersionWhenVersionMissing() {
        val article = sampleArticle(
            title = "Article with software",
            body = null,
            software = SoftwareSummary(name = "نطاق بدون إصدار", version = null, downloadUrl = "https://example.com/a"),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_software, "نطاق بدون إصدار"))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.software_version, "x")).assertDoesNotExist()
    }

    @Test
    fun softwareDownloadClickInvokesHandlerWithValidatedUrl() {
        var openedUrl: String? = null
        val article = sampleArticle(
            title = "Article with software",
            body = null,
            software = SoftwareSummary(
                name = "App",
                version = null,
                downloadUrl = "   https://example.com/download/app-release.apk   ",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onDownloadClick = { openedUrl = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("software_download").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("https://example.com/download/app-release.apk", openedUrl)
    }

    @Test
    fun softwareCardShowsUnavailableWhenDownloadUrlMissing() {
        val article = sampleArticle(
            title = "Article without a link",
            body = null,
            software = SoftwareSummary(name = "App", version = "1.0.0", downloadUrl = null),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.software_unavailable))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("software_download").assertDoesNotExist()
    }

    @Test
    fun videoCardWatchActionInvokesHandlerWithValidatedUrl() {
        var openedUrl: String? = null
        val article = sampleArticle(
            title = "Article with video",
            body = null,
            video = VideoRef(kind = "embed", url = "   https://example.com/watch/abc   "),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onOpenVideo = { openedUrl = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.article_video_watch))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertEquals("https://example.com/watch/abc", openedUrl)
    }

    @Test
    fun softwareCardShowsUnavailableForUnsupportedScheme() {
        val article = sampleArticle(
            title = "Article with a bad link",
            body = null,
            software = SoftwareSummary(name = "App", version = null, downloadUrl = "javascript:alert(1)"),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.software_unavailable))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("software_download").assertDoesNotExist()
    }

    @Test
    fun softwareCardShowsUnavailableForBlankUrlWhitespace() {
        val article = sampleArticle(
            title = "Article with a whitespace link",
            body = null,
            software = SoftwareSummary(name = "App", version = null, downloadUrl = "   "),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.software_unavailable))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("software_download").assertDoesNotExist()
    }

    @Test
    fun softwareCardUsesFileUrlWhenOnlyStoragePresent() {
        var openedUrl: String? = null
        val article = sampleArticle(
            title = "Article with a storage download",
            body = null,
            software = SoftwareSummary(
                name = "Toolbar",
                version = "2.4",
                downloadUrl = null,
                fileUrl = "https://cdn.example/toolbar-v2.4.apk",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onDownloadClick = { openedUrl = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("software_download").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("https://cdn.example/toolbar-v2.4.apk", openedUrl)
        composeRule.onNodeWithTag("software_download_file").assertDoesNotExist()
    }

    @Test
    fun softwareCardShowsBothTargetsWhenExternalAndStoragePresent() {
        var openedUrl: String? = null
        val article = sampleArticle(
            title = "Article with two download targets",
            body = null,
            software = SoftwareSummary(
                name = "Toolbar",
                version = "2.4",
                downloadUrl = "https://releases.example/toolbar.apk",
                fileUrl = "https://cdn.example/toolbar-v2.4.apk",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onDownloadClick = { openedUrl = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("software_download").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("software_download_file").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("https://cdn.example/toolbar-v2.4.apk", openedUrl)
    }

    @Test
    fun softwareCardIgnoresUnsafeFileUrl() {
        val article = sampleArticle(
            title = "Article with an unsafe storage download",
            body = null,
            software = SoftwareSummary(
                name = "App",
                version = null,
                downloadUrl = "https://example.com/a",
                fileUrl = "javascript:alert(1)",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("software_download").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("software_download_file").assertDoesNotExist()
    }

    @Test
    fun tagPillClickInvokesHandlerWithSlug() {
        var clickedSlug: String? = null
        val article = sampleArticle(
            title = "Article with a clickable tag",
            tags = listOf(com.techvisiondz.app.core.data.model.TagSummary(slug = "ai", label = "ذكاء اصطناعي")),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme {
                ArticleDetailScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onTagClick = { clickedSlug = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("ذكاء اصطناعي").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("ai", clickedSlug)
    }

    @Test
    fun authorStripShowsBioAndAvatarWhenPresent() {
        val article = sampleArticle(
            title = "Article with an author bio and avatar",
            author = com.techvisiondz.app.core.data.model.AuthorSummary(
                name = "محرر TECH VISION DZ",
                bio = "صحفي تقني يغطي أخبار التكنولوجيا في الجزائر.",
                avatarUrl = "https://cdn.example/avatars/editor.png",
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("صحفي تقني يغطي أخبار التكنولوجيا في الجزائر.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("محرر TECH VISION DZ").assertExists()
    }

    @Test
    fun authorStripHiddenWithoutBioAndAvatar() {
        val article = sampleArticle(
            title = "Article without author extras",
            author = com.techvisiondz.app.core.data.model.AuthorSummary(
                name = "محرر TECH VISION DZ",
                bio = null,
                avatarUrl = null,
            ),
        )
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply { detailArticle = article },
            slug = article.slug,
        )

        composeRule.setContent {
            TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("محرر TECH VISION DZ").assertDoesNotExist()
    }

    // ---------------------------------------------------------------------
    // Language selector
    // ---------------------------------------------------------------------

    @Test
    fun selectorShowsEveryPublishedVersionForAFullyTranslatedArticle() {
        val viewModel = ArticleDetailViewModel(
            repository = multilingualRepository(),
            slug = SLUG_ARQ,
        )

        setContent(viewModel)

        composeRule.onNodeWithTag("article_language_selector").assertExists()
        listOf("arq", "ar", "fr", "en").forEach {
            composeRule.onNodeWithTag("article_language_$it").assertExists()
        }
        // Each version is labelled with its own name, flag included.
        composeRule.onNodeWithText(activity.getString(R.string.article_language_darija)).assertExists()
        composeRule.onNodeWithText(activity.getString(R.string.article_language_french)).assertExists()
    }

    @Test
    fun selectorShowsOnlyTheTwoVersionsThatExist() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply {
                detailArticle = versionIn("arq", SLUG_ARQ, "عنوان بالدارجة")
                articleTranslations = listOf(
                    ArticleTranslationRef("arq", SLUG_ARQ),
                    ArticleTranslationRef("ar", SLUG_AR),
                )
            },
            slug = SLUG_ARQ,
        )

        setContent(viewModel)

        composeRule.onNodeWithTag("article_language_arq").assertExists()
        composeRule.onNodeWithTag("article_language_ar").assertExists()
        composeRule.onNodeWithTag("article_language_fr").assertDoesNotExist()
        composeRule.onNodeWithTag("article_language_en").assertDoesNotExist()
    }

    @Test
    fun selectorIsAbsentForASingleLanguageArticle() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply {
                detailArticle = versionIn("arq", SLUG_ARQ, "مقال بالدارجة فقط")
                articleTranslations = listOf(ArticleTranslationRef("arq", SLUG_ARQ))
            },
            slug = SLUG_ARQ,
        )

        setContent(viewModel)

        composeRule.onNodeWithText("مقال بالدارجة فقط").assertIsDisplayed()
        composeRule.onNodeWithTag("article_language_selector").assertDoesNotExist()
        composeRule.onNodeWithTag("article_language_arq").assertDoesNotExist()
    }

    @Test
    fun selectorIsAbsentWhenTheArticleHasNoTranslationMetadata() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply {
                detailArticle = versionIn("arq", SLUG_ARQ, "مقال قديم")
                articleTranslations = emptyList()
            },
            slug = SLUG_ARQ,
        )

        setContent(viewModel)

        composeRule.onNodeWithText("مقال قديم").assertIsDisplayed()
        composeRule.onNodeWithTag("article_language_selector").assertDoesNotExist()
    }

    @Test
    fun tappingAVersionShowsThatVersionOfTheSameArticle() {
        val viewModel = ArticleDetailViewModel(
            repository = multilingualRepository(),
            slug = SLUG_ARQ,
        )
        setContent(viewModel)
        composeRule.onNodeWithText("MediaTek تكشف عن المعالج").assertExists()

        composeRule.onNodeWithTag("article_language_fr").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("MediaTek dévoile le processeur").assertExists()
        composeRule.onNodeWithText("MediaHex unveils the processor").assertDoesNotExist()
    }

    @Test
    fun anUnavailableVersionLeavesTheArticleOnScreen() {
        val viewModel = ArticleDetailViewModel(
            repository = FakeArticleRepository().apply {
                detailArticle = versionIn("arq", SLUG_ARQ, "مقال بالدارجة")
                articleTranslations = listOf(
                    ArticleTranslationRef("arq", SLUG_ARQ),
                    ArticleTranslationRef("fr", SLUG_FR),
                )
            },
            slug = SLUG_ARQ,
        )
        setContent(viewModel)

        composeRule.onNodeWithTag("article_language_fr").performClick()
        composeRule.waitForIdle()

        // The fake falls back to the Darija article, so nothing blanked out and
        // no "not found" state replaced the content.
        composeRule.onNodeWithText("مقال بالدارجة").assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.article_not_found)).assertDoesNotExist()
    }

    @Test
    fun theSelectorStaysUsableAfterSwitchingToAnLtrVersion() {
        val viewModel = ArticleDetailViewModel(
            repository = multilingualRepository(),
            slug = SLUG_ARQ,
        )
        setContent(viewModel)

        // The RTL -> LTR mapping itself is pinned in ArticleLocaleTest
        // (ArticleLocales.isRtl); Compose's test API cannot observe a composition
        // local, so this covers what is observable: the LTR version renders and
        // the control remains interactive, letting the reader switch back.
        composeRule.onNodeWithTag("article_language_en").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MediaHex unveils the processor").assertExists()

        composeRule.onNodeWithTag("article_language_arq").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MediaTek تكشف عن المعالج").assertExists()
    }

    private fun setContent(viewModel: ArticleDetailViewModel) {
        composeRule.setContent { TechVisionDzTheme { ArticleDetailScreen(viewModel = viewModel, onBack = {}) } }
        composeRule.waitForIdle()
    }

    private val activity: ComponentActivity get() = composeRule.activity

    /** One locale of an article published in all four languages. */
    private fun versionIn(language: String, slug: String, title: String) = sampleArticle(
        slug = slug,
        title = title,
        languageCode = language,
    ).copy(id = "article-mediatek")

    private fun multilingualRepository() = FakeArticleRepository().apply {
        articleTranslations = listOf(
            ArticleTranslationRef("arq", SLUG_ARQ),
            ArticleTranslationRef("ar", SLUG_AR),
            ArticleTranslationRef("fr", SLUG_FR),
            ArticleTranslationRef("en", SLUG_EN),
        )
        articlesBySlug = mapOf(
            SLUG_ARQ to versionIn("arq", SLUG_ARQ, "MediaTek تكشف عن المعالج"),
            SLUG_AR to versionIn("ar", SLUG_AR, "MediaTek تكشف عن المعالج رسميا"),
            SLUG_FR to versionIn("fr", SLUG_FR, "MediaTek dévoile le processeur"),
            SLUG_EN to versionIn("en", SLUG_EN, "MediaHex unveils the processor"),
        )
        detailArticle = versionIn("arq", SLUG_ARQ, "MediaTek تكشف عن المعالج")
    }

    private companion object {
        const val SLUG_ARQ = "mediatek-darija-slug"
        const val SLUG_AR = "mediatek-fusha-slug"
        const val SLUG_FR = "mediatek-french-slug"
        const val SLUG_EN = "mediatek-english-slug"
    }
}