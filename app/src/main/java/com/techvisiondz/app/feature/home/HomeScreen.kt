package com.techvisiondz.app.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.ArticleCard
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Home screen. Loads the real published article feed from the existing TECH
 * VISION DZ Supabase backend through the [HomeViewModel] and renders it as an
 * RTL-friendly card list (localized strings + per-article Arabic fallback).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                        ArticleFeed(articles = state.data.articles)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleFeed(articles: List<ArticleCard>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = articles, key = { it.id }) { article ->
            ArticleCardItem(article = article)
        }
    }
}

@Composable
private fun ArticleCardItem(article: ArticleCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            article.coverUrl?.let { coverUrl ->
                AsyncImage(
                    model = coverUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!article.excerpt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val meta = listOfNotNull(
                    article.authorName,
                    article.categoryName,
                    formatPublishedAt(article.publishedAt),
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun formatPublishedAt(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        OffsetDateTime.parse(iso)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    } catch (e: Exception) {
        ""
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleCardItemPreview() {
    TechVisionDzTheme {
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            ArticleCardItem(
                article = ArticleCard(
                    id = "preview-1",
                    slug = "preview",
                    title = "مقال تجريبي عن أحدث التقنيات في الجزائر",
                    excerpt = "هذا وصف قصير للمقال التجريبي المعروض في معاينة التصميم.",
                    featured = true,
                    publishedAt = "2026-08-01T09:00:00Z",
                    readingTimeMinutes = 5,
                    viewsCount = 120,
                    categoryName = "تقنية",
                    authorName = "TECH VISION DZ",
                    coverUrl = null,
                ),
            )
        }
    }
}