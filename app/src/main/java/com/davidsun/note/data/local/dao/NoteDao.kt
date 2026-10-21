package com.davidsun.note.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.davidsun.note.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getAnyById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE syncStatus != 'SYNCED'")
    suspend fun getPending(): List<NoteEntity>

    @Query("UPDATE notes SET syncStatus = 'SYNCED' WHERE id = :id AND revision = :revision")
    suspend fun markSynced(id: String, revision: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)
}
