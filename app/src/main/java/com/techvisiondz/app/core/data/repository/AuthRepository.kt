package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthException
import com.techvisiondz.app.core.data.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the Supabase Auth (GoTrue) session for the TECH VISION DZ app.
 *
 * Exposes session observation, the current identity, email/password sign-in,
 * sign-up, password recovery and sign-out. Authentication failures are reported
 * as [AuthException] so the UI never sees SDK-specific types or raw server
 * messages.
 *
 * Credentials are consumed here and never retained, logged or exposed; tokens
 * remain owned by the backing implementation.
 */
interface AuthRepository {

    /** Continuously emits the current [AuthState]. Starts at [AuthState.Loading]. */
    val authState: Flow<AuthState>

    /** The current [AuthState] at the moment it is queried. */
    fun currentAuthState(): AuthState

    /** Identity of the signed-in user, or null when signed out / still loading. */
    fun currentUserOrNull(): AuthenticatedUserInfo?

    /**
     * Signs the user in with email + password. On success the session becomes
     * active and [authState] emits [AuthState.Authenticated]. Throws
     * [AuthException] on invalid credentials, unconfirmed email, rate limiting,
     * network or backend failure.
     */
    suspend fun signInWithEmail(email: String, password: String)

    /**
     * Registers a new account with email + password. When email confirmation is
     * enabled the user stays unauthenticated until they confirm the email. Throws
     * [AuthException] on already-registered email, weak password, invalid email,
     * rate limiting, network or backend failure.
     */
    suspend fun signUpWithEmail(email: String, password: String)

    /**
     * Requests a password-reset email. To avoid account enumeration the backend
     * responds successfully for unknown addresses too; failures are only reported
     * for malformed/invalid emails, rate limiting, network or backend errors.
     *
     * @throws AuthException on failure.
     */
    suspend fun resetPassword(email: String)

    /**
     * Signs the current user out and clears the persisted session. Succeeds even
     * when there is no active session. Throws on network/backend failure.
     */
    suspend fun signOut()
}