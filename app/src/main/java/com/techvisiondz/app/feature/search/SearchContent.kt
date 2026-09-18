package com.techvisiondz.app.feature.search

import com.techvisiondz.app.core.data.model.ArticleCard

/**
 * Content model for the search results.
 *
 * Mirrors [com.techvisiondz.app.feature.home.HomeContent]: incremental load-more
 * paging over the currently active query ([hasMore] / [isLoadingMore] /
 * [loadMoreError]) while keeping the already-fetched results stable in memory.
 */
data class SearchContent(
    val articles: List<ArticleCard> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null,
)