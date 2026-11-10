package com.davidsun.note.widget

import android.content.Context
import com.davidsun.note.R
import com.davidsun.note.domain.repository.NoteRepository
import com.davidsun.note.domain.repository.TaskRepository
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.usecase.TaskClassifier
import com.davidsun.note.domain.vocab.VocabSamples
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class WidgetRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val important: Boolean = false,
)

data class WidgetSnapshot(
    val tasks: List<WidgetRow>,
    val notes: List<WidgetRow>,
    val vocab: List<WidgetRow>,
)

@Singleton
class WidgetSnapshotLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
    private val vocab: VocabRepository,
    private val samples: VocabSamples,
    private val classifier: TaskClassifier,
) {
    suspend fun load(): WidgetSnapshot {
        val taskRows = WidgetLists.openTasks(classifier.group(tasks.activeTasks())).map { task ->
            WidgetRow(
                id = task.id,
                title = task.title,
                subtitle = task.description?.let { WidgetLists.summary(it, SUBTITLE_LIMIT) }.orEmpty(),
                important = task.isImportant,
            )
        }
        val noteRows = WidgetLists.recentNotes(notes.observeActiveNotes().first()).map { note ->
            WidgetRow(
                id = note.id,
                title = note.title.ifBlank { context.getString(R.string.untitled_note) },
                subtitle = WidgetLists.summary(note.content).ifBlank {
                    context.getString(R.string.no_note_content)
                },
                important = note.isImportant,
            )
        }
        val vocabRows = loadVocab()
        return WidgetSnapshot(tasks = taskRows, notes = noteRows, vocab = vocabRows)
    }

    private suspend fun loadVocab(): List<WidgetRow> = try {
        val sample = samples.ensure()
        WidgetLists.sampledWords(vocab.entries(), sample).map { entry ->
            WidgetRow(
                id = entry.id,
                title = entry.word,
                subtitle = WidgetLists.vocabSubtitle(entry).ifBlank {
                    context.getString(R.string.vocab_no_meaning)
                },
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val SUBTITLE_LIMIT = 60
    }
}
