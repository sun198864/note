package com.davidsun.note.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val reminderType: ReminderType,
    val reminderAt: Long?,
    val intervalDays: Int?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isHidden: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Long,
    val syncStatus: SyncStatus,
)
