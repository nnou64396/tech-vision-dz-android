import com.android.build.api.artifact.SingleArtifact
import com.techvisiondz.build.ReleaseApkVerifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Public digest of the production upload signing certificate. It is embedded in
// every released APK (not a secret) and is the exact certificate the updater's
// verification rules require. verifyReleaseApk refuses any other certificate.
private val productionSigningCertSha256 =
    "1fbf843195367e3895d1bb614481f7a3d7f0db1da8ffe69eba3ce4716be20d33"

// Local (untracked) configuration such as Supabase URL / anon key.
// Read from local.properties only (gitignored), so secrets are never committed.
val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}

fun secret(name: String): String = localProps.getProperty(name) ?: ""

// Release (upload) signing credentials. Read from the gitignored
// keystore.properties so secrets are never committed. When the file is
// absent (e.g. a fresh clone without signing access), release stays
// unsigned and a warning is emitted instead of failing the build.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.techvisiondz.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.techvisiondz.app"
        minSdk = 26
        targetSdk = 37
        // versionCode is the authoritative update ordering key. versionName is
        // display-only and normalized to match the v1.0.0 GitHub release.
        versionCode = 9
        versionName = "1.1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public client configuration. These are NOT secret credentials and are
        // overridden per environment below.
        buildConfigField("String", "SUPABASE_URL", "\"${secret("TECHVISION_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("TECHVISION_SUPABASE_ANON_KEY")}\"")
        // Public article website base used to build canonical share URLs. Public,
        // not a secret — kept here to follow the same config architecture.
        buildConfigField("String", "ARTICLE_BASE_URL", "\"${secret("TECHVISION_ARTICLE_BASE_URL")}\"")
        // Temporary pre-Google-Play in-app updater configuration. The manifest
        // URL is a single pinned value (a stable GitHub Releases "latest" asset)
        // so remote versions can never point the app at an arbitrary host.
        // Flip UPDATER_ENABLED to false once the app is distributed via Google
        // Play and the manual updater is retired.
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json\"",
        )
        buildConfigField("boolean", "UPDATER_ENABLED", "true")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("keystore.properties not found: release builds will not use the upload signing config.")
            }
            // Enables code + resource optimization (R8) for AGP 9.3+.
            // Keep rules live in src/main/keepRules/*.keep.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/**
 * Verifies the final signed release APK produced by the current release build.
 *
 * Runs `aapt2` and `apksigner` (resolved from the Android SDK), then applies the
 * pure rules in [com.techvisiondz.build.ReleaseApkVerifier]. It depends on
 * [assembleRelease] so the verified artifact is always freshly generated by this
 * build — never a stale APK left over from an earlier run.
 */
abstract class VerifyReleaseApkTask : DefaultTask() {

    @get:InputFile
    abstract val apkFile: RegularFileProperty

    @get:Input
    abstract val expectedApplicationId: Property<String>

    @get:Input
    abstract val expectedVersionCode: Property<String>

    @get:Input
    abstract val expectedVersionName: Property<String>

    @get:Input
    abstract val expectedCertificateSha256: Property<String>

    @get:Internal
    abstract val sdkDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val apk = apkFile.get().asFile
        val failures = mutableListOf<ReleaseApkVerifier.CheckFailure>()
        var detected: Detected? = null

        if (!apk.isFile) {
            failures += ReleaseApkVerifier.CheckFailure(
                check = "Release APK exists",
                expected = "APK file present at ${apk.absolutePath}",
                actual = "no file at ${apk.absolutePath}",
                fix = "verifyReleaseApk runs after assembleRelease; ensure the release build completed and produced the APK.",
            )
        } else {
            val buildToolsDir = resolveBuildToolsDir()
            if (buildToolsDir == null) {
                failures += ReleaseApkVerifier.CheckFailure(
                    check = "Android build-tools available",
                    expected = "a build-tools version containing aapt2 and apksigner under <SDK>/build-tools",
                    actual = "none found under ${sdkDirectory.get().asFile.absolutePath}",
                    fix = "Install a build-tools package via the Android SDK Manager (or let AGP install its default) and rerun.",
                )
            } else {
                val aapt2 = sdkExecutable(buildToolsDir, "aapt2")
                val apksigner = sdkExecutable(buildToolsDir, "apksigner")
                if (aapt2 == null || apksigner == null) {
                    failures += ReleaseApkVerifier.CheckFailure(
                        check = "Android build-tools available",
                        expected = "aapt2 and apksigner executables in ${buildToolsDir.absolutePath}",
                        actual = "aapt2 found=${aapt2 != null}, apksigner found=${apksigner != null}",
                        fix = "Reinstall or update the build-tools package via the Android SDK Manager.",
                    )
                } else {
                    val badging = runSdkTool(aapt2, listOf("dump", "badging"), apk, "aapt2 dump badging")
                    val manifestXml = runSdkTool(
                        aapt2,
                        listOf("dump", "xmltree", "--file", "AndroidManifest.xml"),
                        apk,
                        "aapt2 dump xmltree AndroidManifest.xml",
                    )
                    val filePathsXml = runSdkTool(
                        aapt2,
                        listOf(
                            "dump",
                            "xmltree",
                            "--file",
                            resolvePackagedResourcePath(aapt2, apk, "xml/file_paths") ?: "res/xml/file_paths.xml",
                        ),
                        apk,
                        "aapt2 dump xmltree res/xml/file_paths.xml",
                    )
                    val apkSign = runSdkTool(apksigner, listOf("verify", "--print-certs"), apk, "apksigner verify")

                    if (apk.length() == 0L) {
                        failures += ReleaseApkVerifier.CheckFailure(
                            check = "Release APK exists",
                            expected = "non-empty APK file",
                            actual = "file is 0 bytes",
                            fix = "The release build produced an empty APK; clean and rebuild.",
                        )
                    }

                    val apkSha256 = ReleaseApkVerifier.sha256(apk)
                    val report = ReleaseApkVerifier.verify(
                        expected = ReleaseApkVerifier.Expected(
                            applicationId = expectedApplicationId.get(),
                            versionCode = expectedVersionCode.get(),
                            versionName = expectedVersionName.get(),
                            certificateSha256 = expectedCertificateSha256.get(),
                        ),
                        apkSha256 = apkSha256,
                        badgingOutput = badging?.output,
                        manifestXmlTreeOutput = manifestXml?.output,
                        filePathsXmlTreeOutput = filePathsXml?.output,
                        apksignerOutput = apkSign?.output,
                        apksignerExitOk = apkSign?.exitOk == true,
                    )
                    failures += report.failures
                    detected = Detected(
                        apkPath = apk.absolutePath,
                        applicationId = reportApplicationId(badging?.output),
                        versionCode = reportVersionCode(badging?.output),
                        versionName = reportVersionName(badging?.output),
                        permissionDeclared = reportPermissionDeclared(badging?.output),
                        providerConfigurationOk = ReleaseApkVerifier.providerConfigurationOk(
                            manifestXml?.output,
                            "${expectedApplicationId.get()}.fileprovider",
                        ),
                        certificateSha256 = reportCertificateSha256(apkSign?.output),
                        apkSha256 = apkSha256,
                    )
                }
            }
        }

        if (failures.isEmpty()) {
            val d = detected!!
            logger.lifecycle(
                """
                VERIFY OK: release APK passed all checks.
                  path: ${d.apkPath}
                  applicationId: ${d.applicationId}
                  versionCode: ${d.versionCode}
                  versionName: ${d.versionName}
                  REQUEST_INSTALL_PACKAGES: ${if (d.permissionDeclared) "declared" else "missing"}
                  FileProvider config: ${if (d.providerConfigurationOk) "authority/exported/grant/paths ok" else "invalid"}
                  signing certificate SHA-256: ${d.certificateSha256}
                  APK SHA-256: ${d.apkSha256}
                """.trimIndent(),
            )
        } else {
            val message = failures.joinToString("\n") { it.render() }
            logger.lifecycle(message)
            throw GradleException("verifyReleaseApk FAILED: ${failures.size} check(s) did not pass.")
        }
    }

    /** Highest installed build-tools version that bundles both aapt2 and apksigner. */
    private fun resolveBuildToolsDir(): File? {
        val buildTools = File(sdkDirectory.get().asFile, "build-tools")
        if (!buildTools.isDirectory) return null
        return buildTools.listFiles()
            ?.filter { it.isDirectory && hasTools(it) }
            ?.maxWithOrNull(Comparator { a, b ->
                compareVersionParts(versionParts(a.name), versionParts(b.name))
            })
    }

    private fun compareVersionParts(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    private fun hasTools(dir: File): Boolean {
        val aapt2 = listOf("aapt2.exe", "aapt2").any { File(dir, it).isFile }
        val apksigner = listOf("apksigner.bat", "apksigner.exe", "apksigner").any { File(dir, it).isFile }
        return aapt2 && apksigner
    }

    private fun sdkExecutable(buildTools: File, base: String): File? {
        val names = if (isWindows()) listOf("$base.exe", "$base.bat") else listOf(base)
        return names.map { File(buildTools, it) }.firstOrNull { it.isFile }
    }

    private fun versionParts(name: String): List<Int> = name.split(".").map { it.toIntOrNull() ?: -1 }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    /**
     * Maps a resource table name (`xml/file_paths`) to its packaged path inside
     * the APK (`res/8K.xml`). Release builds obfuscate XML resource file names,
     * so the conventional path is not present and must be looked up.
     */
    private fun resolvePackagedResourcePath(aapt2: File, apk: File, resourceName: String): String? {
        val resources = runSdkTool(aapt2, listOf("dump", "resources"), apk, "aapt2 dump resources") ?: return null
        val lines = resources.output.lineSequence().toList()
        val marker = lines.indexOfFirst { Regex("""resource 0x[0-9a-fA-F]+ $resourceName$""").containsMatchIn(it.trim()) }
        if (marker < 0) return null
        val fileLine = lines.drop(marker + 1).take(6).firstOrNull { "(file)" in it } ?: return null
        return Regex("""res/\S+""").find(fileLine)?.value
    }

    private fun runSdkTool(executable: File, args: List<String>, apk: File, what: String): ToolOutput? {
        val sink = ByteArrayOutputStream()
        val command = if (isWindows() && executable.name.endsWith(".bat")) {
            listOf("cmd", "/c", executable.absolutePath) + args + apk.absolutePath
        } else {
            listOf(executable.absolutePath) + args + apk.absolutePath
        }
        return try {
            val result = execOperations.exec {
                commandLine(command)
                // apksigner.bat needs a real JVM; point it at the Gradle JVM so the
                // tool works regardless of the caller's environment.
                environment("JAVA_HOME", File(System.getProperty("java.home")).absolutePath)
                isIgnoreExitValue = true
                standardOutput = sink
                errorOutput = sink
            }
            ToolOutput(result.exitValue == 0, sink.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            logger.warn("Could not run $what: ${e.message}")
            null
        }
    }

    private data class ToolOutput(val exitOk: Boolean, val output: String)

    private data class Detected(
        val apkPath: String,
        val applicationId: String,
        val versionCode: String,
        val versionName: String,
        val permissionDeclared: Boolean,
        val providerConfigurationOk: Boolean,
        val certificateSha256: String,
        val apkSha256: String,
    )

    private fun reportApplicationId(badging: String?): String = parseBadging(badging)?.applicationId ?: "unknown"
    private fun reportVersionCode(badging: String?): String = parseBadging(badging)?.versionCode ?: "unknown"
    private fun reportVersionName(badging: String?): String = parseBadging(badging)?.versionName ?: "unknown"
    private fun reportPermissionDeclared(badging: String?): Boolean =
        parseBadging(badging)?.permissions?.contains(ReleaseApkVerifier.REQUIRED_INSTALL_PERMISSION) == true

    private fun reportCertificateSha256(apksignerOutput: String?): String =
        apksignerOutput?.let { ReleaseApkVerifier.parseFirstSignerSha256(it) } ?: "unknown"

    private fun parseBadging(badging: String?): ReleaseApkVerifier.ApkInfo? =
        badging?.let { ReleaseApkVerifier.parseBadging(it) }
}

private val appExtension =
    extensions.getByType<com.android.build.api.dsl.ApplicationExtension>()

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        tasks.register<VerifyReleaseApkTask>("verifyReleaseApk") {
            group = "verification"
            description = "Verifies the final signed release APK: applicationId, versionCode/versionName, REQUEST_INSTALL_PACKAGES, FileProvider install configuration, production signing certificate, structural validity, and SHA-256."
            // Always verify the artifact freshly produced by the current release build.
            dependsOn("assembleRelease")
            // Resolved from AGP, so this matches the actual artifact of this build
            // (not a hardcoded or stale path) and the source-of-truth version values.
            // APK is a Directory artifact in AGP 9; the directory holds exactly one
            // built APK for this app, resolved here instead of hardcoding a filename.
            apkFile.set(
                variant.artifacts.get(SingleArtifact.APK).map { outputDir ->
                    val apkFiles =
                        outputDir.asFile.listFiles { f -> f.isFile && f.extension == "apk" }.orEmpty()
                    check(apkFiles.size == 1) {
                        "Expected exactly one release APK in ${outputDir.asFile.absolutePath} " +
                            "(found ${apkFiles.size}); nothing safe to verify."
                    }
                    outputDir.file(apkFiles.single().name)
                },
            )
            expectedApplicationId.set(variant.applicationId)
            expectedVersionCode.set(appExtension.defaultConfig.versionCode?.toString())
            expectedVersionName.set(appExtension.defaultConfig.versionName)
            expectedCertificateSha256.set(productionSigningCertSha256)
            sdkDirectory.set(sdkComponents.sdkDirectory)
        }
    }
}
