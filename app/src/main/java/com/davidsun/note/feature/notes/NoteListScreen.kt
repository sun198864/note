package com.davidsun.note.feature.notes

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.ListOrder
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
fun NoteListScreen(
    state: NoteListUiState,
    actions: NoteListActions,
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
                title = { Text(text = stringResource(R.string.tab_notes)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            if (state.status == ScreenStatus.Content) {
                FloatingActionButton(onClick = actions.onAddClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_note),
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
                                R.string.empty_important_notes_title
                            } else {
                                R.string.empty_notes_title
                            },
                        ),
                        body = stringResource(
                            if (state.importantOnly) {
                                R.string.empty_important_notes_body
                            } else {
                                R.string.empty_notes_body
                            },
                        ),
                        actionLabel = stringResource(R.string.add_note),
                        onAction = actions.onAddClick,
                    )
                }
                ScreenStatus.Content -> {
                    val initialIndex = remember(state.notes, state.actionFailed, focusId) {
                        focusId?.let { noteScrollIndex(state.notes, it, state.actionFailed) } ?: 0
                    }
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                    val order = rememberReorderableOrder(
                        source = state.notes,
                        listState = listState,
                        move = { items, fromKey, toKey -> ListOrder.move(items, fromKey, toKey) { it.id } },
                    )
                    LaunchedEffect(focusId, state.notes, state.actionFailed) {
                        val id = focusId ?: return@LaunchedEffect
                        val index = noteScrollIndex(state.notes, id, state.actionFailed)
                        if (index != null) listState.scrollToItem(index)
                        onFocusConsumed()
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.actionFailed) {
                            item(key = "error") {
                                ActionFailedBanner(text = stringResource(R.string.action_error))
                            }
                        }
                        items(items = order.items, key = { note -> note.id }) { note ->
                            ReorderableItem(order.state, key = note.id) { _ ->
                                NoteCard(
                                    note = note,
                                    onClick = { actions.onNoteClick(note.id) },
                                    onDelete = { actions.onDeleteRequest(note.id, note.title) },
                                    onToggleImportant = {
                                        actions.onToggleImportant(note.id, !note.isImportant)
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
        val title = pendingDelete.title.ifBlank { stringResource(R.string.untitled_note) }
        ConfirmDialog(
            title = stringResource(R.string.delete_note_title),
            message = stringResource(R.string.delete_note_message, title),
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
private fun NoteListScreenPreview() {
    NoteTheme {
        NoteListScreen(
            state = NoteListUiState(
                status = ScreenStatus.Content,
                notes = listOf(
                    NoteItemUi("1", "會議紀錄", "下週一確認設計稿。", "14:30", true),
                    NoteItemUi("2", "", "", "10月12日"),
                ),
            ),
            actions = previewNoteListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Notes, onSelect = {}) },
        )
    }
}

@Preview(name = "Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NoteListEmptyPreview() {
    NoteTheme {
        NoteListScreen(
            state = NoteListUiState(status = ScreenStatus.Empty),
            actions = previewNoteListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Notes, onSelect = {}) },
        )
    }
}

private fun previewNoteListActions() = NoteListActions(
    onNoteClick = {},
    onAddClick = {},
    onDeleteRequest = { _, _ -> },
    onConfirmDelete = {},
    onDismissDelete = {},
    onToggleImportant = { _, _ -> },
    onImportantOnlyChange = {},
    onReorder = {},
)
