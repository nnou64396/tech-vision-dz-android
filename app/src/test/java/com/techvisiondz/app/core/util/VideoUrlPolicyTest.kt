package com.techvisiondz.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic JVM tests for [VideoUrlPolicy] — the safety gate that decides
 * whether an article's video URL is safe to reveal and launch externally.
 */
class VideoUrlPolicyTest {

    @Test
    fun `null raw url is rejected`() {
        assertNull(VideoUrlPolicy.normalize(null))
        assertFalse(VideoUrlPolicy.isSafe(null))
    }

    @Test
    fun `blank raw url is rejected`() {
        assertNull(VideoUrlPolicy.normalize(""))
        assertNull(VideoUrlPolicy.normalize("   "))
        assertNull(VideoUrlPolicy.normalize("\n\t"))
        assertFalse(VideoUrlPolicy.isSafe("   "))
    }

    @Test
    fun `https url is accepted unchanged`() {
        assertEquals(
            "https://youtube.com/watch?v=abc123",
            VideoUrlPolicy.normalize("https://youtube.com/watch?v=abc123"),
        )
        assertTrue(VideoUrlPolicy.isSafe("https://youtube.com/watch?v=abc123"))
    }

    @Test
    fun `http url is accepted unchanged`() {
        assertEquals(
            "http://example.com/video.mp4",
            VideoUrlPolicy.normalize("http://example.com/video.mp4"),
        )
        assertTrue(VideoUrlPolicy.isSafe("http://example.com/video.mp4"))
    }

    @Test
    fun `surrounding whitespace is trimmed before acceptance`() {
        assertEquals(
            "https://example.com/video.mp4",
            VideoUrlPolicy.normalize("   https://example.com/video.mp4   "),
        )
    }

    @Test
    fun `uppercase scheme is canonicalized to lowercase`() {
        assertEquals(
            "https://example.com/video.mp4",
            VideoUrlPolicy.normalize("HTTPS://example.com/video.mp4"),
        )
        assertEquals(
            "http://example.com/video.mp4",
            VideoUrlPolicy.normalize("HTTP://example.com/video.mp4"),
        )
    }

    @Test
    fun `scheme without any target is rejected`() {
        assertNull(VideoUrlPolicy.normalize("https://"))
        assertNull(VideoUrlPolicy.normalize("http://"))
        assertFalse(VideoUrlPolicy.isSafe("https://"))
    }

    @Test
    fun `non http schemes are rejected`() {
        assertNull(VideoUrlPolicy.normalize("javascript:alert(1)"))
        assertNull(VideoUrlPolicy.normalize("file:///etc/passwd"))
        assertNull(VideoUrlPolicy.normalize("content://media/video/1"))
        assertNull(VideoUrlPolicy.normalize("ftp://example.com/video.mp4"))
        assertNull(VideoUrlPolicy.normalize("intent://example.com/video"))
        assertFalse(VideoUrlPolicy.isSafe("javascript:alert(1)"))
    }

    @Test
    fun `value without a scheme is rejected`() {
        assertNull(VideoUrlPolicy.normalize("youtube.com/watch?v=abc123"))
        assertNull(VideoUrlPolicy.normalize("/local/video.mp4"))
        assertFalse(VideoUrlPolicy.isSafe("/local/video.mp4"))
    }

    @Test
    fun `value with a bare colon scheme is rejected`() {
        assertNull(VideoUrlPolicy.normalize("https:example.com/video.mp4"))
    }
}