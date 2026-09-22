package com.techvisiondz.app.core.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UpdateRepository comparison and failure mapping, driven by a fake fetcher.
 * No network access.
 */
class UpdateRepositoryTest {

    private class FakeUpdateManifestFetcher(
        var manifest: String? = null,
        var error: Throwable? = null,
    ) : UpdateManifestFetcher {
        var fetchCount = 0
        override suspend fun fetchManifest(rawUrl: String): String {
            fetchCount++
            error?.let { throw it }
            return manifest ?: throw UpdateTransportException("no manifest configured")
        }
    }

    private fun repository(
        fetcher: FakeUpdateManifestFetcher,
        currentVersionCode: Int = 2,
        enabled: Boolean = true,
    ) = UpdateRepository(
        manifestUrl = UpdateTestFixtures.TRUSTED_MANIFEST_URL,
        currentVersionCode = currentVersionCode,
        enabled = enabled,
        fetcher = fetcher,
    )

    @Test
    fun `disabled updater short circuits and never fetches`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = UpdateTestFixtures.manifest())
        val result = repository(fetcher, enabled = false).checkForUpdate()

        assertEquals(UpdateCheckResult.Disabled, result)
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun `reports no update when remote equals current`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = UpdateTestFixtures.manifest(versionCode = 2))
        assertEquals(UpdateCheckResult.NoUpdate, repository(fetcher, currentVersionCode = 2).checkForUpdate())
        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `reports no update when current is newer than remote`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = UpdateTestFixtures.manifest(versionCode = 2))
        assertEquals(UpdateCheckResult.NoUpdate, repository(fetcher, currentVersionCode = 3).checkForUpdate())
    }

    @Test
    fun `reports update available when remote is newer`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = UpdateTestFixtures.manifest(versionCode = 3))
        val result = repository(fetcher, currentVersionCode = 2).checkForUpdate()

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val update = (result as UpdateCheckResult.UpdateAvailable).update
        assertEquals(3, update.versionCode)
        assertEquals("1.1.0", update.versionName)
    }

    @Test
    fun `installed version below the manifest minimum maps to below minimum failure`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(
            manifest = UpdateTestFixtures.manifest(versionCode = 5, minimumVersionCode = 4),
        )
        val result = repository(fetcher, currentVersionCode = 2).checkForUpdate()

        assertEquals(UpdateCheckResult.Failed(UpdateError.BelowMinimum), result)
        // The check still ran exactly once; only the download would be skipped.
        assertEquals(1, fetcher.fetchCount)
    }

    @Test
    fun `malformed manifest maps to malformed failure`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = "{ not json")
        assertEquals(
            UpdateCheckResult.Failed(UpdateError.MalformedManifest),
            repository(fetcher).checkForUpdate(),
        )
    }

    @Test
    fun `manifest pointing at an untrusted url maps to untrusted failure`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(manifest = UpdateTestFixtures.manifest(downloadUrl = "https://evil.example/app.apk"))
        assertEquals(
            UpdateCheckResult.Failed(UpdateError.UntrustedUrl),
            repository(fetcher).checkForUpdate(),
        )
    }

    @Test
    fun `transport failure maps to network failure`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(error = UpdateTransportException("dns failure"))
        assertEquals(
            UpdateCheckResult.Failed(UpdateError.Network),
            repository(fetcher).checkForUpdate(),
        )
    }

    @Test
    fun `unsuccessful http response maps to http failure`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(error = UnsuccessfulUpdateResponse(500, "boom"))
        assertEquals(
            UpdateCheckResult.Failed(UpdateError.Http),
            repository(fetcher).checkForUpdate(),
        )
    }

    @Test
    fun `unexpected runtime failure maps to network failure without crashing`() = runTest {
        val fetcher = FakeUpdateManifestFetcher(error = IllegalStateException("whoops"))
        assertEquals(
            UpdateCheckResult.Failed(UpdateError.Network),
            repository(fetcher).checkForUpdate(),
        )
    }
}