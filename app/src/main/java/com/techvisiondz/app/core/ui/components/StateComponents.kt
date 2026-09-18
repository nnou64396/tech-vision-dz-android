package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/** Branded loading state: the site's spinner tone + a loading label. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Branded error state with an icon glyph, message and a gradient Retry button. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StateGlyph(
            icon = Icons.Filled.Warning,
            tint = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        onRetry?.let { retry ->
            Spacer(modifier = Modifier.height(20.dp))
            TechGradientButton(text = stringResource(R.string.retry), onClick = retry)
        }
    }
}

/** Branded empty state anchored by the gradient wordmark. */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        CenterBrandMark(
            message = message,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(24.dp),
        )
    }
}

/**
 * Footer of a paginated article list. While [hasMore] is true it requests the
 * next page as soon as it scrolls into view (auto load-more), shows the
 * loading-more indicator while a page is in flight and, on failure, keeps the
 * already-loaded cards and offers an accessible Retry action. When [error] is
 * set no automatic request is made so the user can deliberately retry.
 */
@Composable
fun LoadMoreFooter(
    hasMore: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(isLoadingMore, error) {
        if (hasMore && !isLoadingMore && error == null) {
            onLoadMore()
        }
    }

    if (error != null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = TechVisionSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = error.ifBlank { stringResource(R.string.load_more_error) },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
            TechGradientButton(
                text = stringResource(R.string.retry),
                onClick = onRetry,
                modifier = Modifier.testTag("load_more_retry"),
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = TechVisionSpacing.Lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
            Text(
                text = stringResource(R.string.load_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StateGlyph(icon: ImageVector, tint: Color, containerColor: Color) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        modifier = Modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    TechVisionDzTheme {
        ErrorState(message = "تعذر تحميل البيانات", onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    TechVisionDzTheme {
        EmptyState(message = "لا توجد عناصر بعد")
    }
}