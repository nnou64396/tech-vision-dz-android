package com.techvisiondz.app.core.ui

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats an ISO-8601 timestamp as a localized medium date, or "" when the
 * input is blank/invalid so callers can safely drop it from metadata.
 */
fun formatPublishedAt(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        OffsetDateTime.parse(iso)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    } catch (e: Exception) {
        ""
    }
}