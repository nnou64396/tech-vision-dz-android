package com.techvisiondz.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.techvisiondz.app.R
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.core.util.ArticleUrlBuilder
import com.techvisiondz.app.core.util.launchArticleShareChooser
import com.techvisiondz.app.feature.article.ArticleDetailScreen
import com.techvisiondz.app.feature.article.ArticleDetailViewModel
import com.techvisiondz.app.feature.author.AuthorArticlesScreen
import com.techvisiondz.app.feature.author.AuthorArticlesViewModel
import com.techvisiondz.app.feature.author.AuthorListScreen
import com.techvisiondz.app.feature.author.AuthorViewModel
import com.techvisiondz.app.feature.category.CategoryArticlesScreen
import com.techvisiondz.app.feature.category.CategoryArticlesViewModel
import com.techvisiondz.app.feature.category.CategoryListScreen
import com.techvisiondz.app.feature.category.CategoryViewModel
import com.techvisiondz.app.feature.home.HomeScreen
import com.techvisiondz.app.feature.home.HomeViewModel
import com.techvisiondz.app.feature.search.SearchScreen
import com.techvisiondz.app.feature.search.SearchViewModel
import com.techvisiondz.app.feature.tag.TagArticlesScreen
import com.techvisiondz.app.feature.tag.TagArticlesViewModel
import com.techvisiondz.app.feature.tag.TagListScreen
import com.techvisiondz.app.feature.tag.TagViewModel

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
    val context = LocalContext.current
    val shareArticleLabel = stringResource(R.string.share_article)

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable<Routes.Home> {
            HomeScreen(
                onArticleClick = { slug -> navController.navigate(Routes.ArticleDetail(slug)) },
                onSearchClick = { navController.navigate(Routes.Search) },
                onCategoriesClick = { navController.navigate(Routes.Categories) },
                onAuthorsClick = { navController.navigate(Routes.Authors) },
                onTagsClick = { navController.navigate(Routes.Tags) },
                viewModel = viewModel(factory = HomeViewModel.factory(repository)),
            )
        }
        composable<Routes.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { slug -> navController.navigate(Routes.ArticleDetail(slug)) },
                viewModel = viewModel(factory = SearchViewModel.factory(repository)),
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
                onShareArticle = { article ->
                    val url = runCatching {
                        ArticleUrlBuilder.buildArticleUrl(AppConfig.ARTICLE_BASE_URL, article.slug)
                    }.getOrNull()
                    if (url != null) {
                        launchArticleShareChooser(
                            context = context,
                            url = url,
                            title = article.title,
                            chooserTitle = shareArticleLabel,
                        )
                    }
                },
            )
        }
        composable<Routes.Categories> {
            CategoryListScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { slug -> navController.navigate(Routes.CategoryArticles(slug)) },
                viewModel = viewModel(factory = CategoryViewModel.factory(repository)),
            )
        }
        composable<Routes.CategoryArticles> { backStackEntry ->
            val slug = backStackEntry.toRoute<Routes.CategoryArticles>().slug
            CategoryArticlesScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { articleSlug -> navController.navigate(Routes.ArticleDetail(articleSlug)) },
                viewModel = viewModel(
                    key = "category-articles-$slug",
                    factory = CategoryArticlesViewModel.factory(slug, repository),
                ),
            )
        }
        composable<Routes.Authors> {
            AuthorListScreen(
                onBack = { navController.popBackStack() },
                onAuthorClick = { slug -> navController.navigate(Routes.AuthorArticles(slug)) },
                viewModel = viewModel(factory = AuthorViewModel.factory(repository)),
            )
        }
        composable<Routes.AuthorArticles> { backStackEntry ->
            val slug = backStackEntry.toRoute<Routes.AuthorArticles>().slug
            AuthorArticlesScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { articleSlug -> navController.navigate(Routes.ArticleDetail(articleSlug)) },
                viewModel = viewModel(
                    key = "author-articles-$slug",
                    factory = AuthorArticlesViewModel.factory(slug, repository),
                ),
            )
        }
        composable<Routes.Tags> {
            TagListScreen(
                onBack = { navController.popBackStack() },
                onTagClick = { slug -> navController.navigate(Routes.TagArticles(slug)) },
                viewModel = viewModel(factory = TagViewModel.factory(repository)),
            )
        }
        composable<Routes.TagArticles> { backStackEntry ->
            val slug = backStackEntry.toRoute<Routes.TagArticles>().slug
            TagArticlesScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { articleSlug -> navController.navigate(Routes.ArticleDetail(articleSlug)) },
                viewModel = viewModel(
                    key = "tag-articles-$slug",
                    factory = TagArticlesViewModel.factory(slug, repository),
                ),
            )
        }
    }
}