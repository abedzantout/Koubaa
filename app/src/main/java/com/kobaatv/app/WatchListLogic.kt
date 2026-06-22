package com.kobaatv.app

/**
 * Pure list operations for the favorites and recently-watched stores, with no
 * Android or storage dependencies so they can be unit-tested directly. The
 * Android stores handle persistence and delegate the list math here.
 */
object WatchListLogic {

    /** Toggles [item] in a favorites list: removes if present (by id), else prepends. */
    fun toggleFavorite(current: List<WatchItem>, item: WatchItem): List<WatchItem> =
        if (current.any { it.streamId == item.streamId }) {
            current.filterNot { it.streamId == item.streamId }
        } else {
            listOf(item) + current
        }

    fun isFavorite(current: List<WatchItem>, streamId: Int): Boolean =
        current.any { it.streamId == streamId }

    /**
     * Records [item] as most-recently watched: moves it (or adds it) to the front,
     * removes any earlier duplicate, and caps the list at [max] entries.
     */
    fun recordWatched(current: List<WatchItem>, item: WatchItem, max: Int = 20): List<WatchItem> =
        (listOf(item) + current.filterNot { it.streamId == item.streamId }).take(max)

    fun remove(current: List<WatchItem>, streamId: Int): List<WatchItem> =
        current.filterNot { it.streamId == streamId }
}
