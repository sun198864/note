package com.davidsun.note.data.local.mapper

import com.davidsun.note.data.local.entity.TaskEntity
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.Task

internal fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    reminderType = reminderType.toReminderType(),
    reminderAt = reminderAt,
    intervalDays = intervalDays,
    weekdays = weekdays,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
    syncStatus = syncStatus.toSyncStatus(),
)

internal fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    reminderType = reminderType.name,
    reminderAt = reminderAt,
    intervalDays = intervalDays,
    weekdays = weekdays,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
    syncStatus = syncStatus.name,
)

internal fun String.toReminderType(): ReminderType =
    runCatching { ReminderType.valueOf(this) }.getOrDefault(ReminderType.NONE)
