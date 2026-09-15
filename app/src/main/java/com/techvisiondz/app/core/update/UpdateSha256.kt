package com.techvisiondz.app.core.update

import java.io.File
import java.security.MessageDigest

/**
 * Streaming SHA-256 helpers shared by manifest validation and APK verification.
 *
 * Only the platform [MessageDigest] implementation is used — no custom
 * cryptography. Hashes are always compared case-insensitively and never
 * derived from filenames.
 */
internal object UpdateSha256 {

    private val SHA256_HEX = Regex("[0-9a-fA-F]{64}")

    /** True for a non-blank, well-formed 64-character hexadecimal SHA-256 value. */
    fun isValidHex(value: String?): Boolean =
        !value.isNullOrBlank() && SHA256_HEX.matches(value)

    /** Streaming SHA-256 digest of [file] as lowercase hex. Never loads the file in memory. */
    fun of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /** Case-insensitive hex comparison; a missing/malformed [expectedHex] never matches. */
    fun matches(actualHex: String, expectedHex: String?): Boolean =
        isValidHex(expectedHex) && actualHex.equals(expectedHex, ignoreCase = true)

    private const val BUFFER_SIZE = 8 * 1024
}