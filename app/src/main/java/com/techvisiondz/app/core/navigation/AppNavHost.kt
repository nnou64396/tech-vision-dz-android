package com.techvisiondz.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.techvisiondz.app.feature.home.HomeScreen

/**
 * Root navigation graph for the app.
 *
 * New destinations are added here as they are implemented - each screen gets a
 * route constant in [Routes] and is registered with [composable].
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}