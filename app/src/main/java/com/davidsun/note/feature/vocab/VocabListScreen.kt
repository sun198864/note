package com.davidsun.note.feature.vocab

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Checklist
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    val ready = !state.isLoading && !state.loadFailed
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.tab_vocab))
                        if (ready) {
                            IconButton(onClick = actions.onOpenProgress) {
                                Icon(
                                    imageVector = Icons.Filled.Checklist,
                                    contentDescription = stringResource(R.string.vocab_progress),
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (ready) {
                        VocabScriptToggle(
                            label = stringResource(R.string.vocab_script_chinese),
                            shown = state.showChinese,
                            description = stringResource(
                                if (state.showChinese) {
                                    R.string.vocab_hide_chinese
                                } else {
                                    R.string.vocab_show_chinese
                                },
                            ),
                            onClick = actions.onToggleChinese,
                        )
                        VocabScriptToggle(
                            label = stringResource(R.string.vocab_script_english),
                            shown = state.showEnglish,
                            description = stringResource(
                                if (state.showEnglish) {
                                    R.string.vocab_hide_english
                                } else {
                                    R.string.vocab_show_english
                                },
                            ),
                            onClick = actions.onToggleEnglish,
                        )
                        IconButton(onClick = { actions.onSearchOpenChange(!state.searchOpen) }) {
                            Icon(
                                imageVector = if (state.searchOpen) {
                                    Icons.Filled.Close
                                } else {
                                    Icons.Filled.Search
                                },
                                contentDescription = stringResource(
                                    if (state.searchOpen) {
                                        R.string.vocab_close_search
                                    } else {
                                        R.string.vocab_search_hint
                                    },
                                ),
                            )
                        }
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
                AnimatedVisibility(
                    visible = state.searchOpen,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    VocabSearchField(
                        query = state.query,
                        onQueryChange = actions.onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
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
                                showWord = state.showEnglish,
                                showMeaning = state.showChinese,
                                onClick = { actions.onItemClick(item.id) },
                                onMasteredChange = { mastered ->
                                    actions.onMasteredChange(item.id, mastered)
                                },
                                onSpeak = { actions.onSpeak(item.word) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabScriptToggle(
    label: String,
    shown: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slash = MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                modifier = Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!shown) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(
                        color = slash,
                        start = Offset(0f, size.height * 0.15f),
                        end = Offset(size.width, size.height * 0.85f),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text(text = stringResource(R.string.vocab_search_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
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
}

@Composable
fun VocabRow(
    item: VocabItemUi,
    onClick: () -> Unit,
    onMasteredChange: (Boolean) -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
    showWord: Boolean = true,
    showMeaning: Boolean = true,
) {
    val meaning = item.meaning.ifBlank { stringResource(R.string.vocab_no_meaning) }
    val masteredDescription = stringResource(
        if (item.mastered) R.string.vocab_mastered_on else R.string.vocab_mastered_off,
    )
    val speakDescription = if (showWord) {
        stringResource(R.string.vocab_speak_word, item.word)
    } else {
        stringResource(R.string.vocab_speak)
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSpeak) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = speakDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.word,
                    modifier = if (showWord) {
                        Modifier
                    } else {
                        Modifier
                            .alpha(0f)
                            .clearAndSetSemantics { }
                    },
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
                    if (item.partOfSpeech.isNotBlank()) {
                        Text(
                            text = item.partOfSpeech,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = meaning,
                        modifier = Modifier
                            .padding(start = if (item.partOfSpeech.isNotBlank()) 8.dp else 0.dp)
                            .then(
                                if (showMeaning) {
                                    Modifier
                                } else {
                                    Modifier
                                        .alpha(0f)
                                        .clearAndSetSemantics { }
                                },
                            ),
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
            state = previewVocabListState().copy(showEnglish = false),
            actions = previewVocabListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Search open", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Search open dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabListSearchOpenPreview() {
    NoteTheme {
        VocabListScreen(
            state = previewVocabListState().copy(searchOpen = true, query = "ab"),
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
            state = VocabListUiState(isLoading = false, searchOpen = true, query = "zzzz"),
            actions = previewVocabListActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabSearchFieldPreview() {
    NoteTheme {
        VocabSearchField(
            query = "ab",
            onQueryChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            onSpeak = {},
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

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabScriptTogglePreview() {
    NoteTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            VocabScriptToggle(
                label = "中",
                shown = true,
                description = "隱藏中文",
                onClick = {},
            )
            VocabScriptToggle(
                label = "英",
                shown = false,
                description = "顯示英文",
                onClick = {},
            )
        }
    }
}

@Preview(name = "English hidden", showBackground = true)
@Composable
private fun VocabRowEnglishHiddenPreview() {
    NoteTheme {
        VocabRow(
            item = VocabItemUi("1", "abandon", "v.", "放棄", mastered = false, level = 4, toeic = true),
            showWord = false,
            showMeaning = true,
            onClick = {},
            onMasteredChange = {},
            onSpeak = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewVocabListActions() = VocabListActions(
    onQueryChange = {},
    onSearchOpenChange = {},
    onRefresh = {},
    onMasteredChange = { _, _ -> },
    onItemClick = {},
    onOpenProgress = {},
    onSpeak = {},
    onToggleChinese = {},
    onToggleEnglish = {},
)
