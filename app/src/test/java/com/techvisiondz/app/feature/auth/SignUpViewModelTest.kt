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
class SignUpViewModelTest {

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
    fun `blank fields are all flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)

        viewModel.signUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FieldError.Required, state.emailError)
        assertEquals(FieldError.Required, state.passwordError)
        assertEquals(FieldError.Required, state.confirmPasswordError)
        assertEquals(0, repository.signUpCalls)
        assertFalse(state.isLoading)
    }

    @Test
    fun `password mismatch is flagged without calling the repository`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password124")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(FieldError.PasswordsDoNotMatch, viewModel.uiState.value.confirmPasswordError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun `short password is flagged before any request`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("reader@example.com")
        viewModel.onPasswordChange("12345")
        viewModel.onConfirmPasswordChange("12345")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(FieldError.PasswordTooShort, viewModel.uiState.value.passwordError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun `malformed email is flagged before any request`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(FieldError.InvalidEmail, viewModel.uiState.value.emailError)
        assertEquals(0, repository.signUpCalls)
    }

    @Test
    fun `successful sign up reports confirmation pending`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.signUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.signUpSucceeded)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertEquals("new@example.com", state.signedUpEmail)
        assertEquals(1, repository.signUpCalls)
        assertEquals("new@example.com", repository.lastSignUpEmail)
    }

    @Test
    fun `already registered failure maps to the registered auth error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signUpError = AuthException.EmailAlreadyRegistered()
        }
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("taken@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.signUp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.signUpSucceeded)
        assertEquals(AuthError.EmailAlreadyRegistered, state.authError)
    }

    @Test
    fun `backend weak password failure maps to the weak password error`() = runTest(dispatcher) {
        val repository = FakeAuthRepository().apply {
            signUpError = AuthException.WeakPassword()
        }
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(AuthError.WeakPassword, viewModel.uiState.value.authError)
    }

    @Test
    fun `loading is reported while the request is in flight`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.signUp()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.signUpSucceeded)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.signUpSucceeded)
    }

    @Test
    fun `editing a field clears the previous password errors`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)

        viewModel.onEmailChange("new@example.com")
        viewModel.onPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(FieldError.PasswordTooShort, viewModel.uiState.value.passwordError)

        viewModel.onPasswordChange("longpassword")

        assertNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.confirmPasswordError)
    }
}