package com.techvisiondz.app.feature.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Source
import com.techvisiondz.app.core.data.model.SourceCategory
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.CategoryChip
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.util.DownloadUrlPolicy
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    viewModel: SourcesViewModel = viewModel(factory = SourcesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val websiteErrorMessage = stringResource(R.string.settings_website_error)
    val configuration = LocalConfiguration.current
    val isArabic = configuration.locales[0].language.equals("ar", ignoreCase = true)

    val openUrl: (String) -> Unit = { rawUrl ->
        val safeUrl = DownloadUrlPolicy.normalize(rawUrl)
        if (safeUrl == null) {
            scope.launch { snackbarHostState.showSnackbar(websiteErrorMessage) }
        } else {
            runCatching { uriHandler.openUri(safeUrl) }
                .onFailure {
                    scope.launch { snackbarHostState.showSnackbar(websiteErrorMessage) }
                }
        }
    }

    BackTopBarScreen(
        title = stringResource(R.string.sources_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = TechVisionSpacing.Lg)
                .testTag("sources_screen_content"),
            verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Lg),
        ) {
            item {
                Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
                Text(
                    text = stringResource(R.string.sources_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(TechVisionSpacing.Md))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sources_search_field"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.sources_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                Spacer(modifier = Modifier.height(TechVisionSpacing.Md))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
                    verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Xs),
                ) {
                    state.categories.forEach { category ->
                        val isSelected = category == state.selectedCategory
                        val label = if (category == SourceCategory.ALL) {
                            stringResource(R.string.sources_filter_all)
                        } else if (isArabic) {
                            category.titleAr
                        } else {
                            category.titleEn
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("source_category_chip_${category.name}"),
                        )
                    }
                }
            }

            if (state.filteredSources.isEmpty()) {
                item {
                    EmptyState(message = stringResource(R.string.sources_empty))
                }
            } else {
                items(
                    items = state.filteredSources,
                    key = { it.id },
                ) { source ->
                    SourceCard(
                        source = source,
                        isArabic = isArabic,
                        onVisitWebsite = { openUrl(source.url) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = stringResource(R.string.developer_attribution),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(TechVisionSpacing.Lg),
                    )
                }
                Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))
            }
        }
    }
}

@Composable
private fun SourceCard(
    source: Source,
    isArabic: Boolean,
    onVisitWebsite: () -> Unit,
) {
    val description = if (isArabic) source.descriptionAr else source.descriptionEn

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!source.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = source.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                        error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                    Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                }

                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(TechVisionSpacing.Sm))

                CategoryChip(label = if (isArabic) source.category.titleAr else source.category.titleEn)
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Md))

            Surface(
                onClick = onVisitWebsite,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.testTag("source_visit_button_${source.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.sources_visit_website),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(TechVisionSpacing.Xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
