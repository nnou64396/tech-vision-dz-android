package com.techvisiondz.app.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.UiState
import com.techvisiondz.app.core.ui.components.EmptyState
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.ui.theme.TechVisionDzTheme

/**
 * Minimal home placeholder screen.
 *
 * Validates the architecture only: MVVM + StateFlow + UiState + reusable
 * loading/error/empty components. The real home feed (articles from the
 * existing Supabase backend) is implemented in the next phase.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
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

                is UiState.Empty -> EmptyState(message = state.message)

                is UiState.Success -> {
                    if (state.data.articles.isEmpty()) {
                        EmptyState(message = stringResource(R.string.home_empty))
                    } else {
                        HomePlaceholder()
                    }
                }
            }
        }
    }
}

/**
 * Temporary branded placeholder rendered only when real content exists.
 * The article feed UI replaces this in the next phase.
 */
@Composable
private fun HomePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    TechVisionDzTheme {
        HomePlaceholder()
    }
}