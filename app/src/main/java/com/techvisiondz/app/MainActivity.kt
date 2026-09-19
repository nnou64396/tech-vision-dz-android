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

class MainActivity : ComponentActivity() {

    /**
     * The most recently delivered deep link, resolved to a navigation target.
     * Held as Compose state so a new VIEW intent while the activity is alive
     * re-composes the root and routes it; re-delivering the same link is a no-op
     * (structural equality does not re-trigger), which prevents duplicate
     * navigation for duplicate intents.
     */
    private var deepLinkTarget: DeepLinkTarget? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only honor the cold-start intent on the first creation. On a
        // configuration change the ViewModel/navigation state is restored, so
        // re-resolving the launching intent would push a duplicate screen.
        if (savedInstanceState == null) {
            deepLinkTarget = DeepLinkResolver.resolve(intent?.data?.toString())
        }
        setContent {
            TechVisionDzApp(deepLinkTarget = deepLinkTarget)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTarget = DeepLinkResolver.resolve(intent.data?.toString())
    }
}