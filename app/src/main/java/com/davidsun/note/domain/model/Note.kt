package com.davidsun.note.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Long,
    val syncStatus: SyncStatus,
    val isImportant: Boolean = false,
    val sortIndex: Long = 0,
)
