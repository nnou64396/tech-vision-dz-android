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
 * UI state for the Forgot Password screen.
 *
 * @property email The raw email the user is typing.
 * @property emailError Field validation error for the email, or null.
 * @property authError Authentication failure to surface, or null.
 * @property isLoading True while the reset request is in flight.
 * @property resetSent True once the repository accepted the request; the screen
 *   then explains that a reset link was sent (or that an account lookup matched).
 * @property resetEmail The email the user asked to reset, for the success hint.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val authError: AuthError? = null,
    val isLoading: Boolean = false,
    val resetSent: Boolean = false,
    val resetEmail: String = "",
)

/**
 * Forgot Password state holder. Validates the email format locally before
 * calling the repository and reports failures through
 * [ForgotPasswordUiState.authError] as a user-facing [AuthError].
 */
class ForgotPasswordViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, authError = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError, authError = null) }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }
        viewModelScope.launch {
            try {
                val resetEmail = state.email.trim()
                repository.resetPassword(resetEmail)
                _uiState.update {
                    it.copy(isLoading = false, resetSent = true, resetEmail = resetEmail)
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
            initializer { ForgotPasswordViewModel(repository) }
        }
    }
}