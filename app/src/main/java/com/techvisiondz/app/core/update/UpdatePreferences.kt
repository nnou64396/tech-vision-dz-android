package com.techvisiondz.app.core.update

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persistent, updater-only state.
 *
 * Kept behind a small interface so the check/defer logic stays JVM-testable
 * with an in-memory fake. When the app moves to Google Play the updater is
 * disabled at the configuration layer and this file simply stops being read.
 */
interface UpdatePreferences {

    /** The start of the last "Later" deferral window, as epoch millis, or null. */
    fun lastDeferredAtMillis(): Long?

    /** Records that the update prompt was deferred at [timestampMillis]. */
    fun markDeferred(timestampMillis: Long)

    fun clear()
}

/**
 * SharedPreferences-backed implementation using a dedicated, private
 * preferences file scoped to the updater (nothing shared with other app
 * state, so it is trivially removable).
 */
class SharedPrefsUpdatePreferences(context: Context) : UpdatePreferences {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "techvision_update_prefs",
            Context.MODE_PRIVATE,
        )

    override fun lastDeferredAtMillis(): Long? =
        prefs.getLong(KEY_DEFERRED_AT, -1L).takeIf { it >= 0 }

    override fun markDeferred(timestampMillis: Long) {
        prefs.edit { putLong(KEY_DEFERRED_AT, timestampMillis) }
    }

    override fun clear() {
        prefs.edit { remove(KEY_DEFERRED_AT) }
    }

    private companion object {
        const val KEY_DEFERRED_AT = "deferred_at_epoch_millis"
    }
}