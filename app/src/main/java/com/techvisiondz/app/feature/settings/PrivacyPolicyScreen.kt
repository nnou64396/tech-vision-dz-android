package com.techvisiondz.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techvisiondz.app.R
import com.techvisiondz.app.core.ui.components.BackTopBarScreen
import com.techvisiondz.app.ui.theme.TechVisionSpacing

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
) {
    BackTopBarScreen(
        title = stringResource(R.string.privacy_policy_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TechVisionSpacing.Lg)
                .padding(top = TechVisionSpacing.Xl, bottom = TechVisionSpacing.Xl)
                .testTag("privacy_policy_content"),
        ) {
            Text(
                text = stringResource(R.string.privacy_policy_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Xl))

            PrivacySection(
                title = stringResource(R.string.privacy_policy_collection_title),
                body = stringResource(R.string.privacy_policy_collection_body),
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))

            PrivacySection(
                title = stringResource(R.string.privacy_policy_storage_title),
                body = stringResource(R.string.privacy_policy_storage_body),
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))

            PrivacySection(
                title = stringResource(R.string.privacy_policy_network_title),
                body = stringResource(R.string.privacy_policy_network_body),
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))

            PrivacySection(
                title = stringResource(R.string.privacy_policy_security_title),
                body = stringResource(R.string.privacy_policy_security_body),
            )

            Spacer(modifier = Modifier.height(TechVisionSpacing.Lg))

            PrivacySection(
                title = stringResource(R.string.privacy_policy_contact_title),
                body = stringResource(R.string.privacy_policy_contact_body),
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(TechVisionSpacing.Lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(TechVisionSpacing.Sm))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
