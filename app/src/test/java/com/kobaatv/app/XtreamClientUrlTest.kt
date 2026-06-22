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
    fun hlsUrl_pathReservedCharsInCredentials_arePercentEncoded() {
        // Use chars that OkHttp's PATH_SEGMENT_ENCODE_SET actually encodes: a
        // space and a '/'. '/' is the important one — left raw it would split
        // the credential into extra path segments and change the route.
        val client = XtreamClient("http://example.com:8080", "a b", "p/w")
        val url = client.hlsUrl(1)

        // Space -> %20, '/' -> %2F, so neither splits or alters the path.
        assertTrue("space should be %20: $url", url.contains("a%20b"))
        assertTrue("slash should be %2F: $url", url.contains("p%2Fw"))
        // The password must remain a single segment between user and stream id.
        assertEquals("http://example.com:8080/live/a%20b/p%2Fw/1.m3u8", url)
    }

    @Test
    fun hlsUrl_preservesHostPort() {
        val client = XtreamClient("http://1.2.3.4:25461", "u", "p")
        assertTrue(client.hlsUrl(99).startsWith("http://1.2.3.4:25461/live/"))
    }

    @Test
    fun hlsUrl_httpsSchemePreserved() {
        // Non-default https port so it survives (HttpUrl elides :443).
        val client = XtreamClient("https://example.com:8443", "u", "p")
        assertEquals("https://example.com:8443/live/u/p/5.m3u8", client.hlsUrl(5))
    }

    @Test
    fun url_hostWithTrailingSlash_doesNotDoubleSlash() {
        // HttpUrl normalizes the host's trailing slash so the live path is clean.
        val client = XtreamClient("http://example.com:8080/", "u", "p")
        assertEquals("http://example.com:8080/live/u/p/3.m3u8", client.hlsUrl(3))
    }

    @Test
    fun tsUrl_specialCharsEncoded() {
        val client = XtreamClient("http://example.com:8080", "a b", "p/w")
        assertEquals("http://example.com:8080/live/a%20b/p%2Fw/9.ts", client.tsUrl(9))
    }
}
