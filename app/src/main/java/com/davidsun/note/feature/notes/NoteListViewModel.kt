package com.davidsun.note.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppClock
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.ListOrder
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.usecase.NoteUseCase
import com.davidsun.note.ui.PendingDelete
import com.davidsun.note.ui.ScreenStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteUseCase: NoteUseCase,
    private val dateFormatter: AppDateFormatter,
    private val clock: AppClock,
) : ViewModel() {
    private val pendingDelete = MutableStateFlow<PendingDelete?>(null)
    private val actionFailed = MutableStateFlow(false)
    private val importantOnly = MutableStateFlow(false)
    private var latestNotes: List<Note> = emptyList()
    private val snapshots = noteUseCase.observeNotes()
        .map { notes -> NoteSnapshot(notes = notes, loaded = true) }
        .catch { emit(NoteSnapshot(loaded = true, failed = true)) }

    val uiState: StateFlow<NoteListUiState> = combine(
        snapshots,
        pendingDelete,
        actionFailed,
        importantOnly,
    ) { snapshot, pending, failed, onlyImportant ->
        when {
            !snapshot.loaded -> NoteListUiState(importantOnly = onlyImportant)
            snapshot.failed || snapshot.notes == null -> NoteListUiState(
                status = ScreenStatus.Error,
                importantOnly = onlyImportant,
                actionFailed = failed,
            )
            else -> {
                val sorted = ListOrder.notes(snapshot.notes)
                latestNotes = sorted
                val visible = if (onlyImportant) sorted.filter { it.isImportant } else sorted
                if (visible.isEmpty()) {
                    NoteListUiState(
                        status = ScreenStatus.Empty,
                        importantOnly = onlyImportant,
                        pendingDelete = pending,
                        actionFailed = failed,
                    )
                } else {
                    NoteListUiState(
                        status = ScreenStatus.Content,
                        notes = visible.map { note -> note.toItem() },
                        importantOnly = onlyImportant,
                        pendingDelete = pending,
                        actionFailed = failed,
                    )
                }
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NoteListUiState(),
    )

    fun setImportant(id: String, important: Boolean) {
        launchUpdate { noteUseCase.setImportant(id, important) }
    }

    fun onImportantOnlyChange(enabled: Boolean) {
        importantOnly.value = enabled
    }

    fun reorder(visibleIds: List<String>) {
        val merged = ListOrder.merge(latestNotes.map { it.id }, visibleIds)
        launchUpdate { noteUseCase.reorder(merged) }
    }

    fun onDeleteRequest(id: String, title: String) {
        pendingDelete.value = PendingDelete(id = id, title = title)
    }

    fun onDismissDelete() {
        pendingDelete.value = null
    }

    fun onConfirmDelete() {
        val pending = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch {
            try {
                noteUseCase.delete(pending.id)
                actionFailed.value = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                actionFailed.value = true
            }
        }
    }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                actionFailed.value = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                actionFailed.value = true
            }
        }
    }

    private fun Note.toItem(): NoteItemUi = NoteItemUi(
        id = id,
        title = title,
        summary = summarize(content),
        editedLabel = dateFormatter.formatEditedAt(updatedAt, clock.nowMillis()),
        isImportant = isImportant,
    )

    private fun summarize(content: String): String {
        val singleLine = content.replace(WHITESPACE, " ").trim()
        return when {
            singleLine.isEmpty() -> ""
            singleLine.length <= SUMMARY_LENGTH -> singleLine
            else -> singleLine.take(SUMMARY_LENGTH) + "…"
        }
    }

    private data class NoteSnapshot(
        val notes: List<Note>? = null,
        val loaded: Boolean = false,
        val failed: Boolean = false,
    )

    private companion object {
        const val SUMMARY_LENGTH = 80
        val WHITESPACE = Regex("\\s+")
    }
}
