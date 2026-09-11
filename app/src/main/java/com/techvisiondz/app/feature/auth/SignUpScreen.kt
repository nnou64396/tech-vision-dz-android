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
 * Sign Up screen. When email confirmation is enabled the screen swaps to a
 * success panel explaining the confirmation step instead of signing the user in.
 */
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onBackToSignInClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.signUpSucceeded) {
        AuthScreen(
            title = stringResource(R.string.auth_sign_up_confirmation_title),
            subtitle = stringResource(
                R.string.auth_sign_up_confirmation_message,
                uiState.signedUpEmail,
            ),
        ) {
            AuthLinkAction(
                label = stringResource(R.string.auth_back_to_sign_in),
                onClick = onBackToSignInClick,
                modifier = Modifier.testTag("sign_up_success"),
            )
        }
        return
    }

    AuthScreen(
        title = stringResource(R.string.auth_sign_up_title),
        subtitle = stringResource(R.string.auth_sign_up_subtitle),
    ) {
        AuthFormError(error = uiState.authError)
        AuthField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.auth_email),
            errorMessage = uiState.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.testTag("sign_up_email"),
        )
        AuthField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.auth_password),
            errorMessage = uiState.passwordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            modifier = Modifier.testTag("sign_up_password"),
        )
        AuthField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = stringResource(R.string.auth_confirm_password),
            errorMessage = uiState.confirmPasswordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::signUp,
            isPassword = true,
            modifier = Modifier.testTag("sign_up_confirm_password"),
        )
        AuthPrimaryButton(
            text = stringResource(R.string.auth_sign_up),
            onClick = viewModel::signUp,
            isLoading = uiState.isLoading,
            loadingContentDescription = stringResource(R.string.auth_sign_up_loading),
            modifier = Modifier.testTag("sign_up_submit"),
        )
        AuthLinkAction(
            label = stringResource(R.string.auth_have_account),
            onClick = onBackToSignInClick,
            modifier = Modifier.testTag("sign_up_back_to_sign_in"),
        )
    }
}