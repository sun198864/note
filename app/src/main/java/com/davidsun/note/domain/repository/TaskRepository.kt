package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeActiveTasks(): Flow<List<Task>>

    suspend fun getTask(id: String): Task?

    suspend fun activeTasks(): List<Task>

    suspend fun upsert(task: Task)
}
