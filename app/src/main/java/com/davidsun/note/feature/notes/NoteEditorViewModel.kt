package com.davidsun.note.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.usecase.NoteUseCase
import com.davidsun.note.domain.usecase.SaveNoteResult
import com.davidsun.note.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteUseCase: NoteUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val routeId: String? = savedStateHandle.get<String>(NOTE_ID)?.takeUnless { it == AppRoute.NewId }
    private val noteId: String = routeId
        ?: savedStateHandle.get<String>(DRAFT_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[DRAFT_ID] = it }

    private var lastSavedTitle: String? = savedTitle()
    private var lastSavedContent: String? = savedContent()
    private var hasPersisted: Boolean = savedStateHandle.get<Boolean>(HAS_PERSISTED) == true
    private var closing = false
    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    private val initialTitle = savedStateHandle.get<String>(TITLE).orEmpty()
    private val initialContent = savedStateHandle.get<String>(CONTENT).orEmpty()
    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            isLoading = routeId != null && savedStateHandle.get<Boolean>(READY) != true,
            openedAsNew = routeId == null,
            title = initialTitle,
            content = initialContent,
            saveStatus = statusFor(initialTitle, initialContent),
        ),
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        when {
            savedStateHandle.get<Boolean>(READY) == true -> {
                if (_uiState.value.saveStatus == NoteSaveStatus.Saving) scheduleSave()
            }
            routeId == null -> savedStateHandle[READY] = true
            else -> viewModelScope.launch { load(routeId) }
        }
    }

    fun onTitleChange(value: String) {
        savedStateHandle[TITLE] = value
        val nextStatus = statusFor(value, _uiState.value.content)
        _uiState.update { it.copy(title = value, saveStatus = nextStatus) }
        onEdited(nextStatus)
    }

    fun onContentChange(value: String) {
        savedStateHandle[CONTENT] = value
        val nextStatus = statusFor(_uiState.value.title, value)
        _uiState.update { it.copy(content = value, saveStatus = nextStatus) }
        onEdited(nextStatus)
    }

    suspend fun onLeave(): Boolean {
        if (closing) return false
        closing = true
        saveJob?.cancel()
        val saved = persist()
        if (!saved) closing = false
        return saved
    }

    private fun onEdited(status: NoteSaveStatus) {
        if (status == NoteSaveStatus.Saving) {
            scheduleSave()
        } else {
            saveJob?.cancel()
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private suspend fun load(id: String) {
        val existing = noteUseCase.getNote(id)
        if (existing == null) {
            _uiState.update { it.copy(isLoading = false, missing = true) }
        } else {
            applyExisting(existing)
        }
        savedStateHandle[READY] = true
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun applyExisting(note: Note) {
        lastSavedTitle = note.title
        lastSavedContent = note.content
        hasPersisted = true
        savedStateHandle[TITLE] = note.title
        savedStateHandle[CONTENT] = note.content
        savedStateHandle[SAVED_TITLE] = note.title
        savedStateHandle[SAVED_CONTENT] = note.content
        savedStateHandle[HAS_PERSISTED] = true
        _uiState.update {
            it.copy(
                isLoading = false,
                missing = false,
                title = note.title,
                content = note.content,
                saveStatus = NoteSaveStatus.Saved,
            )
        }
    }

    private suspend fun persist(): Boolean = saveMutex.withLock {
        val state = _uiState.value
        if (state.isLoading || state.missing) return@withLock true
        val result = try {
            withContext(NonCancellable) {
                noteUseCase.save(noteId, state.title, state.content)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            _uiState.update { it.copy(saveStatus = NoteSaveStatus.Error) }
            return@withLock false
        }
        when (result) {
            SaveNoteResult.IgnoredEmpty -> {
                _uiState.update { it.copy(saveStatus = NoteSaveStatus.Idle) }
                true
            }
            is SaveNoteResult.Saved -> {
                lastSavedTitle = result.title
                lastSavedContent = result.content
                hasPersisted = true
                savedStateHandle[SAVED_TITLE] = result.title
                savedStateHandle[SAVED_CONTENT] = result.content
                savedStateHandle[HAS_PERSISTED] = true
                _uiState.update { it.copy(saveStatus = NoteSaveStatus.Saved) }
                true
            }
        }
    }

    private fun statusFor(title: String, content: String): NoteSaveStatus {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (hasPersisted && trimmedTitle == lastSavedTitle && trimmedContent == lastSavedContent) {
            return NoteSaveStatus.Saved
        }
        if (!hasPersisted && trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
            return NoteSaveStatus.Idle
        }
        return NoteSaveStatus.Saving
    }

    private fun savedTitle(): String? = if (savedStateHandle.contains(SAVED_TITLE)) {
        savedStateHandle.get<String>(SAVED_TITLE).orEmpty()
    } else {
        null
    }

    private fun savedContent(): String? = if (savedStateHandle.contains(SAVED_CONTENT)) {
        savedStateHandle.get<String>(SAVED_CONTENT).orEmpty()
    } else {
        null
    }

    private companion object {
        const val NOTE_ID = "noteId"
        const val DRAFT_ID = "draftNoteId"
        const val READY = "noteEditorReady"
        const val TITLE = "noteTitle"
        const val CONTENT = "noteContent"
        const val SAVED_TITLE = "noteSavedTitle"
        const val SAVED_CONTENT = "noteSavedContent"
        const val HAS_PERSISTED = "noteHasPersisted"
        const val SAVE_DEBOUNCE_MS = 500L
    }
}
