package com.techvisiondz.build

import com.techvisiondz.build.ReleaseApkVerifier.allDescendants
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

    // aapt2 (Build Tools 36) dump xmltree AndroidManifest.xml shape: attribute
    // names are full namespace URIs and simple values are bare booleans/integers.
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
            E: uses-permission (line=11)
              A: http://schemas.android.com/apk/res/android:name(0x01010003)="android.permission.REQUEST_INSTALL_PACKAGES" (Raw: "android.permission.REQUEST_INSTALL_PACKAGES")
            E: queries (line=27)
              E: intent (line=29)
                E: action (line=30)
                  A: http://schemas.android.com/apk/res/android:name(0x01010003)="android.intent.action.VIEW" (Raw: "android.intent.action.VIEW")
                E: data (line=31)
                  A: http://schemas.android.com/apk/res/android:mimeType(0x01010011)="application/vnd.android.package-archive" (Raw: "application/vnd.android.package-archive")
            E: application (line=36)
              A: http://schemas.android.com/apk/res/android:allowBackup(0x01010080)=false
              E: activity (line=46)
                A: http://schemas.android.com/apk/res/android:name(0x01010003)="com.techvisiondz.app.MainActivity" (Raw: "com.techvisiondz.app.MainActivity")
              E: provider (line=113)
                A: http://schemas.android.com/apk/res/android:name(0x01010003)="androidx.core.content.FileProvider" (Raw: "androidx.core.content.FileProvider")
                A: http://schemas.android.com/apk/res/android:authorities(0x01010018)="com.techvisiondz.app.fileprovider" (Raw: "com.techvisiondz.app.fileprovider")
                A: http://schemas.android.com/apk/res/android:exported(0x01010010)=false
                A: http://schemas.android.com/apk/res/android:grantUriPermissions(0x0101001b)=true
                E: meta-data (line=116)
                  A: http://schemas.android.com/apk/res/android:name(0x01010003)="android.support.FILE_PROVIDER_PATHS" (Raw: "android.support.FILE_PROVIDER_PATHS")
                  A: http://schemas.android.com/apk/res/android:resource(0x01010025)=@0x7f0c0002 (Raw: "res/xml/file_paths")
    """.trimIndent()

    // aapt2 resource dump of the (obfuscated) file_paths XML: attrs drop the
    // android: namespace prefix and the 0x resource id.
    private val realFilePathsXml = """
          E: paths (line=10)
            E: cache-path (line=13)
              A: name="updater" (Raw: "updater")
              A: path="updater/" (Raw: "updater/")
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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
            manifestXmlTreeOutput = realManifestXml,
            filePathsXmlTreeOutput = realFilePathsXml,
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

    // --- Structure / signature tooling --------------------------------------

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

    // --- SHA-256 ------------------------------------------------------------

    @Test
    fun `sha256 of empty bytes is the empty-input digest`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ReleaseApkVerifier.sha256(ByteArray(0)),
        )
    }

    // --- Manifest / file-paths XML structure (xmltree) -------------------

    @Test
    fun `parseXmlTree reads names authorities and boolean attributes`() {
        val tree = ReleaseApkVerifier.parseXmlTree(realManifestXml)
        assertEquals("manifest", tree.single().tag)

        val provider = tree.flatMap { it.allDescendants() }
            .single { it.tag == "provider" }
        assertEquals("androidx.core.content.FileProvider", provider.attributes["android:name"])
        assertEquals("com.techvisiondz.app.fileprovider", provider.attributes["android:authorities"])
        assertEquals("0x0", provider.attributes["android:exported"])
        assertEquals("0xffffffff", provider.attributes["android:grantUriPermissions"])
    }

    @Test
    fun `parseXmlTree reads the cache path element with bare namespace-less attributes`() {
        val tree = ReleaseApkVerifier.parseXmlTree(realFilePathsXml)
        val cachePath = tree.flatMap { it.allDescendants() }.single { it.tag == "cache-path" }
        assertEquals("updater", ReleaseApkVerifier.attr(cachePath, "android:name"))
        assertEquals("updater/", ReleaseApkVerifier.attr(cachePath, "android:path"))
    }

    @Test
    fun `manifest xml is unreadable when aapt2 produced no output`() {
        val failures = ReleaseApkVerifier.verifyManifestStructure(null, expected)
        assertFalse(failures.isEmpty())
        assertTrue(failures.single().check.contains("manifest XML can be read"))
    }

    @Test
    fun `missing fileprovider fails the manifest check`() {
        val xml = realManifestXml.replace(
            "androidx.core.content.FileProvider",
            "androidx.core.content.SomeOtherProvider",
        )
        val failures = ReleaseApkVerifier.verifyManifestStructure(xml, expected)
        assertTrue(failures.any { it.check == "AndroidManifest declares the update FileProvider" })
    }

    @Test
    fun `wrong fileprovider authority fails the manifest check`() {
        val xml = realManifestXml.replace(
            "com.techvisiondz.app.fileprovider",
            "com.other.app.fileprovider",
        )
        val failures = ReleaseApkVerifier.verifyManifestStructure(xml, expected)
        assertTrue(failures.any { it.check == "Update FileProvider authority" })
    }

    @Test
    fun `exported fileprovider fails the manifest check`() {
        val xml = realManifestXml.replace(
            "A: http://schemas.android.com/apk/res/android:exported(0x01010010)=false",
            "A: http://schemas.android.com/apk/res/android:exported(0x01010010)=true",
        )
        val failures = ReleaseApkVerifier.verifyManifestStructure(xml, expected)
        assertTrue(failures.any { it.check == "Update FileProvider is not exported" })
    }

    @Test
    fun `fileprovider without uri grants fails the manifest check`() {
        val xml = realManifestXml.replace(
            "A: http://schemas.android.com/apk/res/android:grantUriPermissions(0x0101001b)=true",
            "A: http://schemas.android.com/apk/res/android:grantUriPermissions(0x0101001b)=false",
        )
        val failures = ReleaseApkVerifier.verifyManifestStructure(xml, expected)
        assertTrue(failures.any { it.check == "Update FileProvider grants URI permissions" })
    }

    @Test
    fun `missing queries block fails the manifest check`() {
        val withoutQueries = realManifestXml.replace("E: queries (line=27)\n", "")
        val failures = ReleaseApkVerifier.verifyManifestStructure(withoutQueries, expected)
        assertTrue(failures.any { it.check.contains("<queries>") })
    }

    @Test
    fun `file paths xml is unreadable when aapt2 produced no output`() {
        val failures = ReleaseApkVerifier.verifyFilePathsStructure(null, expected)
        assertFalse(failures.isEmpty())
        assertTrue(failures.single().check.contains("file-paths XML can be read"))
    }

    @Test
    fun `file paths xml without the updater cache path fails`() {
        val xml = realFilePathsXml.replace(
            """A: path="updater/" (Raw: "updater/")""",
            """A: path="other/" (Raw: "other/")""",
        )
        val failures = ReleaseApkVerifier.verifyFilePathsStructure(xml, expected)
        assertTrue(failures.any { it.check.contains("updater cache path") })
    }

    @Test
    fun `conformant xml passes the structural checks`() {
        assertTrue(ReleaseApkVerifier.verifyManifestStructure(realManifestXml, expected).isEmpty())
        assertTrue(ReleaseApkVerifier.verifyFilePathsStructure(realFilePathsXml, expected).isEmpty())
    }

    @Test
    fun `providerConfigurationOk reports valid and invalid configurations`() {
        assertTrue(
            ReleaseApkVerifier.providerConfigurationOk(
                realManifestXml,
                "com.techvisiondz.app.fileprovider",
            ),
        )
        assertFalse(
            ReleaseApkVerifier.providerConfigurationOk(
                realManifestXml,
                "com.other.fileprovider",
            ),
        )
        assertFalse(ReleaseApkVerifier.providerConfigurationOk(null, "com.techvisiondz.app.fileprovider"))
    }
}