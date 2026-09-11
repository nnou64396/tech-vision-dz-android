package com.techvisiondz.app.feature.account

import com.techvisiondz.app.core.data.AuthException
import com.techvisiondz.app.core.data.AuthenticatedUserInfo
import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
import com.techvisiondz.app.core.data.repository.FakeProfileRepository
import com.techvisiondz.app.core.data.repository.sampleUserProfile
import com.techvisiondz.app.feature.auth.AuthError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun authenticatedAuthRepository() = FakeAuthRepository().apply {
        setSession(AuthenticatedUserInfo(id = "user-1", email = "reader@example.com", displayName = "Sarah"))
    }

    @Test
    fun `profile loads and is exposed through the state`() = runTest(dispatcher) {
        val profileRepository = FakeProfileRepository(
            profile = sampleUserProfile(
                id = "user-1",
                email = "reader@example.com",
                displayName = "Sarah",
            ),
        )
        val viewModel = AccountViewModel(authenticatedAuthRepository(), profileRepository)

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Sarah", state.profile?.displayName)
        assertEquals("reader@example.com", state.profile?.email)
        assertEquals(1, profileRepository.loadCalls)
    }

    @Test
    fun `load stays loading until the query returns`() = runTest(dispatcher) {
        val viewModel = AccountViewModel(authenticatedAuthRepository(), FakeProfileRepository())

        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.profile)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `profile load failure falls back to the auth identity`() = runTest(dispatcher) {
        val profileRepository = FakeProfileRepository(
            error = DataException.Network(),
        )
        val viewModel = AccountViewModel(authenticatedAuthRepository(), profileRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        // The screen still has something to render: the authenticated identity.
        assertEquals("user-1", state.profile?.id)
        assertEquals("Sarah", state.profile?.displayName)
        assertNull(state.profile?.avatarUrl)
    }

    @Test
    fun `successful sign out clears loading but keeps the profile`() = runTest(dispatcher) {
        val authRepository = authenticatedAuthRepository()
        val viewModel = AccountViewModel(authRepository, FakeProfileRepository())

        advanceUntilIdle()

        viewModel.signOut()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoggingOut)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggingOut)
        assertNull(state.error)
        assertEquals(1, authRepository.signOutCalls)
    }

    @Test
    fun `failed sign out surfaces the mapped auth error`() = runTest(dispatcher) {
        val authRepository = authenticatedAuthRepository().apply {
            signOutError = AuthException.Network()
        }
        val viewModel = AccountViewModel(authRepository, FakeProfileRepository())

        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggingOut)
        assertEquals(AuthError.Network, state.error)
    }

    @Test
    fun `a second sign-out while one is in flight is ignored`() = runTest(dispatcher) {
        val authRepository = authenticatedAuthRepository()
        val viewModel = AccountViewModel(authRepository, FakeProfileRepository())

        advanceUntilIdle()

        viewModel.signOut()
        runCurrent()
        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(1, authRepository.signOutCalls)
    }

    @Test
    fun `clearError removes the surfaced error`() = runTest(dispatcher) {
        val authRepository = authenticatedAuthRepository().apply {
            signOutError = AuthException.Server()
        }
        val viewModel = AccountViewModel(authRepository, FakeProfileRepository())

        advanceUntilIdle()
        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(AuthError.Server, viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retry after a load failure reloads the profile`() = runTest(dispatcher) {
        val profileRepository = FakeProfileRepository(
            error = DataException.Network(),
        )
        val viewModel = AccountViewModel(authenticatedAuthRepository(), profileRepository)

        advanceUntilIdle()
        assertEquals(1, profileRepository.loadCalls)

        profileRepository.error = null
        profileRepository.profile = sampleUserProfile(
            id = "user-1",
            email = "reader@example.com",
            displayName = "Sarah",
            avatarUrl = "https://example.com/avatar.png",
        )
        viewModel.loadProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("https://example.com/avatar.png", state.profile?.avatarUrl)
        assertEquals(2, profileRepository.loadCalls)
    }

    @Test
    fun `profile fallback keeps the identities a user sees before sign in resolves`() = runTest(dispatcher) {
        // The screen is reached only when authenticated, but the identity can
        // momentarily be absent while the supabase SDK settles; the UI must not
        // crash on a null profile and should render an empty, retryable state.
        val viewModel = AccountViewModel(
            FakeAuthRepository(),
            FakeProfileRepository(error = DataException.Unknown()),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.profile)
        assertNull(state.error)
    }
}