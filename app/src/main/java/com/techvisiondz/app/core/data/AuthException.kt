package com.techvisiondz.app.core.data

/**
 * Domain failure reported by the authentication data layer (sign-in, sign-up,
 * password recovery).
 *
 * Mirrors [DataException]'s contract but stays auth-specific so the UI can map
 * each failure to a localized, user-readable message without ever exposing the
 * raw Supabase SDK error or the underlying server text. Tokens are never part
 * of any [AuthException].
 *
 * The categories cover the common GoTrue failures the app surfaces:
 * invalid credentials, invalid email, weak password, already-registered email,
 * unconfirmed email, rate limiting, and network/server failures.
 */
sealed class AuthException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    data class InvalidCredentials(override val cause: Throwable? = null) :
        AuthException("Invalid email or password", cause)

    /** The email address was rejected as malformed. */
    data class InvalidEmail(override val cause: Throwable? = null) :
        AuthException("Invalid email address", cause)

    /** Sign-up password does not meet the backend's strength rules. */
    data class WeakPassword(override val cause: Throwable? = null) :
        AuthException("Password is too weak", cause)

    /** Sign-up with an email that already has an account. */
    data class EmailAlreadyRegistered(override val cause: Throwable? = null) :
        AuthException("Email already registered", cause)

    /** The account exists but its email has not been confirmed yet. */
    data class EmailNotConfirmed(override val cause: Throwable? = null) :
        AuthException("Email not confirmed", cause)

    /** Auth endpoint rate-limited the request. */
    data class RateLimited(override val cause: Throwable? = null) :
        AuthException("Too many attempts, try again later", cause)

    /** Connectivity/timeout failure reaching the auth backend. */
    data class Network(override val cause: Throwable? = null) :
        AuthException("Could not reach the server. Check your connection", cause)

    /** The auth backend reported a server-side failure. */
    data class Server(override val cause: Throwable? = null) :
        AuthException("The server reported an error", cause)

    /** Anything else that is not classified above. */
    data class Unknown(override val cause: Throwable? = null) :
        AuthException("Something went wrong", cause)
}