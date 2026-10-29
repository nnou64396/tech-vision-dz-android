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

    private val realBadging = """
        package: name='com.techvisiondz.app' versionCode='6' versionName='1.1.3' platformBuildVersionName='17'
        platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
        sdkVersion:'26'
        targetSdkVersion:'37'
        uses-permission: name='android.permission.INTERNET'
        uses-permission: name='com.techvisiondz.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
    """.trimIndent()

    private val realManifestXml = """
        N: android=http://schemas.android.com/apk/res/android
          E: manifest (line=2)
            A: http://schemas.android.com/apk/res/android:versionCode(0x0101021b)=8
            A: http://schemas.android.com/apk/res/android:versionName(0x0101021c)="1.1.5" (Raw: "1.1.5")
            A: package="com.techvisiondz.app" (Raw: "com.techvisiondz.app")
            E: uses-sdk (line=7)
              A: http://schemas.android.com/apk/res/android:minSdkVersion(0x0101020c)=26
              A: http://schemas.android.com/apk/res/android:targetSdkVersion(0x01010270)=37
            E: uses-permission (line=9)
              A: http://schemas.android.com/apk/res/android:name(0x01010003)="android.permission.INTERNET" (Raw: "android.permission.INTERNET")
            E: application (line=36)
              A: http://schemas.android.com/apk/res/android:allowBackup(0x01010080)=false
              E: activity (line=46)
                A: http://schemas.android.com/apk/res/android:name(0x01010003)="com.techvisiondz.app.MainActivity" (Raw: "com.techvisiondz.app.MainActivity")
    """.trimIndent()

    private val realFilePathsXml = """
          E: paths (line=10)
    """.trimIndent()

    private val realApksigner = """
        Signer #1 certificate DN: CN=TECH VISION DZ, OU=Android, O=TECH VISION DZ, C=DZ
        Signer #1 certificate SHA-256 digest: 1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33
        Signer #1 certificate SHA-1 digest: 24c967e2b4f66d32c61aeedf9afe2a51e7a24609
        Signer #1 certificate MD5 digest: 1e4dd176598d67c0e1e692895f1f7eda
    """.trimIndent()

    private val dummySha256 = "0".repeat(64)

    @Test
    fun `conformant apk passes every check`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )
        assertTrue(result.failures.joinToString("\n") { it.render() }, result.passed)
    }

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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = buildTools37Certs,
            apksignerExitOk = true,
        )
        assertTrue(result.failures.joinToString("\n") { it.render() }, result.passed)
    }

    @Test
    fun `declaring request install permission fails the build`() {
        val badgingWithPermission = realBadging + "\n        uses-permission: name='android.permission.REQUEST_INSTALL_PACKAGES'"
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = badgingWithPermission,
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        val failure = result.failures.single()
        assertTrue(failure.render(), failure.check.contains("REQUEST_INSTALL_PACKAGES"))
        assertEquals("permission absent", failure.expected)
        assertTrue(failure.render(), failure.actual.contains("permission declared"))
    }

    @Test
    fun `declaring update FileProvider fails the build`() {
        val manifestWithProvider = realManifestXml + "\n          E: provider (line=113)\n            A: android:name=\"androidx.core.content.FileProvider\""
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            manifestXmlTreeOutput = manifestWithProvider,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertTrue(result.failures.any { it.check.contains("FileProvider must be removed") })
    }

    @Test
    fun `declaring installer queries fails the manifest check`() {
        val manifestWithQueries = """
            N: android=http://schemas.android.com/apk/res/android
              E: manifest (line=2)
                E: queries (line=27)
                  E: intent (line=29)
                    E: action (line=30)
                      A: android:name="android.intent.action.VIEW"
                    E: data (line=31)
                      A: android:mimeType="application/vnd.android.package-archive"
        """.trimIndent()
        val failures = ReleaseApkVerifier.verifyManifestStructure(manifestWithQueries, expected)
        assertTrue(failures.any { it.check.contains("Installer <queries> must be removed") })
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = apksignerWithoutDigest,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK signing certificate", result.failures.single().check)
        assertTrue(result.failures.single().actual.contains("no certificate digest found"))
        assertTrue(result.failures.single().actual.contains("CN=Unknown"))
    }

    @Test
    fun `wrong application id fails the build`() {
        val badging = realBadging.replace("com.techvisiondz.app", "com.other.app")
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = badging,
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals(2, result.failures.size)
        assertTrue(result.failures.any { it.check == "Release APK versionCode" })
        assertTrue(result.failures.any { it.check == "Release APK versionName" })
    }

    @Test
    fun `apksigner nonzero exit or missing output fails the build`() {
        val result = ReleaseApkVerifier.verify(
            expected = expected,
            apkSha256 = dummySha256,
            badgingOutput = realBadging,
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = realApksigner,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertTrue(result.failures.single().check.contains("package metadata"))
    }

    @Test
    fun `parseBadging extracts metadata and permissions`() {
        val info = ReleaseApkVerifier.parseBadging(realBadging)!!

        assertEquals("com.techvisiondz.app", info.applicationId)
        assertEquals("6", info.versionCode)
        assertEquals("1.1.3", info.versionName)
        assertFalse(info.permissions.contains(ReleaseApkVerifier.REQUIRED_INSTALL_PERMISSION))
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
            "1FBF843195367E3895D1BB614481F7A3D7F0DB1DA8FFE69EBA3CE4716BE20d33",
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
            apksignerOutput = malformed,
            apksignerExitOk = true,
        )

        assertFalse(result.passed)
        assertEquals("Release APK signing certificate", result.failures.single().check)
        assertTrue(result.failures.single().actual.contains("no certificate digest found"))
        assertTrue(result.failures.single().actual.contains("V2 Signer: certificate SHA-256 digest: not-a-hex-digest"))
    }

    @Test
    fun `sha256 of empty bytes is the empty-input digest`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ReleaseApkVerifier.sha256(ByteArray(0)),
        )
    }

    @Test
    fun `manifest xml is unreadable when aapt2 produced no output`() {
        val failures = ReleaseApkVerifier.verifyManifestStructure(null, expected)
        assertFalse(failures.isEmpty())
        assertTrue(failures.single().check.contains("manifest XML can be read"))
    }

    @Test
    fun `file paths xml is unreadable when aapt2 produced no output`() {
        val failures = ReleaseApkVerifier.verifyFilePathsStructure(null, expected)
        assertFalse(failures.isEmpty())
        assertTrue(failures.single().check.contains("file-paths XML can be read"))
    }

    @Test
    fun `conformant xml passes the structural checks`() {
        assertTrue(ReleaseApkVerifier.verifyManifestStructure(realManifestXml, expected).isEmpty())
        assertTrue(ReleaseApkVerifier.verifyFilePathsStructure(realFilePathsXml, expected).isEmpty())
    }
}
