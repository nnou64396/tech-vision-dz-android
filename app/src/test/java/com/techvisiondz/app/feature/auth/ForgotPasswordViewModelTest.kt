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
class ForgotPasswordViewModelTest {

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
    fun `blank email is flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(repository)

        viewModel.resetPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldError.Required, state.emailError)
        assertEquals(0, repository.resetPasswordCalls)
        assertFalse(state.isLoading)
        assertFalse(state.resetSent)
    }

    @Test
    fun `malformed email is flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@")

        viewModel.resetPassword()
        advanceUntilIdle()

        assertEquals(FieldError.InvalidEmail, viewModel.uiState.value.emailError)
        assertEquals(0, repository.resetPasswordCalls)
    }

    @Test
    fun `successful reset reports the email the link was sent to`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@example.com")

        viewModel.resetPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.resetSent)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertEquals("reader@example.com", state.resetEmail)
        assertEquals(1, repository.resetPasswordCalls)
        assertEquals("reader@example.com", repository.lastResetEmail)
    }

    @Test
    fun `rate limited failure maps to the rate limited error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            resetPasswordError = AuthException.RateLimited()
        }
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@example.com")

        viewModel.resetPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.resetSent)
        assertEquals(AuthError.RateLimited, state.authError)
    }

    @Test
    fun `server failure maps to the server error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            resetPasswordError = AuthException.Server()
        }
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@example.com")

        viewModel.resetPassword()
        advanceUntilIdle()

        assertEquals(AuthError.Server, viewModel.uiState.value.authError)
    }

    @Test
    fun `loading is reported while the request is in flight`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@example.com")

        viewModel.resetPassword()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.resetSent)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.resetSent)
    }

    @Test
    fun `editing the email clears the auth error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            resetPasswordError = AuthException.Unknown()
        }
        val viewModel = ForgotPasswordViewModel(repository)
        viewModel.onEmailChange("reader@example.com")

        viewModel.resetPassword()
        advanceUntilIdle()
        assertEquals(AuthError.Unknown, viewModel.uiState.value.authError)

        viewModel.onEmailChange("reader@example.org")

        assertNull(viewModel.uiState.value.authError)
        assertNull(viewModel.uiState.value.emailError)
    }
}