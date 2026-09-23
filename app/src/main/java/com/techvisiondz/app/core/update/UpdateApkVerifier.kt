package com.techvisiondz.app.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.android.apksig.ApkVerifier
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

    /** Signatures could not be cryptographically verified or read. */
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
 * Android implementation that reads package metadata through [PackageManager]
 * archive APIs but verifies the update's signing certificates directly from the
 * APK's signing block via `apksig` (the same library that powers the
 * build-tools `apksigner`, running the exact scheme validation the system
 * installer will run again). The comparison only ever uses the public
 * certificate bytes — the private signing key is not accessible via any of
 * these APIs and is never touched.
 *
 * Signing certificates are compared only for their public identity:
 *  - the staged APK's certificates come from [apkSigningBlockCertificates],
 *    which cryptographically verifies the APK's Signature Scheme (v1/v2/v3)
 *    and returns the DER-encoded X.509 signer certificates byte-for-byte the
 *    way the installer's `SigningInfo.apkContentsSigners` would report them;
 *  - the installed app's certificates are read through [PackageManager]
 *    (API 28+ `GET_SIGNING_CERTIFICATES`, API 26/27 `GET_SIGNATURES`).
 *
 * `PackageManager.getPackageArchiveInfo` is deliberately NOT used for the
 * update APK's signers: on some API levels/ROMs it reports no signers for a
 * valid APK that is signed only with v2/v3 (the default for recent AGP
 * builds), which would wrongly abort the update as unverifiable.
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

            when (val apkSigners = apkSigningBlockCertificates(apk)) {
                is ApkSigners.Verified -> when (compareSignatures(readInstalledSigners(), apkSigners.certificates)) {
                    SignatureCompare.Match -> ApkVerificationResult.Success
                    SignatureCompare.Mismatch -> ApkVerificationResult.SignatureMismatch
                    SignatureCompare.Unverifiable -> ApkVerificationResult.SignatureUnverifiable
                }
                ApkSigners.Unverifiable -> ApkVerificationResult.SignatureUnverifiable
            }
        }

    /** Package identity/version metadata only; signers never come from here. */
    private data class ArchiveMetadata(
        val packageName: String?,
        val versionCode: Int?,
        val versionName: String?,
    )

    private fun readArchiveMetadata(apk: File): ArchiveMetadata? {
        val info: PackageInfo? = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
        return info?.let {
            ArchiveMetadata(
                packageName = it.packageName,
                versionCode = it.versionCode(),
                versionName = it.versionName,
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

/**
 * Outcome of extracting the staged APK's signing certificates straight from the
 * APK Signing Block using `apksig` — never from [PackageManager] archive
 * parsing, which on some API levels/ROMs reports no signers for a valid
 * v2/v3-only APK.
 */
internal sealed interface ApkSigners {
    /**
     * The signing block cryptographically verified and [certificates] carries
     * the DER-encoded X.509 signer certificates — byte-identical to
     * `android.content.pm.Signature.toByteArray()` for the same certificate,
     * which is what [compareSignatures] receives for the installed app.
     */
    data class Verified(val certificates: List<ByteArray>) : ApkSigners

    /** The signing block could not be verified or contains no certificates. */
    data object Unverifiable : ApkSigners
}

/**
 * Cryptographically verifies the APK's Signature Scheme (v1/v2/v3) in [apk]
 * with `apksig` — the same verification `apksigner verify` performs and the
 * system package installer will perform again — and returns the DER-encoded
 * signer certificates on success.
 *
 * [ApkSigners.Verified] is produced only after [ApkVerifier.Result.isVerified];
 * signers are never reported present on the strength of unverified parsed
 * bytes. Every failure — missing/unknown scheme, structural problem,
 * cryptographic mismatch — collapses to [ApkSigners.Unverifiable] so the caller
 * never treats an unauthenticated APK as signed.
 */
internal fun apkSigningBlockCertificates(apk: File): ApkSigners {
    val result = try {
        ApkVerifier.Builder(apk).build().verify()
    } catch (e: Exception) {
        return ApkSigners.Unverifiable
    }
    if (!result.isVerified) return ApkSigners.Unverifiable

    val certificates = ArrayList<ByteArray>(result.signerCertificates.size)
    for (certificate in result.signerCertificates) {
        val encoded = runCatching { certificate.encoded }.getOrNull()
            ?: return ApkSigners.Unverifiable
        certificates.add(encoded)
    }
    if (certificates.isEmpty()) return ApkSigners.Unverifiable
    return ApkSigners.Verified(certificates)
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
 * signer equals any APK signer; empty sets are unverifiable, never treated as a
 * match (a missing installed signature — e.g. an API < 28 device reading a
 * v2-only installed app — must not let a stale or bogus APK through).
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