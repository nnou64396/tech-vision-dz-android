package com.techvisiondz.app.feature.article

import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.formatPublishedAt

/**
 * Article details screen.
 *
 * Loads the selected article by slug through the [ArticleDetailViewModel] and
 * renders it RTL-first: a top bar with back navigation, the cover image, title,
 * author/category/date metadata, tags, excerpt and the HTML article body.
 * Missing content and failures show dedicated empty/error states with retry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? UiState.Success)?.data?.title
                            ?: stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()

                is UiState.Error -> ErrorState(
                    message = state.message.ifBlank { stringResource(R.string.article_error) },
                    onRetry = viewModel::loadArticle,
                )

                is UiState.Empty -> EmptyState(
                    message = stringResource(R.string.article_not_found),
                )

                is UiState.Success -> ArticleDetailContent(article = state.data)
            }
        }
    }
}

@Composable
private fun ArticleDetailContent(article: Article) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        article.coverUrl?.let { coverUrl ->
            AsyncImage(
                model = coverUrl,
                contentDescription = article.coverAlt ?: article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        val meta = buildList {
            article.author?.name?.takeIf { it.isNotBlank() }?.let(::add)
            article.category?.name?.takeIf { it.isNotBlank() }?.let(::add)
            val date = formatPublishedAt(article.publishedAt)
            if (date.isNotBlank()) add(date)
        }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (article.tags.isNotEmpty()) {
            Text(
                text = article.tags.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        if (!article.excerpt.isNullOrBlank()) {
            Text(
                text = article.excerpt,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val body = htmlToPlainText(article.body)
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        article.software?.let { software ->
            val softwareText = buildString {
                append(software.name)
                software.version?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            if (softwareText.isNotBlank()) {
                Text(
                    text = stringResource(R.string.article_software, softwareText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Converts the article's rich HTML body (as produced by the website) into
 * plain text for display. The video reference is intentionally not rendered:
 * embedding a player is a separate future feature.
 */
private fun htmlToPlainText(html: String?): String {
    if (html.isNullOrBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}