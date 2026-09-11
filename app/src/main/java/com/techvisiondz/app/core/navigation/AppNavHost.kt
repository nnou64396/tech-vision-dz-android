package com.techvisiondz.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.techvisiondz.app.R
import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.AuthState
import com.techvisiondz.app.core.data.repository.AuthRepository
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.ProfileRepository
import com.techvisiondz.app.core.data.repository.SavedArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseArticleRepository
import com.techvisiondz.app.core.data.repository.SupabaseAuthRepository
import com.techvisiondz.app.core.data.repository.SupabaseProfileRepository
import com.techvisiondz.app.core.data.repository.SupabaseSavedArticleRepository
import com.techvisiondz.app.core.util.ArticleUrlBuilder
import com.techvisiondz.app.core.util.launchArticleShareChooser
import com.techvisiondz.app.feature.account.AccountScreen
import com.techvisiondz.app.feature.account.AccountViewModel
import com.techvisiondz.app.feature.article.ArticleDetailScreen
import com.techvisiondz.app.feature.article.ArticleDetailViewModel
import com.techvisiondz.app.feature.auth.AuthLoadingScreen
import com.techvisiondz.app.feature.auth.ForgotPasswordScreen
import com.techvisiondz.app.feature.auth.ForgotPasswordViewModel
import com.techvisiondz.app.feature.auth.SignInScreen
import com.techvisiondz.app.feature.auth.SignInViewModel
import com.techvisiondz.app.feature.auth.SignUpScreen
import com.techvisiondz.app.feature.auth.SignUpViewModel
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
import com.techvisiondz.app.feature.saved.SavedArticlesScreen
import com.techvisiondz.app.feature.saved.SavedArticlesViewModel
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
 * The [ArticleRepository], [AuthRepository] and [ProfileRepository] defaults are
 * the real Supabase-backed implementations but can be injected by tests so the
 * whole graph runs deterministically with fakes.
 *
 * The graph is auth-aware: while [AuthState.Loading] a branded loading screen is
 * shown; once the session settles, unauthenticated users land on Sign In and
 * authenticated users on the existing Home. All destinations live in one graph,
 * and a [LaunchedEffect] reacts to auth-state changes to move the user between
 * the two — no duplicate destinations, no navigation loops.
 */
@Composable
fun AppNavHost(
    repository: ArticleRepository = remember { SupabaseArticleRepository() },
    authRepository: AuthRepository = remember { SupabaseAuthRepository() },
    profileRepository: ProfileRepository = remember { SupabaseProfileRepository() },
    savedArticleRepository: SavedArticleRepository = remember { SupabaseSavedArticleRepository() },
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val shareArticleLabel = stringResource(R.string.share_article)
    val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)

    when (authState) {
        is AuthState.Loading -> AuthLoadingScreen()

        else -> {
            // Unauthenticated (and the transient Error state, where the SDK keeps
            // the stored session) start in the auth flow; authenticated users go
            // straight to the app.
            val startDestination = if (authState is AuthState.Authenticated) Routes.Home else Routes.SignIn
            val isAuthenticated = authState is AuthState.Authenticated

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable<Routes.SignIn> {
                    SignInScreen(
                        viewModel = viewModel(factory = SignInViewModel.factory(authRepository)),
                        onForgotPasswordClick = { navController.navigate(Routes.ForgotPassword) },
                        onSignUpClick = { navController.navigate(Routes.SignUp) },
                    )
                }
                composable<Routes.SignUp> {
                    SignUpScreen(
                        viewModel = viewModel(factory = SignUpViewModel.factory(authRepository)),
                        onBackToSignInClick = { navController.popBackStack() },
                    )
                }
                composable<Routes.ForgotPassword> {
                    ForgotPasswordScreen(
                        viewModel = viewModel(factory = ForgotPasswordViewModel.factory(authRepository)),
                        onBackToSignInClick = { navController.popBackStack() },
                    )
                }
                composable<Routes.Home> {
                    HomeScreen(
                        onArticleClick = { slug -> navController.navigate(Routes.ArticleDetail(slug)) },
                        onSearchClick = { navController.navigate(Routes.Search) },
                        onCategoriesClick = { navController.navigate(Routes.Categories) },
                        onAuthorsClick = { navController.navigate(Routes.Authors) },
                        onTagsClick = { navController.navigate(Routes.Tags) },
                        onAccountClick = { navController.navigate(Routes.Account) },
                        viewModel = viewModel(factory = HomeViewModel.factory(repository)),
                    )
                }
                composable<Routes.Account> {
                    AccountScreen(
                        onBack = { navController.popBackStack() },
                        onSavedArticlesClick = { navController.navigate(Routes.SavedArticles) },
                        viewModel = viewModel(factory = AccountViewModel.factory(authRepository, profileRepository)),
                    )
                }
                composable<Routes.SavedArticles> {
                    SavedArticlesScreen(
                        onBack = { navController.popBackStack() },
                        onArticleClick = { slug -> navController.navigate(Routes.ArticleDetail(slug)) },
                        viewModel = viewModel(factory = SavedArticlesViewModel.factory(savedArticleRepository)),
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
                            factory = ArticleDetailViewModel.factory(slug, repository, savedArticleRepository),
                        ),
                        isAuthenticated = isAuthenticated,
                        onRequireSignIn = {
                            navController.navigate(Routes.SignIn) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
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

            // Moves the user between the auth graph and the app when the session
            // changes (sign-in success, future sign-out). Pop-to-root swaps the
            // whole stack so the previous graph never lingers behind the new one.
            LaunchedEffect(authState) {
                val destination = navController.currentBackStackEntry?.destination
                if (isAuthenticated) {
                    val onAuthScreen = destination?.hasRoute(Routes.SignIn::class) == true ||
                        destination?.hasRoute(Routes.SignUp::class) == true ||
                        destination?.hasRoute(Routes.ForgotPassword::class) == true
                    if (onAuthScreen) {
                        navController.navigate(Routes.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                } else {
                    val onMainScreen = destination?.hasRoute(Routes.Home::class) == true ||
                        destination?.hasRoute(Routes.Account::class) == true ||
                        destination?.hasRoute(Routes.SavedArticles::class) == true ||
                        destination?.hasRoute(Routes.Search::class) == true ||
                        destination?.hasRoute(Routes.ArticleDetail::class) == true ||
                        destination?.hasRoute(Routes.Categories::class) == true ||
                        destination?.hasRoute(Routes.CategoryArticles::class) == true ||
                        destination?.hasRoute(Routes.Authors::class) == true ||
                        destination?.hasRoute(Routes.AuthorArticles::class) == true ||
                        destination?.hasRoute(Routes.Tags::class) == true ||
                        destination?.hasRoute(Routes.TagArticles::class) == true
                    if (onMainScreen) {
                        navController.navigate(Routes.SignIn) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}