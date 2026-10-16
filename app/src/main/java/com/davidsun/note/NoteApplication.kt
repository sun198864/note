package com.davidsun.note

import android.app.Application
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.notification.TaskNotifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NoteApplication : Application() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        TaskNotifications.ensureChannel(this)
        reminderScheduler.restoreOnAppStart()
    }
}
