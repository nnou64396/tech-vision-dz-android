package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Deterministic [AuthRepository] for unit tests. No network or real Supabase
 * session is involved. Tests drive the state through [setSession] /
 * [signOut] helpers exactly as a signed-out app behaves: the flow starts at
 * [AuthState.Loading], then settles to [AuthState.Unauthenticated]; tests can
 * move it to [AuthState.Authenticated].
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

    override suspend fun signOut() {
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