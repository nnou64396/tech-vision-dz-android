package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthException
import com.techvisiondz.app.core.data.AuthState
import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure mapping of Supabase auth [SessionStatus]/[UserInfo]
 * into the app's [AuthState] / [AuthenticatedUserInfo]. No network or real
 * Supabase client is involved.
 */
class SupabaseAuthRepositoryTest {

    // ------------------------------------------------------------------
    // SessionStatus -> AuthState
    // ------------------------------------------------------------------

    @Test
    fun `initializing maps to loading`() {
        assertEquals(AuthState.Loading, SessionStatus.Initializing.toAuthState())
    }

    @Test
    fun `not authenticated maps to unauthenticated`() {
        val state = SessionStatus.NotAuthenticated().toAuthState()
        assertEquals(AuthState.Unauthenticated, state)
    }

    @Test
    fun `not authenticated after sign out maps to unauthenticated`() {
        val state = SessionStatus.NotAuthenticated(isSignOut = true).toAuthState()
        assertEquals(AuthState.Unauthenticated, state)
    }

    @Test
    fun `authenticated with user maps to authenticated identity`() {
        val user = sampleUserInfo(id = "user-1", email = "reader@example.com")
        val status = SessionStatus.Authenticated(sampleSession(user = user))

        val state = status.toAuthState()

        assertTrue(state is AuthState.Authenticated)
        val wrapped = (state as AuthState.Authenticated).user
        assertEquals("user-1", wrapped.id)
        assertEquals("reader@example.com", wrapped.email)
        assertEquals("Sarah", wrapped.displayName)
    }

    @Test
    fun `authenticated maps display name from snake case metadata`() {
        val metadata = buildJsonObject { put(DISPLAY_NAME, JsonPrimitive("Sarah")) }
        val user = sampleUserInfo(userMetadata = metadata)

        val info = user.toAuthenticatedUserInfo()

        assertEquals("Sarah", info.displayName)
    }

    @Test
    fun `authenticated maps display name from camel case metadata`() {
        val metadata = buildJsonObject { put("displayName", JsonPrimitive("Sarah")) }
        val user = sampleUserInfo(userMetadata = metadata)

        val info = user.toAuthenticatedUserInfo()

        assertEquals("Sarah", info.displayName)
    }

    @Test
    fun `authenticated user without display name keeps null`() {
        val user = sampleUserInfo(userMetadata = buildJsonObject { put("locale", JsonPrimitive("ar")) })

        val info = user.toAuthenticatedUserInfo()

        assertNull(info.displayName)
    }

    @Test
    fun `non string display name is not coerced`() {
        val metadata = buildJsonObject { put(DISPLAY_NAME, buildJsonObject { }) }
        val user = sampleUserInfo(userMetadata = metadata)

        val info = user.toAuthenticatedUserInfo()

        assertNull(info.displayName)
    }

    @Test
    fun `authenticated without user maps to error`() {
        val status = SessionStatus.Authenticated(sampleSession(user = null))

        val state = status.toAuthState()

        assertTrue(state is AuthState.Error)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `refresh failure maps to a transient error`() {
        val status = SessionStatus.RefreshFailure(
            RefreshFailureCause.NetworkError(IllegalStateException("offline")),
        )

        val state = status.toAuthState()

        assertTrue(state is AuthState.Error)
    }

    @Test
    fun `empty user metadata is safe`() {
        val user = sampleUserInfo(userMetadata = buildJsonObject { })

        val info = user.toAuthenticatedUserInfo()

        assertEquals("user-1", info.id)
        assertNull(info.displayName)
    }

    // ------------------------------------------------------------------
    // JsonElement.stringOrNull
    // ------------------------------------------------------------------

    @Test
    fun `stringOrNull returns string primitive content safely`() {
        assertEquals("value", JsonPrimitive("value").stringOrNull())
    }

    @Test
    fun `stringOrNull returns null for non primitive or missing`() {
        assertNull(buildJsonObject { }.stringOrNull())
        assertNull(JsonPrimitive(false).stringOrNull())
        assertNull(null.stringOrNull())
    }

    // ------------------------------------------------------------------
    // mapAuthFailure: errorCode + statusCode -> AuthException
    // ------------------------------------------------------------------

    @Test
    fun `invalid credentials code maps to invalid credentials`() {
        val exception = mapAuthFailure(AuthErrorCode.InvalidCredentials, 400)

        assertEquals(AuthException.InvalidCredentials(), exception)
    }

    @Test
    fun `user not found and non authorized email map to invalid credentials`() {
        assertEquals(AuthException.InvalidCredentials(), mapAuthFailure(AuthErrorCode.UserNotFound, 400))
        assertEquals(
            AuthException.InvalidCredentials(),
            mapAuthFailure(AuthErrorCode.EmailAddressNotAuthorized, 400),
        )
    }

    @Test
    fun `unconfirmed email maps to email not confirmed`() {
        assertEquals(AuthException.EmailNotConfirmed(), mapAuthFailure(AuthErrorCode.EmailNotConfirmed, 400))
    }

    @Test
    fun `existing email codes map to already registered`() {
        assertEquals(AuthException.EmailAlreadyRegistered(), mapAuthFailure(AuthErrorCode.EmailExists, 400))
        assertEquals(AuthException.EmailAlreadyRegistered(), mapAuthFailure(AuthErrorCode.UserAlreadyExists, 400))
    }

    @Test
    fun `weak password maps to weak password`() {
        assertEquals(AuthException.WeakPassword(), mapAuthFailure(AuthErrorCode.WeakPassword, 400))
    }

    @Test
    fun `rate limit codes map to rate limited`() {
        val code = AuthException.RateLimited()
        assertEquals(code, mapAuthFailure(AuthErrorCode.OverRequestRateLimit, 429))
        assertEquals(code, mapAuthFailure(AuthErrorCode.OverEmailSendRateLimit, 429))
        assertEquals(code, mapAuthFailure(AuthErrorCode.OverSmsSendRateLimit, 429))
    }

    @Test
    fun `validation codes map to invalid email`() {
        assertEquals(AuthException.InvalidEmail(), mapAuthFailure(AuthErrorCode.ValidationFailed, 400))
        assertEquals(AuthException.InvalidEmail(), mapAuthFailure(AuthErrorCode.EmailAddressInvalid, 400))
    }

    @Test
    fun `unknown code with 400 maps to invalid email by status`() {
        assertEquals(AuthException.InvalidEmail(), mapAuthFailure(AuthErrorCode.BadJson, 400))
    }

    @Test
    fun `unknown code with auth statuses maps to invalid credentials`() {
        assertEquals(AuthException.InvalidCredentials(), mapAuthFailure(AuthErrorCode.BadJwt, 401))
        assertEquals(AuthException.InvalidCredentials(), mapAuthFailure(AuthErrorCode.BadJwt, 403))
        assertEquals(AuthException.InvalidCredentials(), mapAuthFailure(AuthErrorCode.BadJwt, 404))
    }

    @Test
    fun `unknown code with 429 maps to rate limited`() {
        assertEquals(AuthException.RateLimited(), mapAuthFailure(AuthErrorCode.BadJwt, 429))
    }

    @Test
    fun `unknown code with server status maps to server`() {
        val server = AuthException.Server()
        assertEquals(server, mapAuthFailure(AuthErrorCode.BadJwt, 500))
        assertEquals(server, mapAuthFailure(AuthErrorCode.BadJwt, 502))
        assertEquals(server, mapAuthFailure(AuthErrorCode.BadJwt, 504))
    }

    @Test
    fun `null code and status maps to unknown`() {
        assertEquals(AuthException.Unknown(), mapAuthFailure(null, null))
    }

    // ------------------------------------------------------------------
    // toAuthException: raw throwables (no SDK response objects needed)
    // ------------------------------------------------------------------

    @Test
    fun `network failures map to network`() {
        assertTrue(IOException("offline").toAuthException() is AuthException.Network)
    }

    @Test
    fun `generic failures map to unknown`() {
        assertTrue(IllegalStateException("boom").toAuthException() is AuthException.Unknown)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private companion object {
        const val DISPLAY_NAME = "display_name"

        fun sampleSession(user: UserInfo?) = UserSession(
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token",
            expiresIn = 3600L,
            tokenType = "bearer",
            user = user,
        )

        fun sampleUserInfo(
            id: String = "user-1",
            email: String? = null,
            userMetadata: JsonObject? = buildJsonObject { put(DISPLAY_NAME, JsonPrimitive("Sarah")) },
        ) = UserInfo(
            aud = "authenticated",
            id = id,
            email = email,
            userMetadata = userMetadata,
        )
    }
}