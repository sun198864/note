package com.davidsun.note.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.davidsun.note.data.local.dao.NoteDao
import com.davidsun.note.data.local.dao.TaskDao
import com.davidsun.note.data.local.entity.NoteEntity
import com.davidsun.note.data.local.entity.TaskEntity

@Database(
    entities = [NoteEntity::class, TaskEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    abstract fun taskDao(): TaskDao
}
