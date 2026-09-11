package com.techvisiondz.app.core.data.repository

import com.techvisiondz.app.core.data.model.UserProfile
import kotlinx.coroutines.delay

/**
 * Deterministic [ProfileRepository] for unit tests. No network is involved.
 * Tests configure the returned profile via [profile] and can force a failure
 * via [error]. The [delay] mirrors the auth fakes so loading states are
 * observable under `runCurrent()`.
 */
class FakeProfileRepository(
    var profile: UserProfile = sampleUserProfile(),
    var error: Exception? = null,
) : ProfileRepository {

    /** Number of times [getOwnProfile] was called. */
    var loadCalls: Int = 0
        private set

    override suspend fun getOwnProfile(): UserProfile {
        delay(1)
        loadCalls++
        error?.let { throw it }
        return profile
    }
}

fun sampleUserProfile(
    id: String = "user-1",
    email: String = "reader@example.com",
    displayName: String? = "Sarah",
    avatarUrl: String? = null,
    createdAt: String? = null,
) = UserProfile(
    id = id,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)