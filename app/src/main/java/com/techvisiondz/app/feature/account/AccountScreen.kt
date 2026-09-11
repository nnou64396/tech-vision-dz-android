package com.techvisiondz.app.feature.account

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.model.UserProfile
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.core.ui.components.ErrorState
import com.techvisiondz.app.core.ui.components.LoadingState
import com.techvisiondz.app.core.ui.TechVisionIcons
import com.techvisiondz.app.core.ui.formatPublishedAt
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.ui.theme.TechVisionSpacing

/**
 * Account / Profile screen. Shows the signed-in user's identity (avatar when
 * the profile provides one, display name, email, member-since date) and a
 * branded sign-out action. The screen is only reachable while authenticated;
 * the navigation host reacts to sign-out via
 * [com.techvisiondz.app.core.data.AuthState.Unauthenticated].
 */
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    onSavedArticlesClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    BackTopBarScreen(title = stringResource(R.string.account_title), onBack = onBack) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val profile = uiState.profile
            when {
                uiState.isLoading -> LoadingState()

                profile != null -> AccountContent(
                    profile = profile,
                    isLoggingOut = uiState.isLoggingOut,
                    signOutError = uiState.error,
                    onSignOut = viewModel::signOut,
                    onSavedArticlesClick = onSavedArticlesClick,
                )

                else -> ErrorState(
                    message = stringResource(R.string.account_error),
                    onRetry = viewModel::loadProfile,
                )
            }
        }
    }
}

@Composable
private fun AccountContent(
    profile: UserProfile,
    isLoggingOut: Boolean,
    signOutError: AuthError?,
    onSignOut: () -> Unit,
    onSavedArticlesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TechVisionSpacing.Lg)
            .padding(top = TechVisionSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val displayName = profile.displayName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.account_guest)
        Avatar(
            avatarUrl = profile.avatarUrl,
            contentDescription = displayName,
        )
        Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))

        profile.email?.takeIf { it.isNotBlank() }?.let { email ->
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
        }

        Text(
            text = stringResource(
                R.string.account_member_since,
                formatPublishedAt(profile.createdAt.orEmpty()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

        SavedArticlesRow(onClick = onSavedArticlesClick)

        Spacer(modifier = Modifier.weight(1f))

        if (signOutError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = stringResource(signOutError.messageRes),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(TechVisionSpacing.Md),
                )
            }
            Spacer(modifier = Modifier.height(TechVisionSpacing.Md))
        }

        SignOutButton(
            isLoading = isLoggingOut,
            onSignOut = onSignOut,
        )
        Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))
    }
}

/** Navigation row opening the Saved Articles screen. */
@Composable
private fun SavedArticlesRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_saved_articles"),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TechVisionSpacing.Md,
                    vertical = TechVisionSpacing.Md,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TechVisionSpacing.Md),
        ) {
            Icon(
                imageVector = TechVisionIcons.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.saved_articles_menu),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The user's avatar, or a branded person fallback when not provided. */
@Composable
private fun Avatar(
    avatarUrl: String?,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        val url = avatarUrl?.takeIf { it.startsWith("http") }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun SignOutButton(
    isLoading: Boolean,
    onSignOut: () -> Unit,
) {
    Surface(
        onClick = onSignOut,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_sign_out"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(vertical = TechVisionSpacing.Md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.width(TechVisionSpacing.Sm))
            }
            Text(
                text = stringResource(
                    if (isLoading) R.string.account_sign_out_loading else R.string.account_sign_out,
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}