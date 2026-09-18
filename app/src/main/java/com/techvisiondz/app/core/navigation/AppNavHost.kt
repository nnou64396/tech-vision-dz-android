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
import com.techvisiondz.app.feature.settings.SettingsScreen
import com.techvisiondz.app.feature.settings.SettingsViewModel
import com.techvisiondz.app.feature.tag.TagArticlesScreen
import com.techvisiondz.app.feature.tag.TagArticlesViewModel
import com.techvisiondz.app.feature.tag.TagListScreen
import com.techvisiondz.app.feature.tag.TagViewModel
import com.techvisiondz.app.feature.update.UpdateViewModel

/**
 * Root navigation graph for the app.
 *
 * Uses type-safe routes ([Routes]) so every destination is compile-time checked.
 * The [ArticleRepository], [AuthRepository], [ProfileRepository] and
 * [SavedArticleRepository] defaults are the real Supabase-backed implementations
 * but can be injected by tests so the whole graph runs deterministically with
 * fakes.
 *
 * The graph is public-by-default: it always starts on the public [Routes.Home]
 * feed, and [AuthState.Loading] (session restoration) never blocks public
 * content. Guests can browse Home, Search, Article Detail, Categories, Authors
 * and Tags freely. Authentication is only required for protected routes
 * ([Routes.Account], [Routes.SavedArticles]) and for saving/bookmarking an
 * article. A guest who triggers one of those actions is sent to Sign In
 * without wiping the public back stack, and after a successful sign-in the
 * [LaunchedEffect] below returns them to the requesting context (the same
 * Article Detail, or Account for an account request) via pop-back.
 */
@Composable
fun AppNavHost(
    repository: ArticleRepository = remember { SupabaseArticleRepository() },
    authRepository: AuthRepository = remember { SupabaseAuthRepository() },
    profileRepository: ProfileRepository = remember { SupabaseProfileRepository() },
    savedArticleRepository: SavedArticleRepository = remember { SupabaseSavedArticleRepository() },
    updateViewModel: UpdateViewModel? = null,
    settingsViewModel: SettingsViewModel? = null,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val shareArticleLabel = stringResource(R.string.share_article)
    val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)
    val isAuthenticated = authState is AuthState.Authenticated

    // Navigating to the article already shown (e.g. tapping the same related
    // card) would push a duplicate detail screen on top of itself. Guarding
    // here keeps the back stack stable and the same article from stacking.
    fun navigateToArticle(slug: String) {
        val current = navController.currentBackStackEntry
        val alreadyOpen = current?.destination?.hasRoute(Routes.ArticleDetail::class) == true &&
            current.toRoute<Routes.ArticleDetail>().slug == slug
        if (!alreadyOpen) {
            navController.navigate(Routes.ArticleDetail(slug))
        }
    }

    // An updater ViewModel passed from the app root is shared app-wide (single
    // instance for both the startup dialog host and the Account row). When
    // none is supplied (tests / standalone embedding), a session-scoped one is
    // created here so the Account screen still works.
    val effectiveUpdateViewModel = updateViewModel
        ?: viewModel(factory = UpdateViewModel.Factory)

    // Like the update ViewModel, a Settings ViewModel passed from the app root
    // is shared app-wide (single source of truth for the theme + locale the
    // root derives from it). When none is supplied (tests / standalone
    // embedding), a session-scoped one backed by the real preferences is
    // created here so the Settings screen still works.
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
                onAuthorsClick = { navController.navigate(Routes.Authors) },
                onTagsClick = { navController.navigate(Routes.Tags) },
                onAccountClick = {
                    if (isAuthenticated) {
                        navController.navigate(Routes.Account)
                    } else {
                        // Tag this Sign In as an account request so a successful
                        // sign-in returns to Account instead of public content.
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
        composable<Routes.Account> {
            AccountScreen(
                onBack = { navController.popBackStack() },
                onSavedArticlesClick = { navController.navigate(Routes.SavedArticles) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                viewModel = viewModel(factory = AccountViewModel.factory(authRepository, profileRepository)),
                updateViewModel = effectiveUpdateViewModel,
            )
        }
        composable<Routes.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = effectiveSettingsViewModel,
                updateViewModel = effectiveUpdateViewModel,
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
                onTagClick = { slug -> navController.navigate(Routes.TagArticles(slug)) },
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

    // Reacts to session changes once the app is mounted. Public screens are
    // never evicted for guests: only the protected Account / Saved Articles
    // routes are removed when the session is lost. On a successful sign-in
    // while on an auth screen, pop back to the requesting page (the same
    // Article Detail or the page under Sign In); if a guest requested Account,
    // land there afterwards. Falls back to Home only when there is no entry to
    // pop back to.
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

/** Internal nav-result key marking a guest's Sign In request for a protected destination. */
private const val AUTH_RETURN_KEY = "techvisiondz.auth.returnTo"

/** Value of [AUTH_RETURN_KEY] meaning the sign-in was requested from the Account action. */
private const val AUTH_RETURN_ACCOUNT = "account"