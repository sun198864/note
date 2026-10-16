package com.davidsun.note.data.repository

import com.davidsun.note.data.local.dao.NoteDao
import com.davidsun.note.data.local.mapper.toDomain
import com.davidsun.note.data.local.mapper.toEntity
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {
    override fun observeActiveNotes(): Flow<List<Note>> =
        noteDao.observeActive().map { notes -> notes.map { it.toDomain() } }

    override suspend fun getNote(id: String): Note? = noteDao.getById(id)?.toDomain()

    override suspend fun upsert(note: Note) {
        noteDao.upsert(note.toEntity())
    }
}
