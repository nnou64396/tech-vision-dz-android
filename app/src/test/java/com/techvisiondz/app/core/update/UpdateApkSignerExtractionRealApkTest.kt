package com.techvisiondz.app.core.update

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Guards the actual production failure: the released v1.1.6 APK (signed with
 * the v2 scheme only) must cryptographically verify through the signing block,
 * and its signer must be byte-identical to the production certificate the app
 * embeds. Skips (not fails) when no APK path is supplied.
 *
 * Run with:
 *   gradlew testDebugUnitTest -Dtechvision.update.test.apk=<path-to-v1.1.6.apk>
 */
class UpdateApkSignerExtractionRealApkTest {

    @Test
    fun `production v1_1_6 apk verifies and matches the production certificate`() {
        val path = System.getProperty("techvision.update.test.apk", "").orEmpty()
        assumeTrue("set -Dtechvision.update.test.apk=<path> to run this test", path.isNotBlank())
        val apk = File(path)
        assumeTrue("APK not found at $path", apk.isFile)

        val extraction = apkSigningBlockCertificates(apk)
        assertTrue("production APK must cryptographically verify", extraction is ApkSigners.Verified)
        val certificates = (extraction as ApkSigners.Verified).certificates

        assertEquals("exactly one production signer", 1, certificates.size)
        assertEquals(
            "signing-block certificate must equal the embedded production certificate",
            PRODUCTION_SIGNING_CERT_SHA256,
            sha256Hex(certificates.single()),
        )
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PRODUCTION_SIGNING_CERT_SHA256 =
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33"
    }
}