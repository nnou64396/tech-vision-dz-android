package com.techvisiondz.app.core.network

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Owns the single [SupabaseClient] used by the app.
 *
 * The client is created lazily on first use and only carries the *anon*
 * (publishable) key. Configuration comes from `BuildConfig`, which is populated
 * at build time from the gitignored `local.properties` file:
 *   TECHVISION_SUPABASE_URL=https://<project-ref>.supabase.co
 *   TECHVISION_SUPABASE_ANON_KEY=<anon-key>
 *
 * The service role key must never be placed in the Android app.
 *
 * Auth (GoTrue) is installed alongside Postgrest and Storage. Session state is
 * observed through [Auth.sessionStatus]; sessions are persisted with the SDK's
 * default `SettingsSessionManager` (SharedPreferences on Android) and restored
 * automatically on startup via lifecycle callbacks. No custom token storage is
 * used.
 */
object SupabaseClientProvider {

    private val url: String get() = AppConfig.SUPABASE_URL.trim()
    private val anonKey: String get() = AppConfig.SUPABASE_ANON_KEY.trim()

    val client: SupabaseClient by lazy {
        if (url.isEmpty() || anonKey.isEmpty()) {
            throw DataException.ConfigurationNotSet(
                "Supabase is not configured. Add TECHVISION_SUPABASE_URL and " +
                    "TECHVISION_SUPABASE_ANON_KEY to local.properties and rebuild.",
            )
        }
        createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    val auth: Auth get() = client.auth

    val postgrest: Postgrest get() = client.postgrest

    val storage: Storage get() = client.storage

    /** Resolves a path inside a public storage bucket to an absolute HTTPS URL. */
    fun publicMediaUrl(bucket: String, storagePath: String): String =
        storage.from(bucket).publicUrl(storagePath)
}