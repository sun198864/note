package com.davidsun.note.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRoutesTest {
    @Test
    fun listRoutesOpenWithoutAFocusedItem() {
        assertEquals(WidgetOpenRequest("tasks", null), WidgetRoutes.openRequest("tasks"))
        assertEquals(WidgetOpenRequest("notes", null), WidgetRoutes.openRequest("notes"))
        assertEquals(WidgetOpenRequest("vocab", null), WidgetRoutes.openRequest("vocab"))
    }

    @Test
    fun itemRoutesOpenTheListAndFocusThatItem() {
        assertEquals(WidgetOpenRequest("tasks", "task-1"), WidgetRoutes.openRequest("tasks/editor/task-1"))
        assertEquals(WidgetOpenRequest("notes", "note-1"), WidgetRoutes.openRequest("notes/editor/note-1"))
        assertEquals(
            WidgetOpenRequest("vocab/detail/word-1", null),
            WidgetRoutes.openRequest("vocab/detail/word-1"),
        )
    }

    @Test
    fun vocabDetailOpensTheListThenThatWord() {
        assertEquals("vocab", WidgetRoutes.listRoute("vocab"))
        assertEquals("vocab", WidgetRoutes.listRoute("vocab/detail/word-1"))
        assertEquals("vocab/detail/word-1", WidgetRoutes.detailRoute("vocab/detail/word-1"))
        assertNull(WidgetRoutes.detailRoute("vocab"))
        assertNull(WidgetRoutes.listRoute("vocab/detail/a/b"))
    }

    @Test
    fun unknownRoutesStayInTheApp() {
        assertNull(WidgetRoutes.openRequest("settings"))
        assertNull(WidgetRoutes.openRequest("tasks/editor/new"))
        assertNull(WidgetRoutes.openRequest("tasks/editor/a/b"))
        assertNull(WidgetRoutes.openRequest(""))
    }
}
