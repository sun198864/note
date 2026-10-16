package com.davidsun.note.navigation

object AppRoute {
    const val Tasks = "tasks"
    const val Notes = "notes"
    const val Settings = "settings"
    const val HiddenTasks = "tasks/hidden"
    const val TaskEditor = "tasks/editor/{taskId}"
    const val NoteEditor = "notes/editor/{noteId}"
    const val NewId = "new"

    fun taskEditor(taskId: String?): String = "tasks/editor/${taskId ?: NewId}"

    fun noteEditor(noteId: String?): String = "notes/editor/${noteId ?: NewId}"
}
