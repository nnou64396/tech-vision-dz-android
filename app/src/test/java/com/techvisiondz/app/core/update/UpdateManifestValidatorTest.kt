package com.techvisiondz.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure manifest parsing/validation rules. No network involved.
 */
class UpdateManifestValidatorTest {

    private fun validUpdate(rawJson: String): UpdateInfo {
        val validation = UpdateManifestValidator.validate(rawJson)
        return (validation as? UpdateManifestValidator.Validation.Valid)?.update
            ?: error("expected Valid, got $validation")
    }

    @Test
    fun `valid manifest is accepted`() {
        val update = validUpdate(UpdateTestFixtures.manifest())
        assertEquals(3, update.versionCode)
        assertEquals("1.1.0", update.versionName)
        assertEquals(UpdateTestFixtures.TRUSTED_APK_URL, update.downloadUrl)
        assertEquals(UpdateTestFixtures.VALID_SHA256, update.sha256)
        assertEquals("Bug fixes and improvements.", update.releaseNotes)
        assertEquals(2, update.minimumVersionCode)
    }

    @Test
    fun `valid manifest without optional fields is accepted`() {
        val update = validUpdate(
            UpdateTestFixtures.manifest(releaseNotes = null, minimumVersionCode = null),
        )
        assertEquals(null, update.releaseNotes)
        assertEquals(null, update.minimumVersionCode)
    }

    @Test
    fun `empty version name is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(versionName = ""))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `blank version name is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(versionName = "   "))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `version code zero is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(versionCode = 0))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `negative version code is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(versionCode = -1))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `non hexadecimal sha256 is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(sha256 = "not-a-hash"))
        assertEquals(UpdateManifestValidator.Validation.InvalidHash, result)
    }

    @Test
    fun `short sha256 is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(sha256 = "abc"))
        assertEquals(UpdateManifestValidator.Validation.InvalidHash, result)
    }

    @Test
    fun `blank sha256 is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(sha256 = ""))
        assertEquals(UpdateManifestValidator.Validation.InvalidHash, result)
    }

    @Test
    fun `uppercase sha256 is accepted`() {
        val update = validUpdate(UpdateTestFixtures.manifest(sha256 = UpdateTestFixtures.VALID_SHA256_UPPERCASE))
        assertEquals(UpdateTestFixtures.VALID_SHA256_UPPERCASE, update.sha256)
    }

    @Test
    fun `lowercase sha256 is accepted`() {
        assertEquals(UpdateTestFixtures.VALID_SHA256, validUpdate(UpdateTestFixtures.manifest()).sha256)
    }

    @Test
    fun `untrusted download host is rejected`() {
        val result = UpdateManifestValidator.validate(
            UpdateTestFixtures.manifest(downloadUrl = "https://evil.example/app-release.apk"),
        )
        assertEquals(UpdateManifestValidator.Validation.UntrustedUrl, result)
    }

    @Test
    fun `http download url is rejected`() {
        val result = UpdateManifestValidator.validate(
            UpdateTestFixtures.manifest(downloadUrl = "http://github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.0/app-release.apk"),
        )
        assertEquals(UpdateManifestValidator.Validation.UntrustedUrl, result)
    }

    @Test
    fun `github trusted url is accepted`() {
        assertTrue(UpdateManifestValidator.validate(UpdateTestFixtures.manifest()) is UpdateManifestValidator.Validation.Valid)
    }

    @Test
    fun `objectsgithubusercontent trusted url is accepted`() {
        val result = UpdateManifestValidator.validate(
            UpdateTestFixtures.manifest(
                downloadUrl = "https://objects.githubusercontent.com/github-production-release-asset-2e65be/000000/0000-0000-0000-0000-000000000000/app-release.apk",
            ),
        )
        assertTrue(result is UpdateManifestValidator.Validation.Valid)
    }

    @Test
    fun `download url containing userinfo is rejected`() {
        val result = UpdateManifestValidator.validate(
            UpdateTestFixtures.manifest(downloadUrl = "https://user:pass@github.com/nnou64396/tech-vision-dz-android/releases/download/v1.1.0/app-release.apk"),
        )
        assertEquals(UpdateManifestValidator.Validation.UntrustedUrl, result)
    }

    @Test
    fun `download url with a non default port is rejected`() {
        val result = UpdateManifestValidator.validate(
            UpdateTestFixtures.manifest(downloadUrl = "https://github.com:8443/nnou64396/tech-vision-dz-android/releases/download/v1.1.0/app-release.apk"),
        )
        assertEquals(UpdateManifestValidator.Validation.UntrustedUrl, result)
    }

    @Test
    fun `malformed json is rejected`() {
        val result = UpdateManifestValidator.validate("{ this is not valid json ")
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `empty body is rejected`() {
        assertEquals(UpdateManifestValidator.Validation.Malformed, UpdateManifestValidator.validate(""))
    }

    @Test
    fun `missing required fields is rejected`() {
        val result = UpdateManifestValidator.validate("{\"versionName\":\"1.1.0\"}")
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `missing download url is rejected`() {
        val result = UpdateManifestValidator.validate(
            "{\"versionCode\":3,\"versionName\":\"1.1.0\",\"sha256\":\"${UpdateTestFixtures.VALID_SHA256}\"}",
        )
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `minimum version code zero is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(minimumVersionCode = 0))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `minimum version code negative is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(minimumVersionCode = -1))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }

    @Test
    fun `minimum version code higher than the shipped version is rejected`() {
        val result = UpdateManifestValidator.validate(UpdateTestFixtures.manifest(minimumVersionCode = 4))
        assertEquals(UpdateManifestValidator.Validation.Malformed, result)
    }
}