package com.techvisiondz.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Sign Up screen.
 *
 * @property email The raw email the user is typing.
 * @property password The raw password the user is typing.
 * @property confirmPassword The raw confirmation password the user is typing.
 * @property emailError Field validation error for the email, or null.
 * @property passwordError Field validation error for the password, or null.
 * @property confirmPasswordError Field validation error for confirmation, or null.
 * @property authError Authentication failure to surface, or null.
 * @property isLoading True while a sign-up request is in flight.
 * @property signUpSucceeded True once the repository accepted the account; the
 *   screen then explains the email-confirmation step.
 * @property signedUpEmail The email the user registered, for the success hint.
 */
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmPasswordError: FieldError? = null,
    val authError: AuthError? = null,
    val isLoading: Boolean = false,
    val signUpSucceeded: Boolean = false,
    val signedUpEmail: String = "",
)

/**
 * Sign Up state holder. Validates locally (email format, password strength and
 * confirmation match) before calling the repository, then reports auth
 * failures through [SignUpUiState.authError] as a user-facing [AuthError].
 */
class SignUpViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, authError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, confirmPasswordError = null, authError = null)
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, authError = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)
        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    authError = null,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }
        viewModelScope.launch {
            try {
                val signedUpEmail = state.email.trim()
                repository.signUpWithEmail(signedUpEmail, state.password)
                _uiState.update {
                    it.copy(isLoading = false, signUpSucceeded = true, signedUpEmail = signedUpEmail)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, authError = e.toAuthError()) }
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SignUpViewModel(repository) }
        }
    }
}