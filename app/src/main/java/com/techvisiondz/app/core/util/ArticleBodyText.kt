package com.techvisiondz.app.core.util

import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Lightweight article body rendering helpers.
 *
 * The body stays on the existing `Html.fromHtml` approach (plain text, no
 * WebView, no third-party HTML engine, no JavaScript) and only maps link spans
 * into Compose [LinkAnnotation]s. Tapping a link opens it with the system
 * browser via the platform [androidx.compose.ui.platform.UriHandler] — no
 * custom browser and no script execution is possible.
 */

/** Parses the article HTML body into a [Spanned], or null when blank. */
fun htmlBodySpanned(html: String?): Spanned? {
    if (html.isNullOrBlank()) return null
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
}

/**
 * Converts a [Spanned] (from [htmlBodySpanned]) into a Compose [AnnotatedString]:
 *
 * - bold/italic/underline spans are preserved,
 * - `http(s)` links are underlined, colored, and actionable (opened via the
 *   system browser by the standard [LinkAnnotation] handling in `Text`).
 *
 * Non-http(s) schemes (e.g. `javascript:`) are deliberately left as plain
 * text and are never clickable, so scripts can never run.
 */
fun spannedToAnnotatedString(
    spanned: Spanned,
    linkColor: Color,
): AnnotatedString {
    val raw = spanned.toString()
    val leading = raw.length - raw.trimStart().length
    val text = raw.trim()
    if (text.isEmpty()) return AnnotatedString("")

    val builder = AnnotatedString.Builder(text)

    fun range(start: Int, end: Int): IntRange? {
        val s = (start - leading).coerceIn(0, text.length)
        val e = (end - leading).coerceIn(s, text.length)
        return if (e > s) s until e else null
    }

    for (span in spanned.getSpans(0, spanned.length, StyleSpan::class.java)) {
        val at = range(spanned.getSpanStart(span), spanned.getSpanEnd(span)) ?: continue
        val isBold = span.style and Typeface.BOLD != 0
        val isItalic = span.style and Typeface.ITALIC != 0
        if (isBold || isItalic) {
            builder.addStyle(
                SpanStyle(
                    fontWeight = if (isBold) FontWeight.Bold else null,
                    fontStyle = if (isItalic) FontStyle.Italic else null,
                ),
                at.first,
                at.last + 1,
            )
        }
    }
    for (span in spanned.getSpans(0, spanned.length, UnderlineSpan::class.java)) {
        val at = range(spanned.getSpanStart(span), spanned.getSpanEnd(span)) ?: continue
        builder.addStyle(
            SpanStyle(textDecoration = TextDecoration.Underline),
            at.first,
            at.last + 1,
        )
    }
    for (span in spanned.getSpans(0, spanned.length, URLSpan::class.java)) {
        val at = range(spanned.getSpanStart(span), spanned.getSpanEnd(span)) ?: continue
        val url = span.url.trim()
        if (!isSafeLinkUrl(url)) continue
        builder.addLink(
            LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
            ),
            at.first,
            at.last + 1,
        )
    }
    return builder.toAnnotatedString()
}

/** Only `http(s)` links are actionable; other schemes are kept as plain text. */
private fun isSafeLinkUrl(url: String): Boolean =
    url.startsWith("https://") || url.startsWith("http://")