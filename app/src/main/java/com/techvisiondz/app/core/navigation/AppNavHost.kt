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
import com.techvisiondz.app.core.data.repository.ArticleRepository
import com.techvisiondz.app.core.data.repository.AuthRepository
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
import com.techvisiondz.app.feature.settings.AboutScreen
import com.techvisiondz.app.feature.settings.NotificationsSettingsScreen
import com.techvisiondz.app.feature.settings.PermissionsDetailsScreen
import com.techvisiondz.app.feature.settings.PrivacyPolicyScreen
import com.techvisiondz.app.feature.settings.SettingsScreen
import com.techvisiondz.app.feature.settings.SettingsViewModel
import com.techvisiondz.app.feature.sources.SourcesScreen
import com.techvisiondz.app.feature.tag.TagArticlesScreen
import com.techvisiondz.app.feature.tag.TagArticlesViewModel
import com.techvisiondz.app.feature.tag.TagListScreen
import com.techvisiondz.app.feature.tag.TagViewModel
/**
 * Root navigation graph for the app.
 */
@Composable
fun AppNavHost(
    repository: ArticleRepository = remember { SupabaseArticleRepository() },
    authRepository: AuthRepository = remember { SupabaseAuthRepository() },
    profileRepository: ProfileRepository = remember { SupabaseProfileRepository() },
    savedArticleRepository: SavedArticleRepository = remember { SupabaseSavedArticleRepository() },
    settingsViewModel: SettingsViewModel? = null,
    deepLinkTarget: DeepLinkTarget? = null,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val shareArticleLabel = stringResource(R.string.share_article)
    val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)
    val isAuthenticated = authState is AuthState.Authenticated

    fun navigateToArticle(slug: String) {
        val current = navController.currentBackStackEntry
        val alreadyOpen = current?.destination?.hasRoute(Routes.ArticleDetail::class) == true &&
            current.toRoute<Routes.ArticleDetail>().slug == slug
        if (!alreadyOpen) {
            navController.navigate(Routes.ArticleDetail(slug))
        }
    }

    fun navigateToTagArticles(slug: String) {
        val current = navController.currentBackStackEntry
        val alreadyOpen = current?.destination?.hasRoute(Routes.TagArticles::class) == true &&
            current.toRoute<Routes.TagArticles>().slug == slug
        if (!alreadyOpen) {
            navController.navigate(Routes.TagArticles(slug))
        }
    }

    LaunchedEffect(deepLinkTarget) {
        val target = deepLinkTarget ?: return@LaunchedEffect
        when (target) {
            is DeepLinkTarget.Article -> navigateToArticle(target.slug)
        }
    }

    val effectiveSettingsViewModel = settingsViewModel
        ?: viewModel(factory = SettingsViewModel.Factory)

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
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
                onArticleClick = ::navigateToArticle,
                onSearchClick = { navController.navigate(Routes.Search) },
                onCategoriesClick = { navController.navigate(Routes.Categories) },
                onSourcesClick = { navController.navigate(Routes.Sources) },
                onAuthorsClick = { navController.navigate(Routes.Authors) },
                onTagsClick = { navController.navigate(Routes.Tags) },
                onAccountClick = {
                    if (isAuthenticated) {
                        navController.navigate(Routes.Account)
                    } else {
                        navController.navigate(Routes.SignIn)
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(AUTH_RETURN_KEY, AUTH_RETURN_ACCOUNT)
                    }
                },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                viewModel = viewModel(factory = HomeViewModel.factory(repository)),
            )
        }
        composable<Routes.Sources> {
            SourcesScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Routes.Account> {
            AccountScreen(
                onBack = { navController.popBackStack() },
                onSavedArticlesClick = { navController.navigate(Routes.SavedArticles) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                viewModel = viewModel(factory = AccountViewModel.factory(authRepository, profileRepository)),
            )
        }
        composable<Routes.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAboutClick = { navController.navigate(Routes.About) },
                onPrivacyPolicyClick = { navController.navigate(Routes.PrivacyPolicy) },
                onPermissionsClick = { navController.navigate(Routes.PermissionsDetails) },
                onNotificationsClick = { navController.navigate(Routes.Notifications) },
                onSourcesClick = { navController.navigate(Routes.Sources) },
                viewModel = effectiveSettingsViewModel,
            )
        }
        composable<Routes.About> {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Routes.PrivacyPolicy> {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Routes.PermissionsDetails> {
            PermissionsDetailsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<Routes.Notifications> {
            NotificationsSettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = effectiveSettingsViewModel,
            )
        }
        composable<Routes.SavedArticles> {
            SavedArticlesScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = ::navigateToArticle,
                viewModel = viewModel(factory = SavedArticlesViewModel.factory(savedArticleRepository)),
            )
        }
        composable<Routes.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = ::navigateToArticle,
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
                onRelatedArticleClick = ::navigateToArticle,
                onTagClick = ::navigateToTagArticles,
                isAuthenticated = isAuthenticated,
                onRequireSignIn = { navController.navigate(Routes.SignIn) },
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
                onArticleClick = ::navigateToArticle,
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
                onArticleClick = ::navigateToArticle,
                viewModel = viewModel(
                    key = "author-articles-$slug",
                    factory = AuthorArticlesViewModel.factory(slug, repository),
                ),
            )
        }
        composable<Routes.Tags> {
            TagListScreen(
                onBack = { navController.popBackStack() },
                onTagClick = ::navigateToTagArticles,
                viewModel = viewModel(factory = TagViewModel.factory(repository)),
            )
        }
        composable<Routes.TagArticles> { backStackEntry ->
            val slug = backStackEntry.toRoute<Routes.TagArticles>().slug
            TagArticlesScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = ::navigateToArticle,
                viewModel = viewModel(
                    key = "tag-articles-$slug",
                    factory = TagArticlesViewModel.factory(slug, repository),
                ),
            )
        }
    }

    LaunchedEffect(authState) {
        val destination = navController.currentBackStackEntry?.destination
        if (isAuthenticated) {
            val onAuthScreen = destination?.hasRoute(Routes.SignIn::class) == true ||
                destination?.hasRoute(Routes.SignUp::class) == true ||
                destination?.hasRoute(Routes.ForgotPassword::class) == true
            if (onAuthScreen) {
                val requestedAccount = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(AUTH_RETURN_KEY) == AUTH_RETURN_ACCOUNT
                val popped = navController.popBackStack()
                if (!popped) {
                    navController.navigate(Routes.Home) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
                if (requestedAccount) {
                    navController.navigate(Routes.Account)
                }
            }
        } else if (authState !is AuthState.Loading) {
            val onProtectedScreen = destination?.hasRoute(Routes.Account::class) == true ||
                destination?.hasRoute(Routes.SavedArticles::class) == true
            if (onProtectedScreen) {
                navController.navigate(Routes.Home) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }
}

private const val AUTH_RETURN_KEY = "techvisiondz.auth.returnTo"
private const val AUTH_RETURN_ACCOUNT = "account"
