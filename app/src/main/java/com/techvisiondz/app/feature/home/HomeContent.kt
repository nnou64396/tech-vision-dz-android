package com.techvisiondz.app.feature.home

import com.techvisiondz.app.core.data.model.ArticleCard

/**
 * Content model for the home screen.
 *
 * Supports incremental "load more" paging of the feed: [hasMore] reports
 * whether the backend may return another page, [isLoadingMore] guards against
 * concurrent next-page requests and [loadMoreError] surfaces a next-page
 * failure without discarding the already-loaded articles.
 */
data class HomeContent(
    val articles: List<ArticleCard> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null,
)