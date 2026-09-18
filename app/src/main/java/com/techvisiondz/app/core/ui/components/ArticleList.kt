package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Scrollable list of [ArticleCard] items used by the home feed and every
 * discovery article listing (category / author / tag / search results).
 *
 * [featuredFirst] renders the first item as the large editorial hero card;
 * [header] injects a pinned section heading as the first lazy item. Cards are
 * clickable and tagged `article_card_{slug}` for deterministic navigation tests.
 *
 * When [hasMore] is true a [LoadMoreFooter] item is appended that requests the
 * next page as it scrolls into view ([onLoadMore]), shows the loading state
 * ([isLoadingMore]) and surfaces a retryable failure ([loadMoreError]). The
 * footer is omitted entirely on the final page, so a fully loaded list never
 * renders a trailing spinner.
 */
@Composable
fun ArticleList(
    articles: List<ArticleCard>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    featuredFirst: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    loadMoreError: String? = null,
    onLoadMore: () -> Unit = {},
    onRetryLoadMore: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = TechVisionSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        header?.let {
            item(key = "feed_header") {
                Box(modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg)) { it() }
            }
        }
        itemsIndexed(items = articles, key = { _, article -> article.id }) { index, article ->
            ArticleCard(
                article = article,
                variant = if (featuredFirst && index == 0) ArticleCardVariant.Hero else ArticleCardVariant.Standard,
                onClick = { onArticleClick(article.slug) },
                modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg),
            )
        }
        if (hasMore || isLoadingMore || loadMoreError != null) {
            item(key = "feed_load_more") {
                LoadMoreFooter(
                    hasMore = hasMore,
                    isLoadingMore = isLoadingMore,
                    error = loadMoreError,
                    onLoadMore = onLoadMore,
                    onRetry = onRetryLoadMore,
                    modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg),
                )
            }
        }
    }
}