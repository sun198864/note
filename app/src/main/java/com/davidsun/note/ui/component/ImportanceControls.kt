package com.davidsun.note.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.ui.theme.NoteTheme
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ImportanceStar(
    important: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
            imageVector = if (important) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = stringResource(
                if (important) R.string.unmark_important else R.string.mark_important,
            ),
            tint = if (important) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.drag_to_reorder),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(40.dp).padding(8.dp),
    )
}

@Composable
fun ImportantFilter(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = stringResource(R.string.filter_important),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

class ReorderableOrder<T>(
    val items: List<T>,
    val state: ReorderableLazyListState,
    val current: () -> List<T>,
)

@Composable
fun <T> rememberReorderableOrder(
    source: List<T>,
    listState: LazyListState,
    move: (items: List<T>, fromKey: String, toKey: String) -> List<T>?,
): ReorderableOrder<T> {
    val itemsState = remember(source) { mutableStateOf(source) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        val moved = move(itemsState.value, fromKey, toKey) ?: return@rememberReorderableLazyListState
        itemsState.value = moved
    }
    return ReorderableOrder(
        items = itemsState.value,
        state = reorderState,
        current = { itemsState.value },
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportanceControlsPreview() {
    NoteTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DragHandle()
            ImportanceStar(important = true, onClick = {})
            ImportanceStar(important = false, onClick = {})
            ImportantFilter(checked = true, onCheckedChange = {})
        }
    }
}
