package com.davidsun.note.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

const val MIN_INTERVAL_DAYS = 1
const val MAX_INTERVAL_DAYS = 365

fun isValidIntervalDays(days: Int): Boolean = days in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS

const val WEEKDAY_MASK_ALL = 0b1111111

fun weekdayBit(isoDay: Int): Int {
    require(isoDay in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value)
    return 1 shl (isoDay - 1)
}

fun isValidWeekdays(mask: Int): Boolean = mask in 1..WEEKDAY_MASK_ALL

fun isWeekdaySelected(mask: Int, isoDay: Int): Boolean =
    isoDay in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value && mask and weekdayBit(isoDay) != 0

fun toggleWeekday(mask: Int, isoDay: Int): Int = mask xor weekdayBit(isoDay)

fun weekdayLabel(isoDay: Int): String = when (isoDay) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> ""
}

fun formatWeekdaySelection(mask: Int): String =
    (DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value)
        .filter { isWeekdaySelected(mask, it) }
        .joinToString("、", transform = ::weekdayLabel)

fun localDateToDatePickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun datePickerMillisToLocalDate(pickerMillis: Long): LocalDate =
    Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate()

fun reminderAtToDatePickerMillis(reminderAt: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val date = Instant.ofEpochMilli(reminderAt).atZone(zone).toLocalDate()
    return localDateToDatePickerMillis(date)
}

fun combineDateAndTime(
    pickerMillis: Long,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val date = datePickerMillisToLocalDate(pickerMillis)
    return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}

fun firstIntervalTriggerAt(
    nowMillis: Long,
    intervalDays: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    require(isValidIntervalDays(intervalDays))
    val todayAtTime = Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .atTime(hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
    return if (todayAtTime > nowMillis) {
        todayAtTime
    } else {
        nextIntervalTriggerAt(todayAtTime, intervalDays, zone)
    }
}

fun nextIntervalTriggerAt(
    triggerAt: Long,
    intervalDays: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    require(isValidIntervalDays(intervalDays))
    return Instant.ofEpochMilli(triggerAt)
        .atZone(zone)
        .plusDays(intervalDays.toLong())
        .toInstant()
        .toEpochMilli()
}

fun nextFutureIntervalTriggerAt(
    triggerAt: Long,
    intervalDays: Int,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    var next = triggerAt
    var guard = 0
    while (next <= nowMillis && guard < 10_000) {
        next = nextIntervalTriggerAt(next, intervalDays, zone)
        guard++
    }
    return next
}

fun firstWeeklyTriggerAt(
    nowMillis: Long,
    weekdays: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    require(isValidWeekdays(weekdays))
    val start = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    for (offset in 0..DayOfWeek.SUNDAY.value) {
        val date = start.plusDays(offset.toLong())
        if (!isWeekdaySelected(weekdays, date.dayOfWeek.value)) continue
        val trigger = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        if (trigger > nowMillis) return trigger
    }
    error("星期提醒找不到下一次時間")
}

fun nextWeeklyTriggerAt(
    triggerAt: Long,
    weekdays: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    require(isValidWeekdays(weekdays))
    val zoned = Instant.ofEpochMilli(triggerAt).atZone(zone)
    val time = zoned.toLocalTime()
    for (offset in 1..DayOfWeek.SUNDAY.value) {
        val date = zoned.toLocalDate().plusDays(offset.toLong())
        if (isWeekdaySelected(weekdays, date.dayOfWeek.value)) {
            return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
    }
    error("星期提醒找不到下一次時間")
}

fun nextFutureWeeklyTriggerAt(
    triggerAt: Long,
    weekdays: Int,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    var next = triggerAt
    var guard = 0
    while (next <= nowMillis && guard < 10_000) {
        next = nextWeeklyTriggerAt(next, weekdays, zone)
        guard++
    }
    return next
}
