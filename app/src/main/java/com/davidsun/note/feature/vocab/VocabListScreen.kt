package com.davidsun.note.feature.vocab

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.navigation.AppTab
import com.davidsun.note.navigation.NoteBottomBar
import com.davidsun.note.ui.component.EmptyState
import com.davidsun.note.ui.theme.NoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabListScreen(
    state: VocabListUiState,
    actions: VocabListActions,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val ready = !state.isLoading && !state.loadFailed
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_vocab)) },
                actions = {
                    if (ready) {
                        IconButton(onClick = actions.onRefresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.vocab_refresh),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
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
            state.loadFailed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.vocab_load_error),
                    body = stringResource(R.string.vocab_load_error_body),
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = actions.onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.vocab_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { actions.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.vocab_clear_search),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
                if (state.items.isNotEmpty()) {
                    Text(
                        text = if (state.query.isBlank()) {
                            stringResource(R.string.vocab_sample_hint, state.items.size)
                        } else {
                            stringResource(R.string.vocab_search_count, state.items.size)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.query.isBlank()) {
                            EmptyState(
                                title = stringResource(R.string.vocab_empty_title),
                                body = stringResource(R.string.vocab_empty_body),
                            )
                        } else {
                            EmptyState(
                                title = stringResource(R.string.vocab_search_empty_title),
                                body = stringResource(R.string.vocab_search_empty_body),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.items, key = { item -> item.id }) { item ->
                            VocabRow(
                                item = item,
                                onClick = { actions.onItemClick(item.id) },
                                onMasteredChange = { mastered ->
                                    actions.onMasteredChange(item.id, mastered)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VocabRow(
    item: VocabItemUi,
    onClick: () -> Unit,
    onMasteredChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meaning = item.meaning.ifBlank { stringResource(R.string.vocab_no_meaning) }
    val masteredDescription = stringResource(
        if (item.mastered) R.string.vocab_mastered_on else R.string.vocab_mastered_off,
    )
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.mastered) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.partOfSpeech,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = meaning,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Checkbox(
                checked = item.mastered,
                onCheckedChange = onMasteredChange,
                modifier = Modifier.semantics { contentDescription = masteredDescription },
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabListScreenPreview() {
    NoteTheme {
        VocabListScreen(
            state = previewVocabListState(),
            actions = previewVocabListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun VocabListLargeFontPreview() {
    NoteTheme(fontScale = FontScale.LARGE) {
        VocabListScreen(
            state = previewVocabListState(),
            actions = previewVocabListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Search empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VocabListSearchEmptyPreview() {
    NoteTheme {
        VocabListScreen(
            state = VocabListUiState(isLoading = false, query = "zzzz"),
            actions = previewVocabListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabRowPreview() {
    NoteTheme {
        VocabRow(
            item = VocabItemUi("1", "abandon", "v.", "放棄", mastered = true),
            onClick = {},
            onMasteredChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewVocabListState() = VocabListUiState(
    isLoading = false,
    items = listOf(
        VocabItemUi("1", "abandon", "v.", "放棄", mastered = false),
        VocabItemUi("2", "ability", "n.", "能力", mastered = true),
        VocabItemUi("3", "schedule", "n./v.", "時間表；預定", mastered = false),
    ),
)

private fun previewVocabListActions() = VocabListActions(
    onQueryChange = {},
    onRefresh = {},
    onMasteredChange = { _, _ -> },
    onItemClick = {},
)
