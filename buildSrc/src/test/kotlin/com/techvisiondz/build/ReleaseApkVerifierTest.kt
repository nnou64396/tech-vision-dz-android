package com.techvisiondz.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseApkVerifierTest {

    private val expected = ReleaseApkVerifier.Expected(
        applicationId = "com.techvisiondz.app",
        versionCode = "6",
        versionName = "1.1.3",
        certificateSha256 = "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
    )

    // Real `aapt2 dump badging` output captured from the v1.1.3 release APK.
    private val realBadging = """
        package: name='com.techvisiondz.app' versionCode='6' versionName='1.1.3' platformBuildVersionName='17'
        platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
        sdkVersion:'26'
        targetSdkVersion:'37'
        uses-permission: name='android.permission.INTERNET'
        uses-permission: name='android.permission.REQUEST_INSTALL_PACKAGES'
        uses-permission: name='com.techvisiondz.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
    """.trimIndent()

    // Real `apksigner verify --print-certs` output for the production certificate.
    private val realApksigner = """
        Signer #1 certificate DN: CN=TECH VISION DZ, OU=Android, O=TECH VISION DZ, C=DZ
        Signer #1 certificate SHA-256 digest: 1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33
        Signer #1 certificate SHA-1 digest: 24c967e2b4f66d32c61aeedf9afe2a51e7a24609
        Signer #1 certificate MD5 digest: 1e4dd176598d67c0e1e692895f1f7eda
    """.trimIndent()

    private val dummySha256 = "0".repeat(64)

    // --- Happy path ---------------------------------------------------------

    @Test
    fun `conformant apk passes every check`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )
        assertTrue(result.failures.joinToString("\n") { it.render() }, result.passed)
    }

    // Build Tools 37 renamed the apksigner signer label from "Signer #1 ..." to
    // "V2 Signer: ...". The digest value is identical; the parser must be tolerant.
    private val buildTools37Certs = """
        V2 Signer: certificate DN: CN=TECH VISION DZ, OU=Android, O=TECH VISION DZ, C=DZ
        V2 Signer: certificate SHA-256 digest: 1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33
        V2 Signer: certificate SHA-1 digest: 24c967e2b4f66d32c61aeedf9afe2a51e7a24609
        V2 Signer: certificate MD5 digest: 1e4dd176598d67c0e1e692895f1f7eda
    """.trimIndent()

    @Test
    fun `build tools 37 apksigner output passes every check`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = buildTools37Certs,
            apksignerExitOk = true,
        )
        assertTrue(result.failures.joinToString("\n") { it.render() }, result.passed)
    }

    // --- Permission (the critical v1.1.2 regression) -------------------------

    @Test
    fun `missing request install permission fails the build`() {
        val badgingWithoutPermission = realBadging.replace(
            "uses-permission: name='android.permission.REQUEST_INSTALL_PACKAGES'\n",
            "",
        )
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = badgingWithoutPermission,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        val failure = result.failures.single()
        assertTrue(failure.render(), failure.check.contains("REQUEST_INSTALL_PACKAGES"))
        assertEquals(expected.requiredPermission, failure.expected)
        assertTrue(failure.render(), failure.actual.contains("permission missing"))
        assertTrue(failure.render(), failure.fix.contains("cannot install future updates"))
    }

    @Test
    fun `parseBadging reports permission list without the required permission`() {
        val info = ReleaseApkVerifier.parseBadging(
            """
            package: name='com.techvisiondz.app' versionCode='6' versionName='1.1.3'
            uses-permission: name='android.permission.INTERNET'
            """.trimIndent(),
        )!!

        assertFalse(info.permissions.contains(ReleaseApkVerifier.REQUIRED_INSTALL_PERMISSION))
    }

    // --- Certificate --------------------------------------------------------

    @Test
    fun `wrong certificate sha256 fails the build`() {
        val wrongCerts = realApksigner.replace(
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            "0".repeat(64),
        )
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = wrongCerts,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK signing certificate", result.failures.single().check)
        assertTrue(result.failures.single().fix.contains("must not be released"))
    }

    @Test
    fun `no certificate digest found fails the build`() {
        val apksignerWithoutDigest = "Signer #1 certificate DN: CN=Unknown\n"
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = apksignerWithoutDigest,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK signing certificate", result.failures.single().check)
        // Diagnostics include a safe excerpt of the apksigner output.
        assertTrue(result.failures.single().actual.contains("no certificate digest found"))
        assertTrue(result.failures.single().actual.contains("CN=Unknown"))
    }

    // --- Application id / version -------------------------------------------

    @Test
    fun `wrong application id fails the build`() {
        val badging = realBadging.replace("com.techvisiondz.app", "com.other.app")
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = badging,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK application ID", result.failures.single().check)
    }

    @Test
    fun `version drift between gradle and apk fails the build`() {
        val badging = realBadging
            .replace("versionCode='6'", "versionCode='42'")
            .replace("versionName='1.1.3'", "versionName='9.9.9'")
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = badging,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals(2, result.failures.size)
        assertTrue(result.failures.any { it.check == "Release APK versionCode" })
        assertTrue(result.failures.any { it.check == "Release APK versionName" })
    }

    // --- Structure / signature tooling --------------------------------------

    @Test
    fun `apksigner nonzero exit or missing output fails the build`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = "ERROR: failed to verify",
            apksignerExitOk = false,
        )

        assertFalse(result.passed)
        assertTrue(result.failures.single().check.contains("structurally valid"))
    }

    @Test
    fun `aapt2 failure fails the build`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = null,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertTrue(result.failures.single().check.contains("manifest can be read"))
    }

    @Test
    fun `unparseable badging output fails the build`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = "totally-not-aapt2-output\n",
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertTrue(result.failures.single().check.contains("package metadata"))
    }

    // --- Parsers ------------------------------------------------------------

    @Test
    fun `parseBadging extracts metadata and permissions`() {
        val info = ReleaseApkVerifier.parseBadging(realBadging)!!

        assertEquals("com.techvisiondz.app", info.applicationId)
        assertEquals("6", info.versionCode)
        assertEquals("1.1.3", info.versionName)
        assertTrue(info.permissions.contains(ReleaseApkVerifier.REQUIRED_INSTALL_PERMISSION))
    }

    @Test
    fun `parseBadging returns null when package line is missing`() {
        assertEquals(null, ReleaseApkVerifier.parseBadging("uses-permission: name='android.permission.INTERNET'"))
    }

    @Test
    fun `parseFirstSignerSha256 extracts the production digest`() {
        assertEquals(
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            ReleaseApkVerifier.parseFirstSignerSha256(realApksigner),
        )
    }

    @Test
    fun `parseFirstSignerSha256 extracts the digest from build tools 37 label`() {
        assertEquals(
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            ReleaseApkVerifier.parseFirstSignerSha256(buildTools37Certs),
        )
    }

    @Test
    fun `parseFirstSignerSha256 returns null for unrelated sha256 text`() {
        val unrelated = """
            Signer #1 public key SHA-256 digest: 1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33
            APK SHA-256: 1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33
            Signer #1 certificate SHA-1 digest: 24c967e2b4f66d32c61aeedf9afe2a51e7a24609
            Verified using v2 scheme (APK Signature Scheme v2): true
        """.trimIndent()
        assertEquals(null, ReleaseApkVerifier.parseFirstSignerSha256(unrelated))
    }

    @Test
    fun `parseFirstSignerSha256 rejects malformed digest values`() {
        val realDigest = "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33"
        val malformed = listOf(
            "Signer #1 certificate SHA-256 digest: not-a-hex-digest",
            "Signer #1 certificate SHA-256 digest: ${realDigest.dropLast(1)}",
            "Signer #1 certificate SHA-256 digest: ${realDigest + "1"}",
            "V2 Signer: certificate SHA-256 digest: Zfbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            "Signer #1 certificate SHA-256 digest: $realDigest $realDigest",
            "Signer #1 certificate SHA-256 digest: $realDigest trailing-junk",
        )
        for (output in malformed) {
            assertEquals("expected null for: $output", null, ReleaseApkVerifier.parseFirstSignerSha256(output))
        }
    }

    @Test
    fun `parseFirstSignerSha256 normalizes an uppercase digest to lowercase`() {
        val uppercase = realApksigner.replace(
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            "1FBF843195367E3895D1BB614481F7A3D7F0DB1DA8FFE69EBA3CE4716BE20D33",
        )
        assertEquals(
            "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33",
            ReleaseApkVerifier.parseFirstSignerSha256(uppercase),
        )
    }

    @Test
    fun `malformed certificate digest fails the build with output excerpt`() {
        val malformed = "V2 Signer: certificate DN: CN=TECH VISION DZ, OU=Android, O=TECH VISION DZ, C=DZ\n" +
            "V2 Signer: certificate SHA-256 digest: not-a-hex-digest\n"
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            apksignerOutput = malformed,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK signing certificate", result.failures.single().check)
        assertTrue(result.failures.single().actual.contains("no certificate digest found"))
        assertTrue(result.failures.single().actual.contains("V2 Signer: certificate SHA-256 digest: not-a-hex-digest"))
    }

    // --- SHA-256 ------------------------------------------------------------

    @Test
    fun `sha256 of empty bytes is the empty-input digest`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ReleaseApkVerifier.sha256(ByteArray(0)),
        )
    }
}