package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.Task

interface ReminderScheduler {
    suspend fun sync(task: Task)

    fun restoreOnAppStart()

    suspend fun restoreAll()
}
