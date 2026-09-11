package com.techvisiondz.app.feature.article

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
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
import com.techvisiondz.app.core.ui.components.CategoryChip
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.components.TechGradientButton
import com.techvisiondz.app.core.ui.TechVisionIcons
import com.techvisiondz.app.core.ui.formatPublishedAt
import com.techvisiondz.app.core.ui.formatViewsCount
import com.techvisiondz.app.core.util.htmlBodySpanned
import com.techvisiondz.app.core.util.spannedToAnnotatedString
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import com.techvisiondz.app.ui.theme.brandGradient

/**
 * Article details screen — the app's premium editorial surface.
 *
 * Hierarchy mirrors the website: hero image → category chip → large title →
 * bordered metadata pill → reading time + views → excerpt → comfortable body →
 * tag pills → software callout. The top bar keeps back navigation and the
 * native share action (Phase 5). Missing content and failures show dedicated
 * empty/error states with retry.
 *
 * [onShareArticle] stays an optional callback so screens can test the action
 * deterministically; the production wiring launches the Android sharesheet.
 *
 * The save/bookmark action is only rendered for authenticated contexts:
 * [isAuthenticated] gates the icon, and tapping bookmarks through the view
 * model. When the user is not authenticated an [onRequireSignIn] tap routes to
 * the sign-in flow (no pending action is stored).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
    onShareArticle: ((Article) -> Unit)? = null,
    isAuthenticated: Boolean = false,
    onRequireSignIn: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
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
                        if (state is UiState.Success) {
                            if (isAuthenticated || onRequireSignIn != null) {
                                BookmarkAction(
                                    saveState = saveState,
                                    onToggleSave = {
                                        if (isAuthenticated) {
                                            viewModel.toggleSave()
                                        } else {
                                            onRequireSignIn?.invoke()
                                        }
                                    },
                                )
                            }
                            if (onShareArticle != null) {
                                IconButton(onClick = { onShareArticle(state.data) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = stringResource(R.string.share_article),
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
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

                is UiState.Success -> ArticleDetailContent(
                    article = state.data,
                    saveError = saveState.error,
                )
            }
        }
    }
}

@Composable
private fun BookmarkAction(
    saveState: ArticleSaveUiState,
    onToggleSave: () -> Unit,
) {
    IconButton(
        onClick = onToggleSave,
        enabled = !saveState.isSaving,
        modifier = Modifier.testTag("article_bookmark"),
    ) {
        if (saveState.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = if (saveState.saved) TechVisionIcons.Bookmark else TechVisionIcons.BookmarkBorder,
                contentDescription = stringResource(
                    if (saveState.saved) R.string.saved_article_remove else R.string.saved_article_save,
                ),
                tint = if (saveState.saved) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleDetailContent(
    article: Article,
    saveError: AuthError?,
) {
    val uriHandler = LocalUriHandler.current
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TechVisionSpacing.Lg)
            .padding(top = TechVisionSpacing.Md, bottom = TechVisionSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
    ) {
        if (saveError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = stringResource(saveError.messageRes),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(TechVisionSpacing.Md),
                )
            }
        }
        article.coverUrl?.let { coverUrl ->
            AsyncImage(
                model = coverUrl,
                contentDescription = article.coverAlt ?: article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(TechVisionRadii.Lg)),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(placeholderColor),
                error = ColorPainter(placeholderColor),
            )
            Spacer(modifier = Modifier.size(TechVisionSpacing.Sm))
        }

        article.category?.name?.takeIf { it.isNotBlank() }?.let { CategoryChip(label = it) }

        Text(
            text = article.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        val meta = buildList {
            article.author?.name?.takeIf { it.isNotBlank() }?.let(::add)
            article.category?.name?.takeIf { it.isNotBlank() }?.let(::add)
            val date = formatPublishedAt(article.publishedAt)
            if (date.isNotBlank()) add(date)
        }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(TechVisionRadii.Md),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = TechVisionSpacing.Lg, vertical = TechVisionSpacing.Md),
                )
            }
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

        article.excerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
            Spacer(modifier = Modifier.size(TechVisionSpacing.Sm))
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val bodySpanned = htmlBodySpanned(article.body)
        if (bodySpanned != null && bodySpanned.isNotBlank()) {
            Spacer(modifier = Modifier.size(TechVisionSpacing.Sm))
            Text(
                text = spannedToAnnotatedString(
                    spanned = bodySpanned,
                    linkColor = MaterialTheme.colorScheme.primary,
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 32.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (article.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
            ) {
                article.tags.forEach { tag ->
                    Surface(
                        shape = TechVisionRadii.Full,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = tag.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = TechVisionSpacing.Md, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        article.software?.let { software ->
            val softwareText = buildString {
                append(software.name)
                software.version?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
            if (softwareText.isNotBlank()) {
                Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
                Surface(
                    shape = RoundedCornerShape(TechVisionRadii.Lg),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(TechVisionSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(TechVisionRadii.Sm))
                                    .background(brush = MaterialTheme.colorScheme.brandGradient()),
                            )
                            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                            Text(
                                text = stringResource(R.string.article_software, softwareText),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        software.downloadUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            TechGradientButton(
                                text = stringResource(R.string.download),
                                onClick = { uriHandler.openUri(url) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}