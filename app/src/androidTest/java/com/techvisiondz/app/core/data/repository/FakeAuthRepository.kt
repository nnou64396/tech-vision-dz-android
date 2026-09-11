package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthState
import com.techvisiondz.app.core.data.AuthException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay

/**
 * Deterministic [AuthRepository] for androidTest navigation tests.
 *
 * In contrast to the unit-test fake this one starts immediately at
 * [initialState] — no [AuthState.Loading] → settling delay — so compose tests
 * can assert the expected screen in a single `waitForIdle()`.
 */
class FakeAuthRepository(
    initialState: AuthState = AuthState.Loading,
) : AuthRepository {

    private val _authState = MutableStateFlow(initialState)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    override fun currentAuthState(): AuthState = _authState.value

    override fun currentUserOrNull(): AuthenticatedUserInfo? =
        (currentAuthState() as? AuthState.Authenticated)?.user

    var signInCalls: Int = 0
        private set
    var signInError: Exception? = null
    var signUpCalls: Int = 0
        private set
    var signUpError: Exception? = null
    var resetPasswordCalls: Int = 0
        private set
    var resetPasswordError: Exception? = null
    var lastSignInEmail: String? = null
        private set
    var lastSignInPassword: String? = null
        private set
    var signOutCalls: Int = 0
        private set
    var signOutError: Exception? = null

    override suspend fun signInWithEmail(email: String, password: String) {
        delay(1)
        signInCalls++
        lastSignInEmail = email
        lastSignInPassword = password
        signInError?.let { throw it }
        _authState.value = AuthState.Authenticated(
            AuthenticatedUserInfo(
                id = "test-user",
                email = email.trim(),
                displayName = null,
            ),
        )
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        delay(1)
        signUpCalls++
        signUpError?.let { throw it }
    }

    override suspend fun resetPassword(email: String) {
        delay(1)
        resetPasswordCalls++
        resetPasswordError?.let { throw it }
    }

    override suspend fun signOut() {
        delay(1)
        signOutCalls++
        signOutError?.let { throw it }
        _authState.update { AuthState.Unauthenticated }
    }

    /**
     * Synchronously flips the auth state to [AuthState.Unauthenticated]
     * without the [signOut] delay or call-count increment — useful for testing
     * navigation-guard behaviour in Compose tests without going through the UI.
     */
    fun forceUnauthenticated() {
        _authState.value = AuthState.Unauthenticated
    }

    companion object {
        fun authenticated(
            user: AuthenticatedUserInfo = sampleAuthenticatedUser(),
        ): FakeAuthRepository = FakeAuthRepository(AuthState.Authenticated(user))

        fun unauthenticated(): FakeAuthRepository = FakeAuthRepository(AuthState.Unauthenticated)
    }
}

fun sampleAuthenticatedUser(
    id: String = "test-user",
    email: String = "test@example.com",
    displayName: String? = null,
): AuthenticatedUserInfo = AuthenticatedUserInfo(
    id = id,
    email = email,
    displayName = displayName,
)
