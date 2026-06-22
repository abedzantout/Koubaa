package com.kobaatv.app

/**
 * A minimal snapshot of a watched/favorited channel — enough to render a home
 * tile (name + logo) without re-fetching the full channel list. Identified by
 * [streamId].
 */
data class WatchItem(
    val streamId: Int,
    val name: String,
    val logo: String,
) {
    companion object {
        fun from(channel: XtreamClient.Channel) =
            WatchItem(channel.streamId, channel.name, channel.logo)
    }
}
