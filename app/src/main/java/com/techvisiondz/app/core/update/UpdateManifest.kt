package com.techvisiondz.app.core.update

import kotlinx.serialization.Serializable

/**
 * Version manifest consumed from the pinned update endpoint
 * ([com.techvisiondz.app.core.config.AppConfig.UPDATE_MANIFEST_URL]).
 *
 * The manifest is served as a GitHub Release asset (`update-manifest.json`)
 * uploaded alongside the release APK. Field validation happens in
 * [UpdateRepository]; anything malformed or untrusted is rejected before a
 * download starts. [releaseNotes] and [minimumVersionCode] are optional
 * metadata (nullable, forward-compatible), while the remaining fields are
 * required.
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String? = null,
    val minimumVersionCode: Int? = null,
)