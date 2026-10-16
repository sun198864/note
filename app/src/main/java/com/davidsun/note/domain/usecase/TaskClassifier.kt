package com.davidsun.note.domain.usecase

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.domain.model.GroupedTasks
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.Task
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class TaskClassifier @Inject constructor(
    private val clock: AppClock,
) {
    fun group(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()): GroupedTasks {
        val today = clock.today(zone)
        val todayTasks = mutableListOf<Task>()
        val upcoming = mutableListOf<Task>()
        val undated = mutableListOf<Task>()
        val completed = mutableListOf<Task>()
        val hidden = mutableListOf<Task>()

        tasks.forEach { task ->
            if (task.deletedAt != null) return@forEach
            if (task.isHidden) {
                hidden += task
                return@forEach
            }
            if (task.isCompleted) {
                completed += task
                return@forEach
            }
            val reminderDate = task.reminderDate(zone)
            when {
                reminderDate == null -> undated += task
                reminderDate > today -> upcoming += task
                else -> todayTasks += task
            }
        }

        return GroupedTasks(
            today = todayTasks.sortedWith(compareBy({ it.reminderAt ?: Long.MAX_VALUE }, { it.title })),
            upcoming = upcoming.sortedWith(compareBy({ it.reminderAt ?: Long.MAX_VALUE }, { it.title })),
            undated = undated.sortedByDescending { it.updatedAt },
            completed = completed.sortedByDescending { it.completedAt ?: it.updatedAt },
            hidden = hidden.sortedByDescending { it.updatedAt },
        )
    }

    fun isOverdue(task: Task, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (task.deletedAt != null || task.isHidden || task.isCompleted) return false
        val reminderDate = task.reminderDate(zone) ?: return false
        return reminderDate < clock.today(zone)
    }

    private fun Task.reminderDate(zone: ZoneId) = reminderAt
        ?.takeIf { reminderType != ReminderType.NONE }
        ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
}
