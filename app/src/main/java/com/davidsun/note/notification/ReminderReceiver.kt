package com.davidsun.note.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.davidsun.note.core.common.AppClock
import com.davidsun.note.core.common.isValidIntervalDays
import com.davidsun.note.core.common.nextIntervalTriggerAt
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var taskRepository: TaskRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var clock: AppClock

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotifications.EXTRA_TASK_ID) ?: return
        val triggerAt = intent.getLongExtra(TaskNotifications.EXTRA_TRIGGER_AT, 0L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deliver(context, taskId, triggerAt)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun deliver(context: Context, taskId: String, triggerAt: Long) {
        val task = taskRepository.getTask(taskId) ?: return
        if (!task.shouldNotify(triggerAt)) return
        TaskNotifications.show(context, task)
        if (task.reminderType != ReminderType.INTERVAL) return
        val intervalDays = task.intervalDays ?: return
        if (!isValidIntervalDays(intervalDays)) return
        val nextAt = nextIntervalTriggerAt(task.reminderAt ?: triggerAt, intervalDays, ZoneId.systemDefault())
        val updated = task.copy(
            reminderAt = nextAt,
            updatedAt = clock.nowMillis(),
            revision = task.revision + 1L,
            syncStatus = SyncStatus.PENDING,
        )
        taskRepository.upsert(updated)
        reminderScheduler.sync(updated)
    }

    private fun Task.shouldNotify(triggerAt: Long): Boolean {
        if (deletedAt != null || isCompleted || isHidden) return false
        if (reminderType == ReminderType.NONE) return false
        val scheduled = reminderAt ?: return false
        return scheduled == triggerAt
    }
}
