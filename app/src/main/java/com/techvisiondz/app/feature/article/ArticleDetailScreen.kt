package com.techvisiondz.app.feature.article

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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.Article
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.formatPublishedAt
import com.techvisiondz.app.core.ui.formatViewsCount
import com.techvisiondz.app.core.util.htmlBodySpanned
import com.techvisiondz.app.core.util.spannedToAnnotatedString

/**
 * Article details screen.
 *
 * Loads the selected article by slug through the [ArticleDetailViewModel] and
 * renders it RTL-first: a top bar with back navigation, a native share action,
 * the cover image, title, author/category/date metadata, reading time, views,
 * tags, excerpt and the article body (bold/italic preserved, body links open
 * in the system browser). Missing content and failures show dedicated
 * empty/error states with retry.
 *
 * [onShareArticle] stays an optional callback so screens can test the action
 * deterministically; the production wiring launches the Android sharesheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
    onShareArticle: ((Article) -> Unit)? = null,
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
                actions = {
                    val state = uiState
                    if (state is UiState.Success && onShareArticle != null) {
                        IconButton(onClick = { onShareArticle(state.data) }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.share_article),
                            )
                        }
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
        val details = buildList {
            article.readingTimeMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(pluralStringResource(R.plurals.reading_time_minutes, minutes, minutes))
            }
            add(stringResource(R.string.article_views, formatViewsCount(article.viewsCount)))
        }.joinToString(" · ")
        if (details.isNotBlank()) {
            Text(
                text = details,
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
        val bodySpanned = htmlBodySpanned(article.body)
        if (bodySpanned != null && bodySpanned.isNotBlank()) {
            val linkColor = MaterialTheme.colorScheme.primary
            Text(
                text = spannedToAnnotatedString(
                    spanned = bodySpanned,
                    linkColor = linkColor,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
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