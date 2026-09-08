package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.ArticleCard as ArticleCardData
import com.techvisiondz.app.core.ui.formatPublishedAt
import com.techvisiondz.app.core.ui.formatViewsCount
import com.techvisiondz.app.ui.theme.TechVisionElevation
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/** Visual variants of the shared [ArticleCard]: a large editorial hero and a compact row card. */
enum class ArticleCardVariant { Hero, Standard }

/**
 * Single reusable article card used by every list in the app (home feed, search
 * results, category/author/tag article lists). Both variants share the same
 * bordered editorial surface, category chip, title/excerpt hierarchy and the
 * standalone metadata row — following the website's card design.
 */
@Composable
fun ArticleCard(
    article: ArticleCardData,
    variant: ArticleCardVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (variant) {
        ArticleCardVariant.Hero -> HeroArticleCard(article = article, onClick = onClick, modifier = modifier)
        ArticleCardVariant.Standard -> StandardArticleCard(article = article, onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun HeroArticleCard(
    article: ArticleCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("article_card_${article.slug}"),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = TechVisionElevation.Flat,
    ) {
        Column {
            article.coverUrl?.let { coverUrl ->
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = TechVisionRadii.Lg, topEnd = TechVisionRadii.Lg)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            Column(
                modifier = Modifier.padding(TechVisionSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
            ) {
                article.categoryName?.takeIf { it.isNotBlank() }?.let { CategoryChip(label = it) }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                article.excerpt?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val meta = buildCardMeta(article = article, includeViews = true)
                if (meta.isNotEmpty()) {
                    DottedMetaRow(parts = meta)
                }
            }
        }
    }
}

@Composable
private fun StandardArticleCard(
    article: ArticleCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("article_card_${article.slug}"),
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = TechVisionElevation.Flat,
    ) {
        Row(
            modifier = Modifier.padding(TechVisionSpacing.Md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Md),
        ) {
            article.coverUrl?.let { coverUrl ->
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(TechVisionRadii.Md)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                article.excerpt?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val meta = buildCardMeta(article = article, includeViews = true)
                if (meta.isNotEmpty()) {
                    DottedMetaRow(parts = meta)
                }
            }
        }
    }
}

/** Author (standalone semantics element) · category · date · reading time · views. */
@Composable
private fun buildCardMeta(article: ArticleCardData, includeViews: Boolean): List<String> {
    val date = formatPublishedAt(article.publishedAt).takeIf { it.isNotBlank() }
    val reading = article.readingTimeMinutes?.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.reading_time_minutes, it, it) }
    val views = if (includeViews) {
        stringResource(R.string.article_views, formatViewsCount(article.viewsCount))
    } else {
        null
    }
    return buildList {
        article.authorName?.takeIf { it.isNotBlank() }?.let(::add)
        article.categoryName?.takeIf { it.isNotBlank() }?.let(::add)
        date?.let(::add)
        reading?.let(::add)
        views?.let(::add)
    }
}