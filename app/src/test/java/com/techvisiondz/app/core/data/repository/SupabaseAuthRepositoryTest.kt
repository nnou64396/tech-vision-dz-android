package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthState
import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
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