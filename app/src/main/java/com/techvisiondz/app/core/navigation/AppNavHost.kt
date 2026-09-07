package com.techvisiondz.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.feature.article.ArticleDetailScreen
import com.techvisiondz.app.feature.article.ArticleDetailViewModel
import com.techvisiondz.app.feature.home.HomeScreen
import com.techvisiondz.app.feature.home.HomeViewModel

/**
 * Root navigation graph for the app.
 *
 * Uses type-safe routes ([Routes]) so every destination is compile-time checked.
 * The [repository] defaults to the real Supabase-backed implementation but can
 * be injected by tests so the whole graph runs deterministically with a fake.
 */
@Composable
fun AppNavHost(
    repository: ArticleRepository = remember { SupabaseArticleRepository() },
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable<Routes.Home> {
            HomeScreen(
                onArticleClick = { slug -> navController.navigate(Routes.ArticleDetail(slug)) },
                viewModel = viewModel(factory = HomeViewModel.factory(repository)),
            )
        }
        composable<Routes.ArticleDetail> { backStackEntry ->
            val slug = backStackEntry.toRoute<Routes.ArticleDetail>().slug
            ArticleDetailScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel(
                    key = "article-detail-$slug",
                    factory = ArticleDetailViewModel.factory(slug, repository),
                ),
            )
        }
    }
}