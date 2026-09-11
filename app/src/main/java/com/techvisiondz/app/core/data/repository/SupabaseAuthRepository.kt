package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.AuthException
import com.techvisiondz.app.core.data.AuthState
import com.techvisiondz.app.core.data.toDataException
import com.techvisiondz.app.core.network.SupabaseClientProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Supabase-backed [AuthRepository] for the existing TECH VISION DZ project.
 *
 * Wraps the GoTrue [Auth] plugin:
 *  - the session status [Flow] is mapped to [AuthState] — Loading / Unauth / Auth / Error;
 *  - the current user identity is read from the SDK's current session;
 *  - sign-in/sign-up/password recovery call GoTrue directly;
 *  - sign-out revokes and clears the session.
 *
 * Tokens and credentials are never exposed or logged: they remain owned by the
 * SDK and are attached to requests automatically. The session itself is
 * persisted by the SDK's default `SettingsSessionManager` (SharedPreferences on
 * Android) and reloaded on process restart, so no custom token storage exists.
 *
 * SDK failures are converted to [AuthException] so the UI maps them to
 * localized messages instead of raw server errors.
 */
class SupabaseAuthRepository(
    private val auth: Auth = SupabaseClientProvider.auth,
) : AuthRepository {

    override val authState: Flow<AuthState> = auth.sessionStatus.map { it.toAuthState() }

    override fun currentAuthState(): AuthState = auth.sessionStatus.value.toAuthState()

    override fun currentUserOrNull(): AuthenticatedUserInfo? =
        auth.currentUserOrNull()?.toAuthenticatedUserInfo()

    override suspend fun signInWithEmail(email: String, password: String) {
        runAuth {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        runAuth {
            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }
    }

    override suspend fun resetPassword(email: String) {
        runAuth {
            auth.resetPasswordForEmail(email.trim())
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toDataException()
        }
    }
}

/** Runs an auth call, rethrowing cancellation and converting failures to [AuthException]. */
private suspend fun runAuth(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toAuthException()
    }
}

/**
 * Maps a raw Supabase/Ktor auth failure into an [AuthException].
 *
 * `internal` and deliberately independent of the SDK response objects where
 * possible: the classification of an [AuthErrorCode] + HTTP status is a pure
 * decision so the contract can be unit-tested with synthetic inputs.
 */
internal fun Throwable.toAuthException(): AuthException {
    if (this is CancellationException) throw this
    val rest = this as? RestException
    val errorCode = (this as? AuthRestException)?.errorCode
    if (rest != null) {
        return mapAuthFailure(errorCode, rest.statusCode)
    }
    return when (this) {
        is HttpRequestTimeoutException, is HttpRequestException, is IOException ->
            AuthException.Network(this)
        else -> AuthException.Unknown(this)
    }
}

/**
 * Classifies a GoTrue [AuthErrorCode] (+ HTTP status) into an [AuthException].
 * `internal` and pure so synthetic codes/statuses are unit-testable.
 */
internal fun mapAuthFailure(errorCode: AuthErrorCode?, statusCode: Int?): AuthException {
    val classified = when (errorCode) {
        AuthErrorCode.InvalidCredentials,
        AuthErrorCode.UserNotFound,
        AuthErrorCode.EmailAddressNotAuthorized,
        -> AuthException.InvalidCredentials()

        AuthErrorCode.EmailNotConfirmed -> AuthException.EmailNotConfirmed()

        AuthErrorCode.EmailExists,
        AuthErrorCode.UserAlreadyExists,
        -> AuthException.EmailAlreadyRegistered()

        AuthErrorCode.WeakPassword -> AuthException.WeakPassword()

        AuthErrorCode.OverRequestRateLimit,
        AuthErrorCode.OverEmailSendRateLimit,
        AuthErrorCode.OverSmsSendRateLimit,
        -> AuthException.RateLimited()

        AuthErrorCode.ValidationFailed,
        AuthErrorCode.EmailAddressInvalid,
        -> AuthException.InvalidEmail()

        null -> null
        else -> null
    }
    if (classified != null) return classified

    return when (statusCode) {
        400 -> AuthException.InvalidEmail()
        401, 403, 404 -> AuthException.InvalidCredentials()
        429 -> AuthException.RateLimited()
        500, 502, 503, 504 -> AuthException.Server()
        else -> AuthException.Unknown()
    }
}

/**
 * Maps a raw [SessionStatus] into the app's [AuthState].
 *
 * `internal` and pure on purpose so the mapping contract can be unit-tested
 * with synthetic statuses, without any network or Supabase client.
 */
internal fun SessionStatus.toAuthState(): AuthState = when (this) {
    is SessionStatus.Initializing -> AuthState.Loading

    is SessionStatus.Authenticated -> session.user?.toAuthenticatedUserInfo().let { user ->
        if (user != null) {
            AuthState.Authenticated(user)
        } else {
            AuthState.Error("An authenticated session is missing the user identity")
        }
    }

    is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated

    // Session expired and refresh failed. The SDK keeps the stored session and
    // retries; surface a transient error instead of logging anyone out.
    is SessionStatus.RefreshFailure -> AuthState.Error("Your session could not be refreshed")
}

/** Maps the SDK's [UserInfo] into the minimal identity the app exposes. */
internal fun UserInfo.toAuthenticatedUserInfo(): AuthenticatedUserInfo = AuthenticatedUserInfo(
    id = id,
    email = email,
    displayName = userMetadata?.get(DISPLAY_NAME_KEY)?.stringOrNull()
        ?: userMetadata?.get(CAMEL_DISPLAY_NAME_KEY)?.stringOrNull(),
)

/** Reads a [JsonPrimitive] string safely; non-string values yield null. */
internal fun JsonElement?.stringOrNull(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return if (primitive.isString) primitive.content else null
}

private const val DISPLAY_NAME_KEY = "display_name"
private const val CAMEL_DISPLAY_NAME_KEY = "displayName"