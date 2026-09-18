package com.techvisiondz.app.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Search screen. Debounced, localized search over published article titles and
 * excerpts through the [SearchViewModel]. Results support incremental load-more
 * paging, pull-to-refresh and the shared article list; a card selection opens
 * the existing article-details route. The deduplicated (id) search results keep
 * overlapping pages from ever rendering a repeated card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val activeQuery by viewModel.activeQuery.collectAsState()
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
        title = stringResource(R.string.search),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                onSearch = viewModel::onSearchSubmit,
                onClear = viewModel::onClear,
            )
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is UiState.Loading -> LoadingState()

                        is UiState.Error -> ErrorState(
                            message = state.message,
                            onRetry = viewModel::retry,
                        )

                        is UiState.Empty -> EmptyState(
                            message = activeQuery
                                ?.let { stringResource(R.string.search_no_results, it) }
                                ?: stringResource(R.string.search_hint),
                        )

                        is UiState.Success -> {
                            if (state.data.articles.isEmpty()) {
                                EmptyState(
                                    message = stringResource(R.string.search_no_results, activeQuery.orEmpty()),
                                )
                            } else {
                                ArticleList(
                                    articles = state.data.articles,
                                    onArticleClick = onArticleClick,
                                    hasMore = state.data.hasMore,
                                    isLoadingMore = state.data.isLoadingMore,
                                    loadMoreError = state.data.loadMoreError,
                                    onLoadMore = viewModel::loadMore,
                                    onRetryLoadMore = viewModel::loadMore,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Sm)
            .testTag("search_field"),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        placeholder = { Text(text = stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = onClear, modifier = Modifier.testTag("search_clear")) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}