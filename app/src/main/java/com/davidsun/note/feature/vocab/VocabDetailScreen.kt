package com.davidsun.note.feature.vocab

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.Role
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
fun VocabDetailScreen(
    state: VocabDetailUiState,
    actions: VocabDetailActions,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.vocab_detail_title)) },
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
                EmptyState(
                    title = stringResource(R.string.vocab_missing_title),
                    body = stringResource(R.string.vocab_missing_body),
                )
            }
            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeakButton(
                        onClick = actions.onSpeak,
                        description = stringResource(R.string.vocab_speak),
                    )
                    SelectableVocabText(
                        text = state.word,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        onCopy = actions.onCopy,
                        onTranslate = actions.onTranslate,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.phonetic.isNotBlank()) {
                    SelectableVocabText(
                        text = state.phonetic,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        onCopy = actions.onCopy,
                        onTranslate = actions.onTranslate,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = VocabMeta(state),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .toggleable(
                            value = state.mastered,
                            role = Role.Checkbox,
                            onValueChange = actions.onMasteredChange,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.mastered,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(R.string.vocab_exclude_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                DetailBlock(
                    label = stringResource(R.string.vocab_meaning_label),
                    body = state.meaning.ifBlank { stringResource(R.string.vocab_no_meaning) },
                    onCopy = actions.onCopy.takeIf { state.meaning.isNotBlank() },
                    onTranslate = actions.onTranslate.takeIf { state.meaning.isNotBlank() },
                )
                if (state.definition.isNotBlank()) {
                    DetailBlock(
                        label = stringResource(R.string.vocab_definition_label),
                        body = state.definition,
                        onSpeak = actions.onSpeakDefinition,
                        speakDescription = stringResource(R.string.vocab_speak_definition),
                        onCopy = actions.onCopy,
                        onTranslate = actions.onTranslate,
                    )
                }
                ExampleBlock(
                    label = stringResource(R.string.vocab_example_label),
                    pairs = pairedExamples(state.example, state.exampleZh),
                    emptyText = stringResource(R.string.vocab_no_example),
                    onSpeakLine = actions.onSpeakExample,
                    speakDescription = stringResource(R.string.vocab_speak_example),
                    onCopy = actions.onCopy.takeIf { state.example.isNotBlank() },
                    onTranslate = actions.onTranslate.takeIf { state.example.isNotBlank() },
                )
            }
        }
    }
}

@Composable
private fun SpeakButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val inset = 12.dp.roundToPx()
            layout(placeable.width - inset, placeable.height) {
                placeable.placeRelative(-inset, 0)
            }
        },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DetailBlock(
    label: String,
    body: String,
    modifier: Modifier = Modifier,
    onSpeak: (() -> Unit)? = null,
    speakDescription: String = "",
    onCopy: ((String) -> Unit)? = null,
    onTranslate: ((String) -> Unit)? = null,
) {
    Column(modifier = modifier.padding(top = 20.dp)) {
        SectionLabel(
            label = label,
            onSpeak = onSpeak,
            speakDescription = speakDescription,
        )
        VocabBodyText(
            text = body,
            onCopy = onCopy,
            onTranslate = onTranslate,
        )
    }
}

@Composable
private fun VocabMeta(state: VocabDetailUiState): String = when {
    state.level in 1..6 && state.toeic -> stringResource(
        R.string.vocab_pos_level_toeic,
        state.partOfSpeech,
        state.level,
    )
    state.level in 1..6 -> stringResource(R.string.vocab_pos_level, state.partOfSpeech, state.level)
    state.toeic -> stringResource(R.string.vocab_pos_toeic, state.partOfSpeech)
    else -> state.partOfSpeech
}

@Composable
private fun ExampleBlock(
    label: String,
    pairs: List<Pair<String, String>>,
    emptyText: String,
    modifier: Modifier = Modifier,
    onSpeakLine: ((String) -> Unit)? = null,
    speakDescription: String = "",
    onCopy: ((String) -> Unit)? = null,
    onTranslate: ((String) -> Unit)? = null,
) {
    Column(modifier = modifier.padding(top = 20.dp)) {
        SectionLabel(
            label = label,
            onSpeak = null,
            speakDescription = "",
        )
        if (pairs.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            pairs.forEachIndexed { index, (english, chinese) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (onSpeakLine != null) {
                        SpeakButton(
                            onClick = { onSpeakLine(english) },
                            description = speakDescription,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        VocabBodyText(
                            text = english,
                            onCopy = onCopy,
                            onTranslate = onTranslate,
                        )
                        if (chinese.isNotBlank()) {
                            VocabBodyText(
                                text = chinese,
                                onCopy = onCopy,
                                onTranslate = onTranslate,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    label: String,
    onSpeak: (() -> Unit)?,
    speakDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onSpeak != null) {
            SpeakButton(
                onClick = onSpeak,
                description = speakDescription,
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabDetailScreenPreview() {
    NoteTheme {
        VocabDetailScreen(
            state = previewVocabDetailState(),
            actions = previewVocabDetailActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun VocabDetailLargeFontPreview() {
    NoteTheme(fontScale = FontScale.LARGE) {
        VocabDetailScreen(
            state = previewVocabDetailState(mastered = true),
            actions = previewVocabDetailActions(),
            bottomBar = { NoteBottomBar(selected = AppTab.Vocab, onSelect = {}) },
        )
    }
}

@Composable
private fun VocabBodyText(
    text: String,
    onCopy: ((String) -> Unit)?,
    onTranslate: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onCopy != null && onTranslate != null) {
        SelectableVocabText(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            onCopy = onCopy,
            onTranslate = onTranslate,
            modifier = modifier,
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SelectableVocabText(
    text: String,
    style: TextStyle,
    color: Color,
    onCopy: (String) -> Unit,
    onTranslate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(text) {
                    detectTapGestures(onLongPress = { menuOpen = true })
                },
            style = style,
            color = color,
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.vocab_copy)) },
                onClick = {
                    menuOpen = false
                    onCopy(text)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.vocab_open_translate)) },
                onClick = {
                    menuOpen = false
                    onTranslate(text)
                },
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectableVocabTextPreview() {
    NoteTheme {
        SelectableVocabText(
            text = "abandon",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            onCopy = {},
            onTranslate = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpeakButtonPreview() {
    NoteTheme {
        SpeakButton(
            onClick = {},
            description = "播放念法",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SectionLabelPreview() {
    NoteTheme {
        SectionLabel(
            label = "例句",
            onSpeak = {},
            speakDescription = "播放例句",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailBlockPreview() {
    NoteTheme {
        DetailBlock(
            label = "英文解釋",
            body = "to leave a person or place",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExampleBlockPreview() {
    NoteTheme {
        ExampleBlock(
            label = "例句",
            pairs = pairedExamples(
                example = "1. Why did he abandon his family?\n2. They abandoned the plan.",
                exampleZh = "1. 他為什麼離棄家人？\n2. 他們中止了這個計畫。",
            ),
            emptyText = "這個單字還沒有例句",
            onSpeakLine = {},
            speakDescription = "播放例句",
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewVocabDetailState(mastered: Boolean = false) = VocabDetailUiState(
    isLoading = false,
    word = "abandon",
    phonetic = "/əˈbændən/",
    partOfSpeech = "v.",
    level = 4,
    meaning = "放棄",
    definition = "forsake, leave behind",
    example = "1. Why did he abandon his family?\n2. They abandoned the plan.",
    exampleZh = "1. 他為什麼離棄家人？\n2. 他們中止了這個計畫。",
    mastered = mastered,
    toeic = true,
)

private fun previewVocabDetailActions() = VocabDetailActions(
    onBack = {},
    onMasteredChange = {},
    onSpeak = {},
    onSpeakDefinition = {},
    onSpeakExample = { _ -> },
    onCopy = {},
    onTranslate = {},
)
