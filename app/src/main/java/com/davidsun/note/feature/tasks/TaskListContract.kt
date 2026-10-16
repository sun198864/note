package com.davidsun.note.feature.tasks

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
)
