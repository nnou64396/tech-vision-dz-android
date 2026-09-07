package com.techvisiondz.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState

/**
 * Home screen. Loads the real published article feed from the existing TECH
 * VISION DZ Supabase backend through the [HomeViewModel] and renders it as an
 * RTL-friendly card list (localized strings + per-article Arabic fallback).
 * A compact discovery bar on top links to the categories / authors / tags
 * sections.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    onArticleClick: (String) -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onAuthorsClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DiscoveryBar(
                onCategoriesClick = onCategoriesClick,
                onAuthorsClick = onAuthorsClick,
                onTagsClick = onTagsClick,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is UiState.Loading -> LoadingState()

                    is UiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = viewModel::loadHome,
                    )

                    is UiState.Empty -> EmptyState(
                        message = state.message.ifEmpty { stringResource(R.string.home_empty) },
                    )

                    is UiState.Success -> {
                        if (state.data.articles.isEmpty()) {
                            EmptyState(message = stringResource(R.string.home_empty))
                        } else {
                            ArticleList(
                                articles = state.data.articles,
                                onArticleClick = onArticleClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryBar(
    onCategoriesClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onTagsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = onCategoriesClick,
            label = { Text(text = stringResource(R.string.categories)) },
            modifier = Modifier.weight(1f),
        )
        AssistChip(
            onClick = onAuthorsClick,
            label = { Text(text = stringResource(R.string.authors)) },
            modifier = Modifier.weight(1f),
        )
        AssistChip(
            onClick = onTagsClick,
            label = { Text(text = stringResource(R.string.tags)) },
            modifier = Modifier.weight(1f),
        )
    }
}