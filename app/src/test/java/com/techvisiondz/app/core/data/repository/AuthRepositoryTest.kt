package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the observable [AuthRepository] contract using a deterministic fake:
 * the flow starts at [AuthState.Loading], settles (Unauthenticated for a fresh
 * install), reacts to an authenticated session, and clears state on sign-out.
 */
class AuthRepositoryTest {

    @Test
    fun `starts in loading state`() {
        val repository = FakeAuthRepository()

        assertEquals(AuthState.Loading, repository.currentAuthState())
    }

    @Test
    fun `initial loading flow then settles to unauthenticated`() = runTest {
        val repository = FakeAuthRepository()

        assertEquals(AuthState.Loading, repository.authState.first())

        repository.completeInitializationNotAuthenticated()

        assertEquals(AuthState.Unauthenticated, repository.currentAuthState())
        assertNull(repository.currentUserOrNull())
    }

    @Test
    fun `authenticated session maps user identity`() = runTest {
        val repository = FakeAuthRepository()
        repository.completeInitializationNotAuthenticated()

        repository.setSession(sampleAuthenticatedUser())

        val state = repository.currentAuthState()
        assertTrue(state is AuthState.Authenticated)
        val user = (state as AuthState.Authenticated).user
        assertEquals("user-1", user.id)
        assertEquals("reader@example.com", user.email)
        assertEquals("Sarah", user.displayName)
        assertEquals(user, repository.currentUserOrNull())
    }

    @Test
    fun `flow emits authenticated value reactively`() = runTest {
        val repository = FakeAuthRepository()
        repository.setSession(sampleAuthenticatedUser())

        val emitted = repository.authState.first()

        assertTrue(emitted is AuthState.Authenticated)
        assertTrue(repository.authState.first() is AuthState.Authenticated)
    }

    @Test
    fun `sign out clears identity and returns to unauthenticated`() = runTest {
        val repository = FakeAuthRepository()
        repository.setSession(sampleAuthenticatedUser())

        repository.signOut()

        assertEquals(AuthState.Unauthenticated, repository.currentAuthState())
        assertNull(repository.currentUserOrNull())
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `signing out without a session is a no-op success`() = runTest {
        val repository = FakeAuthRepository()
        repository.completeInitializationNotAuthenticated()

        repository.signOut()

        assertEquals(AuthState.Unauthenticated, repository.currentAuthState())
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `failure state keeps no user identity`() = runTest {
        val repository = FakeAuthRepository()
        repository.setSession(sampleAuthenticatedUser())

        repository.fail()

        assertTrue(repository.currentAuthState() is AuthState.Error)
        assertNull(repository.currentUserOrNull())
    }
}