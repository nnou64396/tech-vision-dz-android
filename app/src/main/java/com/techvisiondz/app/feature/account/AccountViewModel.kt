package com.techvisiondz.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.techvisiondz.app.core.data.model.UserProfile
import com.techvisiondz.app.core.data.repository.AuthRepository
import com.techvisiondz.app.core.data.repository.ProfileRepository
import com.techvisiondz.app.core.data.repository.SupabaseProfileRepository
import com.techvisiondz.app.feature.auth.AuthError
import com.techvisiondz.app.feature.auth.toAuthError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Account / Profile screen.
 *
 * @property profile The loaded profile to display, or null while not loaded.
 * @property isLoading True while the profile (or a retry) is being loaded.
 * @property isLoggingOut True while a sign-out request is in flight.
 * @property error The last account-level failure to surface (profile load or
 *   sign-out), or null.
 */
data class AccountUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val error: AuthError? = null,
)

/**
 * Account / Profile state holder.
 *
 * Loads the signed-in user's profile from the existing `profiles` table and
 * exposes it as an immutable [StateFlow]. If that query fails, it falls back to
 * the authenticated identity so the user still sees their account details.
 * Sign-out reuses [AuthRepository.signOut]; the navigation host reacts to the
 * resulting [com.techvisiondz.app.core.data.AuthState.Unauthenticated].
 */
class AccountViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /** (Re)loads the own profile row, falling back to the auth identity. */
    fun loadProfile() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = profileRepository.getOwnProfile()
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The auth identity alone is enough to render the account screen;
                // a missing/rejected profiles row must not lock the user out.
                val fallback = authRepository.currentUserOrNull()?.let { user ->
                    UserProfile(
                        id = user.id,
                        email = user.email,
                        displayName = user.displayName,
                    )
                }
                _uiState.update { it.copy(profile = fallback, isLoading = false) }
            }
        }
    }

    /**
     * Signs the current user out. Duplicate taps while a sign-out is already
     * in flight are ignored. The navigation host observes the auth flow.
     */
    fun signOut() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true, error = null) }
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.update { it.copy(isLoggingOut = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggingOut = false, error = e.toAuthError()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            profileRepository: ProfileRepository = SupabaseProfileRepository(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccountViewModel(authRepository, profileRepository) }
        }
    }
}