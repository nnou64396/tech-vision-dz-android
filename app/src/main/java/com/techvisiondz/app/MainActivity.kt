package com.techvisiondz.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.techvisiondz.app.core.navigation.DeepLinkResolver
import com.techvisiondz.app.core.navigation.DeepLinkTarget
import com.techvisiondz.app.core.notification.NotificationHelper

class MainActivity : ComponentActivity() {

    /**
     * The most recently delivered deep link or notification intent, resolved to a navigation target.
     * Held as Compose state so a new intent while the activity is alive re-composes the root and routes it.
     */
    private var deepLinkTarget: DeepLinkTarget? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            deepLinkTarget = resolveTarget(intent)
        }
        setContent {
            TechVisionDzApp(deepLinkTarget = deepLinkTarget)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTarget = resolveTarget(intent)
    }

    private fun resolveTarget(intent: Intent?): DeepLinkTarget? {
        if (intent == null) return null
        val articleSlugExtra = intent.getStringExtra(NotificationHelper.EXTRA_ARTICLE_SLUG)
        val dataUrl = intent.data?.toString()

        return when {
            !articleSlugExtra.isNullOrBlank() -> DeepLinkTarget.Article(articleSlugExtra)
            !dataUrl.isNullOrBlank() -> DeepLinkResolver.resolve(dataUrl)
            else -> null
        }
    }
}
