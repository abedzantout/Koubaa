package com.kobaatv.app

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drives XtreamClient against a MockWebServer to verify response parsing for
 * login, live streams, and the empty/error cases.
 */
class XtreamClientParsingTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): XtreamClient {
        // MockWebServer URL is http://host:port/ ; drop the trailing slash.
        val host = server.url("/").toString().trimEnd('/')
        return XtreamClient(host, "user", "pass")
    }

    @Test
    fun login_authOne_succeeds() {
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":1}}"""))
        val info = client().login()
        assertEquals(1, info.optInt("auth"))
    }

    @Test(expected = IllegalStateException::class)
    fun login_authZero_throws() {
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":0}}"""))
        client().login()
    }

    @Test(expected = IllegalStateException::class)
    fun login_missingUserInfo_throws() {
        server.enqueue(MockResponse().setBody("""{"something_else":true}"""))
        client().login()
    }

    @Test
    fun liveStreams_parsesChannels() {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"stream_id":10,"name":"Chan A","stream_icon":"a.png","category_id":"1"},
                  {"stream_id":20,"name":"Chan B","stream_icon":"b.png","category_id":"2"}
                ]""",
            ),
        )
        val channels = client().liveStreams()
        assertEquals(2, channels.size)
        assertEquals(10, channels[0].streamId)
        assertEquals("Chan A", channels[0].name)
        assertEquals("2", channels[1].categoryId)
    }

    @Test
    fun liveStreams_emptyArray_returnsEmptyList() {
        server.enqueue(MockResponse().setBody("[]"))
        assertTrue(client().liveStreams().isEmpty())
    }

    @Test
    fun liveStreams_sendsEncodedCredentialsAndCategory() {
        server.enqueue(MockResponse().setBody("[]"))
        val host = server.url("/").toString().trimEnd('/')
        XtreamClient(host, "a b", "p@ss").liveStreams(categoryId = "5&6")

        val request = server.takeRequest()
        val path = request.path ?: ""
        // Credentials and category are percent-encoded in the query string.
        assertTrue("username not encoded: $path", path.contains("username=a%20b") || path.contains("username=a+b"))
        assertTrue("password not encoded: $path", path.contains("p%40ss"))
        assertTrue("category not encoded: $path", path.contains("category_id=5%266"))
    }
}
