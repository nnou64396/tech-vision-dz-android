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
 * Sign In screen — the entry point of the authentication flow whenever no
 * active session exists. Fills the TECH VISION DZ identity: brand mark, title,
 * email + password fields, a gradient submit button, and dismissal to
 * password recovery / sign-up. Navigation out of this screen happens through
 * the callbacks; the navigation host reacts to
 * [com.techvisiondz.app.core.data.AuthState.Authenticated] once sign-in succeeds.
 */
@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    AuthScreen(
        title = stringResource(R.string.auth_sign_in_title),
        subtitle = stringResource(R.string.auth_sign_in_subtitle),
    ) {
        AuthFormError(error = uiState.authError)
        AuthField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.auth_email),
            errorMessage = uiState.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.testTag("sign_in_email"),
        )
        AuthField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.auth_password),
            errorMessage = uiState.passwordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::signIn,
            isPassword = true,
            modifier = Modifier.testTag("sign_in_password"),
        )
        AuthPrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = viewModel::signIn,
            isLoading = uiState.isLoading,
            loadingContentDescription = stringResource(R.string.auth_sign_in_loading),
            modifier = Modifier.testTag("sign_in_submit"),
        )
        AuthLinkAction(
            label = stringResource(R.string.auth_forgot_password),
            onClick = onForgotPasswordClick,
            modifier = Modifier.testTag("sign_in_forgot_password"),
        )
        AuthLinkAction(
            label = stringResource(R.string.auth_no_account),
            onClick = onSignUpClick,
            modifier = Modifier.testTag("sign_in_sign_up"),
        )
    }
}