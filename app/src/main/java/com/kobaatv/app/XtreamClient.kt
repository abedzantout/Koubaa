package com.kobaatv.app

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal Xtream Codes API client.
 *
 * Host format: http(s)://domain:port (no trailing slash)
 * Stream URL format (Live, HLS):   {host}/live/{user}/{pass}/{stream_id}.m3u8
 * Stream URL format (Live, TS):    {host}/live/{user}/{pass}/{stream_id}.ts
 *
 * All username/password/category values are passed through HttpUrl, which
 * percent-encodes them, so credentials containing @, &, +, spaces, etc. are
 * sent correctly instead of breaking or altering the request.
 */
class XtreamClient(
    private val host: String,
    private val username: String,
    private val password: String,
    private val http: OkHttpClient = Network.client,
) {
    // Parsed lazily and tolerantly: an empty or malformed host surfaces as a
    // caught exception in the request flow (as before) rather than crashing at
    // construction time.
    private val baseUrl: HttpUrl
        get() = host.toHttpUrlOrNull() ?: error("Invalid host: $host")

    private fun api(action: String? = null, params: Map<String, String> = emptyMap()): HttpUrl =
        baseUrl.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
            .apply {
                if (action != null) addQueryParameter("action", action)
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }
            .build()

    private fun get(url: HttpUrl): String {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    /** Parses user_info on success; throws on a bad response or auth failure. */
    fun login(): AccountInfo {
        val body = get(api())
        val json = JSONObject(body)
        val info = json.optJSONObject("user_info") ?: error("Bad response")
        if (info.optInt("auth", 0) != 1) error("auth failed")
        // exp_date is a Unix-seconds string, or null/"null" for unlimited accounts.
        val expRaw = info.optString("exp_date", "")
        return AccountInfo(
            expiryEpochSeconds = expRaw.toLongOrNull(),
            activeConnections = info.optInt("active_cons", 0),
            maxConnections = info.optInt("max_connections", 0),
        )
    }

    fun liveCategories(): List<Category> {
        val arr = JSONArray(get(api("get_live_categories")))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Category(o.optString("category_id"), o.optString("category_name"))
        }
    }

    fun liveStreams(categoryId: String? = null): List<Channel> {
        val params = if (categoryId.isNullOrEmpty()) emptyMap() else mapOf("category_id" to categoryId)
        val arr = JSONArray(get(api("get_live_streams", params)))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Channel(
                streamId = o.optInt("stream_id"),
                name = o.optString("name"),
                logo = o.optString("stream_icon"),
                categoryId = o.optString("category_id"),
            )
        }
    }

    fun hlsUrl(streamId: Int): String =
        liveUrl("$streamId.m3u8")

    fun tsUrl(streamId: Int): String =
        liveUrl("$streamId.ts")

    private fun liveUrl(lastSegment: String): String =
        baseUrl.newBuilder()
            .addPathSegment("live")
            .addPathSegment(username)
            .addPathSegment(password)
            .addPathSegment(lastSegment)
            .build()
            .toString()

    /** Subset of Xtream user_info shown in the UI and used by failover. */
    data class AccountInfo(
        val expiryEpochSeconds: Long?,
        val activeConnections: Int,
        val maxConnections: Int,
    )

    data class Category(val id: String, val name: String)
    data class Channel(val streamId: Int, val name: String, val logo: String, val categoryId: String)
}
