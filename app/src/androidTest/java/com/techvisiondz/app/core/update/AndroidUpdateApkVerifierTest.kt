package com.techvisiondz.app.core.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * On-device regression test for the production updater failure: the released
 * v1.1.6 APK is signed with the v2 scheme only, and must not be discarded as
 * unverifiable by the verifier while running against a production-signed
 * install (v1.1.5 or later).
 *
 * The update APK is supplied explicitly, never downloaded by the test:
 *
 *   adb push v1.1.6.apk /sdcard/Download/
 *   ./gradlew connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.apkPath=/sdcard/Download/v1.1.6.apk
 *
 * Skipped (not failed) when apkPath is absent or the file is missing.
 */
class AndroidUpdateApkVerifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun argument(name: String): String? =
        InstrumentationRegistry.getArguments().getString(name)

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun installedCertSha256(): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val first = signatures?.firstOrNull() ?: return null
        return sha256Hex(first.toByteArray())
    }

    private fun verifyProductionUpdate(apk: File): ApkVerificationResult {
        val expected = UpdateInfo(
            versionCode = 9,
            versionName = "1.1.6",
            downloadUrl = "https://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.6/app-release.apk",
            sha256 = "7805dd656f585e4f2a7026b15e4cc8c8408db4f6f69cee950405c4abe06bba74",
            releaseNotes = null,
            minimumVersionCode = 6,
        )
        return runBlocking { AndroidUpdateApkVerifier(context).verify(apk, expected) }
    }

    @Test
    fun productionUpdateApkIsNeverDiscardedAsUnverifiable() {
        val apkPath = argument("apkPath")
        assumeTrue(
            "pass -e apkPath <path-to-v1.1.6.apk> (or the Gradle apkPath runner argument)",
            apkPath != null && File(apkPath).isFile,
        )

        val apk = File(apkPath)
        val installedCert = installedCertSha256()
        assumeTrue("installed package reports no signers", installedCert != null)

        val result = verifyProductionUpdate(apk)

        // The exact regression: a valid, cryptographically sound v2-only update
        // APK must never be reported as SignatureUnverifiable.
        assertNotEquals(
            "production update must not be discarded as unverifiable (installed cert=$installedCert)",
            ApkVerificationResult.SignatureUnverifiable,
            result,
        )

        if (installedCert == PRODUCTION_SIGNING_CERT_SHA256) {
            assertEquals(
                "production v1.1.5 -> v1.1.6 must verify as Success",
                ApkVerificationResult.Success,
                result,
            )
        } else {
            // Non-production install (e.g. debug): the answer must still be a
            // definite SignatureMismatch — deterministic, never Unverifiable.
            assertEquals(
                "non-production install must yield a definite SignatureMismatch",
                ApkVerificationResult.SignatureMismatch,
                result,
            )
        }
    }

    @Test
    fun garbageFileIsDefinitivelyInvalid() {
        val garbage = File(context.cacheDir, "not-an-apk.bin")
        garbage.writeBytes("not an apk".encodeToByteArray())
        try {
            // Hash mismatch short-circuits first; the point is a deterministic,
            // typed answer with no crash and no SignatureUnverifiable.
            val result = verifyProductionUpdate(garbage)
            assertNotEquals(
                ApkVerificationResult.SignatureUnverifiable,
                result,
            )
            assertEquals(ApkVerificationResult.HashMismatch, result)
        } finally {
            garbage.delete()
        }
    }

    private companion object {
        const val PRODUCTION_SIGNING_CERT_SHA256 =
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33"
    }
}