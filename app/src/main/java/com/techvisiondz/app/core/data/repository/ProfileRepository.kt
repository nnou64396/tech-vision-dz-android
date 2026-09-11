package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.UserProfile

/**
 * Read access to the authenticated user's own profile.
 *
 * Backed by the existing Supabase `public.profiles` table (migration 0004),
 * whose own-row RLS policies (`profiles_select_own`) mean a signed-in user can
 * only ever read their own row — no additional backend structure is required.
 *
 * Implementations must never expose credentials and must map transport/backend
 * failures for clean UI reporting.
 */
interface ProfileRepository {

    /**
     * Loads the signed-in user's profile row. Fails with
     * [com.techvisiondz.app.core.data.DataException] when there is no active
     * session or the backend cannot be reached.
     */
    suspend fun getOwnProfile(): UserProfile
}