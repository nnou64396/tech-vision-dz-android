package com.techvisiondz.app.feature.auth

import androidx.annotation.StringRes
import com.techvisiondz.app.R

/**
 * Field-level validation failure shown against a form input. Screens map the
 * error to its own localized string resource; no user-facing text is hardcoded
 * in composables or ViewModels.
 */
enum class FieldError(@StringRes val messageRes: Int) {
    Required(R.string.auth_error_required),
    InvalidEmail(R.string.auth_error_invalid_email),
    PasswordTooShort(R.string.auth_error_password_too_short),
    PasswordsDoNotMatch(R.string.auth_error_passwords_mismatch),
}

/**
 * Minimum password length enforced locally before a request is sent. Matches a
 * reasonable baseline; the backend may still reject weaker passwords, which is
 * reported through [com.techvisiondz.app.core.data.AuthException.WeakPassword].
 */
const val MIN_PASSWORD_LENGTH = 8

// Deliberately simple: a single @ with a non-empty local part and a dotted
// domain. Enough to catch obvious typos before they reach the server.
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Validates an email address and returns the [FieldError] to show, or null. */
fun validateEmail(email: String): FieldError? {
    val trimmed = email.trim()
    return when {
        trimmed.isEmpty() -> FieldError.Required
        !EMAIL_REGEX.matches(trimmed) -> FieldError.InvalidEmail
        else -> null
    }
}

/** Validates that a field is non-blank (e.g. the sign-in password). */
fun validateRequired(value: String): FieldError? =
    if (value.isBlank()) FieldError.Required else null

/** Validates a password (non-empty and long enough) and returns the error or null. */
fun validatePassword(password: String): FieldError? = when {
    password.isEmpty() -> FieldError.Required
    password.length < MIN_PASSWORD_LENGTH -> FieldError.PasswordTooShort
    else -> null
}

/** Validates the confirmation field against the chosen password. */
fun validateConfirmPassword(password: String, confirmPassword: String): FieldError? = when {
    confirmPassword.isEmpty() -> FieldError.Required
    confirmPassword != password -> FieldError.PasswordsDoNotMatch
    else -> null
}