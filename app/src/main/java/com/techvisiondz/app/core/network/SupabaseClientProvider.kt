package com.techvisiondz.app.core.network

import com.techvisiondz.app.core.config.AppConfig
import com.techvisiondz.app.core.data.DataException
import io.github.jan.supabase.SupabaseClient
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
            install(Postgrest)
            install(Storage)
        }
    }

    val postgrest: Postgrest get() = client.postgrest

    val storage: Storage get() = client.storage

    /** Resolves a path inside a public storage bucket to an absolute HTTPS URL. */
    fun publicMediaUrl(bucket: String, storagePath: String): String =
        storage.from(bucket).publicUrl(storagePath)
}