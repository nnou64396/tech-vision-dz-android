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

    /** The FileProvider class the update flow depends on. */
    const val UPDATE_FILE_PROVIDER_CLASS = "androidx.core.content.FileProvider"

    /** Attribute-data path the provider must expose for the staged update APK. */
    const val UPDATE_FILE_PROVIDER_PATH_NAME = "updater"
    const val UPDATE_FILE_PROVIDER_PATH = "updater/"

    /** Intent the <queries> block must declare so the installer can be resolved on API 30+. */
    const val INSTALL_INTENT_ACTION = "android.intent.action.VIEW"
    const val INSTALL_INTENT_MIME = "application/vnd.android.package-archive"

    /** Everything the task expects to find in the final APK. */
    data class Expected(
        val applicationId: String,
        val versionCode: String,
        val versionName: String,
        val certificateSha256: String,
        val requiredPermission: String = REQUIRED_INSTALL_PERMISSION,
    ) {
        /** The FileProvider authority the release manifest must declare. */
        val fileProviderAuthority: String get() = "$applicationId.fileprovider"
    }

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
     * @param manifestXmlTreeOutput raw stdout+stderr of `aapt2 dump xmltree --file AndroidManifest.xml`,
     * or null when the tool failed. Null always fails: the FileProvider/`<queries>`
     * install configuration is a hard release invariant.
     * @param filePathsXmlTreeOutput raw stdout+stderr of `aapt2 dump xmltree --file res/xml/file_paths.xml`,
     * or null when the tool failed. Null always fails for the same reason.
     * @param apksignerOutput raw stdout+stderr of `apksigner verify --print-certs`, or null when it failed
     * @param apksignerExitOk true when `apksigner verify` exited 0 (structure + signature valid)
     */
    fun verify(
        expected: Expected,
        apkSha256: String,
        badgingOutput: String?,
        manifestXmlTreeOutput: String?,
        filePathsXmlTreeOutput: String?,
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
                    expected = "a 64-char hex certificate SHA-256 digest in apksigner output",
                    actual = "no certificate digest found" + apksignerOutputExcerpt(apksignerOutput),
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

        failures += verifyManifestStructure(manifestXmlTreeOutput, expected)
        failures += verifyFilePathsStructure(filePathsXmlTreeOutput, expected)

        return Result(apkSha256, failures)
    }

    /**
     * Validates the merged AndroidManifest.xml (from `aapt2 dump xmltree`) for the
     * update install path:
     *  - the FileProvider exists, uses the exact `${applicationId}.fileprovider`
     *    authority, is not exported, and grants URI permissions;
     *  - the `<queries>` block declares the install VIEW intent so the system
     *    package installer can be resolved on Android 11+.
     */
    internal fun verifyManifestStructure(
        output: String?,
        expected: Expected,
    ): List<CheckFailure> {
        val failures = mutableListOf<CheckFailure>()
        if (output == null) {
            failures += CheckFailure(
                check = "APK manifest XML can be read",
                expected = "aapt2 dump xmltree AndroidManifest.xml succeeds and reports the merged manifest",
                actual = "aapt2 failed or produced no output for AndroidManifest.xml",
                fix = "The APK is corrupt or has no readable manifest. Rebuild the release APK and rerun.",
            )
            return failures
        }

        val tree = parseXmlTree(output)
        val provider = treeProvider(tree, UPDATE_FILE_PROVIDER_CLASS)
        if (provider == null) {
            failures += CheckFailure(
                check = "AndroidManifest declares the update FileProvider",
                expected = "a <provider> with android:name=\"${UPDATE_FILE_PROVIDER_CLASS}\" in the merged manifest",
                actual = "no such provider found",
                fix = "Add the FileProvider block (androidx.core.content.FileProvider) to AndroidManifest.xml and rebuild. " +
                    "The in-app updater cannot hand the verified APK to the system installer without it.",
            )
        } else {
            val authority = attr(provider, "android:authorities")
            if (authority != expected.fileProviderAuthority) {
                failures += CheckFailure(
                    check = "Update FileProvider authority",
                    expected = expected.fileProviderAuthority,
                    actual = authority ?: "missing android:authorities",
                    fix = "Set android:authorities=\"\${applicationId}.fileprovider\" on the FileProvider and rebuild.",
                )
            }
            val exported = attr(provider, "android:exported")
            if (exported != "0x0") {
                failures += CheckFailure(
                    check = "Update FileProvider is not exported",
                    expected = "android:exported=\"false\" (0x0)",
                    actual = exported ?: "missing android:exported (defaults to true for API <17)",
                    fix = "Set android:exported=\"false\" on the FileProvider; only the system package installer may reach it via scoped grants.",
                )
            }
            val grant = attr(provider, "android:grantUriPermissions")
            if (grant != "0xffffffff") {
                failures += CheckFailure(
                    check = "Update FileProvider grants URI permissions",
                    expected = "android:grantUriPermissions=\"true\" (0xffffffff)",
                    actual = grant ?: "missing android:grantUriPermissions (defaults to false)",
                    fix = "Set android:grantUriPermissions=\"true\" on the FileProvider so the installer can read the staged APK for the single launch.",
                )
            }
        }

        val queriesHasInstallIntent = queriesDeclareInstallIntent(tree)
        if (!queriesHasInstallIntent) {
            failures += CheckFailure(
                check = "AndroidManifest <queries> resolves the system installer",
                expected = "<queries> with VIEW intent and mimeType ${INSTALL_INTENT_MIME}",
                actual = "no matching <queries> block found",
                fix = "Add the <queries> VIEW/package-archive intent to AndroidManifest.xml and rebuild; without it the " +
                    "system installer can be invisible on Android 11+ and every install is reported unavailable.",
            )
        }
        return failures
    }

    /**
     * Validates res/xml/file_paths.xml (from `aapt2 dump xmltree`): the provider
     * must expose exactly the dedicated updater cache path the app stages APKs
     * into (`UpdateStash.directory` = cacheDir/updater).
     */
    internal fun verifyFilePathsStructure(
        output: String?,
        expected: Expected,
    ): List<CheckFailure> {
        val failures = mutableListOf<CheckFailure>()
        if (output == null) {
            failures += CheckFailure(
                check = "APK file-paths XML can be read",
                expected = "aapt2 dump xmltree res/xml/file_paths.xml succeeds",
                actual = "aapt2 failed or produced no output for res/xml/file_paths.xml",
                fix = "The APK does not contain res/xml/file_paths.xml. Rebuild the release APK and rerun.",
            )
            return failures
        }

        val tree = parseXmlTree(output)
        val cachePath = tree.flatMap { it.allDescendants() }
            .firstOrNull { el ->
                el.tag == "cache-path" &&
                    attr(el, "android:name") == UPDATE_FILE_PROVIDER_PATH_NAME &&
                    attr(el, "android:path") == UPDATE_FILE_PROVIDER_PATH
            }
        if (cachePath == null) {
            failures += CheckFailure(
                check = "FileProvider exposes the updater cache path",
                expected = "<cache-path android:name=\"${UPDATE_FILE_PROVIDER_PATH_NAME}\" android:path=\"${UPDATE_FILE_PROVIDER_PATH}\">",
                actual = "no matching <cache-path> entry",
                fix = "Add the updater cache-path entry to res/xml/file_paths.xml (mirroring UpdateStash's cacheDir/updater) and rebuild.",
            )
        }
        return failures
    }

    /**
     * Convenience for CI logging: true when the manifest XML carries a fully
     * valid update FileProvider (class, authority, exported=false, grants=true).
     */
    fun providerConfigurationOk(
        manifestXmlTreeOutput: String?,
        expectedAuthority: String,
    ): Boolean {
        val output = manifestXmlTreeOutput ?: return false
        val tree = parseXmlTree(output)
        val provider = treeProvider(tree, UPDATE_FILE_PROVIDER_CLASS) ?: return false
        return attr(provider, "android:authorities") == expectedAuthority &&
            attr(provider, "android:exported") == "0x0" &&
            attr(provider, "android:grantUriPermissions") == "0xffffffff"
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

    /**
     * First certificate SHA-256 digest from `apksigner verify --print-certs` output.
     *
     * The signer label varies across Build Tools releases:
     *   Build Tools 36: "Signer #1 certificate SHA-256 digest: <hex>"
     *   Build Tools 37: "V2 Signer: certificate SHA-256 digest: <hex>"
     * The digest value itself is identical either way, so only the
     * `certificate SHA-256 digest:` marker and the trailing 64-char hex value are
     * matched here, keeping the parser tolerant of future label changes.
     *
     * Returns null when no line carries a valid 64-char hex certificate SHA-256
     * digest. Unrelated SHA-256 text (e.g. public-key, SHA-1, MD5, or APK digests)
     * is deliberately not accepted.
     */
    fun parseFirstSignerSha256(output: String): String? =
        output.lineSequence()
            .mapNotNull { line ->
                certificateSha256DigestRegex.find(line.trim())?.groupValues?.getOrNull(1)?.lowercase()
            }
            .firstOrNull()

    private val certificateSha256DigestRegex =
        Regex("""certificate SHA-256 digest:\s*([0-9A-Fa-f]{64})\s*$""")

    fun sha256(file: File): String =
        file.inputStream().use { input -> sha256(input.readBytes()) }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /**
     * Small safe excerpt of `apksigner` output (first 10 non-empty lines) for
     * failure diagnostics. apksigner only prints public certificate data, but the
     * excerpt is kept tiny so secrets could never leak through it.
     */
    private fun apksignerOutputExcerpt(output: String): String {
        val lines = output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(10)
            .joinToString("\n  ")
        return if (lines.isNotEmpty()) "\n  apksigner output:\n  $lines" else ""
    }

    private fun parseQuotedValues(line: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        for (match in Regex("([A-Za-z0-9_]+)='([^']*)'").findAll(line)) {
            values[match.groupValues[1]] = match.groupValues[2]
        }
        return values
    }

    /** An element from `aapt2 dump xmltree` output. */
    data class XmlTreeElement(
        val tag: String,
        val attributes: Map<String, String>,
        val children: List<XmlTreeElement>,
    )

    private val elementLineRegex = Regex("""^([ \t]*)E: ([^ (]+)""")

    /**
     * Matches an aapt2 xmltree attribute line. group 1 = the attribute name
     * (either a short name like `android:exported` or a full namespace URI like
     * `http://schemas.android.com/apk/res/android:exported`), group 2 =
     * everything after the optional `(0x..)` resource id and the `=` (the quoted
     * string value, a typed `(type 0x..)0x..` token, or a bare `true`/`false`/
     * integer).
     */
    private val attributeLineRegex = Regex("""^([ \t]*)A: ([^ (=]+)(?:\(0x[0-9a-fA-F]+\))?=?(.*)$""")
    private val typedValueHexRegex = Regex("""\b0x[0-9a-fA-F]+\b""")

    /**
     * Parses the tree of `aapt2 dump xmltree` output into [XmlTreeElement]s.
     * Tolerant of the aapt2 attribute styles seen across Build Tools releases:
     * short names (`android:name`), full namespace URIs
     * (`http://schemas.android.com/apk/res/android:name`), quoted values,
     * typed `(type 0x..)0x..` values and bare `true`/`false`/integers — and of
     * namespace (`N:`) lines. Only meaningful for elements, not namespaces,
     * resources or references.
     */
    fun parseXmlTree(output: String): List<XmlTreeElement> {
        val roots = mutableListOf<XmlTreeElement>()
        val stack = mutableListOf<Pair<Int, XmlTreeElement>>()
        var current: XmlTreeElement? = null

        for (line in output.lineSequence()) {
            val elementMatch = elementLineRegex.find(line)
            if (elementMatch != null) {
                val depth = elementMatch.groupValues[1].count { it == ' ' } / 2
                val element = XmlTreeElement(elementMatch.groupValues[2], mutableMapOf(), mutableListOf())
                while (stack.isNotEmpty() && stack.last().first >= depth) stack.removeLast()
                if (stack.isEmpty()) {
                    roots += element
                } else {
                    val parent = stack.last().second
                    (parent.children as MutableList<XmlTreeElement>).add(element)
                }
                stack += depth to element
                current = element
                continue
            }

            val attributeMatch = attributeLineRegex.find(line)
            if (attributeMatch != null && current != null) {
                // Normalize a full namespace URI to its local name
                // (http://schemas.android.com/apk/res/android:name -> android:name);
                // short names pass through unchanged (android:name, package, name).
                val name = attributeMatch.groupValues[2].substringAfterLast('/')
                val rawValue = attributeMatch.groupValues[3]
                val value = when {
                    rawValue.startsWith("\"") ->
                        rawValue.substringAfter("\"").substringBefore("\"")
                    rawValue == "true" -> "0xffffffff"
                    rawValue == "false" -> "0x0"
                    else -> typedValueHexRegex.findAll(rawValue).lastOrNull()?.value.orEmpty()
                }
                (current.attributes as MutableMap<String, String>)[name] = value
            }
        }
        return roots
    }

    /**
     * Attribute lookup that tolerates the namespace handling differences of the
     * SDK tool variants: the manifest dump may emit `android:name` or the full
     * URL, while resource XML dumps (file_paths) drop the `android:` prefix.
     */
    internal fun attr(element: XmlTreeElement, name: String): String? =
        element.attributes[name] ?: element.attributes[name.removePrefix("android:")]

    internal fun XmlTreeElement.allDescendants(): Sequence<XmlTreeElement> =
        sequence {
            for (child in children) {
                yield(child)
                yieldAll(child.allDescendants())
            }
        }

    /** First FileProvider element matching [className], following manifest structure. */
    private fun treeProvider(tree: List<XmlTreeElement>, className: String): XmlTreeElement? =
        tree.asSequence().flatMap { it.allDescendants() }.plus(tree.asSequence())
            .firstOrNull { provider -> provider.tag == "provider" && attr(provider, "android:name") == className }

    private fun queriesDeclareInstallIntent(tree: List<XmlTreeElement>): Boolean {
        val queries = tree.asSequence().flatMap { it.allDescendants() }.plus(tree.asSequence())
            .firstOrNull { it.tag == "queries" } ?: return false
        return queries.allDescendants().any { intent ->
            intent.tag == "intent" &&
                intent.allDescendants().any {
                    it.tag == "action" && attr(it, "android:name") == INSTALL_INTENT_ACTION
                } &&
                intent.allDescendants().any {
                    it.tag == "data" && attr(it, "android:mimeType") == INSTALL_INTENT_MIME
                }
        }
    }
}