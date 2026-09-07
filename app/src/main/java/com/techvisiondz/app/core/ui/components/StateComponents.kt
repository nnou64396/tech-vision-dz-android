package com.techvisiondz.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.ui.theme.TechVisionDzTheme

/** Centered loading indicator used while data is being fetched. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

/** Centered error message with an optional retry action. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    StateMessage(
        title = message,
        modifier = modifier,
        action = onRetry?.let { retry ->
            {
                Button(onClick = retry) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        },
    )
}

/** Empty state shown when a request succeeds but returns no content. */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    StateMessage(
        title = message,
        modifier = modifier,
    )
}

@Composable
private fun StateMessage(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
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