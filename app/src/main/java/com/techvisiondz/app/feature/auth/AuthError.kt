package com.techvisiondz.app.feature.auth

import androidx.annotation.StringRes
import com.techvisiondz.app.R
import com.techvisiondz.app.core.data.AuthException

/**
 * A signing-in/signing-up/reset failure the user sees, resolved from an
 * [AuthException] so screens only ever display their own localized string
 * resources — never a raw SDK message or server error.
 */
enum class AuthError(@StringRes val messageRes: Int) {
    InvalidCredentials(R.string.auth_error_invalid_credentials),
    InvalidEmail(R.string.auth_error_invalid_email),
    WeakPassword(R.string.auth_error_weak_password),
    EmailAlreadyRegistered(R.string.auth_error_email_already_registered),
    EmailNotConfirmed(R.string.auth_error_email_not_confirmed),
    RateLimited(R.string.auth_error_rate_limited),
    Network(R.string.auth_error_network),
    Server(R.string.auth_error_server),
    Unknown(R.string.auth_error_unknown),
}

/** Converts a domain-level [AuthException] into the user-facing [AuthError]. */
internal fun Throwable.toAuthError(): AuthError = when (this) {
    is AuthException.InvalidCredentials -> AuthError.InvalidCredentials
    is AuthException.InvalidEmail -> AuthError.InvalidEmail
    is AuthException.WeakPassword -> AuthError.WeakPassword
    is AuthException.EmailAlreadyRegistered -> AuthError.EmailAlreadyRegistered
    is AuthException.EmailNotConfirmed -> AuthError.EmailNotConfirmed
    is AuthException.RateLimited -> AuthError.RateLimited
    is AuthException.Network -> AuthError.Network
    is AuthException.Server -> AuthError.Server
    is AuthException.Unknown -> AuthError.Unknown
    // Nothing else should reach the auth screens; treat unexpected domain
    // failures the same as an unclassified auth error.
    else -> AuthError.Unknown
}