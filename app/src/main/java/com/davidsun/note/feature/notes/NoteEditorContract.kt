package com.davidsun.note.feature.notes

enum class NoteSaveStatus {
    Idle,
    Saving,
    Saved,
    Error,
}

data class NoteEditorUiState(
    val isLoading: Boolean = true,
    val openedAsNew: Boolean = true,
    val missing: Boolean = false,
    val title: String = "",
    val content: String = "",
    val saveStatus: NoteSaveStatus = NoteSaveStatus.Idle,
    val isImportant: Boolean = false,
)

data class NoteEditorActions(
    val onTitleChange: (String) -> Unit,
    val onContentChange: (String) -> Unit,
    val onBack: () -> Unit,
    val onToggleImportant: () -> Unit,
)
