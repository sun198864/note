package com.davidsun.note.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.davidsun.note.core.common.isValidIntervalDays
import com.davidsun.note.core.common.isValidWeekdays
import com.davidsun.note.core.common.nextFutureIntervalTriggerAt
import com.davidsun.note.core.common.nextFutureWeeklyTriggerAt
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.repository.TaskRepository
import com.davidsun.note.core.common.AppClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val clock: AppClock,
) : ReminderScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun restoreOnAppStart() {
        scope.launch {
            runCatching { restoreAll() }
        }
    }

    override suspend fun restoreAll() {
        taskRepository.activeTasks().forEach { task ->
            sync(task)
        }
    }

    override suspend fun sync(task: Task) {
        cancel(task.id)
        if (!task.canNotify()) return
        when (task.reminderType) {
            ReminderType.NONE -> Unit
            ReminderType.ONCE -> {
                val triggerAt = task.reminderAt ?: return
                if (triggerAt > clock.nowMillis()) schedule(task.id, triggerAt)
            }
            ReminderType.INTERVAL -> scheduleInterval(task)
            ReminderType.WEEKLY -> scheduleWeekly(task)
        }
    }

    private suspend fun scheduleInterval(task: Task) {
        val reminderAt = task.reminderAt ?: return
        val intervalDays = task.intervalDays ?: return
        if (!isValidIntervalDays(intervalDays)) return
        val now = clock.nowMillis()
        val triggerAt = if (reminderAt > now) {
            reminderAt
        } else {
            nextFutureIntervalTriggerAt(reminderAt, intervalDays, now)
        }
        if (triggerAt != reminderAt) {
            taskRepository.upsert(
                task.copy(
                    reminderAt = triggerAt,
                    updatedAt = now,
                    revision = task.revision + 1L,
                    syncStatus = SyncStatus.PENDING,
                ),
            )
        }
        schedule(task.id, triggerAt)
    }

    private suspend fun scheduleWeekly(task: Task) {
        val reminderAt = task.reminderAt ?: return
        val weekdays = task.weekdays ?: return
        if (!isValidWeekdays(weekdays)) return
        val now = clock.nowMillis()
        val triggerAt = if (reminderAt > now) {
            reminderAt
        } else {
            nextFutureWeeklyTriggerAt(reminderAt, weekdays, now)
        }
        if (triggerAt != reminderAt) {
            taskRepository.upsert(
                task.copy(
                    reminderAt = triggerAt,
                    updatedAt = now,
                    revision = task.revision + 1L,
                    syncStatus = SyncStatus.PENDING,
                ),
            )
        }
        schedule(task.id, triggerAt)
    }

    private fun schedule(taskId: String, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmIntent(taskId, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancel(taskId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = existingAlarmIntent(taskId) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun alarmIntent(taskId: String, triggerAt: Long, flags: Int): PendingIntent {
        val intent = reminderIntent(taskId, triggerAt)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingAlarmIntent(taskId: String): PendingIntent? {
        val intent = reminderIntent(taskId, triggerAt = 0L)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderIntent(taskId: String, triggerAt: Long): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = "$ACTION_PREFIX$taskId"
            putExtra(TaskNotifications.EXTRA_TASK_ID, taskId)
            putExtra(TaskNotifications.EXTRA_TRIGGER_AT, triggerAt)
        }

    private fun Task.canNotify(): Boolean =
        deletedAt == null && !isCompleted && !isHidden && reminderType != ReminderType.NONE

    private companion object {
        const val ACTION_PREFIX = "com.davidsun.note.REMIND."
    }
}
