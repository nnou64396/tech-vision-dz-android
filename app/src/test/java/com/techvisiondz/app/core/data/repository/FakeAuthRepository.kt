package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay

/**
 * Deterministic [AuthRepository] for unit tests. No network or real Supabase
 * session is involved. Tests drive the state through [setSession] /
 * [signOut] helpers exactly as a signed-out app behaves: the flow starts at
 * [AuthState.Loading], then settles to [AuthState.Unauthenticated]; tests can
 * move it to [AuthState.Authenticated].
 *
 * Sign-in/sign-up/reset calls record their arguments and can be configured to
 * fail via [signInError] / [signUpError] / [resetPasswordError].
 */
class FakeAuthRepository(
    initialUser: AuthenticatedUserInfo? = null,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)

    override val authState: Flow<AuthState> = _authState.asStateFlow()

    /** The last emitted state, driving [currentAuthState]/[currentUserOrNull]. */
    var currentUser: AuthenticatedUserInfo? = initialUser
        private set

    /** Number of times [signOut] was called. */
    var signOutCalls: Int = 0
        private set

    /** Whether the last sign-out threw a failure. */
    var signOutError: Exception? = null

    /** Number of times [signInWithEmail] was called. */
    var signInCalls: Int = 0
        private set

    /** When set, [signInWithEmail]/[signUpWithEmail]/[resetPassword] throw it. */
    var signInError: Exception? = null
    var signUpError: Exception? = null
    var resetPasswordError: Exception? = null

    /** Last arguments observed by the auth calls. */
    var lastSignInEmail: String? = null
        private set
    var lastSignInPassword: String? = null
        private set
    var lastSignUpEmail: String? = null
        private set
    var lastSignUpPassword: String? = null
        private set
    var lastResetEmail: String? = null
        private set

    /** Whether the next [signUpWithEmail] activation should be counted as sent. */
    var signUpCalls: Int = 0
        private set
    var resetPasswordCalls: Int = 0
        private set

    /** Simulates the SDK settling without a session (initial Loading → Unauth). */
    fun completeInitializationNotAuthenticated() {
        _authState.value = AuthState.Unauthenticated
        currentUser = null
    }

    /** Simulates a successful login/session restoration. */
    fun setSession(user: AuthenticatedUserInfo) {
        currentUser = user
        _authState.value = AuthState.Authenticated(user)
    }

    /** Simulates an auth observation failure (e.g. refresh failure). */
    fun fail(message: String = "Your session could not be refreshed") {
        currentUser = null
        _authState.value = AuthState.Error(message)
    }

    override fun currentAuthState(): AuthState = _authState.value

    override fun currentUserOrNull(): AuthenticatedUserInfo? = currentUser

    override suspend fun signInWithEmail(email: String, password: String) {
        delay(1)
        signInCalls++
        lastSignInEmail = email
        lastSignInPassword = password
        signInError?.let { throw it }
        setSession(
            sampleAuthenticatedUser(
                id = "user-$signInCalls",
                email = email,
                displayName = null,
            ),
        )
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        delay(1)
        signUpCalls++
        lastSignUpEmail = email
        lastSignUpPassword = password
        signUpError?.let { throw it }
    }

    override suspend fun resetPassword(email: String) {
        delay(1)
        resetPasswordCalls++
        lastResetEmail = email
        resetPasswordError?.let { throw it }
    }

    override suspend fun signOut() {
        delay(1)
        signOutCalls++
        signOutError?.let { throw it }
        currentUser = null
        _authState.update { AuthState.Unauthenticated }
    }
}

fun sampleAuthenticatedUser(
    id: String = "user-1",
    email: String = "reader@example.com",
    displayName: String? = "Sarah",
) = AuthenticatedUserInfo(
    id = id,
    email = email,
    displayName = displayName,
)