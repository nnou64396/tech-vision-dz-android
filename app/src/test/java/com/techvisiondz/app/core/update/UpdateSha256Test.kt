package com.techvisiondz.app.core.update

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * SHA-256 helpers: hex validation and case-insensitive matching, plus a
 * known-value check against the platform MessageDigest.
 */
class UpdateSha256Test {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    @Test
    fun `sixty four character hex is valid`() {
        assertTrue(UpdateSha256.isValidHex(UpdateTestFixtures.VALID_SHA256))
        assertTrue(UpdateSha256.isValidHex(UpdateTestFixtures.VALID_SHA256_UPPERCASE))
    }

    @Test
    fun `too short or too long values are invalid`() {
        assertFalse(UpdateSha256.isValidHex("a".repeat(63)))
        assertFalse(UpdateSha256.isValidHex("a".repeat(65)))
    }

    @Test
    fun `non hexadecimal characters are invalid`() {
        assertFalse(UpdateSha256.isValidHex("z".repeat(64)))
        assertFalse(UpdateSha256.isValidHex("g".repeat(64)))
    }

    @Test
    fun `blank and null are invalid`() {
        assertFalse(UpdateSha256.isValidHex(""))
        assertFalse(UpdateSha256.isValidHex(null))
    }

    @Test
    fun `correct hash matches`() {
        val file = tempFolder.newFile("tiny.apk")
        file.writeBytes("abc".toByteArray())
        val actual = UpdateSha256.of(file)
        assertTrue(UpdateSha256.matches(actual, actual))
    }

    @Test
    fun `incorrect hash is rejected`() {
        val file = tempFolder.newFile("tiny.apk")
        file.writeBytes("abc".toByteArray())
        val actual = UpdateSha256.of(file)
        val wrong = "c" + actual.drop(1)
        assertFalse(UpdateSha256.matches(actual, wrong))
    }

    @Test
    fun `comparison is case insensitive`() {
        val file = tempFolder.newFile("tiny.apk")
        file.writeBytes("abc".toByteArray())
        val actual = UpdateSha256.of(file)
        assertTrue(UpdateSha256.matches(actual, actual.uppercase()))
    }

    @Test
    fun `malformed expected hash never matches`() {
        val file = tempFolder.newFile("tiny.apk")
        file.writeBytes("abc".toByteArray())
        assertFalse(UpdateSha256.matches(UpdateSha256.of(file), "not-a-hash"))
        assertFalse(UpdateSha256.matches(UpdateSha256.of(file), null))
    }

    @Test
    fun `hashes a known small file correctly`() {
        val file = tempFolder.newFile("known.txt")
        file.writeBytes("abc".toByteArray())
        val expected = sha256Hex("abc".toByteArray())
        assertEquals(expected, UpdateSha256.of(file))
        assertTrue(UpdateSha256.matches(UpdateSha256.of(file), expected))
        assertTrue(UpdateSha256.matches(UpdateSha256.of(file), expected.uppercase()))
    }

    @Test
    fun `different content hashes differently`() {
        val a = tempFolder.newFile("a.txt").apply { writeBytes("abc".toByteArray()) }
        val b = tempFolder.newFile("b.txt").apply { writeBytes("abd".toByteArray()) }
        assertFalse(UpdateSha256.matches(UpdateSha256.of(a), UpdateSha256.of(b)))
    }

    @Test
    fun `empty file streams without error`() {
        val file = tempFolder.newFile("empty")
        assertTrue(UpdateSha256.isValidHex(UpdateSha256.of(file)))
    }

    @Test
    fun `missing file is surfaced as an error`() {
        val missing = File(tempFolder.root, "does-not-exist.apk")
        val digests = runCatching { UpdateSha256.of(missing) }
        assertTrue("expected failure for a missing file, got $digests", digests.isFailure)
    }
}