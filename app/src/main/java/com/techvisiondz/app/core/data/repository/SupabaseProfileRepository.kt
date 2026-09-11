package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.DataException
import com.techvisiondz.app.core.data.model.UserProfile
import com.techvisiondz.app.core.data.toDataException
import com.techvisiondz.app.core.network.SupabaseClientProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

/**
 * Supabase-backed [ProfileRepository] using the existing `public.profiles`
 * table (migration 0004) and its own-row RLS policy (`profiles_select_own`).
 *
 * The query is scoped to the active session's auth user id, so the result is
 * always the caller's own row. Failures are converted to
 * [com.techvisiondz.app.core.data.DataException] for clean UI reporting.
 */
class SupabaseProfileRepository(
    private val postgrest: Postgrest = SupabaseClientProvider.postgrest,
    private val auth: Auth = SupabaseClientProvider.auth,
) : ProfileRepository {

    override suspend fun getOwnProfile(): UserProfile = try {
        val userId = auth.currentUserOrNull()?.id
            ?: throw DataException.Authentication("Not signed in")
        postgrest.from("profiles")
            .select(Columns.list(*PROFILE_COLUMNS)) {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingle<UserProfile>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDataException()
    }

    private companion object {
        // Mirrors the website's account page select
        // (src/pages/account/AccountOverviewPage.tsx) minus email_confirmed_at.
        val PROFILE_COLUMNS = arrayOf(
            "id",
            "email",
            "display_name",
            "avatar_url",
            "created_at",
        )
    }
}