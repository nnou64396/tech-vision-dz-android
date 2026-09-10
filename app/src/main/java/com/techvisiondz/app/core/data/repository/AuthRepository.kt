package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the Supabase Auth (GoTrue) session for the TECH VISION DZ app.
 *
 * This foundation phase only exposes session observation, the current identity,
 * and sign-out. Sign-in/sign-up/password recovery are added in later phases.
 *
 * Implementations must never log or expose tokens or credentials.
 */
interface AuthRepository {

    /** Continuously emits the current [AuthState]. Starts at [AuthState.Loading]. */
    val authState: Flow<AuthState>

    /** The current [AuthState] at the moment it is queried. */
    fun currentAuthState(): AuthState

    /** Identity of the signed-in user, or null when signed out / still loading. */
    fun currentUserOrNull(): AuthenticatedUserInfo?

    /**
     * Signs the current user out and clears the persisted session. Succeeds even
     * when there is no active session. Throws on network/backend failure.
     */
    suspend fun signOut()
}