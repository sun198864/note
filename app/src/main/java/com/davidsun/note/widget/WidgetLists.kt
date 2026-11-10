package com.davidsun.note.widget

import com.davidsun.note.domain.ListOrder
import com.davidsun.note.domain.model.GroupedTasks
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.vocab.VocabPicker

object WidgetLists {
    const val Limit = 20

    fun openTasks(grouped: GroupedTasks): List<Task> =
        (grouped.today + grouped.upcoming + grouped.undated).take(Limit)

    fun recentNotes(notes: List<Note>): List<Note> =
        ListOrder.notes(notes.filter { it.deletedAt == null }).take(Limit)

    fun sampledWords(entries: List<VocabEntry>, sampleIds: List<String>): List<VocabEntry> {
        val byId = entries.associateBy { entry -> entry.id }
        return sampleIds.mapNotNull { id -> byId[id] }.take(VocabPicker.SAMPLE_SIZE)
    }

    fun vocabSubtitle(entry: VocabEntry): String {
        val meaning = entry.meaning.ifBlank {
            summary(entry.definition.ifBlank { entry.example }, limit = 60)
        }
        return listOf(entry.partOfSpeech.trim(), meaning)
            .filter { part -> part.isNotEmpty() }
            .joinToString(" ")
    }

    fun summary(content: String, limit: Int = 80): String {
        val singleLine = content.replace(WHITESPACE, " ").trim()
        return when {
            singleLine.isEmpty() -> ""
            singleLine.length <= limit -> singleLine
            else -> singleLine.take(limit) + "…"
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
