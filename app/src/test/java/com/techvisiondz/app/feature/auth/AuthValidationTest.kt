package com.techvisiondz.app.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    // ------------------------------------------------------------------
    // Email
    // ------------------------------------------------------------------

    @Test
    fun `valid email passes`() {
        assertNull(validateEmail("reader@example.com"))
        assertNull(validateEmail("  reader@example.com  "))
        assertNull(validateEmail("a.b+c@techvision.dz"))
    }

    @Test
    fun `blank email is required`() {
        assertEquals(FieldError.Required, validateEmail(""))
        assertEquals(FieldError.Required, validateEmail("   "))
    }

    @Test
    fun `malformed email is invalid`() {
        assertEquals(FieldError.InvalidEmail, validateEmail("reader@"))
        assertEquals(FieldError.InvalidEmail, validateEmail("reader.example.com"))
        assertEquals(FieldError.InvalidEmail, validateEmail("reader@@example.com"))
        assertEquals(FieldError.InvalidEmail, validateEmail("@example.com"))
    }

    // ------------------------------------------------------------------
    // Required (sign-in password)
    // ------------------------------------------------------------------

    @Test
    fun `required accepts non blank values`() {
        assertNull(validateRequired("anything"))
    }

    @Test
    fun `required rejects blank values`() {
        assertEquals(FieldError.Required, validateRequired(""))
        assertEquals(FieldError.Required, validateRequired("   "))
    }

    // ------------------------------------------------------------------
    // Password
    // ------------------------------------------------------------------

    @Test
    fun `password must not be blank`() {
        assertEquals(FieldError.Required, validatePassword(""))
    }

    @Test
    fun `password below minimum length is too short`() {
        assertEquals(FieldError.PasswordTooShort, validatePassword("1234567"))
        assertEquals(FieldError.PasswordTooShort, validatePassword("short"))
    }

    @Test
    fun `password at minimum length passes`() {
        assertNull(validatePassword("12345678"))
        assertNull(validatePassword("long-enough-password"))
    }

    // ------------------------------------------------------------------
    // Confirmation
    // ------------------------------------------------------------------

    @Test
    fun `blank confirmation is required`() {
        assertEquals(FieldError.Required, validateConfirmPassword("password123", ""))
    }

    @Test
    fun `mismatching confirmation is reported`() {
        assertEquals(FieldError.PasswordsDoNotMatch, validateConfirmPassword("password123", "password124"))
    }

    @Test
    fun `matching confirmation passes`() {
        assertNull(validateConfirmPassword("password123", "password123"))
    }
}