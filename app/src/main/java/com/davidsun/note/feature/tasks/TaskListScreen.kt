package com.davidsun.note.feature.tasks

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.navigation.AppTab
import com.davidsun.note.navigation.NoteBottomBar
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
fun TaskListScreen(
    state: TaskListUiState,
    actions: TaskListActions,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    focusId: String? = null,
    onFocusConsumed: () -> Unit = {},
) {
    val pendingDelete = state.pendingDelete
    LaunchedEffect(focusId, state.status) {
        if (focusId == null || state.status == ScreenStatus.Loading || state.status == ScreenStatus.Content) return@LaunchedEffect
        onFocusConsumed()
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_tasks)) },
                colors = topBarColors(),
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            if (state.status == ScreenStatus.Content) {
                FloatingActionButton(onClick = actions.onAddClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_task),
                    )
                }
            }
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
            if (state.status == ScreenStatus.Empty || state.status == ScreenStatus.Error) {
                TextButton(
                    onClick = actions.onOpenHidden,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(text = stringResource(R.string.hidden_tasks_count, state.hiddenCount))
                }
            }
            if (state.actionFailed) {
                ActionFailedBanner(text = stringResource(R.string.action_error))
            }
            when (state.status) {
                ScreenStatus.Loading -> LoadingBox(modifier = Modifier.weight(1f))
                ScreenStatus.Error -> Box(
                    modifier = Modifier.weight(1f),
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
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(
                            if (state.importantOnly) {
                                R.string.empty_important_tasks_title
                            } else {
                                R.string.empty_tasks_title
                            },
                        ),
                        body = stringResource(
                            if (state.importantOnly) {
                                R.string.empty_important_tasks_body
                            } else {
                                R.string.empty_tasks_body
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        actionLabel = stringResource(R.string.add_task),
                        onAction = actions.onAddClick,
                    )
                }
                ScreenStatus.Content -> {
                    val initialIndex = remember(state.sections, focusId) {
                        focusId?.let { taskScrollIndex(state.sections, it) } ?: 0
                    }
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                    val order = rememberReorderableOrder(
                        source = state.sections,
                        listState = listState,
                        move = { items, fromKey, toKey -> items.moveTask(fromKey, toKey) },
                    )
                    LaunchedEffect(focusId, state.sections) {
                        val id = focusId ?: return@LaunchedEffect
                        val index = taskScrollIndex(state.sections, id)
                        if (index != null) listState.scrollToItem(index)
                        onFocusConsumed()
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    item(key = "hidden") {
                        SectionLabel(
                            text = stringResource(R.string.hidden_tasks_count, state.hiddenCount),
                            onClick = actions.onOpenHidden,
                        )
                    }
                    order.items.forEach { section ->
                        item(key = "header-${section.section.name}") {
                            SectionHeader(section = section.section, count = section.tasks.size)
                        }
                        items(items = section.tasks, key = { task -> task.id }) { task ->
                            ReorderableItem(order.state, key = task.id) { _ ->
                                TaskCard(
                                    item = task,
                                    menuPrimaryLabel = stringResource(R.string.menu_hide),
                                    onClick = { actions.onTaskClick(task.id) },
                                    onToggleComplete = { checked -> actions.onToggleComplete(task.id, checked) },
                                    onMenuPrimary = { actions.onHide(task.id) },
                                    onDelete = { actions.onDeleteRequest(task.id, task.title) },
                                    onToggleImportant = {
                                        actions.onToggleImportant(task.id, !task.isImportant)
                                    },
                                    dragHandleModifier = Modifier.draggableHandle(
                                        onDragStopped = {
                                            val current = order.current()
                                                .firstOrNull { it.section == section.section }
                                            if (current != null) {
                                                actions.onReorder(
                                                    current.section,
                                                    current.tasks.map { it.id },
                                                )
                                            }
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

@Composable
private fun SectionHeader(section: TaskSection, count: Int, modifier: Modifier = Modifier) {
    val title = when (section) {
        TaskSection.Today -> stringResource(R.string.section_today)
        TaskSection.Upcoming -> stringResource(R.string.section_upcoming)
        TaskSection.Undated -> stringResource(R.string.section_undated)
        TaskSection.Completed -> stringResource(R.string.section_completed)
    }
    SectionLabel(
        text = stringResource(R.string.section_count, title, count),
        modifier = modifier,
    )
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text,
        modifier = modifier
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                },
            )
            .padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
)

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskListScreenPreview() {
    NoteTheme {
        TaskListScreen(
            state = previewTaskListState(),
            actions = previewTaskListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Tasks, onSelect = {}) },
        )
    }
}

@Preview(name = "Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TaskListEmptyPreview() {
    NoteTheme {
        TaskListScreen(
            state = TaskListUiState(status = ScreenStatus.Empty, hiddenCount = 1),
            actions = previewTaskListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Tasks, onSelect = {}) },
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun TaskListLargeFontPreview() {
    NoteTheme(fontScale = com.davidsun.note.domain.model.FontScale.LARGE) {
        TaskListScreen(
            state = previewTaskListState(),
            actions = previewTaskListActions(),
        )
    }
}

private fun previewTaskListState() = TaskListUiState(
    status = ScreenStatus.Content,
    hiddenCount = 2,
    sections = listOf(
        TaskSectionUi(
            section = TaskSection.Today,
            tasks = listOf(
                TaskItemUi("1", "回覆信件", "下午前完成", "10月14日 週三", true, false, true),
                TaskItemUi("2", "買菜", null, "10月15日 週四", false, false),
            ),
        ),
        TaskSectionUi(
            section = TaskSection.Completed,
            tasks = listOf(TaskItemUi("3", "繳費", null, "10月10日 週六", false, true)),
        ),
    ),
    pendingDelete = null,
)

private fun previewTaskListActions() = TaskListActions(
    onTaskClick = {},
    onAddClick = {},
    onToggleComplete = { _, _ -> },
    onHide = {},
    onDeleteRequest = { _, _ -> },
    onConfirmDelete = {},
    onDismissDelete = {},
    onOpenHidden = {},
    onToggleImportant = { _, _ -> },
    onImportantOnlyChange = {},
    onReorder = { _, _ -> },
)

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    NoteTheme {
        Column {
            SectionLabel(text = "已隱藏（2）", onClick = {})
            SectionHeader(section = TaskSection.Completed, count = 1)
        }
    }
}
