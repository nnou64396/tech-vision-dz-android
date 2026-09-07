package com.techvisiondz.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appPackageIsCorrect() {
        val context = composeRule.activity
        assertEquals("com.techvisiondz.app", context.packageName)
    }

    @Test
    fun homeScreenShowsEmptyStateWhileNoContentExists() {
        // The home screen renders the empty state until the Supabase-backed
        // content source is connected (default locale strings used in tests).
        composeRule.onNodeWithText("No articles yet").assertExists()
    }
}