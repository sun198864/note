package com.davidsun.note.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.ListOrder
import com.davidsun.note.domain.model.GroupedTasks
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
class TaskListViewModel @Inject constructor(
    private val taskUseCase: TaskUseCase,
    private val classifier: TaskClassifier,
    private val dateFormatter: AppDateFormatter,
) : ViewModel() {
    private val pendingDelete = MutableStateFlow<PendingDelete?>(null)
    private val actionFailed = MutableStateFlow(false)
    private val importantOnly = MutableStateFlow(false)
    private var latestGroups: GroupedTasks? = null
    private val snapshots = taskUseCase.observeTasks()
        .map { tasks -> TaskSnapshot(grouped = classifier.group(tasks), loaded = true) }
        .catch { emit(TaskSnapshot(loaded = true, failed = true)) }

    val uiState: StateFlow<TaskListUiState> = combine(
        snapshots,
        pendingDelete,
        actionFailed,
        importantOnly,
    ) { snapshot, pending, failed, onlyImportant ->
        mapState(snapshot, pending, failed, onlyImportant)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TaskListUiState(),
    )

    fun setCompleted(id: String, completed: Boolean) {
        launchUpdate { taskUseCase.setCompleted(id, completed) }
    }

    fun hide(id: String) {
        launchUpdate { taskUseCase.setHidden(id, true) }
    }

    fun setImportant(id: String, important: Boolean) {
        launchUpdate { taskUseCase.setImportant(id, important) }
    }

    fun onImportantOnlyChange(enabled: Boolean) {
        importantOnly.value = enabled
    }

    fun reorder(section: TaskSection, visibleIds: List<String>) {
        val grouped = latestGroups ?: return
        val allIds = grouped.sectionTasks(section).map { it.id }
        val merged = ListOrder.merge(allIds, visibleIds)
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

    private fun mapState(
        snapshot: TaskSnapshot,
        pending: PendingDelete?,
        failed: Boolean,
        onlyImportant: Boolean,
    ): TaskListUiState {
        if (!snapshot.loaded) return TaskListUiState(status = ScreenStatus.Loading, importantOnly = onlyImportant)
        val grouped = snapshot.grouped
        if (snapshot.failed || grouped == null) {
            return TaskListUiState(status = ScreenStatus.Error, actionFailed = failed, importantOnly = onlyImportant)
        }
        latestGroups = grouped
        val sections = listOf(
            TaskSection.Today to grouped.today,
            TaskSection.Upcoming to grouped.upcoming,
            TaskSection.Undated to grouped.undated,
            TaskSection.Completed to grouped.completed,
        ).mapNotNull { (section, tasks) ->
            val visible = if (onlyImportant) tasks.filter { it.isImportant } else tasks
            if (visible.isEmpty()) {
                null
            } else {
                TaskSectionUi(
                    section = section,
                    tasks = visible.map { task -> task.toItem() },
                )
            }
        }
        return TaskListUiState(
            status = if (sections.isEmpty()) ScreenStatus.Empty else ScreenStatus.Content,
            sections = sections,
            hiddenCount = grouped.hidden.size,
            importantOnly = onlyImportant,
            pendingDelete = pending,
            actionFailed = failed,
        )
    }

    private fun Task.toItem(): TaskItemUi = toTaskItemUi(dateFormatter, classifier.isOverdue(this))

    private fun GroupedTasks.sectionTasks(section: TaskSection): List<Task> = when (section) {
        TaskSection.Today -> today
        TaskSection.Upcoming -> upcoming
        TaskSection.Undated -> undated
        TaskSection.Completed -> completed
    }

    private data class TaskSnapshot(
        val grouped: GroupedTasks? = null,
        val loaded: Boolean = false,
        val failed: Boolean = false,
    )
}
