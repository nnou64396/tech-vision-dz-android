package com.techvisiondz.app.core.config

import com.techvisiondz.app.BuildConfig

/**
 * Public client configuration for TECH VISION DZ.
 *
 * IMPORTANT: These values are the *publishable* configuration of the Android client.
 * They are embedded in the APK and can be read by anyone, so they are NOT secrets.
 * They must be restricted via Supabase RLS / anon-key scoping on the backend.
 *
 * Values are injected at build time from `local.properties` (gitignored):
 *   TECHVISION_SUPABASE_URL="https://<ref>.supabase.co"
 *   TECHVISION_SUPABASE_ANON_KEY="<anon-key>"
 *   TECHVISION_ARTICLE_BASE_URL="https://techvisiondz.com"
 *
 * The service role key must never be placed in the Android app.
 */
object AppConfig {

    /** Base URL of the existing TECH VISION DZ Supabase project. */
    const val SUPABASE_URL: String = BuildConfig.SUPABASE_URL

    /** Publishable (anon) key for the existing TECH VISION DZ Supabase project. */
    const val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    /** Public TECH VISION DZ website base URL, used for canonical article share links. */
    const val ARTICLE_BASE_URL: String = BuildConfig.ARTICLE_BASE_URL

    /**
     * Default language code for public content. Production source articles are
     * authored in Algerian Darija ('arq'); 'ar' is only an AI-produced target
     * translation, so every public query must default to 'arq' to match the
     * backend source content.
     */
    const val DEFAULT_LANGUAGE_CODE: String = "arq"
}
