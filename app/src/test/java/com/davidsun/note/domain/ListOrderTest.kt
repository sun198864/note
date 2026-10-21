package com.davidsun.note.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListOrderTest {
    @Test
    fun movePlacesTheDraggedItemAtTheTarget() {
        val moved = ListOrder.move(listOf("a", "b", "c"), "a", "c") { it }

        assertEquals(listOf("b", "c", "a"), moved)
    }

    @Test
    fun moveIgnoresAMissingOrSameKey() {
        assertNull(ListOrder.move(listOf("a", "b"), "a", "a") { it })
        assertNull(ListOrder.move(listOf("a", "b"), "a", "missing") { it })
    }

    @Test
    fun mergeKeepsItemsThatAreNotInTheVisibleOrder() {
        val merged = ListOrder.merge(listOf("a", "b", "c", "d"), listOf("c", "a"))

        assertEquals(listOf("c", "b", "a", "d"), merged)
    }
}
