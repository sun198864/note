package com.davidsun.note.feature.vocab

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.layout.Spacer
import com.davidsun.note.R
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.ui.component.EmptyState
import com.davidsun.note.ui.theme.NoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabProgressScreen(
    state: VocabProgressUiState,
    actions: VocabProgressActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.vocab_progress_title)) },
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
                VocabProgressHeader(
                    mastered = state.mastered,
                    total = state.total,
                    levels = state.levels,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.vocab_progress_list),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                VocabProgressSearchField(
                    query = state.query,
                    onQueryChange = actions.onQueryChange,
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.query.isBlank()) {
                            EmptyState(
                                title = stringResource(R.string.vocab_progress_empty_title),
                                body = stringResource(R.string.vocab_progress_empty_body),
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
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
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
fun VocabProgressHeader(
    mastered: Int,
    total: Int,
    levels: List<VocabLevelProgressUi>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VocabOverallPie(
            mastered = mastered,
            total = total,
            modifier = Modifier.weight(1f),
        )
        VocabLevelFractions(
            levels = levels,
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun VocabOverallPie(
    mastered: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total <= 0) 0f else mastered.toFloat() / total.toFloat()
    val doneColor = MaterialTheme.colorScheme.primary
    val remainColor = MaterialTheme.colorScheme.surfaceVariant
    val count = stringResource(R.string.vocab_progress_count, mastered, total)
    val description = stringResource(R.string.vocab_progress_all) + " " + count
    Column(
        modifier = modifier.semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(50.dp),
        ) {
            drawArc(
                color = remainColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
            )
            if (fraction > 0f) {
                drawArc(
                    color = doneColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = true,
                )
            }
        }
        Text(
            text = count,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun VocabLevelFractions(
    levels: List<VocabLevelProgressUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        levels.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pair.forEach { level ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.vocab_level_label, level.level),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = stringResource(
                                R.string.vocab_progress_count,
                                level.mastered,
                                level.total,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (pair.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabProgressSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = query,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = innerTextField,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.vocab_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.vocab_clear_search),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onQueryChange("") },
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                )
            },
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabProgressScreenPreview() {
    NoteTheme {
        VocabProgressScreen(
            state = previewVocabProgressState(),
            actions = previewVocabProgressActions(),
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun VocabProgressLargeFontPreview() {
    NoteTheme(fontScale = FontScale.LARGE) {
        VocabProgressScreen(
            state = previewVocabProgressState(),
            actions = previewVocabProgressActions(),
        )
    }
}

@Preview(name = "None checked", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VocabProgressEmptyPreview() {
    NoteTheme {
        VocabProgressScreen(
            state = previewVocabProgressState(items = emptyList(), mastered = 0),
            actions = previewVocabProgressActions(),
        )
    }
}

@Preview(name = "Search empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VocabProgressSearchEmptyPreview() {
    NoteTheme {
        VocabProgressScreen(
            state = previewVocabProgressState(query = "zzzz", items = emptyList()),
            actions = previewVocabProgressActions(),
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabOverallPiePreview() {
    NoteTheme {
        VocabOverallPie(
            mastered = 10,
            total = 100,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 180)
@Preview(name = "Dark", showBackground = true, widthDp = 180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabLevelFractionsPreview() {
    NoteTheme {
        VocabLevelFractions(
            levels = previewLevels(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabProgressSearchFieldPreview() {
    NoteTheme {
        VocabProgressSearchField(
            query = "aban",
            onQueryChange = {},
            onSearch = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VocabProgressHeaderPreview() {
    NoteTheme {
        VocabProgressHeader(
            mastered = 10,
            total = 100,
            levels = previewLevels(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewLevels() = listOf(
    VocabLevelProgressUi(level = 1, mastered = 4, total = 20),
    VocabLevelProgressUi(level = 2, mastered = 2, total = 20),
    VocabLevelProgressUi(level = 3, mastered = 1, total = 20),
    VocabLevelProgressUi(level = 4, mastered = 2, total = 20),
    VocabLevelProgressUi(level = 5, mastered = 1, total = 10),
    VocabLevelProgressUi(level = 6, mastered = 0, total = 10),
)

private fun previewVocabProgressState(
    query: String = "",
    mastered: Int = 10,
    items: List<VocabItemUi> = listOf(
        VocabItemUi("1", "abandon", "v.", "放棄", mastered = true),
        VocabItemUi("2", "ability", "n.", "能力", mastered = true),
    ),
) = VocabProgressUiState(
    isLoading = false,
    query = query,
    mastered = mastered,
    total = 100,
    levels = previewLevels(),
    items = items,
)

private fun previewVocabProgressActions() = VocabProgressActions(
    onBack = {},
    onQueryChange = {},
    onMasteredChange = { _, _ -> },
    onItemClick = {},
)
