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
 * UI state for the Sign In screen.
 *
 * @property email The raw email the user is typing.
 * @property password The raw password the user is typing.
 * @property emailError Field validation error for the email, or null.
 * @property passwordError Field validation error for the password, or null.
 * @property authError Authentication failure to surface, or null.
 * @property isLoading True while a sign-in request is in flight.
 * @property signedIn True once the repository confirmed a session; the
 *   navigation host reacts to [com.techvisiondz.app.core.data.AuthState.Authenticated].
 */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val authError: AuthError? = null,
    val isLoading: Boolean = false,
    val signedIn: Boolean = false,
)

/**
 * Sign In state holder. Validates locally before calling the repository so
 * obviously invalid input never reaches Supabase, then reports auth failures
 * through [SignInUiState.authError] as a user-facing [AuthError].
 */
class SignInViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, authError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, authError = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = validateEmail(state.email)
        val passwordError = validateRequired(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, authError = null)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, authError = null) }
        viewModelScope.launch {
            try {
                repository.signInWithEmail(state.email, state.password)
                _uiState.update { it.copy(isLoading = false, signedIn = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, authError = e.toAuthError()) }
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SignInViewModel(repository) }
        }
    }
}