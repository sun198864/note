package com.davidsun.note.feature.tasks

import com.davidsun.note.ui.PendingDelete
import com.davidsun.note.ui.ScreenStatus

data class HiddenTasksUiState(
    val status: ScreenStatus = ScreenStatus.Loading,
    val tasks: List<TaskItemUi> = emptyList(),
    val importantOnly: Boolean = false,
    val pendingDelete: PendingDelete? = null,
    val actionFailed: Boolean = false,
)

data class HiddenTasksActions(
    val onBack: () -> Unit,
    val onTaskClick: (String) -> Unit,
    val onToggleComplete: (String, Boolean) -> Unit,
    val onRestore: (String) -> Unit,
    val onDeleteRequest: (String, String) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismissDelete: () -> Unit,
    val onToggleImportant: (String, Boolean) -> Unit,
    val onImportantOnlyChange: (Boolean) -> Unit,
    val onReorder: (List<String>) -> Unit,
)
