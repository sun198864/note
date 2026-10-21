package com.davidsun.note.core.common

import com.davidsun.note.domain.model.ReminderType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class AppDateFormatter @Inject constructor() {
    fun formatReminderDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return dateTime.format(REMINDER_DATE_TIME)
    }

    fun formatTaskReminder(
        type: ReminderType,
        reminderAt: Long?,
        intervalDays: Int?,
        weekdays: Int? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String? = when (type) {
        ReminderType.NONE -> null
        ReminderType.ONCE -> reminderAt?.let { formatReminderDateTime(it, zone) }
        ReminderType.INTERVAL -> {
            val at = reminderAt ?: return null
            val days = intervalDays ?: return null
            "每 $days 天 · ${formatReminderDateTime(at, zone)}"
        }
        ReminderType.WEEKLY -> {
            val at = reminderAt
            val days = weekdays
            if (at == null || days == null || !isValidWeekdays(days)) {
                null
            } else {
                "每週${formatWeekdaySelection(days)} · ${formatReminderDateTime(at, zone)}"
            }
        }
    }

    fun formatEditedAt(
        epochMillis: Long,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val date = dateTime.toLocalDate()
        return when {
            date == today -> dateTime.format(TIME)
            date.year == today.year -> date.format(MONTH_DAY)
            else -> date.format(YEAR_MONTH_DAY)
        }
    }

    private companion object {
        val REMINDER_DATE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M月d日 EEE HH:mm", Locale.TAIWAN)
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.TAIWAN)
        val MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.TAIWAN)
        val YEAR_MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.TAIWAN)
    }
}
