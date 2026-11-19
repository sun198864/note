package com.davidsun.note.feature.vocab

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                actions = {
                    if (!state.isLoading && !state.loadFailed) {
                        IconButton(onClick = actions.onToggleBars) {
                            Icon(
                                imageVector = if (state.barsExpanded) {
                                    Icons.Filled.ExpandLess
                                } else {
                                    Icons.Filled.ExpandMore
                                },
                                contentDescription = stringResource(
                                    if (state.barsExpanded) {
                                        R.string.vocab_progress_collapse
                                    } else {
                                        R.string.vocab_progress_expand
                                    },
                                ),
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
                    visible = state.barsExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    VocabProgressHeader(
                        levels = state.levels,
                        toeicMastered = state.toeicMastered,
                        toeicTotal = state.toeicTotal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
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
fun VocabProgressHeader(
    levels: List<VocabLevelProgressUi>,
    toeicMastered: Int,
    toeicTotal: Int,
    modifier: Modifier = Modifier,
) {
    val byLevel = levels.associateBy { level -> level.level }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        (1..6).forEach { level ->
            val item = byLevel[level]
            VocabProgressBar(
                label = stringResource(R.string.vocab_level_label, level),
                mastered = item?.mastered ?: 0,
                total = item?.total ?: 0,
            )
        }
        VocabProgressBar(
            label = stringResource(R.string.vocab_progress_toeic),
            mastered = toeicMastered,
            total = toeicTotal,
        )
    }
}

@Composable
private fun VocabProgressBar(
    label: String,
    mastered: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total <= 0) 0f else mastered.toFloat() / total.toFloat()
    val count = stringResource(R.string.vocab_progress_count, mastered, total)
    val labelWidth = with(LocalDensity.current) { 40.sp.toDp() }
    val barHeight = with(LocalDensity.current) { 28.sp.toDp() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label $count" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(labelWidth),
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VocabProgressTrack(
            fraction = fraction.coerceIn(0f, 1f),
            count = count,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .height(barHeight),
        )
    }
}

@Composable
private fun VocabProgressTrack(
    fraction: Float,
    count: String,
    modifier: Modifier = Modifier,
) {
    val fill = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val onFill = MaterialTheme.colorScheme.onPrimary
    val onTrack = MaterialTheme.colorScheme.onSurface
    var barWidth by remember { mutableIntStateOf(0) }
    var countWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .onSizeChanged { barWidth = it.width },
    ) {
        val insetPx = with(density) { 12.dp.toPx() }
        val fillPx = barWidth * fraction
        val measured = barWidth > 0 && countWidth > 0
        val fitsInFill = measured && fillPx >= countWidth + insetPx
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(color = track, cornerRadius = radius)
            if (fraction > 0f) {
                drawRoundRect(
                    color = fill,
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = radius,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (fitsInFill) {
                        Modifier.width(with(density) { fillPx.toDp() })
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count,
                modifier = Modifier
                    .alpha(if (measured) 1f else 0f)
                    .onSizeChanged { countWidth = it.width },
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = if (fitsInFill) onFill else onTrack,
            )
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

@Preview(name = "Bars collapsed", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VocabProgressCollapsedPreview() {
    NoteTheme {
        VocabProgressScreen(
            state = previewVocabProgressState().copy(barsExpanded = false),
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
private fun VocabProgressBarPreview() {
    NoteTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VocabProgressBar(label = "Lv1", mastered = 18, total = 20)
            VocabProgressBar(label = "多益", mastered = 0, total = 40)
        }
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
            levels = previewLevels(),
            toeicMastered = 32,
            toeicTotal = 40,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun previewLevels() = listOf(
    VocabLevelProgressUi(level = 1, mastered = 18, total = 20),
    VocabLevelProgressUi(level = 2, mastered = 15, total = 20),
    VocabLevelProgressUi(level = 3, mastered = 12, total = 20),
    VocabLevelProgressUi(level = 4, mastered = 8, total = 20),
    VocabLevelProgressUi(level = 5, mastered = 4, total = 10),
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
    toeicMastered = 32,
    toeicTotal = 40,
    levels = previewLevels(),
    items = items,
)

private fun previewVocabProgressActions() = VocabProgressActions(
    onBack = {},
    onQueryChange = {},
    onMasteredChange = { _, _ -> },
    onItemClick = {},
    onSpeak = {},
    onToggleBars = {},
)
