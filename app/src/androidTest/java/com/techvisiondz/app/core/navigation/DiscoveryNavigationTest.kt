package com.techvisiondz.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.sampleArticle
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.feature.home.sampleAuthor
import com.techvisiondz.app.feature.home.sampleCategory
import com.techvisiondz.app.feature.home.sampleTag
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Deterministic full-flow navigation tests for the discovery sections using a
 * fake [ArticleRepository]: Home → list → article list → ArticleDetail and back.
 */
class DiscoveryNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeToCategoryArticlesAndBack() {
        val repository = FakeArticleRepository(
            articles = emptyList(),
            categories = listOf(sampleCategory(id = "c1", slug = "alssha", name = "الصحة")),
            categoryArticles = mapOf("alssha" to listOf(sampleArticleCard(id = "a1", title = "مقال الفئة"))),
        )
        repository.detailArticle = sampleArticle(slug = "sample-a1", title = "تفاصيل الفئة")

        composeRule.setContent { TechVisionDzTheme { AppNavHost(repository = repository, authRepository = FakeAuthRepository.authenticated()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.categories)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("الصحة").assertIsDisplayed()

        composeRule.onNodeWithTag("category_item_alssha").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال الفئة").assertIsDisplayed()

        composeRule.onNodeWithTag("article_card_sample-a1").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("تفاصيل الفئة")[0].assertIsDisplayed()
        assertEquals("sample-a1", repository.lastArticleSlug)

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال الفئة").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("الصحة").assertIsDisplayed()
    }

    @Test
    fun homeToAuthorArticlesAndBack() {
        val repository = FakeArticleRepository(
            articles = emptyList(),
            authors = listOf(sampleAuthor(id = "au1", slug = "tech-vision-dz", name = "المحرر")),
            authorArticles = mapOf("tech-vision-dz" to listOf(sampleArticleCard(id = "a2", title = "مقال المؤلف"))),
        )

        composeRule.setContent { TechVisionDzTheme { AppNavHost(repository = repository, authRepository = FakeAuthRepository.authenticated()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.authors)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("المحرر").assertIsDisplayed()

        composeRule.onNodeWithTag("author_item_tech-vision-dz").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال المؤلف").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("المحرر").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.categories)).assertIsDisplayed()
    }

    @Test
    fun homeToTagArticlesAndBack() {
        val repository = FakeArticleRepository(
            articles = emptyList(),
            tags = listOf(sampleTag(id = "t1", slug = "ai", name = "الذكاء")),
            tagArticles = mapOf("ai" to listOf(sampleArticleCard(id = "a3", title = "مقال الوسم"))),
        )

        composeRule.setContent { TechVisionDzTheme { AppNavHost(repository = repository, authRepository = FakeAuthRepository.authenticated()) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.tags)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tag_item_ai").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مقال الوسم").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("الذكاء").assertIsDisplayed()
    }
}