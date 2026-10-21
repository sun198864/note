package com.davidsun.note.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["reminderAt"]),
        Index(value = ["isCompleted"]),
        Index(value = ["isHidden"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val reminderType: String,
    val reminderAt: Long?,
    val intervalDays: Int?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isHidden: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Long,
    val syncStatus: String,
    val weekdays: Int?,
)
