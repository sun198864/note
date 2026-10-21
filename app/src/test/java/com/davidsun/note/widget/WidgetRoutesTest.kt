package com.davidsun.note.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRoutesTest {
    @Test
    fun listRoutesOpenWithoutAFocusedItem() {
        assertEquals(WidgetOpenRequest("tasks", null), WidgetRoutes.openRequest("tasks"))
        assertEquals(WidgetOpenRequest("notes", null), WidgetRoutes.openRequest("notes"))
    }

    @Test
    fun itemRoutesOpenTheListAndFocusThatItem() {
        assertEquals(WidgetOpenRequest("tasks", "task-1"), WidgetRoutes.openRequest("tasks/editor/task-1"))
        assertEquals(WidgetOpenRequest("notes", "note-1"), WidgetRoutes.openRequest("notes/editor/note-1"))
    }

    @Test
    fun unknownRoutesStayInTheApp() {
        assertNull(WidgetRoutes.openRequest("settings"))
        assertNull(WidgetRoutes.openRequest("tasks/editor/new"))
        assertNull(WidgetRoutes.openRequest("tasks/editor/a/b"))
        assertNull(WidgetRoutes.openRequest(""))
    }
}
