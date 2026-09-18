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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.techvisiondz.app.core.data.model.ArticleCard as ArticleCardModel
import com.techvisiondz.app.core.data.model.SoftwareSummary
import com.techvisiondz.app.core.data.model.VideoRef
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.ArticleCard
import com.techvisiondz.app.core.ui.components.ArticleCardVariant
import com.techvisiondz.app.core.ui.components.CategoryChip
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.components.TechGradientButton
import com.techvisiondz.app.core.ui.TechVisionIcons
import com.techvisiondz.app.core.ui.formatPublishedAt
import com.techvisiondz.app.core.ui.formatViewsCount
import com.techvisiondz.app.core.util.DownloadUrlPolicy
import com.techvisiondz.app.core.util.htmlBodySpanned
import com.techvisiondz.app.core.util.spannedToAnnotatedString
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.ui.theme.TechVisionRadii
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import com.techvisiondz.app.ui.theme.brandGradient
import kotlinx.coroutines.launch

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
 *
 * The software download card validates its link as `http`/`https` before
 * opening it externally; a failed launch surfaces a snackbar instead of
 * crashing. [onDownloadClick] is an optional callback so tests can verify the
 * validated URL deterministically; when null the production wiring opens the
 * system browser and guards failures.
 *
 * The video watch action follows the same pattern: the URL is validated as
 * `http`/`https`, the launch is guarded against an unhandled intent (which
 * would otherwise crash the screen) and a failed launch surfaces a localized
 * snackbar. [onOpenVideo] mirrors [onDownloadClick] as a test seam.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
    onShareArticle: ((Article) -> Unit)? = null,
    onRelatedArticleClick: ((String) -> Unit)? = null,
    isAuthenticated: Boolean = false,
    onRequireSignIn: (() -> Unit)? = null,
    onDownloadClick: ((String) -> Unit)? = null,
    onOpenVideo: ((String) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val relatedState by viewModel.relatedArticles.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadFailureMessage = stringResource(R.string.software_download_error)
    val videoFailureMessage = stringResource(R.string.article_video_error)
    val openDownload = onDownloadClick
        ?: rememberExternalUrlLauncher(snackbarHostState = snackbarHostState, failureMessage = downloadFailureMessage)
    val openVideo = onOpenVideo
        ?: rememberExternalUrlLauncher(snackbarHostState = snackbarHostState, failureMessage = videoFailureMessage)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    relatedState = relatedState,
                    onRelatedArticleClick = onRelatedArticleClick ?: {},
                    onOpenDownload = openDownload,
                    onOpenVideo = openVideo,
                )
            }
        }
    }
}

@Composable
private fun VideoPreviewCard(
    video: VideoRef,
    onOpen: (String) -> Unit,
) {
    val url = video.url?.trim().orEmpty()
    if (!isSafeVideoUrl(url)) return

    Surface(
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(TechVisionSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.article_video),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TechGradientButton(
                text = stringResource(R.string.article_video_watch),
                onClick = { onOpen(url) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RelatedArticlesSection(
    state: UiState<List<ArticleCardModel>>,
    onArticleClick: (String) -> Unit,
) {
    when (state) {
        is UiState.Loading -> {
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            LoadingState(modifier = Modifier.fillMaxWidth())
        }
        is UiState.Error -> {
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            Text(
                text = state.message.ifBlank { stringResource(R.string.related_articles_error) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) return
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            Text(
                text = stringResource(R.string.related_articles_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(TechVisionSpacing.Sm))
            state.data.forEach { related ->
                ArticleCard(
                    article = related,
                    variant = ArticleCardVariant.Standard,
                    onClick = { onArticleClick(related.slug) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.size(TechVisionSpacing.Sm))
            }
        }
        is UiState.Empty -> Unit
    }
}

private fun isSafeVideoUrl(url: String): Boolean {
    val clean = url.trim()
    if (clean.isEmpty()) return false
    return clean.startsWith("https://") || clean.startsWith("http://")
}

/**
 * Opens a pre-validated `http(s)` URL in the system browser, guarding against
 * launch failures (for example when no browser or app can handle the intent)
 * so the screen never crashes. A failed launch surfaces a localized snackbar;
 * the user can simply tap the action again to retry. Used for both the
 * software download card and the article video watch action.
 */
@Composable
private fun rememberExternalUrlLauncher(
    snackbarHostState: SnackbarHostState,
    failureMessage: String,
): (String) -> Unit {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, failureMessage, uriHandler, scope) {
        { url ->
            val opened = runCatching { uriHandler.openUri(url) }.isSuccess
            if (!opened) {
                scope.launch { snackbarHostState.showSnackbar(failureMessage) }
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

@Composable
private fun SoftwareDownloadCard(
    software: SoftwareSummary,
    onOpenDownload: (String) -> Unit,
) {
    val name = software.name.trim()
    if (name.isEmpty()) return
    val downloadUrl = DownloadUrlPolicy.normalize(software.downloadUrl)

    Surface(
        shape = RoundedCornerShape(TechVisionRadii.Lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(TechVisionSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(TechVisionSpacing.Sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 40.dp)
                        .clip(RoundedCornerShape(TechVisionRadii.Sm))
                        .background(brush = MaterialTheme.colorScheme.brandGradient()),
                )
                Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.article_software, name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    software.version?.takeIf { it.isNotBlank() }?.let { version ->
                        Text(
                            text = stringResource(R.string.software_version, version.trim()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // External downloads are the only supported flow: the link is
            // validated as http(s) before it is handed to the system browser.
            // The backend does not expose whether a download requires
            // authentication, so the card never presents an auth prompt — the
            // download behaves exactly like reading the article (public).
            if (downloadUrl != null) {
                TechGradientButton(
                    text = stringResource(R.string.download),
                    onClick = { onOpenDownload(downloadUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("software_download"),
                )
                Text(
                    text = stringResource(R.string.software_opens_external),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.software_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleDetailContent(
    article: Article,
    saveError: AuthError?,
    relatedState: UiState<List<ArticleCardModel>>,
    onRelatedArticleClick: (String) -> Unit,
    onOpenDownload: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
) {
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

        article.video?.takeIf { it.url?.let(::isSafeVideoUrl) == true }?.let { video ->
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            VideoPreviewCard(video = video, onOpen = onOpenVideo)
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

        RelatedArticlesSection(
            state = relatedState,
            onArticleClick = onRelatedArticleClick,
        )

        article.software?.let { software ->
            Spacer(modifier = Modifier.size(TechVisionSpacing.Md))
            SoftwareDownloadCard(software = software, onOpenDownload = onOpenDownload)
        }
    }
}