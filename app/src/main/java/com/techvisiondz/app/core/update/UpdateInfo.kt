package com.techvisiondz.app.core.update

/**
 * A validated, installable update descriptor derived from [UpdateManifest].
 *
 * Construction only happens through [UpdateManifestValidator], so this type is
 * only ever produced from a manifest that passed every security/dependability
 * check (URL trust, hash format, version sanity).
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String?,
    val minimumVersionCode: Int?,
)

/** Maps a parsed manifest to its domain form; validation happens before this. */
internal fun UpdateManifest.toUpdateInfo(): UpdateInfo = UpdateInfo(
    versionCode = versionCode,
    versionName = versionName,
    downloadUrl = downloadUrl,
    sha256 = sha256,
    releaseNotes = releaseNotes,
    minimumVersionCode = minimumVersionCode,
)