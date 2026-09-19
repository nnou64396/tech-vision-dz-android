package com.techvisiondz.app.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host trust policy for the updater. All cases are pure string/URL checks.
 */
class UpdateHostPolicyTest {

    @Test
    fun `github https url is trusted`() {
        assertTrue(UpdateHostPolicy.isTrustedUrl("https://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json"))
    }

    @Test
    fun `objectsgithubusercontent https url is trusted`() {
        assertTrue(UpdateHostPolicy.isTrustedUrl("https://objects.githubusercontent.com/00000000-0000-0000-0000-000000000000/app.apk"))
    }

    @Test
    fun `release assets githubusercontent https url is trusted`() {
        assertTrue(UpdateHostPolicy.isTrustedUrl("https://release-assets.githubusercontent.com/github-production-release-asset/app.apk"))
    }

    @Test
    fun `explicit default https port is trusted`() {
        assertTrue(UpdateHostPolicy.isTrustedUrl("https://github.com:443/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json"))
    }

    @Test
    fun `http url is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("http://github.com/nnou64396/tech-vision-dz-android/releases/latest/download/update-manifest.json"))
    }

    @Test
    fun `lookalike suffix host is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://github.com.evil.example/path"))
    }

    @Test
    fun `lookalike prefix host is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://evil-github.com/path"))
    }

    @Test
    fun `userinfo url is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://user:pass@github.com/path"))
    }

    @Test
    fun `unexpected port is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://github.com:8443/path"))
    }

    @Test
    fun `unrelated domain is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://cdn.example.com/app.apk"))
    }

    @Test
    fun `githubusercontent close misspelling is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://objectsgitubusercontent.com/app.apk"))
    }

    @Test
    fun `blank url is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl(""))
        assertFalse(UpdateHostPolicy.isTrustedUrl("   "))
    }

    @Test
    fun `non url string is rejected`() {
        assertFalse(UpdateHostPolicy.isTrustedUrl("not-a-url"))
        assertFalse(UpdateHostPolicy.isTrustedUrl("https://"))
    }
}