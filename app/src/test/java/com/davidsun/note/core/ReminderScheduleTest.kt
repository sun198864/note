package com.davidsun.note.core

import com.davidsun.note.core.common.combineDateAndTime
import com.davidsun.note.core.common.datePickerMillisToLocalDate
import com.davidsun.note.core.common.firstIntervalTriggerAt
import com.davidsun.note.core.common.localDateToDatePickerMillis
import com.davidsun.note.core.common.nextFutureIntervalTriggerAt
import com.davidsun.note.core.common.nextIntervalTriggerAt
import com.davidsun.note.core.common.reminderAtToDatePickerMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun datePickerRoundTripKeepsTheLocalCalendarDate() {
        val reminderAt = LocalDate.of(2026, 10, 15)
            .atTime(9, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val pickerMillis = reminderAtToDatePickerMillis(reminderAt, zone)
        val restored = combineDateAndTime(pickerMillis, hour = 9, minute = 30, zone)

        assertEquals(LocalDate.of(2026, 10, 15), datePickerMillisToLocalDate(pickerMillis))
        assertEquals(reminderAt, restored)
        assertEquals(pickerMillis, localDateToDatePickerMillis(LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun intervalUsesTodayWhenTheClockTimeIsStillAhead() {
        val now = LocalDate.of(2026, 10, 15).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        val trigger = firstIntervalTriggerAt(now, intervalDays = 3, hour = 9, minute = 0, zone = zone)

        assertEquals(at(LocalDate.of(2026, 10, 15), 9, 0), trigger)
    }

    @Test
    fun intervalSkipsToTheNextGapWhenTodaysTimeHasPassed() {
        val now = LocalDate.of(2026, 10, 15).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

        val trigger = firstIntervalTriggerAt(now, intervalDays = 3, hour = 9, minute = 0, zone = zone)

        assertEquals(at(LocalDate.of(2026, 10, 18), 9, 0), trigger)
    }

    @Test
    fun missedIntervalsAdvanceToTheNextFutureSlot() {
        val now = LocalDate.of(2026, 10, 15).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val previous = at(LocalDate.of(2026, 10, 10), 9, 0)

        val trigger = nextFutureIntervalTriggerAt(previous, intervalDays = 3, nowMillis = now, zone = zone)

        assertEquals(at(LocalDate.of(2026, 10, 16), 9, 0), trigger)
        assertEquals(at(LocalDate.of(2026, 10, 13), 9, 0), nextIntervalTriggerAt(previous, 3, zone))
    }

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}
