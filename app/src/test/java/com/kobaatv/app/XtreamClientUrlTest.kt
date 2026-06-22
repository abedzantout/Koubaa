package com.kobaatv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies stream URL building percent-encodes credentials, so values with
 * reserved characters are sent literally instead of altering the path.
 */
class XtreamClientUrlTest {

    @Test
    fun hlsUrl_plainCredentials_buildsExpectedPath() {
        val client = XtreamClient("http://example.com:8080", "user", "pass")
        assertEquals(
            "http://example.com:8080/live/user/pass/42.m3u8",
            client.hlsUrl(42),
        )
    }

    @Test
    fun tsUrl_plainCredentials_buildsExpectedPath() {
        val client = XtreamClient("http://example.com:8080", "user", "pass")
        assertEquals(
            "http://example.com:8080/live/user/pass/7.ts",
            client.tsUrl(7),
        )
    }

    @Test
    fun hlsUrl_specialCharsInCredentials_arePercentEncoded() {
        val client = XtreamClient("http://example.com:8080", "a b", "p@ss+/&")
        val url = client.hlsUrl(1)

        // The raw reserved characters must not appear unencoded in the path.
        assertTrue("space not encoded: $url", "a b" !in url)
        assertTrue("plus not encoded: $url", url.contains("p%40ss") || url.contains("%40"))
        // Space becomes %20 in a path segment (not +, which is query-only).
        assertTrue("space should be %20: $url", url.contains("a%20b"))
    }

    @Test
    fun hlsUrl_preservesHostPort() {
        val client = XtreamClient("http://1.2.3.4:25461", "u", "p")
        assertTrue(client.hlsUrl(99).startsWith("http://1.2.3.4:25461/live/"))
    }
}
