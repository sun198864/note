package com.davidsun.note.domain

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.repository.NoteRepository
import com.davidsun.note.domain.usecase.NoteUseCase
import com.davidsun.note.domain.usecase.SaveNoteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NoteUseCaseTest {
    private val clock = MutableClock(1_000)
    private val repository = FakeNoteRepository()
    private val useCase = NoteUseCase(repository, clock)

    @Test
    fun blankNoteIsNotCreated() = runBlocking {
        val result = useCase.save(id = "n1", title = "  ", content = "\n")

        assertEquals(SaveNoteResult.IgnoredEmpty, result)
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun saveMarksPendingAndKeepsCreatedAtOnLaterEdits() = runBlocking {
        val created = useCase.save(id = "n1", title = "標題", content = "內容") as SaveNoteResult.Saved
        clock.now = 2_000
        useCase.save(id = "n1", title = "標題", content = "新內容")

        val note = repository.notes.getValue("n1")
        assertEquals("標題", created.title)
        assertEquals(1_000, note.createdAt)
        assertEquals(2_000, note.updatedAt)
        assertEquals(2, note.revision)
        assertEquals(SyncStatus.PENDING, note.syncStatus)
        assertNull(note.deletedAt)
    }

    @Test
    fun unchangedSaveDoesNotBumpRevision() = runBlocking {
        useCase.save(id = "n1", title = "標題", content = "內容")
        clock.now = 2_000
        useCase.save(id = "n1", title = " 標題 ", content = "內容")

        assertEquals(1, repository.notes.getValue("n1").revision)
        assertEquals(1_000, repository.notes.getValue("n1").updatedAt)
    }

    @Test
    fun deleteKeepsATombstoneOutOfActiveReads() = runBlocking {
        useCase.save(id = "n1", title = "標題", content = "內容")
        clock.now = 3_000
        useCase.delete("n1")

        val note = repository.notes.getValue("n1")
        assertEquals(3_000L, note.deletedAt)
        assertEquals(SyncStatus.PENDING, note.syncStatus)
        assertNull(useCase.getNote("n1"))
    }

    private class MutableClock(var now: Long) : AppClock {
        override fun nowMillis(): Long = now

        override fun today(zone: ZoneId): LocalDate = LocalDate.of(2026, 10, 15)
    }

    private class FakeNoteRepository : NoteRepository {
        val notes = linkedMapOf<String, Note>()

        override fun observeActiveNotes(): Flow<List<Note>> =
            flowOf(notes.values.filter { it.deletedAt == null })

        override suspend fun getNote(id: String): Note? = notes[id]?.takeIf { it.deletedAt == null }

        override suspend fun upsert(note: Note) {
            notes[note.id] = note
        }
    }
}
