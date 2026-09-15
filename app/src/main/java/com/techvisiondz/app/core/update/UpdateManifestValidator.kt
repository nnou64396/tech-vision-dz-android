package com.techvisiondz.app.core.update

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses and validates the update manifest JSON.
 *
 * A manifest is only ever turned into an [UpdateInfo] after every required
 * field has been verified. The APK URL is accepted only because the rule in
 * [UpdateHostPolicy] is satisfied — never merely because it appears inside
 * valid JSON.
 */
internal object UpdateManifestValidator {

    private val json = Json { ignoreUnknownKeys = true }

    internal sealed interface Validation {
        /** JSON could not be parsed or a required field was invalid. */
        data object Malformed : Validation

        /** The manifest's sha256 field is missing or not valid hex. */
        data object InvalidHash : Validation

        /** The manifest's APK download URL failed the trusted-host policy. */
        data object UntrustedUrl : Validation

        data class Valid(val update: UpdateInfo) : Validation
    }

    fun validate(rawJson: String): Validation {
        val manifest = try {
            json.decodeFromString<UpdateManifest>(rawJson)
        } catch (e: SerializationException) {
            return Validation.Malformed
        }

        if (manifest.versionCode <= 0) return Validation.Malformed
        if (manifest.versionName.isBlank()) return Validation.Malformed
        if (!UpdateSha256.isValidHex(manifest.sha256)) return Validation.InvalidHash

        val minimum = manifest.minimumVersionCode
        // When present, minimumVersionCode must be sane and cannot exceed the
        // version this release actually ships.
        if (minimum != null && (minimum <= 0 || minimum > manifest.versionCode)) {
            return Validation.Malformed
        }

        if (!UpdateHostPolicy.isTrustedUrl(manifest.downloadUrl)) return Validation.UntrustedUrl

        return Validation.Valid(manifest.toUpdateInfo())
    }
}