package com.techvisiondz.app.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The user's application-facing profile from the Supabase `profiles` table
 * (migration 0004) — the account layer, distinct from content authors (0008).
 *
 * Only data that is safe to display is exposed here: identity fields mirrored
 * from Supabase Auth (via the `profiles` row) and, when present, the avatar
 * from the public `avatars` storage bucket (migration 0043). No tokens or
 * session data are ever placed on this model.
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)