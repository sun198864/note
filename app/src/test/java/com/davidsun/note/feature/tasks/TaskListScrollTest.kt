package com.davidsun.note.feature.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskListScrollTest {
    @Test
    fun scrollIndexSkipsTheHiddenRowAndSectionHeaders() {
        val sections = listOf(
            TaskSectionUi(TaskSection.Today, listOf(task("a"), task("b"))),
            TaskSectionUi(TaskSection.Undated, listOf(task("c"))),
        )

        assertEquals(2, taskScrollIndex(sections, "a"))
        assertEquals(3, taskScrollIndex(sections, "b"))
        assertEquals(5, taskScrollIndex(sections, "c"))
        assertNull(taskScrollIndex(sections, "missing"))
    }

    private fun task(id: String) = TaskItemUi(
        id = id,
        title = id,
        description = null,
        reminderLabel = null,
        isOverdue = false,
        isCompleted = false,
    )
}
