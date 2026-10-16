package com.davidsun.note.domain.usecase

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.core.common.isValidIntervalDays
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val clock: AppClock,
    private val reminderScheduler: ReminderScheduler,
) {
    fun observeTasks(): Flow<List<Task>> = repository.observeActiveTasks()

    suspend fun getTask(id: String): Task? = repository.getTask(id)

    suspend fun save(
        id: String,
        title: String,
        description: String,
        reminderType: ReminderType,
        reminderAt: Long?,
        intervalDays: Int?,
    ): SaveTaskResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return SaveTaskResult.InvalidTitle
        val reminder = normalizeReminder(reminderType, reminderAt, intervalDays)
            ?: return SaveTaskResult.InvalidReminder
        val trimmedDescription = description.trim().ifBlank { null }
        val existing = repository.getTask(id)
        if (
            existing != null &&
            existing.title == trimmedTitle &&
            existing.description == trimmedDescription &&
            existing.reminderType == reminder.type &&
            existing.reminderAt == reminder.triggerAt &&
            existing.intervalDays == reminder.intervalDays
        ) {
            return SaveTaskResult.Saved
        }
        val now = clock.nowMillis()
        val task = Task(
            id = id,
            title = trimmedTitle,
            description = trimmedDescription,
            reminderType = reminder.type,
            reminderAt = reminder.triggerAt,
            intervalDays = reminder.intervalDays,
            isCompleted = existing?.isCompleted ?: false,
            completedAt = existing?.completedAt,
            isHidden = existing?.isHidden ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deletedAt = null,
            revision = (existing?.revision ?: 0L) + 1L,
            syncStatus = SyncStatus.PENDING,
        )
        repository.upsert(task)
        reminderScheduler.sync(task)
        return SaveTaskResult.Saved
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        val existing = repository.getTask(id) ?: return
        if (existing.isCompleted == completed) return
        val now = clock.nowMillis()
        val updated = existing.copy(
            isCompleted = completed,
            completedAt = if (completed) now else null,
            updatedAt = now,
            revision = existing.revision + 1L,
            syncStatus = SyncStatus.PENDING,
        )
        repository.upsert(updated)
        reminderScheduler.sync(updated)
    }

    suspend fun setHidden(id: String, hidden: Boolean) {
        val existing = repository.getTask(id) ?: return
        if (existing.isHidden == hidden) return
        val now = clock.nowMillis()
        val updated = existing.copy(
            isHidden = hidden,
            updatedAt = now,
            revision = existing.revision + 1L,
            syncStatus = SyncStatus.PENDING,
        )
        repository.upsert(updated)
        reminderScheduler.sync(updated)
    }

    suspend fun delete(id: String) {
        val existing = repository.getTask(id) ?: return
        val now = clock.nowMillis()
        val updated = existing.copy(
            deletedAt = now,
            updatedAt = now,
            revision = existing.revision + 1L,
            syncStatus = SyncStatus.PENDING,
        )
        repository.upsert(updated)
        reminderScheduler.sync(updated)
    }

    private fun normalizeReminder(
        type: ReminderType,
        reminderAt: Long?,
        intervalDays: Int?,
    ): NormalizedReminder? = when (type) {
        ReminderType.NONE -> NormalizedReminder(ReminderType.NONE, null, null)
        ReminderType.ONCE -> reminderAt?.let { NormalizedReminder(ReminderType.ONCE, it, null) }
        ReminderType.INTERVAL -> {
            if (reminderAt == null || intervalDays == null || !isValidIntervalDays(intervalDays)) {
                null
            } else {
                NormalizedReminder(ReminderType.INTERVAL, reminderAt, intervalDays)
            }
        }
    }

    private data class NormalizedReminder(
        val type: ReminderType,
        val triggerAt: Long?,
        val intervalDays: Int?,
    )
}

sealed interface SaveTaskResult {
    data object Saved : SaveTaskResult

    data object InvalidTitle : SaveTaskResult

    data object InvalidReminder : SaveTaskResult
}
