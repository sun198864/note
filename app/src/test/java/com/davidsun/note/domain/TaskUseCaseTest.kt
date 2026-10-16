package com.davidsun.note.domain

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.repository.TaskRepository
import com.davidsun.note.domain.usecase.SaveTaskResult
import com.davidsun.note.domain.usecase.TaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskUseCaseTest {
    private val clock = MutableClock(1_000)
    private val repository = FakeTaskRepository()
    private val scheduler = RecordingScheduler()
    private val useCase = TaskUseCase(repository, clock, scheduler)

    @Test
    fun blankTitleIsRejected() = runBlocking {
        val result = useCase.save(
            id = "t1",
            title = "  ",
            description = "說明",
            reminderType = ReminderType.NONE,
            reminderAt = null,
            intervalDays = null,
        )

        assertEquals(SaveTaskResult.InvalidTitle, result)
        assertTrue(repository.tasks.isEmpty())
    }

    @Test
    fun saveStoresPendingTaskAndClearsBlankDescription() = runBlocking {
        useCase.save(
            id = "t1",
            title = " 買菜 ",
            description = "  ",
            reminderType = ReminderType.ONCE,
            reminderAt = 50,
            intervalDays = null,
        )

        val task = repository.tasks.getValue("t1")
        assertEquals("買菜", task.title)
        assertNull(task.description)
        assertEquals(ReminderType.ONCE, task.reminderType)
        assertEquals(50L, task.reminderAt)
        assertEquals(listOf("t1"), scheduler.syncedIds)
        assertEquals(SyncStatus.PENDING, task.syncStatus)
        assertEquals(1, task.revision)
    }

    @Test
    fun completingTwiceDoesNotBumpRevision() = runBlocking {
        useCase.save(id = "t1", title = "買菜", description = "", reminderType = ReminderType.NONE, reminderAt = null, intervalDays = null)
        clock.now = 2_000
        useCase.setCompleted("t1", true)
        val completedRevision = repository.tasks.getValue("t1").revision
        useCase.setCompleted("t1", true)

        val task = repository.tasks.getValue("t1")
        assertTrue(task.isCompleted)
        assertEquals(2_000L, task.completedAt)
        assertEquals(completedRevision, task.revision)
    }

    @Test
    fun uncompleteClearsCompletedAtAndHideKeepsTheTask() = runBlocking {
        useCase.save(id = "t1", title = "買菜", description = "", reminderType = ReminderType.NONE, reminderAt = null, intervalDays = null)
        useCase.setCompleted("t1", true)
        clock.now = 3_000
        useCase.setCompleted("t1", false)
        useCase.setHidden("t1", true)

        val task = repository.tasks.getValue("t1")
        assertEquals(false, task.isCompleted)
        assertNull(task.completedAt)
        assertTrue(task.isHidden)
        assertNull(task.deletedAt)
    }

    @Test
    fun deleteMarksTombstoneUntilSync() = runBlocking {
        useCase.save(id = "t1", title = "買菜", description = "", reminderType = ReminderType.NONE, reminderAt = null, intervalDays = null)
        clock.now = 4_000
        useCase.delete("t1")

        val task = repository.tasks.getValue("t1")
        assertEquals(4_000L, task.deletedAt)
        assertEquals(SyncStatus.PENDING, task.syncStatus)
        assertNull(useCase.getTask("t1"))
    }

    private class RecordingScheduler : ReminderScheduler {
        val syncedIds = mutableListOf<String>()

        override suspend fun sync(task: Task) {
            syncedIds += task.id
        }

        override fun restoreOnAppStart() = Unit

        override suspend fun restoreAll() = Unit
    }

    private class MutableClock(var now: Long) : AppClock {
        override fun nowMillis(): Long = now

        override fun today(zone: ZoneId): LocalDate = LocalDate.of(2026, 10, 15)
    }

    private class FakeTaskRepository : TaskRepository {
        val tasks = linkedMapOf<String, Task>()

        override fun observeActiveTasks(): Flow<List<Task>> =
            flowOf(tasks.values.filter { it.deletedAt == null })

        override suspend fun getTask(id: String): Task? = tasks[id]?.takeIf { it.deletedAt == null }

        override suspend fun activeTasks(): List<Task> = tasks.values.filter { it.deletedAt == null }

        override suspend fun upsert(task: Task) {
            tasks[task.id] = task
        }
    }
}
