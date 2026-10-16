package com.davidsun.note.feature.notes

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidsun.note.R
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.ui.theme.NoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    actions: NoteEditorActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    BackHandler(onBack = actions.onBack)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(
                                if (state.openedAsNew) R.string.new_note else R.string.edit_note,
                            ),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!state.isLoading && !state.missing) {
                            Text(
                                text = saveStatusLabel(state.saveStatus),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.saveStatus == NoteSaveStatus.Error) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
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
                Text(text = stringResource(R.string.missing_note))
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
                    label = { Text(text = stringResource(R.string.note_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
                OutlinedTextField(
                    value = state.content,
                    onValueChange = actions.onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp),
                    label = { Text(text = stringResource(R.string.note_content)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun saveStatusLabel(status: NoteSaveStatus): String = when (status) {
    NoteSaveStatus.Idle -> stringResource(R.string.save_idle)
    NoteSaveStatus.Saving -> stringResource(R.string.save_saving)
    NoteSaveStatus.Saved -> stringResource(R.string.save_saved)
    NoteSaveStatus.Error -> stringResource(R.string.save_error)
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoteEditorScreenPreview() {
    NoteTheme {
        NoteEditorScreen(
            state = NoteEditorUiState(
                isLoading = false,
                openedAsNew = false,
                title = "會議紀錄",
                content = "下週一確認設計稿。",
                saveStatus = NoteSaveStatus.Saved,
            ),
            actions = previewNoteEditorActions(),
        )
    }
}

@Preview(name = "Large font", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun NoteEditorLargeFontPreview() {
    NoteTheme(fontScale = FontScale.LARGE) {
        NoteEditorScreen(
            state = NoteEditorUiState(
                isLoading = false,
                title = "靈感",
                content = "把字體放大後，內文仍然要讀得清楚。",
                saveStatus = NoteSaveStatus.Saving,
            ),
            actions = previewNoteEditorActions(),
        )
    }
}

private fun previewNoteEditorActions() = NoteEditorActions(
    onTitleChange = {},
    onContentChange = {},
    onBack = {},
)
