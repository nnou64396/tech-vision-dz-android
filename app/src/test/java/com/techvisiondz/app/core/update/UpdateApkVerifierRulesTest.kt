package com.techvisiondz.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure decision rules of the APK verifier: metadata comparison and public
 * signature comparison. These run entirely on the JVM without Android APIs.
 */
class UpdateApkVerifierRulesTest {

    private fun expected(
        versionCode: Int = 3,
        versionName: String = "1.1.0",
        minimumVersionCode: Int? = null,
    ) = UpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        downloadUrl = UpdateTestFixtures.TRUSTED_APK_URL,
        sha256 = UpdateTestFixtures.VALID_SHA256,
        releaseNotes = null,
        minimumVersionCode = minimumVersionCode,
    )

    private fun signer(vararg bytes: Byte): ByteArray = bytes

    @Test
    fun `correct package and exact version is ok`() {
        assertEquals(
            MetadataStatus.Ok,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `null package name is missing`() {
        assertEquals(
            MetadataStatus.Missing,
            evaluateMetadata(
                apkPackageName = null,
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `null version code is missing`() {
        assertEquals(
            MetadataStatus.Missing,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = null,
                apkVersionName = "1.1.0",
                expected = expected(),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `different package is wrong package`() {
        assertEquals(
            MetadataStatus.WrongPackage,
            evaluateMetadata(
                apkPackageName = "com.evil.app",
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `exact version match is ok`() {
        assertEquals(
            MetadataStatus.Ok,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(versionCode = 3),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `version code different from manifest is wrong version`() {
        assertEquals(
            MetadataStatus.WrongVersion,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 4,
                apkVersionName = "1.1.0",
                expected = expected(versionCode = 3),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `readable version name disagreeing with manifest is version name mismatch`() {
        assertEquals(
            MetadataStatus.VersionNameMismatch,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "9.9.9",
                expected = expected(versionCode = 3),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `blank or null version name does not contradict the manifest`() {
        assertEquals(
            MetadataStatus.Ok,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = null,
                expected = expected(versionCode = 3),
                installedVersionCode = 2,
            ),
        )
        assertEquals(
            MetadataStatus.Ok,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "",
                expected = expected(versionCode = 3),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `installed version below the minimum is below minimum`() {
        assertEquals(
            MetadataStatus.BelowMinimum,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(minimumVersionCode = 4),
                installedVersionCode = 2,
            ),
        )
    }

    @Test
    fun `installed version at or above the minimum is ok`() {
        val okAtBoundary = evaluateMetadata(
            apkPackageName = "com.techvisiondz.app",
            apkVersionCode = 3,
            apkVersionName = "1.1.0",
            expected = expected(minimumVersionCode = 2),
            installedVersionCode = 2,
        )
        val okAbove = evaluateMetadata(
            apkPackageName = "com.techvisiondz.app",
            apkVersionCode = 3,
            apkVersionName = "1.1.0",
            expected = expected(minimumVersionCode = 2),
            installedVersionCode = 3,
        )
        assertEquals(MetadataStatus.Ok, okAtBoundary)
        assertEquals(MetadataStatus.Ok, okAbove)
    }

    @Test
    fun `no minimumVersionCode never fails the minimum rule`() {
        assertEquals(
            MetadataStatus.Ok,
            evaluateMetadata(
                apkPackageName = "com.techvisiondz.app",
                apkVersionCode = 3,
                apkVersionName = "1.1.0",
                expected = expected(minimumVersionCode = null),
                installedVersionCode = 1,
            ),
        )
    }

    @Test
    fun `identical signing certificates match`() {
        val installed = listOf(signer(1, 2, 3))
        val apk = listOf(signer(1, 2, 3))
        assertEquals(SignatureCompare.Match, compareSignatures(installed, apk))
    }

    @Test
    fun `different signing certificates mismatch`() {
        val installed = listOf(signer(1, 2, 3))
        val apk = listOf(signer(4, 5, 6))
        assertEquals(SignatureCompare.Mismatch, compareSignatures(installed, apk))
    }

    @Test
    fun `any matching signer in a multi signer set matches`() {
        val installed = listOf(signer(1, 2, 3), signer(7, 8, 9))
        val apk = listOf(signer(4, 5, 6), signer(7, 8, 9))
        assertEquals(SignatureCompare.Match, compareSignatures(installed, apk))
    }

    @Test
    fun `empty installed signers are unverifiable`() {
        assertEquals(SignatureCompare.Unverifiable, compareSignatures(emptyList(), listOf(signer(1, 2, 3))))
    }

    @Test
    fun `empty apk signers are unverifiable`() {
        assertEquals(SignatureCompare.Unverifiable, compareSignatures(listOf(signer(1, 2, 3)), emptyList()))
    }

    @Test
    fun `signature comparison never treats missing signers as a match`() {
        assertEquals(
            SignatureCompare.Unverifiable,
            compareSignatures(emptyList(), emptyList()),
        )
    }
}