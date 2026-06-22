package com.kobaatv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchListLogicTest {

    private fun item(id: Int) = WatchItem(id, "Channel $id", "logo$id.png")

    // --- favorites ---

    @Test
    fun toggle_addsWhenAbsent_prepends() {
        val result = WatchListLogic.toggleFavorite(listOf(item(1)), item(2))
        assertEquals(listOf(item(2), item(1)), result)
    }

    @Test
    fun toggle_removesWhenPresent() {
        val result = WatchListLogic.toggleFavorite(listOf(item(1), item(2)), item(1))
        assertEquals(listOf(item(2)), result)
    }

    @Test
    fun toggle_twice_returnsToStart() {
        val start = listOf(item(3))
        val once = WatchListLogic.toggleFavorite(start, item(9))
        val twice = WatchListLogic.toggleFavorite(once, item(9))
        assertEquals(start, twice)
    }

    @Test
    fun isFavorite_reflectsMembership() {
        val favs = listOf(item(1), item(2))
        assertTrue(WatchListLogic.isFavorite(favs, 2))
        assertFalse(WatchListLogic.isFavorite(favs, 5))
    }

    // --- history ---

    @Test
    fun recordWatched_movesExistingToFront_noDuplicate() {
        val current = listOf(item(1), item(2), item(3))
        val result = WatchListLogic.recordWatched(current, item(3))
        assertEquals(listOf(item(3), item(1), item(2)), result)
        assertEquals(3, result.size) // no duplicate of 3
    }

    @Test
    fun recordWatched_newItem_prepends() {
        val result = WatchListLogic.recordWatched(listOf(item(1)), item(2))
        assertEquals(listOf(item(2), item(1)), result)
    }

    @Test
    fun recordWatched_capsAtMax_droppingOldest() {
        val current = (1..20).map { item(it) }            // 20 items, newest = 1
        val result = WatchListLogic.recordWatched(current, item(99), max = 20)
        assertEquals(20, result.size)
        assertEquals(99, result.first().streamId)          // new one at front
        assertFalse(result.any { it.streamId == 20 })       // oldest dropped
    }

    @Test
    fun recordWatched_updatedLogoReplacesOld() {
        val current = listOf(WatchItem(1, "Old", "old.png"))
        val result = WatchListLogic.recordWatched(current, WatchItem(1, "New", "new.png"))
        assertEquals(1, result.size)
        assertEquals("New", result.first().name)
        assertEquals("new.png", result.first().logo)
    }

    @Test
    fun remove_dropsById() {
        assertEquals(listOf(item(2)), WatchListLogic.remove(listOf(item(1), item(2)), 1))
    }
}
