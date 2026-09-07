package com.techvisiondz.app.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Category
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent

/**
 * Categories discovery screen. Renders the localized categories as a grid; a
 * selection navigates onward by the category slug.
 */
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    BackTopBarScreen(title = stringResource(R.string.categories), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DiscoveryContent(
                state = uiState,
                onRetry = viewModel::loadCategories,
                errorFallback = stringResource(R.string.categories_error),
                emptyMessage = stringResource(R.string.categories_empty),
            ) { categories ->
                CategoryGrid(categories = categories, onCategoryClick = onCategoryClick)
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, onCategoryClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryCard(category = category, onClick = { onCategoryClick(category.slug) })
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .testTag("category_item_${category.slug}"),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!category.description.isNullOrBlank()) {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}