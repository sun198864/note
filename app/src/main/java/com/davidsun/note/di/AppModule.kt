package com.davidsun.note.di

import android.content.Context
import androidx.room.Room
import com.davidsun.note.core.common.AppClock
import com.davidsun.note.core.common.SystemAppClock
import com.davidsun.note.data.local.MIGRATION_1_2
import com.davidsun.note.data.local.NoteDatabase
import com.davidsun.note.data.local.dao.NoteDao
import com.davidsun.note.data.local.dao.TaskDao
import com.davidsun.note.data.repository.NoteRepositoryImpl
import com.davidsun.note.data.repository.SettingsRepositoryImpl
import com.davidsun.note.data.repository.TaskRepositoryImpl
import com.davidsun.note.domain.repository.NoteRepository
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.repository.SettingsRepository
import com.davidsun.note.domain.repository.TaskRepository
import com.davidsun.note.notification.AlarmReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteDatabase =
        Room.databaseBuilder(context, NoteDatabase::class.java, "note.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTaskDao(database: NoteDatabase): TaskDao = database.taskDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemAppClock): AppClock

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
