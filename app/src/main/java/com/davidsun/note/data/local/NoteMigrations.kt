package com.davidsun.note.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks_new` (
              `id` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `description` TEXT,
              `reminderType` TEXT NOT NULL,
              `reminderAt` INTEGER,
              `intervalDays` INTEGER,
              `isCompleted` INTEGER NOT NULL,
              `completedAt` INTEGER,
              `isHidden` INTEGER NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `updatedAt` INTEGER NOT NULL,
              `deletedAt` INTEGER,
              `revision` INTEGER NOT NULL,
              `syncStatus` TEXT NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tasks_new (
              id, title, description, reminderType, reminderAt, intervalDays,
              isCompleted, completedAt, isHidden, createdAt, updatedAt, deletedAt, revision, syncStatus
            )
            SELECT
              id, title, description,
              CASE WHEN dueAt IS NOT NULL THEN 'ONCE' ELSE 'NONE' END,
              dueAt,
              NULL,
              isCompleted, completedAt, isHidden, createdAt, updatedAt, deletedAt, revision, syncStatus
            FROM tasks
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE tasks")
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_reminderAt` ON `tasks` (`reminderAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isHidden` ON `tasks` (`isHidden`)")
    }
}
