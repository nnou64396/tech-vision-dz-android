package com.techvisiondz.app.core.util

/**
 * Safety gate for software download links opened outside the app.
 *
 * Only absolute `http` / `https` URLs may be handed to the system browser via
 * an [androidx.compose.ui.platform.UriHandler]. Every other scheme — `file:`,
 * `content:`, `javascript:`, `ftp:`, custom `android-app:` intents, malformed
 * strings, blank values — is rejected so the ACTION_VIEW intent can never be
 * pointed at an arbitrary handler or a script. Whitespace is trimmed and the
 * scheme is canonicalized to lowercase so the launched URL is always a plain
 * `http(s)` value.
 *
 * Pure and Android-free on purpose so the policy is unit-testable on the JVM.
 */
object DownloadUrlPolicy {

    /**
     * Returns a clean, launchable `http(s)` URL for [raw], or null when the
     * value cannot be opened safely. The returned URL keeps the trimmed path,
     * query and fragment but always uses a lowercase `http`/`https` scheme.
     */
    fun normalize(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = url.substring(0, schemeEnd)
        if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return null
        val rest = url.substring(schemeEnd + 3)
        if (rest.isEmpty()) return null
        return scheme.lowercase() + "://" + rest
    }
}