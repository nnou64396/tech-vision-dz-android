package com.techvisiondz.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.techvisiondz.app.feature.home.FakeArticleRepository
import com.techvisiondz.app.feature.home.HomeScreen
import com.techvisiondz.app.feature.home.HomeViewModel
import com.techvisiondz.app.feature.home.sampleArticleCard
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appPackageIsCorrect() {
        assertEquals("com.techvisiondz.app", composeRule.activity.packageName)
    }

    @Test
    fun homeScreenRendersPublishedArticleFeedWithoutNetwork() {
        // Deterministic rendering check: a fake repository injects fixed data so
        // the home feed is verified WITHOUT any network, Supabase, or timing
        // dependency (the previous version hit the real production backend).
        val viewModel = HomeViewModel(
            FakeArticleRepository(
                articles = listOf(sampleArticleCard(title = "أول تقنية جزائرية على الصفحة الرئيسية")),
            ),
        )

        composeRule.setContent { TechVisionDzTheme { HomeScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("أول تقنية جزائرية على الصفحة الرئيسية").assertIsDisplayed()
    }
}