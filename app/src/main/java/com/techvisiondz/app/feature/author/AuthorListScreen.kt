package com.techvisiondz.app.feature.author

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Author
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.DiscoveryContent

/**
 * Authors discovery screen. Renders the localized authors; a selection navigates
 * onward by the author slug. An avatar is shown only when the backend provides
 * one - nothing is invented.
 */
@Composable
fun AuthorListScreen(
    viewModel: AuthorViewModel,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    BackTopBarScreen(title = stringResource(R.string.authors), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DiscoveryContent(
                state = uiState,
                onRetry = viewModel::loadAuthors,
                errorFallback = stringResource(R.string.authors_error),
                emptyMessage = stringResource(R.string.authors_empty),
            ) { authors ->
                AuthorList(authors = authors, onAuthorClick = onAuthorClick)
            }
        }
    }
}

@Composable
private fun AuthorList(authors: List<Author>, onAuthorClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = authors, key = { it.id }) { author ->
            AuthorCard(author = author, onClick = { onAuthorClick(author.slug) })
        }
    }
}

@Composable
private fun AuthorCard(author: Author, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("author_item_${author.slug}"),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            author.avatarUrl?.let { avatarUrl ->
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = author.name,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!author.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = author.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}