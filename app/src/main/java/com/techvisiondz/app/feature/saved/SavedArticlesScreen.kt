package com.techvisiondz.app.feature.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.ArticleList
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState

/**
 * Saved (bookmarked) articles screen for authenticated users.
 *
 * Reuses the shared [ArticleList] cards so the visual system is identical to
 * the home feed and discovery listings. The list reloads on every resume so a
 * bookmark removed from an article details screen disappears from the list when
 * the user navigates back.
 */
@Composable
fun SavedArticlesScreen(
    viewModel: SavedArticlesViewModel,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh when the screen becomes visible again (returning from a detail
    // screen) so unsaves are reflected. The first resume is skipped: the
    // ViewModel already performs the initial load. The refresh keeps the
    // existing list visible and swaps in the new content once it settles.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && viewModel.hasLoadedOnce) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackTopBarScreen(title = stringResource(R.string.saved_articles_title), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()

                is UiState.Error -> ErrorState(
                    message = state.message.ifBlank { stringResource(R.string.saved_articles_error) },
                    onRetry = viewModel::loadSaved,
                )

                is UiState.Empty -> EmptyState(
                    message = stringResource(R.string.saved_articles_empty),
                )

                is UiState.Success -> ArticleList(
                    articles = state.data,
                    onArticleClick = onArticleClick,
                )
            }
        }
    }
}