package com.techvisiondz.app.feature.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.techvisiondz.app.ui.theme.TechVisionElevation
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import com.techvisiondz.app.ui.theme.brandGradient

/**
 * Categories discovery screen. Renders the localized categories as a two-column
 * grid of bordered cards, each carrying the brand's gradient accent bar; a
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
        contentPadding = PaddingValues(TechVisionSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Md),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryCard(category = category, onClick = { onCategoryClick(category.slug) })
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .testTag("category_item_${category.slug}"),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = TechVisionElevation.Flat,
    ) {
        Row(
            modifier = Modifier.padding(TechVisionSpacing.Lg),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 48.dp)
                    .background(
                        brush = MaterialTheme.colorScheme.brandGradient(),
                        shape = RoundedCornerShape(TechVisionRadii.Sm),
                    ),
            )
            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                category.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}