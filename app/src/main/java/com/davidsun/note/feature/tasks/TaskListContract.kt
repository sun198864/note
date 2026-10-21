package com.davidsun.note.feature.tasks

import com.davidsun.note.domain.ListOrder
import com.davidsun.note.ui.PendingDelete
import com.davidsun.note.ui.ScreenStatus

enum class TaskSection {
    Today,
    Upcoming,
    Undated,
    Completed,
}

data class TaskSectionUi(
    val section: TaskSection,
    val tasks: List<TaskItemUi>,
)

data class TaskListUiState(
    val status: ScreenStatus = ScreenStatus.Loading,
    val sections: List<TaskSectionUi> = emptyList(),
    val hiddenCount: Int = 0,
    val importantOnly: Boolean = false,
    val pendingDelete: PendingDelete? = null,
    val actionFailed: Boolean = false,
)

data class TaskListActions(
    val onTaskClick: (String) -> Unit,
    val onAddClick: () -> Unit,
    val onToggleComplete: (String, Boolean) -> Unit,
    val onHide: (String) -> Unit,
    val onDeleteRequest: (String, String) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismissDelete: () -> Unit,
    val onOpenHidden: () -> Unit,
    val onToggleImportant: (String, Boolean) -> Unit,
    val onImportantOnlyChange: (Boolean) -> Unit,
    val onReorder: (TaskSection, List<String>) -> Unit,
)

fun taskScrollIndex(sections: List<TaskSectionUi>, taskId: String): Int? {
    var index = 1
    sections.forEach { section ->
        index += 1
        val offset = section.tasks.indexOfFirst { task -> task.id == taskId }
        if (offset >= 0) return index + offset
        index += section.tasks.size
    }
    return null
}

fun List<TaskSectionUi>.moveTask(fromId: String, toId: String): List<TaskSectionUi>? {
    val owner = firstOrNull { section -> section.tasks.any { it.id == fromId } } ?: return null
    if (owner.tasks.none { it.id == toId }) return null
    val moved = ListOrder.move(owner.tasks, fromId, toId) { it.id } ?: return null
    return map { section ->
        if (section.section == owner.section) section.copy(tasks = moved) else section
    }
}
