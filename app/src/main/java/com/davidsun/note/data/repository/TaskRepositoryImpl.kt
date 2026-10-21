package com.davidsun.note.data.repository

import com.davidsun.note.data.local.dao.TaskDao
import com.davidsun.note.data.local.mapper.toDomain
import com.davidsun.note.data.local.mapper.toEntity
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.CloudSyncCoordinator
import com.davidsun.note.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val cloudSync: CloudSyncCoordinator,
) : TaskRepository {
    override fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.observeActive().map { tasks -> tasks.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? = taskDao.getById(id)?.toDomain()

    override suspend fun activeTasks(): List<Task> = taskDao.getActive().map { it.toDomain() }

    override suspend fun upsert(task: Task) {
        taskDao.upsert(task.toEntity())
        cloudSync.notifyLocalDataChanged()
    }
}
