package com.davidsun.note.feature.notes

import com.davidsun.note.ui.PendingDelete
import com.davidsun.note.ui.ScreenStatus

data class NoteItemUi(
    val id: String,
    val title: String,
    val summary: String,
    val editedLabel: String,
)

data class NoteListUiState(
    val status: ScreenStatus = ScreenStatus.Loading,
    val notes: List<NoteItemUi> = emptyList(),
    val pendingDelete: PendingDelete? = null,
    val actionFailed: Boolean = false,
)

data class NoteListActions(
    val onNoteClick: (String) -> Unit,
    val onAddClick: () -> Unit,
    val onDeleteRequest: (String, String) -> Unit,
    val onConfirmDelete: () -> Unit,
    val onDismissDelete: () -> Unit,
)
