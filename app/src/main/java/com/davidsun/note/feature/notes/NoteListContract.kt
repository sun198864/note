package com.davidsun.note.feature.notes

import com.davidsun.note.ui.PendingDelete
import com.davidsun.note.ui.ScreenStatus

data class NoteItemUi(
    val id: String,
    val title: String,
    val summary: String,
    val editedLabel: String,
    val isImportant: Boolean = false,
)

data class NoteListUiState(
    val status: ScreenStatus = ScreenStatus.Loading,
    val notes: List<NoteItemUi> = emptyList(),
    val importantOnly: Boolean = false,
    val pendingDelete: PendingDelete? = null,
    val actionFailed: Boolean = false,
)

data class NoteListActions(
    val onNoteClick: (String) -> Unit,
    val onAddClick: () -> Unit,
    val onDeleteRequest: (String, String) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismissDelete: () -> Unit,
    val onToggleImportant: (String, Boolean) -> Unit,
    val onImportantOnlyChange: (Boolean) -> Unit,
    val onReorder: (List<String>) -> Unit,
)

fun noteScrollIndex(notes: List<NoteItemUi>, noteId: String, hasErrorBanner: Boolean): Int? {
    val offset = notes.indexOfFirst { note -> note.id == noteId }
    if (offset < 0) return null
    return offset + if (hasErrorBanner) 1 else 0
}
