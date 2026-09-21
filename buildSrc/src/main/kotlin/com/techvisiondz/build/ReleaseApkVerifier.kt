package com.techvisiondz.build

import java.io.File
import java.security.MessageDigest

/**
 * Pure verification rules for the final signed release APK.
 *
 * The Gradle task (`verifyReleaseApk`) collects the raw evidence with SDK tools
 * (`aapt2`, `apksigner`) and hands it to [verify], which decides pass/fail. Keeping
 * the rules free of I/O makes them unit-testable, including the negative paths
 * (missing permission, wrong certificate, version drift).
 *
 * Nothing here reads or prints private signing material: only the public
 * certificate digest (which is embedded in every APK anyway) is compared.
 */
object ReleaseApkVerifier {

    const val REQUIRED_INSTALL_PERMISSION = "android.permission.REQUEST_INSTALL_PACKAGES"

    /** Everything the task expects to find in the final APK. */
    data class Expected(
        val applicationId: String,
        val versionCode: String,
        val versionName: String,
        val certificateSha256: String,
        val requiredPermission: String = REQUIRED_INSTALL_PERMISSION,
    )

    /** One failed check, formatted for humans per the project's failure-message contract. */
    data class CheckFailure(
        val check: String,
        val expected: String,
        val actual: String,
        val fix: String,
    ) {
        fun render(prefix: String = "FAIL: "): String = buildString {
            append(prefix).append(check).append('\n')
            append("Expected: ").append(expected).append('\n')
            append("Actual: ").append(actual).append('\n')
            append("Fix: ").append(fix).append('\n')
        }
    }

    /** Metadata parsed from `aapt2 dump badging` output. */
    data class ApkInfo(
        val applicationId: String,
        val versionCode: String,
        val versionName: String,
        val permissions: List<String>,
    )

    data class Result(val apkSha256: String, val failures: List<CheckFailure>) {
        val passed: Boolean get() = failures.isEmpty()
    }

    /**
     * Runs every rule and returns the aggregate result.
     *
     * @param badgingOutput raw stdout+stderr of `aapt2 dump badging`, or null when the tool failed
     * @param apksignerOutput raw stdout+stderr of `apksigner verify --print-certs`, or null when it failed
     * @param apksignerExitOk true when `apksigner verify` exited 0 (structure + signature valid)
     */
    fun verify(
        expected: Expected,
        apkSha256: String,
        badgingOutput: String?,
        apksignerOutput: String?,
        apksignerExitOk: Boolean,
    ): Result {
        val failures = mutableListOf<CheckFailure>()

        if (badgingOutput == null) {
            failures += CheckFailure(
                check = "APK manifest can be read",
                expected = "aapt2 dump badging succeeds and reports package metadata",
                actual = "aapt2 failed or produced no output",
                fix = "The APK is corrupt or not an Android package. Rebuild the release APK and rerun.",
            )
        } else {
            val info = parseBadging(badgingOutput)
            if (info == null) {
                failures += CheckFailure(
                    check = "APK package metadata",
                    expected = "a 'package:' line with name/versionCode/versionName in aapt2 output",
                    actual = "no package metadata found",
                    fix = "The APK manifest is unreadable; rebuild a valid release APK.",
                )
            } else {
                if (info.applicationId != expected.applicationId) {
                    failures += CheckFailure(
                        check = "Release APK application ID",
                        expected = expected.applicationId,
                        actual = info.applicationId,
                        fix = "Set applicationId to '${expected.applicationId}' in defaultConfig (build.gradle.kts) and rebuild.",
                    )
                }
                if (info.versionCode != expected.versionCode) {
                    failures += CheckFailure(
                        check = "Release APK versionCode",
                        expected = expected.versionCode,
                        actual = info.versionCode,
                        fix = "versionCode in the APK must match the Gradle configuration; bump/revert it in defaultConfig and rebuild.",
                    )
                }
                if (info.versionName != expected.versionName) {
                    failures += CheckFailure(
                        check = "Release APK versionName",
                        expected = expected.versionName,
                        actual = info.versionName,
                        fix = "versionName in the APK must match the Gradle configuration; update it in defaultConfig and rebuild.",
                    )
                }
                if (info.permissions.none { it == expected.requiredPermission }) {
                    failures += CheckFailure(
                        check = "Release APK declares REQUEST_INSTALL_PACKAGES",
                        expected = expected.requiredPermission,
                        actual = "permission missing (declared: ${info.permissions.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "none"})",
                        fix = "Add <uses-permission android:name=\"${expected.requiredPermission}\"/> to AndroidManifest.xml and rebuild. " +
                            "This APK must not be released because the in-app updater cannot install future updates without it.",
                    )
                }
            }
        }

        if (apksignerOutput == null || !apksignerExitOk) {
            failures += CheckFailure(
                check = "APK is structurally valid and correctly signed",
                expected = "apksigner verify --print-certs exits 0",
                actual = "apksigner verify failed (structure or signature invalid)" +
                    apksignerOutput?.lineSequence()?.take(3)?.joinToString(prefix = "\n  ", separator = "\n  ").orEmpty(),
                fix = "The release APK must be signed with the production key. Check keystore.properties / signing config and rebuild.",
            )
        } else {
            val certificateSha256 = parseFirstSignerSha256(apksignerOutput)
            if (certificateSha256 == null) {
                failures += CheckFailure(
                    check = "Release APK signing certificate",
                    expected = "Signer #1 SHA-256 digest in apksigner output",
                    actual = "no certificate digest found",
                    fix = "The APK is signed but its certificate could not be read; verify the APK was packaged by AGP.",
                )
            } else if (certificateSha256 != expected.certificateSha256.lowercase()) {
                failures += CheckFailure(
                    check = "Release APK signing certificate",
                    expected = "SHA-256 ${expected.certificateSha256.lowercase()}",
                    actual = "SHA-256 $certificateSha256",
                    fix = "Release builds must use the production signing certificate. This APK must not be released.",
                )
            }
        }

        return Result(apkSha256, failures)
    }

    /**
     * Parses the metadata out of `aapt2 dump badging` output.
     * Returns null when the package line is missing or incomplete.
     */
    fun parseBadging(output: String): ApkInfo? {
        val packageLine = output.lineSequence().firstOrNull { it.startsWith("package: name=") } ?: return null
        val values = parseQuotedValues(packageLine)
        val applicationId = values["name"] ?: return null
        val versionCode = values["versionCode"] ?: return null
        val versionName = values["versionName"] ?: return null
        val permissions = output.lineSequence()
            .filter { it.startsWith("uses-permission: name=") }
            .mapNotNull { parseQuotedValues(it)["name"] }
            .toList()
        return ApkInfo(applicationId, versionCode, versionName, permissions)
    }

    /** First Signer #1 (v2/v3) certificate SHA-256 from `apksigner verify --print-certs` output. */
    fun parseFirstSignerSha256(output: String): String? =
        output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Signer #1 certificate SHA-256 digest:") }
            ?.substringAfter("digest:")
            ?.trim()
            ?.lowercase()

    fun sha256(file: File): String =
        file.inputStream().use { input -> sha256(input.readBytes()) }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun parseQuotedValues(line: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        for (match in Regex("([A-Za-z0-9_]+)='([^']*)'").findAll(line)) {
            values[match.groupValues[1]] = match.groupValues[2]
        }
        return values
    }
}