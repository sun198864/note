package com.davidsun.note.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteListScrollTest {
    @Test
    fun scrollIndexAccountsForTheErrorBanner() {
        val notes = listOf(
            NoteItemUi("1", "一", "", ""),
            NoteItemUi("2", "二", "", ""),
        )

        assertEquals(1, noteScrollIndex(notes, "2", hasErrorBanner = false))
        assertEquals(2, noteScrollIndex(notes, "2", hasErrorBanner = true))
        assertNull(noteScrollIndex(notes, "missing", hasErrorBanner = false))
    }
}
