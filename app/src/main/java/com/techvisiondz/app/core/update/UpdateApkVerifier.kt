package com.techvisiondz.app.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.techvisiondz.app.BuildConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of verifying a staged APK. Every non-[Success] outcome must abort the
 * update — the updater never hands a suspect APK to the system installer.
 */
sealed interface ApkVerificationResult {
    data object Success : ApkVerificationResult

    /** The file was not a readable Android package archive. */
    data object InvalidApk : ApkVerificationResult

    /** The APK hash did not match the manifest hash. */
    data object HashMismatch : ApkVerificationResult

    /** The APK belongs to a different application id. */
    data object WrongPackage : ApkVerificationResult

    /** The APK versionCode does not match the manifest versionCode exactly. */
    data object WrongVersion : ApkVerificationResult

    /** The APK versionName disagrees with the manifest versionName. */
    data object VersionNameMismatch : ApkVerificationResult

    /** The installed app is older than the manifest's minimumVersionCode. */
    data object BelowMinimum : ApkVerificationResult

    /** The APK signing certificates differ from the installed app's. */
    data object SignatureMismatch : ApkVerificationResult

    /** Signatures could not be read on this device (see implementation notes). */
    data object SignatureUnverifiable : ApkVerificationResult
}

interface UpdateApkVerifier {
    /**
     * Verifies [apk] against [expected]. The installed app's version and
     * signature always refer to the running (i.e. installed) package.
     */
    suspend fun verify(apk: File, expected: UpdateInfo): ApkVerificationResult
}

/**
 * Android implementation reading package metadata and signing certificates
 * through [PackageManager] archive APIs. The comparison only ever uses the
 * public certificate bytes — the private signing key is not accessible via
 * these APIs and is never touched.
 *
 * Signing certificates are compared only for their public identity:
 *  - API 28+ uses `GET_SIGNING_CERTIFICATES` (v2/v3-aware) for both the
 *    staged APK and the installed package;
 *  - API 26/27 falls back to v1 `GET_SIGNATURES`; an APK with no v1 signature
 *    (e.g. v2/v3-only, the default for recent AGP builds) yields no signers,
 *    which maps to [ApkVerificationResult.SignatureUnverifiable] rather than
 *    pretending the update is safe.
 *
 * The decision rules live in pure, JVM-testable functions below.
 */
class AndroidUpdateApkVerifier(
    context: Context,
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE,
) : UpdateApkVerifier {

    private val packageManager: PackageManager = context.packageManager
    private val expectedPackageId: String = context.packageName

    override suspend fun verify(apk: File, expected: UpdateInfo): ApkVerificationResult =
        withContext(Dispatchers.IO) {
            val actualSha = runCatching { UpdateSha256.of(apk) }.getOrNull()
                ?: return@withContext ApkVerificationResult.InvalidApk
            if (!UpdateSha256.matches(actualSha, expected.sha256)) {
                return@withContext ApkVerificationResult.HashMismatch
            }

            val archive = readArchiveMetadata(apk)
                ?: return@withContext ApkVerificationResult.InvalidApk

            when (evaluateMetadata(
                apkPackageName = archive.packageName,
                apkVersionCode = archive.versionCode,
                apkVersionName = archive.versionName,
                expected = expected,
                installedVersionCode = installedVersionCode,
            )) {
                MetadataStatus.Ok -> Unit
                MetadataStatus.Missing -> return@withContext ApkVerificationResult.InvalidApk
                MetadataStatus.WrongPackage -> return@withContext ApkVerificationResult.WrongPackage
                MetadataStatus.WrongVersion -> return@withContext ApkVerificationResult.WrongVersion
                MetadataStatus.VersionNameMismatch ->
                    return@withContext ApkVerificationResult.VersionNameMismatch
                MetadataStatus.BelowMinimum -> return@withContext ApkVerificationResult.BelowMinimum
            }

            when (compareSignatures(readInstalledSigners(), archive.signers)) {
                SignatureCompare.Match -> ApkVerificationResult.Success
                SignatureCompare.Mismatch -> ApkVerificationResult.SignatureMismatch
                SignatureCompare.Unverifiable -> ApkVerificationResult.SignatureUnverifiable
            }
        }

    private data class ArchiveMetadata(
        val packageName: String?,
        val versionCode: Int?,
        val versionName: String?,
        val signers: List<ByteArray>,
    )

    private fun readArchiveMetadata(apk: File): ArchiveMetadata? {
        val info: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
        }
        return info?.let {
            ArchiveMetadata(
                packageName = it.packageName,
                versionCode = it.versionCode(),
                versionName = it.versionName,
                signers = signersOf(it),
            )
        }
    }

    private fun readInstalledSigners(): List<ByteArray> {
        val info: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(expectedPackageId, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(expectedPackageId, PackageManager.GET_SIGNATURES)
        }
        return info?.let { signersOf(it) }.orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.versionCode(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode.toInt() else versionCode

    @Suppress("DEPRECATION")
    private fun signersOf(info: PackageInfo): List<ByteArray> {
        val signers: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        return signers.orEmpty().map { it.toByteArray() }
    }
}

/** Decision rules for the manifest-vs-APK metadata comparison. */
internal enum class MetadataStatus { Ok, Missing, WrongPackage, WrongVersion, VersionNameMismatch, BelowMinimum }

/** The application id must never change; this is enforced, not assumed. */
internal const val EXPECTED_UPDATE_PACKAGE_ID = "com.techvisiondz.app"

/**
 * Pure decision logic:
 *  - the APK must carry readable metadata;
 *  - [EXPECTED_UPDATE_PACKAGE_ID] is required;
 *  - the APK versionCode must equal the manifest versionCode exactly
 *    (not merely be greater);
 *  - a readable APK versionName must not contradict the manifest;
 *  - when the manifest declares a minimumVersionCode, the currently installed
 *    version must already be at or above it.
 */
internal fun evaluateMetadata(
    apkPackageName: String?,
    apkVersionCode: Int?,
    apkVersionName: String?,
    expected: UpdateInfo,
    installedVersionCode: Int,
): MetadataStatus {
    if (apkPackageName == null || apkVersionCode == null) return MetadataStatus.Missing
    if (apkPackageName != EXPECTED_UPDATE_PACKAGE_ID) return MetadataStatus.WrongPackage
    if (apkVersionCode != expected.versionCode) return MetadataStatus.WrongVersion
    if (!apkVersionName.isNullOrBlank() && expected.versionName.isNotBlank() &&
        apkVersionName != expected.versionName
    ) {
        return MetadataStatus.VersionNameMismatch
    }
    val minimum = expected.minimumVersionCode
    if (minimum != null && installedVersionCode < minimum) return MetadataStatus.BelowMinimum
    return MetadataStatus.Ok
}

/** Public certificate-identity comparison result. */
internal enum class SignatureCompare { Match, Mismatch, Unverifiable }

/**
 * Compares public signing-certificate byte arrays. Match means any installed
 * signer equals any APK signer; empty sets (e.g. a v2-only APK read on API < 28)
 * are unverifiable, never treated as a match.
 */
internal fun compareSignatures(installed: List<ByteArray>, apk: List<ByteArray>): SignatureCompare {
    if (installed.isEmpty() || apk.isEmpty()) return SignatureCompare.Unverifiable
    val apkKeys = apk.map { it.toList() }.toSet()
    return if (installed.any { it.toList() in apkKeys }) {
        SignatureCompare.Match
    } else {
        SignatureCompare.Mismatch
    }
}