package com.techvisiondz.app.core.update

import java.net.URL

/**
 * Centralized host trust policy for the temporary in-app updater.
 *
 * Only HTTPS URLs on hosts owned by this project's GitHub Releases delivery
 * are trusted:
 *  - `github.com` — the stable `releases/latest/download/...` asset URLs used
 *    as the manifest endpoint and APK download URL;
 *  - `objects.githubusercontent.com` — the GitHub host that actually serves
 *    release assets after the redirect, allowed narrowly so the manifest
 *    cannot redirect the app to an arbitrary download host.
 *  - `release-assets.githubusercontent.com` — GitHub's current release asset
 *    delivery host for the same redirect flow.
 *
 * URLs are parsed with [URL] and compared by exact host, never by string
 * prefixes, so look-alike hosts (e.g. `github.com.evil.example`) are rejected.
 * URLs carrying userinfo or a non-default port are also rejected as
 * defensive hardening.
 */
object UpdateHostPolicy {

    /** Exact hosts the updater is allowed to talk to, over HTTPS only. */
    val trustedHosts: Set<String> = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )

    /**
     * Returns true only for a well-formed HTTPS URL whose host is in
     * [trustedHosts] and which uses no userinfo and the default 443 port.
     */
    fun isTrustedUrl(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        val url = runCatching { URL(rawUrl) }.getOrNull() ?: return false
        if (!url.protocol.equals("https", ignoreCase = true)) return false
        if (url.host !in trustedHosts) return false
        if (!url.userInfo.isNullOrEmpty()) return false
        if (url.port != -1 && url.port != 443) return false
        return true
    }
}