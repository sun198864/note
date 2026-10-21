package com.davidsun.note.core

import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.core.common.weekdayBit
import com.davidsun.note.domain.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AppDateFormatterTest {
    private val formatter = AppDateFormatter()
    private val zone = ZoneId.of("Asia/Taipei")
    private val now = LocalDate.of(2026, 10, 15)
        .atTime(18, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun sameDayShowsTime() {
        val edited = LocalDate.of(2026, 10, 15)
            .atTime(LocalTime.of(9, 5))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        assertEquals("09:05", formatter.formatEditedAt(edited, now, zone))
    }

    @Test
    fun sameYearShowsMonthAndDay() {
        val edited = LocalDate.of(2026, 10, 14).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals("10月14日", formatter.formatEditedAt(edited, now, zone))
    }

    @Test
    fun anotherYearShowsFullDate() {
        val edited = LocalDate.of(2025, 12, 1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals("2025年12月1日", formatter.formatEditedAt(edited, now, zone))
    }

    @Test
    fun reminderLabelsIncludeClockTimeAndInterval() {
        val reminderAt = LocalDate.of(2026, 10, 16)
            .atTime(LocalTime.of(9, 30))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "10月16日 週五 09:30",
            formatter.formatTaskReminder(ReminderType.ONCE, reminderAt, null, zone = zone),
        )
        assertEquals(
            "每 3 天 · 10月16日 週五 09:30",
            formatter.formatTaskReminder(ReminderType.INTERVAL, reminderAt, 3, zone = zone),
        )
        val weekdays = weekdayBit(1) or weekdayBit(3) or weekdayBit(5)
        assertEquals(
            "每週一、三、五 · 10月16日 週五 09:30",
            formatter.formatTaskReminder(ReminderType.WEEKLY, reminderAt, null, weekdays, zone),
        )
        assertNull(formatter.formatTaskReminder(ReminderType.NONE, null, null, zone = zone))
    }
}
