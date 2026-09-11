package com.techvisiondz.app.feature.auth

import com.techvisiondz.app.core.data.AuthException
import com.techvisiondz.app.core.data.repository.FakeAuthRepository
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
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank fields are flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignInViewModel(repository)

        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldError.Required, state.emailError)
        assertEquals(FieldError.Required, state.passwordError)
        assertEquals(0, repository.signInCalls)
        assertFalse(state.isLoading)
    }

    @Test
    fun `malformed email is flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(FieldError.InvalidEmail, viewModel.uiState.value.emailError)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `successful sign in signs the session in`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.signedIn)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertEquals(1, repository.signInCalls)
        assertEquals("reader@example.com", repository.lastSignInEmail)
        assertEquals("password123", repository.lastSignInPassword)
    }

    @Test
    fun `failure surfaces the mapped auth error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signInError = AuthException.InvalidCredentials()
        }
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.signedIn)
        assertFalse(state.isLoading)
        assertEquals(AuthError.InvalidCredentials, state.authError)
    }

    @Test
    fun `network failure maps to the network error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signInError = AuthException.Network()
        }
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(AuthError.Network, viewModel.uiState.value.authError)
    }

    @Test
    fun `unconfirmed email maps to the confirmation error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signInError = AuthException.EmailNotConfirmed()
        }
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(AuthError.EmailNotConfirmed, viewModel.uiState.value.authError)
    }

    @Test
    fun `loading is reported while the request is in flight`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.signedIn)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.signedIn)
    }

    @Test
    fun `a second submit while loading is ignored`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        runCurrent()
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(1, repository.signInCalls)
    }

    @Test
    fun `editing a field clears the auth error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signInError = AuthException.InvalidCredentials()
        }
        val viewModel = SignInViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.signIn()
        advanceUntilIdle()
        assertEquals(AuthError.InvalidCredentials, viewModel.uiState.value.authError)

        viewModel.onEmailChange("reader@example.org")

        assertNull(viewModel.uiState.value.authError)
        assertNull(viewModel.uiState.value.emailError)
    }
}