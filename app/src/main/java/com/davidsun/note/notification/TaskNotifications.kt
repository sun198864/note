package com.davidsun.note.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.davidsun.note.MainActivity
import com.davidsun.note.R
import com.davidsun.note.core.common.formatWeekdaySelection
import com.davidsun.note.core.common.isValidWeekdays
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.Task

object TaskNotifications {
    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_TRIGGER_AT = "triggerAt"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_body)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, task: Task) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val body = notificationBody(context, task)
        val openApp = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(appIcon(context))
            .setContentTitle(task.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
    }

    private fun appIcon(context: Context): Bitmap {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        return drawable.toBitmap(width = size, height = size)
    }

    private fun notificationBody(context: Context, task: Task): String {
        val summary = when (task.reminderType) {
            ReminderType.INTERVAL -> context.getString(
                R.string.notification_interval_body,
                task.intervalDays ?: 1,
            )
            ReminderType.WEEKLY -> {
                val weekdays = task.weekdays?.takeIf(::isValidWeekdays) ?: return context.getString(R.string.notification_once_body)
                context.getString(R.string.notification_weekly_body, formatWeekdaySelection(weekdays))
            }
            else -> context.getString(R.string.notification_once_body)
        }
        val description = task.description?.takeIf { it.isNotBlank() } ?: return summary
        return "$summary\n$description"
    }
}
