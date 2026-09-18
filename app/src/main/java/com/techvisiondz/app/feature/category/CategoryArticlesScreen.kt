package com.techvisiondz.app.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent

/**
 * Category article listing screen. Reuses the shared [ArticleList] cards and
 * supports pull-to-refresh (surfacing refresh failures in a snackbar); a card
 * selection opens the existing article-details screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryArticlesScreen(
    viewModel: CategoryArticlesViewModel,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val refreshErrorMessage = stringResource(R.string.refresh_error)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshError) {
        val message = refreshError
        if (message != null) {
            snackbarHostState.showSnackbar(
                message.ifBlank { refreshErrorMessage },
            )
            viewModel.consumeRefreshError()
        }
    }

    BackTopBarScreen(
        title = stringResource(R.string.categories_articles),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DiscoveryContent(
                    state = uiState,
                    onRetry = viewModel::loadArticles,
                    errorFallback = stringResource(R.string.category_articles_error),
                    emptyMessage = stringResource(R.string.category_articles_empty),
                ) { articles ->
                    ArticleList(articles = articles, onArticleClick = onArticleClick)
                }
            }
        }
    }
}