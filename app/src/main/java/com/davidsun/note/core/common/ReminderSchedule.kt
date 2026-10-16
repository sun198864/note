package com.davidsun.note.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

const val MIN_INTERVAL_DAYS = 1
const val MAX_INTERVAL_DAYS = 365

fun isValidIntervalDays(days: Int): Boolean = days in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS

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
