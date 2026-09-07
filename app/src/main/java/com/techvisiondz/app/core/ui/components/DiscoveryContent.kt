package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.techvisiondz.app.core.ui.UiState

/**
 * Renders a [UiState] for a list-based discovery screen (categories, authors,
 * tags and their article feeds) in one place:
 *   - LoadingState while loading
 *   - ErrorState with [onRetry] on failure
 *   - EmptyState for an explicitly empty result set or an empty successful list
 *   - [content] for the non-empty successful list
 */
@Composable
fun <T> DiscoveryContent(
    state: UiState<List<T>>,
    onRetry: () -> Unit,
    errorFallback: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    content: @Composable (List<T>) -> Unit,
) {
    val boxModifier = modifier.fillMaxSize()
    when (val current = state) {
        is UiState.Loading -> LoadingState(modifier = boxModifier)

        is UiState.Error -> ErrorState(
            message = current.message.ifBlank { errorFallback },
            modifier = boxModifier,
            onRetry = onRetry,
        )

        is UiState.Empty -> EmptyState(message = emptyMessage, modifier = boxModifier)

        is UiState.Success ->
            if (current.data.isEmpty()) {
                EmptyState(message = emptyMessage, modifier = boxModifier)
            } else {
                content(current.data)
            }
    }
}