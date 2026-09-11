package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.UserProfile

/**
 * Deterministic [ProfileRepository] for androidTest navigation tests.
 * No network is involved; tests configure the returned profile via [profile].
 */
class FakeProfileRepository(
    var profile: UserProfile = sampleUserProfile(),
    var error: Exception? = null,
) : ProfileRepository {

    /** Number of times [getOwnProfile] was called. */
    var loadCalls: Int = 0
        private set

    override suspend fun getOwnProfile(): UserProfile {
        loadCalls++
        error?.let { throw it }
        return profile
    }
}

fun sampleUserProfile(
    id: String = "test-user",
    email: String = "test@example.com",
    displayName: String? = "Sarah",
    avatarUrl: String? = null,
    createdAt: String? = null,
): UserProfile = UserProfile(
    id = id,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)