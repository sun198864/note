package com.davidsun.note.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.ListOrder
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
    private val importantOnly = MutableStateFlow(false)
    private var latestHidden: List<Task> = emptyList()
    private val snapshots = taskUseCase.observeTasks()
        .map { tasks -> HiddenSnapshot(tasks = classifier.group(tasks).hidden, loaded = true) }
        .catch { emit(HiddenSnapshot(loaded = true, failed = true)) }

    val uiState: StateFlow<HiddenTasksUiState> = combine(
        snapshots,
        pendingDelete,
        actionFailed,
        importantOnly,
    ) { snapshot, pending, failed, onlyImportant ->
        when {
            !snapshot.loaded -> HiddenTasksUiState(importantOnly = onlyImportant)
            snapshot.failed || snapshot.tasks == null -> HiddenTasksUiState(
                status = ScreenStatus.Error,
                importantOnly = onlyImportant,
                actionFailed = failed,
            )
            else -> {
                latestHidden = snapshot.tasks
                val visible = if (onlyImportant) snapshot.tasks.filter { it.isImportant } else snapshot.tasks
                HiddenTasksUiState(
                    status = if (visible.isEmpty()) ScreenStatus.Empty else ScreenStatus.Content,
                    tasks = visible.map { task -> task.toItem() },
                    importantOnly = onlyImportant,
                    pendingDelete = pending,
                    actionFailed = failed,
                )
            }
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

    fun setImportant(id: String, important: Boolean) {
        launchUpdate { taskUseCase.setImportant(id, important) }
    }

    fun onImportantOnlyChange(enabled: Boolean) {
        importantOnly.value = enabled
    }

    fun reorder(visibleIds: List<String>) {
        val merged = ListOrder.merge(latestHidden.map { it.id }, visibleIds)
        launchUpdate { taskUseCase.reorder(merged) }
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
