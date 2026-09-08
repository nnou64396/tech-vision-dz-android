package com.techvisiondz.app.feature.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Tag
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Tags discovery screen. Renders the localized tags as bordered pill buttons;
 * a selection navigates onward by the tag slug.
 */
@Composable
fun TagListScreen(
    viewModel: TagViewModel,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    BackTopBarScreen(title = stringResource(R.string.tags), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DiscoveryContent(
                state = uiState,
                onRetry = viewModel::loadTags,
                errorFallback = stringResource(R.string.tags_error),
                emptyMessage = stringResource(R.string.tags_empty),
            ) { tags ->
                TagFlow(tags = tags, onTagClick = onTagClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlow(tags: List<Tag>, onTagClick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxSize().padding(TechVisionSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
    ) {
        tags.forEach { tag ->
            Surface(
                onClick = { onTagClick(tag.slug) },
                modifier = Modifier.testTag("tag_item_${tag.slug}"),
                shape = TechVisionRadii.Full,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg, vertical = 10.dp),
                )
            }
        }
    }
}