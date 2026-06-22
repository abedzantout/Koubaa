package com.kobaatv.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal Xtream Codes API client.
 *
 * Host format: http(s)://domain:port (no trailing slash)
 * Stream URL format (Live, HLS):   {host}/live/{user}/{pass}/{stream_id}.m3u8
 * Stream URL format (Live, TS):    {host}/live/{user}/{pass}/{stream_id}.ts
 */
class XtreamClient(
    private val host: String,
    private val username: String,
    private val password: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun api(action: String? = null, extra: String = ""): String {
        val base = "$host/player_api.php?username=$username&password=$password"
        return if (action == null) base else "$base&action=$action$extra"
    }

    private fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    /** Returns user_info auth=1 on success. */
    fun login(): JSONObject {
        val body = get(api())
        val json = JSONObject(body)
        val info = json.optJSONObject("user_info") ?: error("Bad response")
        if (info.optInt("auth", 0) != 1) error("auth failed")
        return info
    }

    fun liveCategories(): List<Category> {
        val arr = JSONArray(get(api("get_live_categories")))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Category(o.optString("category_id"), o.optString("category_name"))
        }
    }

    fun liveStreams(categoryId: String? = null): List<Channel> {
        val extra = if (categoryId.isNullOrEmpty()) "" else "&category_id=$categoryId"
        val arr = JSONArray(get(api("get_live_streams", extra)))
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
        "$host/live/$username/$password/$streamId.m3u8"

    fun tsUrl(streamId: Int): String =
        "$host/live/$username/$password/$streamId.ts"

    data class Category(val id: String, val name: String)
    data class Channel(val streamId: Int, val name: String, val logo: String, val categoryId: String)
}
