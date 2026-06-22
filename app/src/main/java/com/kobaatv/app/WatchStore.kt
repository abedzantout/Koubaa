package com.kobaatv.app

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the favorites and recently-watched lists as JSON arrays in the app's
 * SharedPreferences, delegating all list math to [WatchListLogic].
 *
 * Items are keyed by [WatchItem.streamId]. Stream ids can collide across
 * accounts, so these lists are effectively account-scoped only by coincidence;
 * the Favorites/Recently-Watched UI phase will decide whether to namespace by
 * account.
 *
 * The favorites methods have no UI caller yet — the Favorites screen and the
 * Recently-Watched home row land in a later (device-tested) phase. The list math
 * is already covered by WatchListLogicTest.
 */
object WatchStore {
    private const val KEY_FAVORITES = "favorites_json"
    private const val KEY_HISTORY = "history_json"
    private const val HISTORY_MAX = 20

    // --- favorites ---

    fun favorites(ctx: Context): List<WatchItem> = read(ctx, KEY_FAVORITES)

    /** Toggles the channel's favorite state; returns true if it is now a favorite. */
    fun toggleFavorite(ctx: Context, channel: XtreamClient.Channel): Boolean {
        val updated = WatchListLogic.toggleFavorite(favorites(ctx), WatchItem.from(channel))
        write(ctx, KEY_FAVORITES, updated)
        return WatchListLogic.isFavorite(updated, channel.streamId)
    }

    fun isFavorite(ctx: Context, streamId: Int): Boolean =
        WatchListLogic.isFavorite(favorites(ctx), streamId)

    fun removeFavorite(ctx: Context, streamId: Int) {
        write(ctx, KEY_FAVORITES, WatchListLogic.remove(favorites(ctx), streamId))
    }

    // --- history ---

    fun history(ctx: Context): List<WatchItem> = read(ctx, KEY_HISTORY)

    fun recordWatched(ctx: Context, channel: XtreamClient.Channel) {
        val updated = WatchListLogic.recordWatched(history(ctx), WatchItem.from(channel), HISTORY_MAX)
        write(ctx, KEY_HISTORY, updated)
    }

    // --- json io ---

    private fun read(ctx: Context, key: String): List<WatchItem> {
        val raw = Prefs.get(ctx).getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            WatchItem(o.optInt("streamId"), o.optString("name"), o.optString("logo"))
        }
    }

    private fun write(ctx: Context, key: String, items: List<WatchItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("streamId", item.streamId)
                    .put("name", item.name)
                    .put("logo", item.logo),
            )
        }
        Prefs.get(ctx).edit { putString(key, arr.toString()) }
    }
}
