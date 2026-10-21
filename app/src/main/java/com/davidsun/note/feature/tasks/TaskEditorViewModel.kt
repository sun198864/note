package com.davidsun.note.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppClock
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.core.common.combineDateAndTime
import com.davidsun.note.core.common.firstIntervalTriggerAt
import com.davidsun.note.core.common.firstWeeklyTriggerAt
import com.davidsun.note.core.common.isValidIntervalDays
import com.davidsun.note.core.common.isValidWeekdays
import com.davidsun.note.core.common.reminderAtToDatePickerMillis
import com.davidsun.note.core.common.toggleWeekday
import com.davidsun.note.domain.model.ReminderType
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.usecase.SaveTaskResult
import com.davidsun.note.domain.usecase.TaskUseCase
import com.davidsun.note.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val taskUseCase: TaskUseCase,
    private val dateFormatter: AppDateFormatter,
    private val clock: AppClock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val routeId: String? = savedStateHandle.get<String>(TASK_ID)?.takeUnless { it == AppRoute.NewId }
    private val taskId: String = routeId
        ?: savedStateHandle.get<String>(DRAFT_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[DRAFT_ID] = it }

    private var reminderType: ReminderType = savedType(TYPE_KEY)
    private var reminderAt: Long? = savedStateHandle.get<Long>(AT_KEY)
    private var intervalDays: Int? = savedStateHandle.get<Int>(DAYS_KEY)
    private var weekdays: Int? = savedStateHandle.get<Int>(WEEKDAYS_KEY)
    private var baselineTitle: String = savedStateHandle.get<String>(BASE_TITLE).orEmpty()
    private var baselineDescription: String = savedStateHandle.get<String>(BASE_DESCRIPTION).orEmpty()
    private var baselineType: ReminderType = savedType(BASE_TYPE)
    private var baselineAt: Long? = if (savedStateHandle.contains(BASE_AT)) {
        savedStateHandle.get<Long>(BASE_AT)
    } else {
        null
    }
    private var baselineDays: Int? = if (savedStateHandle.contains(BASE_DAYS)) {
        savedStateHandle.get<Int>(BASE_DAYS)
    } else {
        null
    }
    private var baselineWeekdays: Int? = if (savedStateHandle.contains(BASE_WEEKDAYS)) {
        savedStateHandle.get<Int>(BASE_WEEKDAYS)
    } else {
        null
    }
    private var closing = false

    private val closeChannel = Channel<Unit>(Channel.CONFLATED)
    val closeEvents: Flow<Unit> = closeChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    init {
        when {
            savedStateHandle.get<Boolean>(READY) == true -> Unit
            routeId == null -> savedStateHandle[READY] = true
            else -> viewModelScope.launch { load(routeId) }
        }
    }

    fun onTitleChange(value: String) {
        savedStateHandle[TITLE] = value
        _uiState.update { it.copy(title = value, titleError = false, saveFailed = false) }
    }

    fun onDescriptionChange(value: String) {
        savedStateHandle[DESCRIPTION] = value
        _uiState.update { it.copy(description = value, saveFailed = false) }
    }

    fun onOpenReminder() {
        _uiState.update { it.copy(showReminderOptions = true, reminderPastError = false) }
    }

    fun onDismissReminderOptions() {
        _uiState.update { it.copy(showReminderOptions = false) }
    }

    fun onChooseOnce() {
        val (hour, minute) = if (reminderType == ReminderType.ONCE) {
            hourMinute(reminderAt)
        } else {
            DEFAULT_REMINDER_HOUR to 0
        }
        _uiState.update {
            it.copy(
                showReminderOptions = false,
                showDatePicker = true,
                timePickerHour = hour,
                timePickerMinute = minute,
            )
        }
    }

    fun onChooseInterval() {
        val (hour, minute) = if (reminderType == ReminderType.INTERVAL) {
            hourMinute(reminderAt)
        } else {
            DEFAULT_REMINDER_HOUR to 0
        }
        val days = intervalDays?.takeIf { reminderType == ReminderType.INTERVAL }?.toString() ?: "1"
        _uiState.update {
            it.copy(
                showReminderOptions = false,
                showIntervalDialog = true,
                intervalDaysInput = days,
                intervalDaysError = false,
                timePickerHour = hour,
                timePickerMinute = minute,
            )
        }
    }

    fun onChooseWeekly() {
        val (hour, minute) = if (reminderType == ReminderType.WEEKLY) {
            hourMinute(reminderAt)
        } else {
            DEFAULT_REMINDER_HOUR to 0
        }
        val mask = weekdays?.takeIf { reminderType == ReminderType.WEEKLY } ?: 0
        _uiState.update {
            it.copy(
                showReminderOptions = false,
                showWeeklyDialog = true,
                weekdaysMask = mask,
                weekdaysError = false,
                timePickerHour = hour,
                timePickerMinute = minute,
            )
        }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun onDatePicked(pickerMillis: Long) {
        _uiState.update {
            it.copy(
                showDatePicker = false,
                showTimePicker = true,
                timePickerTarget = ReminderTimeTarget.ONCE,
                pendingDateMillis = pickerMillis,
            )
        }
    }

    fun onDismissTimePicker() {
        _uiState.update { state ->
            when (state.timePickerTarget) {
                ReminderTimeTarget.INTERVAL -> state.copy(showTimePicker = false, showIntervalDialog = true)
                ReminderTimeTarget.WEEKLY -> state.copy(showTimePicker = false, showWeeklyDialog = true)
                ReminderTimeTarget.ONCE -> state.copy(showTimePicker = false)
            }
        }
    }

    fun onTimePicked(hour: Int, minute: Int) {
        val state = _uiState.value
        if (state.timePickerTarget == ReminderTimeTarget.INTERVAL || state.timePickerTarget == ReminderTimeTarget.WEEKLY) {
            _uiState.update {
                it.copy(
                    showTimePicker = false,
                    showIntervalDialog = state.timePickerTarget == ReminderTimeTarget.INTERVAL,
                    showWeeklyDialog = state.timePickerTarget == ReminderTimeTarget.WEEKLY,
                    timePickerHour = hour,
                    timePickerMinute = minute,
                )
            }
            return
        }
        val dateMillis = state.pendingDateMillis ?: return
        val triggerAt = combineDateAndTime(dateMillis, hour, minute)
        if (triggerAt <= clock.nowMillis()) {
            _uiState.update { it.copy(showTimePicker = false, reminderPastError = true) }
            return
        }
        reminderType = ReminderType.ONCE
        reminderAt = triggerAt
        intervalDays = null
        weekdays = null
        persistReminder()
        _uiState.update {
            it.copy(
                showTimePicker = false,
                reminderPastError = false,
                hasReminder = true,
                reminderLabel = currentLabel(),
                datePickerInitialMillis = dateMillis,
                timePickerHour = hour,
                timePickerMinute = minute,
            )
        }
    }

    fun onIntervalDaysChange(value: String) {
        val digits = value.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS)
        _uiState.update { it.copy(intervalDaysInput = digits, intervalDaysError = false) }
    }

    fun onOpenIntervalTime() {
        _uiState.update {
            it.copy(
                showIntervalDialog = false,
                showTimePicker = true,
                timePickerTarget = ReminderTimeTarget.INTERVAL,
            )
        }
    }

    fun onConfirmInterval() {
        val days = _uiState.value.intervalDaysInput.toIntOrNull()
        if (days == null || !isValidIntervalDays(days)) {
            _uiState.update { it.copy(intervalDaysError = true) }
            return
        }
        val state = _uiState.value
        reminderType = ReminderType.INTERVAL
        intervalDays = days
        weekdays = null
        reminderAt = firstIntervalTriggerAt(
            nowMillis = clock.nowMillis(),
            intervalDays = days,
            hour = state.timePickerHour,
            minute = state.timePickerMinute,
        )
        persistReminder()
        _uiState.update {
            it.copy(
                showIntervalDialog = false,
                intervalDaysError = false,
                reminderPastError = false,
                hasReminder = true,
                reminderLabel = currentLabel(),
                datePickerInitialMillis = reminderAt?.let(::reminderAtToDatePickerMillis),
            )
        }
    }

    fun onDismissInterval() {
        _uiState.update { it.copy(showIntervalDialog = false, intervalDaysError = false) }
    }

    fun onToggleWeekday(isoDay: Int) {
        _uiState.update {
            it.copy(weekdaysMask = toggleWeekday(it.weekdaysMask, isoDay), weekdaysError = false)
        }
    }

    fun onOpenWeeklyTime() {
        _uiState.update {
            it.copy(
                showWeeklyDialog = false,
                showTimePicker = true,
                timePickerTarget = ReminderTimeTarget.WEEKLY,
            )
        }
    }

    fun onConfirmWeekly() {
        val mask = _uiState.value.weekdaysMask
        if (!isValidWeekdays(mask)) {
            _uiState.update { it.copy(weekdaysError = true) }
            return
        }
        val state = _uiState.value
        reminderType = ReminderType.WEEKLY
        weekdays = mask
        intervalDays = null
        reminderAt = firstWeeklyTriggerAt(
            nowMillis = clock.nowMillis(),
            weekdays = mask,
            hour = state.timePickerHour,
            minute = state.timePickerMinute,
        )
        persistReminder()
        _uiState.update {
            it.copy(
                showWeeklyDialog = false,
                weekdaysError = false,
                reminderPastError = false,
                hasReminder = true,
                reminderLabel = currentLabel(),
                datePickerInitialMillis = reminderAt?.let(::reminderAtToDatePickerMillis),
            )
        }
    }

    fun onDismissWeekly() {
        _uiState.update { it.copy(showWeeklyDialog = false, weekdaysError = false) }
    }

    fun onClearReminder() {
        reminderType = ReminderType.NONE
        reminderAt = null
        intervalDays = null
        weekdays = null
        persistReminder()
        _uiState.update {
            it.copy(
                hasReminder = false,
                reminderLabel = null,
                reminderPastError = false,
                datePickerInitialMillis = null,
            )
        }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading || current.missing || closing) return
        _uiState.update { it.copy(isSaving = true, titleError = false, saveFailed = false) }
        viewModelScope.launch {
            try {
                when (
                    taskUseCase.save(
                        id = taskId,
                        title = _uiState.value.title,
                        description = _uiState.value.description,
                        reminderType = reminderType,
                        reminderAt = reminderAt,
                        intervalDays = intervalDays,
                        weekdays = weekdays,
                    )
                ) {
                    SaveTaskResult.InvalidTitle -> _uiState.update {
                        it.copy(isSaving = false, titleError = true)
                    }
                    SaveTaskResult.InvalidReminder -> _uiState.update {
                        it.copy(isSaving = false, saveFailed = true)
                    }
                    SaveTaskResult.Saved -> {
                        closing = true
                        closeChannel.send(Unit)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    fun onBack() {
        val current = _uiState.value
        if (closing || current.isSaving) return
        if (current.showDiscardDialog) {
            _uiState.update { it.copy(showDiscardDialog = false) }
            return
        }
        if (isDirty()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            closing = true
            closeChannel.trySend(Unit)
        }
    }

    fun onConfirmDiscard() {
        if (closing) return
        closing = true
        _uiState.update { it.copy(showDiscardDialog = false) }
        closeChannel.trySend(Unit)
    }

    fun onDismissDiscard() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    private suspend fun load(id: String) {
        val existing = taskUseCase.getTask(id)
        if (existing == null) {
            _uiState.update { it.copy(isLoading = false, missing = true) }
        } else {
            applyExisting(existing)
        }
        savedStateHandle[READY] = true
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun applyExisting(task: Task) {
        reminderType = task.reminderType
        reminderAt = task.reminderAt
        intervalDays = task.intervalDays
        weekdays = task.weekdays
        baselineTitle = task.title
        baselineDescription = task.description.orEmpty()
        baselineType = task.reminderType
        baselineAt = task.reminderAt
        baselineDays = task.intervalDays
        baselineWeekdays = task.weekdays
        savedStateHandle[TITLE] = task.title
        savedStateHandle[DESCRIPTION] = task.description.orEmpty()
        savedStateHandle[BASE_TITLE] = task.title
        savedStateHandle[BASE_DESCRIPTION] = task.description.orEmpty()
        persistReminder()
        persistBaseline()
        val (hour, minute) = hourMinute(task.reminderAt)
        _uiState.update {
            it.copy(
                isLoading = false,
                missing = false,
                title = task.title,
                description = task.description.orEmpty(),
                hasReminder = task.reminderType != ReminderType.NONE,
                reminderLabel = dateFormatter.formatTaskReminder(
                    task.reminderType,
                    task.reminderAt,
                    task.intervalDays,
                    task.weekdays,
                ),
                datePickerInitialMillis = task.reminderAt?.let(::reminderAtToDatePickerMillis),
                timePickerHour = hour,
                timePickerMinute = minute,
                intervalDaysInput = task.intervalDays?.toString() ?: "1",
                weekdaysMask = task.weekdays ?: 0,
            )
        }
    }

    private fun isDirty(): Boolean {
        val state = _uiState.value
        return state.title != baselineTitle ||
            state.description != baselineDescription ||
            reminderType != baselineType ||
            reminderAt != baselineAt ||
            intervalDays != baselineDays ||
            weekdays != baselineWeekdays
    }

    private fun initialState(): TaskEditorUiState {
        val ready = savedStateHandle.get<Boolean>(READY) == true
        val (hour, minute) = hourMinute(reminderAt)
        return TaskEditorUiState(
            isLoading = routeId != null && !ready,
            isNew = routeId == null,
            title = savedStateHandle.get<String>(TITLE).orEmpty(),
            description = savedStateHandle.get<String>(DESCRIPTION).orEmpty(),
            reminderLabel = currentLabel(),
            hasReminder = reminderType != ReminderType.NONE,
            datePickerInitialMillis = reminderAt?.let(::reminderAtToDatePickerMillis),
            timePickerHour = hour,
            timePickerMinute = minute,
            intervalDaysInput = intervalDays?.toString() ?: "1",
            weekdaysMask = weekdays ?: 0,
        )
    }

    private fun currentLabel(): String? =
        dateFormatter.formatTaskReminder(reminderType, reminderAt, intervalDays, weekdays)

    private fun persistReminder() {
        savedStateHandle[TYPE_KEY] = reminderType.name
        if (reminderAt != null) {
            savedStateHandle[AT_KEY] = reminderAt
        } else {
            savedStateHandle.remove<Long>(AT_KEY)
        }
        if (intervalDays != null) {
            savedStateHandle[DAYS_KEY] = intervalDays
        } else {
            savedStateHandle.remove<Int>(DAYS_KEY)
        }
        if (weekdays != null) {
            savedStateHandle[WEEKDAYS_KEY] = weekdays
        } else {
            savedStateHandle.remove<Int>(WEEKDAYS_KEY)
        }
    }

    private fun persistBaseline() {
        savedStateHandle[BASE_TYPE] = baselineType.name
        if (baselineAt != null) {
            savedStateHandle[BASE_AT] = baselineAt
        } else {
            savedStateHandle.remove<Long>(BASE_AT)
        }
        if (baselineDays != null) {
            savedStateHandle[BASE_DAYS] = baselineDays
        } else {
            savedStateHandle.remove<Int>(BASE_DAYS)
        }
        if (baselineWeekdays != null) {
            savedStateHandle[BASE_WEEKDAYS] = baselineWeekdays
        } else {
            savedStateHandle.remove<Int>(BASE_WEEKDAYS)
        }
    }

    private fun savedType(key: String): ReminderType {
        val raw = savedStateHandle.get<String>(key) ?: return ReminderType.NONE
        return runCatching { ReminderType.valueOf(raw) }.getOrDefault(ReminderType.NONE)
    }

    private fun hourMinute(epochMillis: Long?): Pair<Int, Int> {
        if (epochMillis == null) return DEFAULT_REMINDER_HOUR to 0
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        return time.hour to time.minute
    }

    private companion object {
        const val TASK_ID = "taskId"
        const val DRAFT_ID = "draftTaskId"
        const val READY = "taskEditorReady"
        const val TITLE = "taskTitle"
        const val DESCRIPTION = "taskDescription"
        const val TYPE_KEY = "taskReminderType"
        const val AT_KEY = "taskReminderAt"
        const val DAYS_KEY = "taskIntervalDays"
        const val WEEKDAYS_KEY = "taskWeekdays"
        const val BASE_TITLE = "taskBaseTitle"
        const val BASE_DESCRIPTION = "taskBaseDescription"
        const val BASE_TYPE = "taskBaseReminderType"
        const val BASE_AT = "taskBaseReminderAt"
        const val BASE_DAYS = "taskBaseIntervalDays"
        const val BASE_WEEKDAYS = "taskBaseWeekdays"
        const val MAX_INTERVAL_DIGITS = 3
    }
}
