package com.techvisiondz.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.BuildConfig
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.BrandMark
import com.techvisiondz.app.core.util.DownloadUrlPolicy
import com.techvisiondz.app.ui.theme.TechVisionSpacing
import kotlinx.coroutines.launch

private const val OFFICIAL_WEBSITE_URL = "https://techvisiondz.com"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val websiteFailureMessage = stringResource(R.string.settings_website_error)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val safeWebsiteUrl = remember { DownloadUrlPolicy.normalize(OFFICIAL_WEBSITE_URL) }

    val openWebsite = {
        if (safeWebsiteUrl == null) {
            scope.launch { snackbarHostState.showSnackbar(websiteFailureMessage) }
        } else {
            runCatching { uriHandler.openUri(safeWebsiteUrl) }
                .onFailure {
                    scope.launch { snackbarHostState.showSnackbar(websiteFailureMessage) }
                }
        }
        Unit
    }

    BackTopBarScreen(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TechVisionSpacing.Lg)
                .padding(top = TechVisionSpacing.Xl, bottom = TechVisionSpacing.Xl),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_card"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 56.dp)
                        Spacer(modifier = Modifier.width(TechVisionSpacing.Md))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.home_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))
                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            // Developer attribution
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_developer_attribution"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
                    Text(
                        text = stringResource(R.string.developer_attribution),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(TechVisionSpacing.Xs))
                    Text(
                        text = "Designed & Engineered for Tech Vision DZ Algeria.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            if (safeWebsiteUrl != null) {
                Surface(
                    onClick = openWebsite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_website_button"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = TechVisionSpacing.Lg,
                            vertical = TechVisionSpacing.Md,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.about_website),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = OFFICIAL_WEBSITE_URL,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
