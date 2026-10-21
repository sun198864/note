package com.davidsun.note.feature.tasks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.ListOrder
import com.davidsun.note.ui.ScreenStatus
import com.davidsun.note.ui.component.ActionFailedBanner
import com.davidsun.note.ui.component.ConfirmDialog
import com.davidsun.note.ui.component.EmptyState
import com.davidsun.note.ui.component.ImportantFilter
import com.davidsun.note.ui.component.rememberReorderableOrder
import com.davidsun.note.ui.theme.NoteTheme
import sh.calvin.reorderable.ReorderableItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenTasksScreen(
    state: HiddenTasksUiState,
    actions: HiddenTasksActions,
    modifier: Modifier = Modifier,
) {
    val pendingDelete = state.pendingDelete
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.hidden_tasks_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.status == ScreenStatus.Empty || state.status == ScreenStatus.Content) {
                ImportantFilter(
                    checked = state.importantOnly,
                    onCheckedChange = actions.onImportantOnlyChange,
                )
            }
            when (state.status) {
                ScreenStatus.Loading -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                ScreenStatus.Error -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(R.string.load_error),
                        body = stringResource(R.string.load_error_body),
                    )
                }
                ScreenStatus.Empty -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(
                            if (state.importantOnly) {
                                R.string.empty_important_hidden_title
                            } else {
                                R.string.empty_hidden_title
                            },
                        ),
                        body = stringResource(
                            if (state.importantOnly) {
                                R.string.empty_important_hidden_body
                            } else {
                                R.string.empty_hidden_body
                            },
                        ),
                    )
                }
                ScreenStatus.Content -> {
                    val listState = rememberLazyListState()
                    val order = rememberReorderableOrder(
                        source = state.tasks,
                        listState = listState,
                        move = { items, fromKey, toKey -> ListOrder.move(items, fromKey, toKey) { it.id } },
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.actionFailed) {
                            item(key = "error") {
                                ActionFailedBanner(text = stringResource(R.string.action_error))
                            }
                        }
                        items(items = order.items, key = { task -> task.id }) { task ->
                            ReorderableItem(order.state, key = task.id) { _ ->
                                TaskCard(
                                    item = task,
                                    menuPrimaryLabel = stringResource(R.string.menu_restore),
                                    onClick = { actions.onTaskClick(task.id) },
                                    onToggleComplete = { checked -> actions.onToggleComplete(task.id, checked) },
                                    onMenuPrimary = { actions.onRestore(task.id) },
                                    onDelete = { actions.onDeleteRequest(task.id, task.title) },
                                    onToggleImportant = {
                                        actions.onToggleImportant(task.id, !task.isImportant)
                                    },
                                    dragHandleModifier = Modifier.draggableHandle(
                                        onDragStopped = {
                                            actions.onReorder(order.current().map { it.id })
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (pendingDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_task_title),
            message = stringResource(R.string.delete_task_message, pendingDelete.title),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onDismissDelete,
            destructive = true,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HiddenTasksScreenPreview() {
    NoteTheme {
        HiddenTasksScreen(
            state = HiddenTasksUiState(
                status = ScreenStatus.Content,
                tasks = listOf(
                    TaskItemUi("1", "舊想法", "先收起來", null, false, false, true),
                ),
            ),
            actions = previewHiddenActions(),
        )
    }
}

@Preview(name = "Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HiddenTasksEmptyPreview() {
    NoteTheme {
        HiddenTasksScreen(
            state = HiddenTasksUiState(status = ScreenStatus.Empty),
            actions = previewHiddenActions(),
        )
    }
}

private fun previewHiddenActions() = HiddenTasksActions(
    onBack = {},
    onTaskClick = {},
    onToggleComplete = { _, _ -> },
    onRestore = {},
    onDeleteRequest = { _, _ -> },
    onConfirmDelete = {},
    onDismissDelete = {},
    onToggleImportant = { _, _ -> },
    onImportantOnlyChange = {},
    onReorder = {},
)
