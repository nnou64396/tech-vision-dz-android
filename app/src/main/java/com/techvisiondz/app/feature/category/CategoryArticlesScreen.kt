package com.techvisiondz.app.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent

/**
 * Category article listing screen. Reuses the shared [ArticleList] cards; a
 * card selection opens the existing article-details screen.
 */
@Composable
fun CategoryArticlesScreen(
    viewModel: CategoryArticlesViewModel,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    BackTopBarScreen(title = stringResource(R.string.categories_articles), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DiscoveryContent(
                state = uiState,
                onRetry = viewModel::loadArticles,
                errorFallback = stringResource(R.string.article_error),
                emptyMessage = stringResource(R.string.home_empty),
            ) { articles ->
                ArticleList(articles = articles, onArticleClick = onArticleClick)
            }
        }
    }
}