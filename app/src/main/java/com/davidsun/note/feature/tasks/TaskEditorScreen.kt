package com.davidsun.note.feature.tasks

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import com.davidsun.note.R
import com.davidsun.note.core.common.isWeekdaySelected
import com.davidsun.note.core.common.weekdayLabel
import com.davidsun.note.ui.component.ConfirmDialog
import com.davidsun.note.ui.component.ImportanceStar
import com.davidsun.note.ui.theme.NoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    state: TaskEditorUiState,
    actions: TaskEditorActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    BackHandler(enabled = !state.showDiscardDialog, onBack = actions.onBack)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isNew) R.string.new_task else R.string.edit_task,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (!state.isLoading && !state.missing) {
                        ImportanceStar(
                            important = state.isImportant,
                            onClick = actions.onToggleImportant,
                        )
                    }
                    TextButton(
                        onClick = actions.onSave,
                        enabled = !state.isSaving && !state.isLoading && !state.missing,
                    ) {
                        Text(
                            text = stringResource(
                                if (state.isSaving) R.string.save_saving else R.string.save,
                            ),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.missing -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.missing_task))
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = actions.onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.task_name)) },
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text(text = stringResource(R.string.task_name_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = actions.onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.task_description)) },
                    minLines = 3,
                )
                ReminderField(
                    value = state.reminderLabel ?: stringResource(R.string.no_reminder),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NotificationManagerCompat.from(context).areNotificationsEnabled()
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        actions.onOpenReminder()
                    },
                    onClear = if (state.hasReminder) actions.onClearReminder else null,
                )
                if (state.reminderPastError) {
                    Text(
                        text = stringResource(R.string.reminder_past_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.saveFailed) {
                    Text(
                        text = stringResource(R.string.save_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
    if (state.showReminderOptions) {
        ReminderOptionsDialog(
            onOnce = actions.onChooseOnce,
            onInterval = actions.onChooseInterval,
            onWeekly = actions.onChooseWeekly,
            onDismiss = actions.onDismissReminderOptions,
        )
    }
    if (state.showDatePicker) {
        TaskReminderDatePicker(
            initialSelectedMillis = state.datePickerInitialMillis,
            onConfirm = actions.onDatePicked,
            onDismiss = actions.onDismissDatePicker,
        )
    }
    if (state.showTimePicker) {
        TaskReminderTimePicker(
            hour = state.timePickerHour,
            minute = state.timePickerMinute,
            onConfirm = actions.onTimePicked,
            onDismiss = actions.onDismissTimePicker,
        )
    }
    if (state.showIntervalDialog) {
        IntervalReminderDialog(
            days = state.intervalDaysInput,
            hour = state.timePickerHour,
            minute = state.timePickerMinute,
            daysError = state.intervalDaysError,
            onDaysChange = actions.onIntervalDaysChange,
            onPickTime = actions.onOpenIntervalTime,
            onConfirm = actions.onConfirmInterval,
            onDismiss = actions.onDismissInterval,
        )
    }
    if (state.showWeeklyDialog) {
        WeeklyReminderDialog(
            weekdays = state.weekdaysMask,
            hour = state.timePickerHour,
            minute = state.timePickerMinute,
            weekdaysError = state.weekdaysError,
            onToggleWeekday = actions.onToggleWeekday,
            onPickTime = actions.onOpenWeeklyTime,
            onConfirm = actions.onConfirmWeekly,
            onDismiss = actions.onDismissWeekly,
        )
    }
    if (state.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            message = stringResource(R.string.discard_message),
            confirmLabel = stringResource(R.string.discard_confirm),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = actions.onConfirmDiscard,
            onDismiss = actions.onDismissDiscard,
            destructive = true,
        )
    }
}

@Composable
fun ReminderField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.reminder_value, value),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
        if (onClear != null) {
            TextButton(onClick = onClear) {
                Text(text = stringResource(R.string.clear_reminder))
            }
        }
    }
}

@Composable
fun ReminderOptionsDialog(
    onOnce: () -> Unit,
    onInterval: () -> Unit,
    onWeekly: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.reminder_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = onOnce, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_once),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    text = stringResource(R.string.reminder_once_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onInterval, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_interval),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    text = stringResource(R.string.reminder_interval_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onWeekly, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_weekly),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Text(
                    text = stringResource(R.string.reminder_weekly_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun IntervalReminderDialog(
    days: String,
    hour: Int,
    minute: Int,
    daysError: Boolean,
    onDaysChange: (String) -> Unit,
    onPickTime: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.reminder_interval),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = onDaysChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.reminder_interval_days)) },
                    isError = daysError,
                    supportingText = if (daysError) {
                        { Text(text = stringResource(R.string.reminder_interval_days_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedButton(onClick = onPickTime, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_time_value, hour, minute),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReminderDialog(
    weekdays: Int,
    hour: Int,
    minute: Int,
    weekdaysError: Boolean,
    onToggleWeekday: (Int) -> Unit,
    onPickTime: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.reminder_weekly),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.reminder_weekly_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..7).forEach { isoDay ->
                        FilterChip(
                            selected = isWeekdaySelected(weekdays, isoDay),
                            onClick = { onToggleWeekday(isoDay) },
                            label = { Text(text = weekdayLabel(isoDay)) },
                        )
                    }
                }
                if (weekdaysError) {
                    Text(
                        text = stringResource(R.string.reminder_weekly_empty),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(onClick = onPickTime, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.reminder_time_value, hour, minute),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onConfirm) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderDatePicker(
    initialSelectedMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let(onConfirm) },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderTimePicker(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = { TimePicker(state = pickerState) },
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskEditorScreenPreview() {
    NoteTheme {
        TaskEditorScreen(
            state = TaskEditorUiState(
                isLoading = false,
                isNew = false,
                title = "買菜",
                description = "雞蛋、牛奶",
                reminderLabel = "10月16日 週五 09:30",
                hasReminder = true,
            ),
            actions = previewTaskEditorActions(),
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun TaskEditorLargeFontPreview() {
    NoteTheme(fontScale = com.davidsun.note.domain.model.FontScale.LARGE) {
        TaskEditorScreen(
            state = TaskEditorUiState(isLoading = false, isNew = true),
            actions = previewTaskEditorActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderFieldPreview() {
    NoteTheme {
        ReminderField(value = "10月16日 週五 09:30", onClick = {}, onClear = {})
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReminderOptionsDialogPreview() {
    NoteTheme {
        ReminderOptionsDialog(onOnce = {}, onInterval = {}, onWeekly = {}, onDismiss = {})
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IntervalReminderDialogPreview() {
    NoteTheme {
        IntervalReminderDialog(
            days = "3",
            hour = 9,
            minute = 0,
            daysError = false,
            onDaysChange = {},
            onPickTime = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, fontScale = 1.3f)
@Composable
private fun WeeklyReminderDialogPreview() {
    NoteTheme {
        WeeklyReminderDialog(
            weekdays = 21,
            hour = 9,
            minute = 0,
            weekdaysError = false,
            onToggleWeekday = {},
            onPickTime = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private fun previewTaskEditorActions() = TaskEditorActions(
    onTitleChange = {},
    onDescriptionChange = {},
    onOpenReminder = {},
    onDismissReminderOptions = {},
    onChooseOnce = {},
    onChooseInterval = {},
    onChooseWeekly = {},
    onDismissDatePicker = {},
    onDatePicked = {},
    onDismissTimePicker = {},
    onTimePicked = { _, _ -> },
    onIntervalDaysChange = {},
    onOpenIntervalTime = {},
    onConfirmInterval = {},
    onDismissInterval = {},
    onToggleWeekday = {},
    onOpenWeeklyTime = {},
    onConfirmWeekly = {},
    onDismissWeekly = {},
    onClearReminder = {},
    onSave = {},
    onBack = {},
    onConfirmDiscard = {},
    onDismissDiscard = {},
    onToggleImportant = {},
)
