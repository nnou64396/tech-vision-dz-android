package com.techvisiondz.app.core.util

/**
 * Safety gate for external video links opened outside the app (the article
 * watch action). Mirrors [DownloadUrlPolicy]: only `http` / `https` URLs may be
 * launched, so the ACTION_VIEW intent is never pointed at an arbitrary scheme
 * or handler, and only videos with a usable URL are rendered.
 *
 * Pure and Android-free on purpose so the policy is unit-testable on the JVM.
 */
object VideoUrlPolicy {

    /**
     * Returns a clean, launchable `http(s)` URL for [raw], or null when the
     * value cannot be opened safely. Whitespace is trimmed and the scheme is
     * canonicalized to lowercase so the launched URL is always a plain
     * `http(s)` value.
     */
    fun normalize(raw: String?): String? {
        val clean = raw?.trim().orEmpty()
        val schemeEnd = clean.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = clean.substring(0, schemeEnd)
        if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return null
        val rest = clean.substring(schemeEnd + 3)
        if (rest.isEmpty()) return null
        return scheme.lowercase() + "://" + rest
    }

    /**
     * Returns true only when [raw] is a non-blank `http` / `https` URL safe to
     * reveal and launch. Equivalent to `normalize(raw) != null`; kept as the
     * assertive entry point for the article's video-card gate.
     */
    fun isSafe(raw: String?): Boolean = normalize(raw) != null
}