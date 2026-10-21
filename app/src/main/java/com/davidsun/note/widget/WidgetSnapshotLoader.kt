package com.davidsun.note.widget

import android.content.Context
import com.davidsun.note.R
import com.davidsun.note.domain.repository.NoteRepository
import com.davidsun.note.domain.repository.TaskRepository
import com.davidsun.note.domain.usecase.TaskClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class WidgetRow(
    val id: String,
    val title: String,
    val subtitle: String,
)

data class WidgetSnapshot(
    val tasks: List<WidgetRow>,
    val notes: List<WidgetRow>,
)

@Singleton
class WidgetSnapshotLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
    private val classifier: TaskClassifier,
) {
    suspend fun load(): WidgetSnapshot {
        val taskRows = WidgetLists.openTasks(classifier.group(tasks.activeTasks())).map { task ->
            WidgetRow(
                id = task.id,
                title = task.title,
                subtitle = task.description?.let { WidgetLists.summary(it, SUBTITLE_LIMIT) }.orEmpty(),
            )
        }
        val noteRows = WidgetLists.recentNotes(notes.observeActiveNotes().first()).map { note ->
            WidgetRow(
                id = note.id,
                title = note.title.ifBlank { context.getString(R.string.untitled_note) },
                subtitle = WidgetLists.summary(note.content).ifBlank {
                    context.getString(R.string.no_note_content)
                },
            )
        }
        return WidgetSnapshot(tasks = taskRows, notes = noteRows)
    }

    private companion object {
        const val SUBTITLE_LIMIT = 60
    }
}
