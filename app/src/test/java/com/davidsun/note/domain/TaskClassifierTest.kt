package com.davidsun.note.domain

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.usecase.TaskClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskClassifierTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val today = LocalDate.of(2026, 10, 15)
    private val classifier = TaskClassifier(clock = FixedClock(today))

    @Test
    fun overdueAndTodayStayInTodayWhileFutureDatesAreUpcoming() {
        val grouped = classifier.group(
            tasks = listOf(
                task("older", "更早", reminderAt = today.minusDays(2).start()),
                task("yesterday", "昨天", reminderAt = today.minusDays(1).start()),
                task("today", "今天", reminderAt = today.start()),
                task("tomorrow", "明天", reminderAt = today.plusDays(1).start()),
                task("undated", "沒日期", reminderType = ReminderType.NONE),
            ),
            zone = zone,
        )

        assertEquals(listOf("更早", "昨天", "今天"), grouped.today.map { it.title })
        assertEquals(listOf("明天"), grouped.upcoming.map { it.title })
        assertEquals(listOf("沒日期"), grouped.undated.map { it.title })
        assertTrue(classifier.isOverdue(grouped.today.first(), zone))
        assertFalse(classifier.isOverdue(grouped.today.last(), zone))
    }

    @Test
    fun completedHiddenAndDeletedTasksLeaveTheOpenLists() {
        val grouped = classifier.group(
            tasks = listOf(
                task("done", "已完成", reminderAt = today.start(), isCompleted = true, completedAt = 20),
                task("hidden", "已隱藏", reminderAt = today.minusDays(3).start(), isHidden = true, updatedAt = 5),
                task("gone", "已刪除", deletedAt = 9),
                task("open", "進行中"),
            ),
            zone = zone,
        )

        assertEquals(listOf("進行中"), grouped.undated.map { it.title })
        assertEquals(listOf("已完成"), grouped.completed.map { it.title })
        assertEquals(listOf("已隱藏"), grouped.hidden.map { it.title })
        assertTrue(grouped.today.isEmpty())
        val visibleIds = listOf(
            grouped.today,
            grouped.upcoming,
            grouped.undated,
            grouped.completed,
            grouped.hidden,
        ).flatten().map { it.id }
        assertFalse(visibleIds.contains("gone"))
        assertEquals(visibleIds.size, visibleIds.distinct().size)
    }

    private fun LocalDate.start(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: String,
        title: String,
        reminderAt: Long? = null,
        reminderType: ReminderType = if (reminderAt == null) ReminderType.NONE else ReminderType.ONCE,
        isCompleted: Boolean = false,
        completedAt: Long? = null,
        isHidden: Boolean = false,
        deletedAt: Long? = null,
        updatedAt: Long = 0,
    ) = Task(
        id = id,
        title = title,
        description = null,
        reminderType = reminderType,
        reminderAt = reminderAt,
        intervalDays = null,
        isCompleted = isCompleted,
        completedAt = completedAt,
        isHidden = isHidden,
        createdAt = 0,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        revision = 1,
        syncStatus = SyncStatus.PENDING,
    )

    private class FixedClock(private val today: LocalDate) : AppClock {
        override fun nowMillis(): Long = 0

        override fun today(zone: ZoneId): LocalDate = today
    }
}
