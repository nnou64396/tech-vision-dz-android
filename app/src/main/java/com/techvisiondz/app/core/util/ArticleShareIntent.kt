package com.techvisiondz.app.core.util

import android.content.Context
import android.content.Intent

/**
 * Pure share payload builder: the article title followed by the canonical URL.
 * Intentionally free of any Supabase/internal identifiers or tracking params.
 */
fun articleShareText(title: String?, url: String): String = buildString {
    val cleanTitle = title?.trim()
    if (!cleanTitle.isNullOrEmpty()) {
        append(cleanTitle)
        append('\n')
    }
    append(url)
}

/**
 * Builds a standard Android `ACTION_SEND` share intent (text/plain) for the
 * article's canonical URL. The chooser-style sharesheet is handled by the OS;
 * no custom sharing UI is involved.
 */
fun createArticleShareIntent(url: String, title: String?): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, articleShareText(title, url))
        title?.trim()?.takeIf { it.isNotEmpty() }?.let { cleanTitle ->
            putExtra(Intent.EXTRA_TITLE, cleanTitle)
        }
    }

/**
 * Launches the Android sharesheet ([Intent.createChooser]) for an article.
 * Opens apps such as WhatsApp, Telegram, email, etc. via the system only.
 */
fun launchArticleShareChooser(
    context: Context,
    url: String,
    title: String?,
    chooserTitle: String? = null,
) {
    val chooser = Intent.createChooser(createArticleShareIntent(url, title), chooserTitle)
    context.startActivity(chooser)
}