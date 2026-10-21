package com.davidsun.note.widget

import com.davidsun.note.navigation.AppRoute

object WidgetRoutes {
    const val Extra = "com.davidsun.note.widget.ROUTE"
    const val ExtraFocusId = "com.davidsun.note.widget.FOCUS_ID"

    private val TaskEditor = Regex("^tasks/editor/([^/]+)$")
    private val NoteEditor = Regex("^notes/editor/([^/]+)$")

    fun openRequest(route: String): WidgetOpenRequest? = when (route) {
        AppRoute.Tasks, AppRoute.Notes -> WidgetOpenRequest(route = route, focusId = null)
        else -> {
            val taskId = TaskEditor.matchEntire(route)?.groupValues?.get(1)
            val noteId = NoteEditor.matchEntire(route)?.groupValues?.get(1)
            when {
                taskId != null && taskId != AppRoute.NewId -> WidgetOpenRequest(AppRoute.Tasks, taskId)
                noteId != null && noteId != AppRoute.NewId -> WidgetOpenRequest(AppRoute.Notes, noteId)
                else -> null
            }
        }
    }
}

data class WidgetOpenRequest(
    val route: String,
    val focusId: String?,
)
