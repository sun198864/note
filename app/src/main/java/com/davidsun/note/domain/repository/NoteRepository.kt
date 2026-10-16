package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>

    suspend fun getNote(id: String): Note?

    suspend fun upsert(note: Note)
}
