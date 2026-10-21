package com.davidsun.note.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.davidsun.note.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    suspend fun getActive(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getAnyById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED'")
    suspend fun getPending(): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = :id AND revision = :revision")
    suspend fun markSynced(id: String, revision: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)
}
