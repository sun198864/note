package com.davidsun.note.domain

import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.sync.SyncMerge
import com.davidsun.note.data.remote.noteFromMap
import com.davidsun.note.data.remote.taskFromMap
import com.davidsun.note.data.remote.toFirestoreMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {
    @Test
    fun newerUpdatedAtWins() {
        assertTrue(SyncMerge.remoteWins(1_000, 3, 2_000, 1))
        assertFalse(SyncMerge.remoteWins(2_000, 1, 1_000, 9))
    }

    @Test
    fun equalTimestampUsesHigherRevision() {
        assertTrue(SyncMerge.remoteWins(1_000, 1, 1_000, 2))
        assertFalse(SyncMerge.remoteWins(1_000, 2, 1_000, 2))
        assertFalse(SyncMerge.remoteWins(1_000, 4, 1_000, 3))
    }
}

class CloudDocumentTest {
    @Test
    fun noteRoundTripKeepsNullDeletedAt() {
        val note = Note(
            id = "n1",
            title = "標題",
            content = "內容",
            createdAt = 10,
            updatedAt = 20,
            deletedAt = null,
            revision = 2,
            syncStatus = SyncStatus.PENDING,
        )

        val restored = noteFromMap("n1", note.toFirestoreMap())

        assertEquals(note.copy(syncStatus = SyncStatus.SYNCED), restored)
    }

    @Test
    fun taskRoundTripKeepsOptionalFields() {
        val task = Task(
            id = "t1",
            title = "買菜",
            description = null,
            reminderType = ReminderType.WEEKLY,
            reminderAt = 30,
            intervalDays = null,
            weekdays = 3,
            isCompleted = false,
            completedAt = null,
            isHidden = false,
            createdAt = 10,
            updatedAt = 20,
            deletedAt = null,
            revision = 1,
            syncStatus = SyncStatus.PENDING,
        )

        val restored = taskFromMap("t1", task.toFirestoreMap())

        assertEquals(task.copy(syncStatus = SyncStatus.SYNCED), restored)
    }

    @Test
    fun malformedNoteIsIgnored() {
        assertNull(noteFromMap("n1", mapOf("title" to "只有標題")))
    }
}
