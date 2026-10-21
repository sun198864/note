package com.davidsun.note.domain.usecase

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val clock: AppClock,
) {
    fun observeNotes(): Flow<List<Note>> = repository.observeActiveNotes()

    suspend fun getNote(id: String): Note? = repository.getNote(id)

    suspend fun save(id: String, title: String, content: String, isImportant: Boolean = false): SaveNoteResult {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        val existing = repository.getNote(id)
        if (existing == null && trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
            return SaveNoteResult.IgnoredEmpty
        }
        if (existing != null &&
            existing.title == trimmedTitle &&
            existing.content == trimmedContent &&
            existing.isImportant == isImportant
        ) {
            return SaveNoteResult.Saved(title = trimmedTitle, content = trimmedContent)
        }
        val now = clock.nowMillis()
        repository.upsert(
            Note(
                id = id,
                title = trimmedTitle,
                content = trimmedContent,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deletedAt = null,
                revision = (existing?.revision ?: 0L) + 1L,
                syncStatus = SyncStatus.PENDING,
                isImportant = isImportant,
                sortIndex = existing?.sortIndex ?: -now,
            ),
        )
        return SaveNoteResult.Saved(title = trimmedTitle, content = trimmedContent)
    }

    suspend fun setImportant(id: String, important: Boolean) {
        val existing = repository.getNote(id) ?: return
        if (existing.isImportant == important) return
        val now = clock.nowMillis()
        repository.upsert(
            existing.copy(
                isImportant = important,
                updatedAt = now,
                revision = existing.revision + 1L,
                syncStatus = SyncStatus.PENDING,
            ),
        )
    }

    suspend fun reorder(idsInOrder: List<String>) {
        val now = clock.nowMillis()
        idsInOrder.forEachIndexed { index, id ->
            val existing = repository.getNote(id) ?: return@forEachIndexed
            val sortIndex = index.toLong()
            if (existing.sortIndex == sortIndex) return@forEachIndexed
            repository.upsert(
                existing.copy(
                    sortIndex = sortIndex,
                    updatedAt = now,
                    revision = existing.revision + 1L,
                    syncStatus = SyncStatus.PENDING,
                ),
            )
        }
    }

    suspend fun delete(id: String) {
        val existing = repository.getNote(id) ?: return
        val now = clock.nowMillis()
        repository.upsert(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                revision = existing.revision + 1L,
                syncStatus = SyncStatus.PENDING,
            ),
        )
    }
}

sealed interface SaveNoteResult {
    data class Saved(val title: String, val content: String) : SaveNoteResult

    data object IgnoredEmpty : SaveNoteResult
}
