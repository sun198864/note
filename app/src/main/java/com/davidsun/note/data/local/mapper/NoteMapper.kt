package com.davidsun.note.data.local.mapper

import com.davidsun.note.data.local.entity.NoteEntity
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.SyncStatus

internal fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
    syncStatus = syncStatus.toSyncStatus(),
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
    syncStatus = syncStatus.name,
)

internal fun String.toSyncStatus(): SyncStatus =
    SyncStatus.entries.firstOrNull { it.name == this } ?: SyncStatus.PENDING
