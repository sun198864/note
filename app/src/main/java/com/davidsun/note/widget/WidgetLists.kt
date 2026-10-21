package com.davidsun.note.widget

import com.davidsun.note.domain.model.GroupedTasks
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.Task

object WidgetLists {
    const val Limit = 20

    fun openTasks(grouped: GroupedTasks): List<Task> =
        (grouped.today + grouped.upcoming + grouped.undated).take(Limit)

    fun recentNotes(notes: List<Note>): List<Note> =
        notes.asSequence()
            .filter { it.deletedAt == null }
            .sortedByDescending { it.updatedAt }
            .take(Limit)
            .toList()

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
