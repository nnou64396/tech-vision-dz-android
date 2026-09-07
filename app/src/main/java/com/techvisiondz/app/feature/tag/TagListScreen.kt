package com.techvisiondz.app.feature.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Tag
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent

/**
 * Tags discovery screen. Renders the localized tags as clickable chips; a
 * selection navigates onward by the tag slug.
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { onTagClick(tag.slug) },
                label = { Text(text = tag.name) },
                modifier = Modifier.testTag("tag_item_${tag.slug}"),
            )
        }
    }
}