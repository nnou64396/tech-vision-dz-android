package com.techvisiondz.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.BrandMark
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.components.SectionHeader
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Home screen — the app's brand command center. Loads the real published feed
 * through the [HomeViewModel] and renders it as an Arabic-first editorial list:
 * brand header, prominent search entry, discovery menu, then a hero-first feed.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    onArticleClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onAuthorsClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(bottom = TechVisionSpacing.Lg)) {
            HomeHeader(
                onSearchClick = onSearchClick,
                onCategoriesClick = onCategoriesClick,
                onAuthorsClick = onAuthorsClick,
                onTagsClick = onTagsClick,
                onAccountClick = onAccountClick,
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
                                featuredFirst = true,
                                header = {
                                    SectionHeader(title = stringResource(R.string.home_feed))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onTagsClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TechVisionSpacing.Lg,
                    end = TechVisionSpacing.Lg,
                    top = TechVisionSpacing.Md,
                    bottom = TechVisionSpacing.Sm,
                )
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 36.dp)
            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier.testTag("home_account"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.account_title),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SearchEntry(onClick = onSearchClick)
        DiscoveryBar(
            onCategoriesClick = onCategoriesClick,
            onAuthorsClick = onAuthorsClick,
            onTagsClick = onTagsClick,
        )
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Sm)
            .testTag("search_entry"),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_articles),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
            Text(
                text = stringResource(R.string.search_articles),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TechVisionSpacing.Lg, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
    ) {
        DiscoveryTile(
            icon = Icons.AutoMirrored.Filled.List,
            label = stringResource(R.string.categories),
            onClick = onCategoriesClick,
            modifier = Modifier.weight(1f),
        )
        DiscoveryTile(
            icon = Icons.Filled.Person,
            label = stringResource(R.string.authors),
            onClick = onAuthorsClick,
            modifier = Modifier.weight(1f),
        )
        DiscoveryTile(
            icon = Icons.Filled.Star,
            label = stringResource(R.string.tags),
            onClick = onTagsClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DiscoveryTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}