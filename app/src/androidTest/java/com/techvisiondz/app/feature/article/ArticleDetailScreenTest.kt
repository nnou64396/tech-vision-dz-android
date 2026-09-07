package com.techvisiondz.app.feature.article

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
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
}