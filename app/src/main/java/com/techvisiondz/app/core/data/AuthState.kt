package com.techvisiondz.app.core.data

/**
 * The authentication state of the app, mapped from the Supabase session status.
 *
 * Only the identity information the app's architecture needs is exposed. No
 * access/refresh tokens or authorization headers are ever placed on this state;
 * they stay owned by the Supabase Auth SDK.
 */
sealed interface AuthState {

    /** The session has not finished loading from the SDK's storage yet. */
    data object Loading : AuthState

    /** No active session; the user is signed out or has never signed in. */
    data object Unauthenticated : AuthState

    /** An active session exists and exposes the authenticated user identity. */
    data class Authenticated(val user: AuthenticatedUserInfo) : AuthState

    /** Auth observation failed (e.g. session refresh failure) and recovered locally. */
    data class Error(val message: String) : AuthState
}

/**
 * Minimal identity of the authenticated user, mapped from the Supabase user.
 *
 * Deliberately excludes tokens: the SDK retains them internally and attaches
 * them to requests automatically.
 *
 * @property id The Supabase `uuid` of the auth user.
 * @property email The user's primary email, when confirmed.
 * @property displayName The user's display name, when the app/profile provides one.
 */
data class AuthenticatedUserInfo(
    val id: String,
    val email: String?,
    val displayName: String?,
)