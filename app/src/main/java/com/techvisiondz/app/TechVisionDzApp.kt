package com.techvisiondz.app

import androidx.compose.runtime.Composable
import com.techvisiondz.app.core.navigation.AppNavHost
import com.techvisiondz.app.ui.theme.TechVisionDzTheme

/**
 * Root composable of the app.
 * Wires the TECH VISION DZ theme around the navigation graph.
 */
@Composable
fun TechVisionDzApp() {
    TechVisionDzTheme {
        AppNavHost()
    }
}