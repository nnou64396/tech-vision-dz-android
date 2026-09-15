package com.techvisiondz.app.core.update

/**
 * Shared fixtures for updater unit tests. Contains no secrets and never
 * touches the network.
 */
internal object UpdateTestFixtures {

    const val TRUSTED_MANIFEST_URL =
        "https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json"

    const val TRUSTED_APK_URL =
        "https://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.0/app-release.apk"

    const val VALID_SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    const val VALID_SHA256_UPPERCASE = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

    fun manifest(
        versionCode: Int = 3,
        versionName: String = "1.1.0",
        downloadUrl: String = TRUSTED_APK_URL,
        sha256: String = VALID_SHA256,
        releaseNotes: String? = "Bug fixes and improvements.",
        minimumVersionCode: Int? = 2,
    ): String = buildString {
        append("{\"versionCode\":$versionCode,")
        append("\"versionName\":${jsonString(versionName)},")
        append("\"downloadUrl\":${jsonString(downloadUrl)},")
        append("\"sha256\":${jsonString(sha256)}")
        if (releaseNotes != null) {
            append(",\"releaseNotes\":${jsonString(releaseNotes)}")
        }
        if (minimumVersionCode != null) {
            append(",\"minimumVersionCode\":$minimumVersionCode")
        }
        append("}")
    }

    fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}