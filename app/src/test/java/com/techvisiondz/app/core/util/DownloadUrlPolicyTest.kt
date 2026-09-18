package com.techvisiondz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deterministic JVM tests for [DownloadUrlPolicy] — the safety gate that makes
 * software download links safe to open externally.
 */
class DownloadUrlPolicyTest {

    @Test
    fun `null raw url is rejected`() {
        assertNull(DownloadUrlPolicy.normalize(null))
    }

    @Test
    fun `blank raw url is rejected`() {
        assertNull(DownloadUrlPolicy.normalize(""))
        assertNull(DownloadUrlPolicy.normalize("   "))
        assertNull(DownloadUrlPolicy.normalize("\n\t"))
    }

    @Test
    fun `https url is accepted unchanged`() {
        assertEquals(
            "https://example.com/app/app-release.apk",
            DownloadUrlPolicy.normalize("https://example.com/app/app-release.apk"),
        )
    }

    @Test
    fun `http url is accepted unchanged`() {
        assertEquals(
            "http://example.com/app.apk",
            DownloadUrlPolicy.normalize("http://example.com/app.apk"),
        )
    }

    @Test
    fun `surrounding whitespace is trimmed before acceptance`() {
        assertEquals(
            "https://example.com/app.apk",
            DownloadUrlPolicy.normalize("   https://example.com/app.apk   "),
        )
    }

    @Test
    fun `uppercase scheme is canonicalized to lowercase`() {
        assertEquals(
            "https://example.com/app.apk",
            DownloadUrlPolicy.normalize("HTTPS://example.com/app.apk"),
        )
        assertEquals(
            "http://example.com/app.apk",
            DownloadUrlPolicy.normalize("HTTP://example.com/app.apk"),
        )
    }

    @Test
    fun `query and fragment survive normalization`() {
        assertEquals(
            "https://example.com/app?flavor=stable#v2_0",
            DownloadUrlPolicy.normalize("https://example.com/app?flavor=stable#v2_0"),
        )
    }

    @Test
    fun `scheme without any target is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("https://"))
        assertNull(DownloadUrlPolicy.normalize("http://"))
    }

    @Test
    fun `mixed-case scheme letters are accepted and lowered`() {
        assertEquals("https://x.dev/a", DownloadUrlPolicy.normalize("HtTpS://x.dev/a"))
    }

    @Test
    fun `javascript scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("javascript:alert(1)"))
        assertNull(DownloadUrlPolicy.normalize("javascript://example.com/x"))
    }

    @Test
    fun `file scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("file:///sdcard/malware.apk"))
        assertNull(DownloadUrlPolicy.normalize("file://etc/passwd"))
    }

    @Test
    fun `content scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("content://media/external/downloads/1"))
    }

    @Test
    fun `ftp scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("ftp://example.com/app.apk"))
    }

    @Test
    fun `mailto scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("mailto:someone@example.com"))
    }

    @Test
    fun `android app custom scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("android-app://com.example/downloader"))
    }

    @Test
    fun `value without a scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("example.com/app.apk"))
        assertNull(DownloadUrlPolicy.normalize("/sdcard/app.apk"))
    }

    @Test
    fun `value with a bare colon scheme is rejected`() {
        assertNull(DownloadUrlPolicy.normalize("https:example.com/app.apk"))
    }
}