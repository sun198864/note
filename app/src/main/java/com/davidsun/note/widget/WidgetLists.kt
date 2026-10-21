package com.davidsun.note.widget

import com.davidsun.note.domain.ListOrder
import com.davidsun.note.domain.model.GroupedTasks
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.Task

object WidgetLists {
    const val Limit = 20

    fun openTasks(grouped: GroupedTasks): List<Task> =
        (grouped.today + grouped.upcoming + grouped.undated).take(Limit)

    fun recentNotes(notes: List<Note>): List<Note> =
        ListOrder.notes(notes.filter { it.deletedAt == null }).take(Limit)

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
