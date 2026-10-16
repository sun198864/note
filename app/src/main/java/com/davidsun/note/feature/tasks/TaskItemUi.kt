package com.davidsun.note.feature.tasks

import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.model.Task

data class TaskItemUi(
    val id: String,
    val title: String,
    val description: String?,
    val reminderLabel: String?,
    val isOverdue: Boolean,
    val isCompleted: Boolean,
)

fun Task.toTaskItemUi(formatter: AppDateFormatter, isOverdue: Boolean): TaskItemUi = TaskItemUi(
    id = id,
    title = title,
    description = description?.takeIf { it.isNotBlank() },
    reminderLabel = formatter.formatTaskReminder(reminderType, reminderAt, intervalDays),
    isOverdue = isOverdue,
    isCompleted = isCompleted,
)
