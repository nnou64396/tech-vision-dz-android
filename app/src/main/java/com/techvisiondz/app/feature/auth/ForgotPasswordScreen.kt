package com.techvisiondz.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.techvisiondz.app.R

/**
 * Forgot Password screen. Submits the email to the auth backend and, on
 * success, swaps to an explanation that a reset link was sent (the backend
 * answers the same way for unknown accounts to avoid enumeration).
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackToSignInClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.resetSent) {
        AuthScreen(
            title = stringResource(R.string.auth_reset_sent_title),
            subtitle = stringResource(R.string.auth_reset_sent_message, uiState.resetEmail),
        ) {
            AuthLinkAction(
                label = stringResource(R.string.auth_back_to_sign_in),
                onClick = onBackToSignInClick,
                modifier = Modifier.testTag("forgot_success"),
            )
        }
        return
    }

    AuthScreen(
        title = stringResource(R.string.auth_forgot_password_title),
        subtitle = stringResource(R.string.auth_forgot_password_subtitle),
    ) {
        AuthFormError(error = uiState.authError)
        AuthField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.auth_email),
            errorMessage = uiState.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::resetPassword,
            modifier = Modifier.testTag("forgot_email"),
        )
        AuthPrimaryButton(
            text = stringResource(R.string.auth_reset_password),
            onClick = viewModel::resetPassword,
            isLoading = uiState.isLoading,
            loadingContentDescription = stringResource(R.string.auth_reset_loading),
            modifier = Modifier.testTag("forgot_submit"),
        )
        AuthLinkAction(
            label = stringResource(R.string.auth_back_to_sign_in),
            onClick = onBackToSignInClick,
            modifier = Modifier.testTag("forgot_back_to_sign_in"),
        )
    }
}