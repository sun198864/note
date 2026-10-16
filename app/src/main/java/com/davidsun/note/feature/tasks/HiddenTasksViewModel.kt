package com.davidsun.note.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.usecase.TaskClassifier
import com.davidsun.note.domain.usecase.TaskUseCase
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
class HiddenTasksViewModel @Inject constructor(
    private val taskUseCase: TaskUseCase,
    private val classifier: TaskClassifier,
    private val dateFormatter: AppDateFormatter,
) : ViewModel() {
    private val pendingDelete = MutableStateFlow<PendingDelete?>(null)
    private val actionFailed = MutableStateFlow(false)
    private val snapshots = taskUseCase.observeTasks()
        .map { tasks -> HiddenSnapshot(tasks = classifier.group(tasks).hidden, loaded = true) }
        .catch { emit(HiddenSnapshot(loaded = true, failed = true)) }

    val uiState: StateFlow<HiddenTasksUiState> = combine(
        snapshots,
        pendingDelete,
        actionFailed,
    ) { snapshot, pending, failed ->
        when {
            !snapshot.loaded -> HiddenTasksUiState()
            snapshot.failed || snapshot.tasks == null -> HiddenTasksUiState(
                status = ScreenStatus.Error,
                actionFailed = failed,
            )
            else -> HiddenTasksUiState(
                status = if (snapshot.tasks.isEmpty()) ScreenStatus.Empty else ScreenStatus.Content,
                tasks = snapshot.tasks.map { task -> task.toItem() },
                pendingDelete = pending,
                actionFailed = failed,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HiddenTasksUiState(),
    )

    fun setCompleted(id: String, completed: Boolean) {
        launchUpdate { taskUseCase.setCompleted(id, completed) }
    }

    fun restore(id: String) {
        launchUpdate { taskUseCase.setHidden(id, false) }
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
        launchUpdate { taskUseCase.delete(pending.id) }
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

    private fun Task.toItem(): TaskItemUi = toTaskItemUi(dateFormatter, isOverdue = false)

    private data class HiddenSnapshot(
        val tasks: List<Task>? = null,
        val loaded: Boolean = false,
        val failed: Boolean = false,
    )
}
