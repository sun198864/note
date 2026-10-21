package com.davidsun.note

import android.app.Application
import com.davidsun.note.domain.repository.CloudSyncCoordinator
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.notification.TaskNotifications
import com.davidsun.note.widget.WidgetRefreshCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NoteApplication : Application() {
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var cloudSync: CloudSyncCoordinator
    @Inject lateinit var widgetRefresh: WidgetRefreshCoordinator

    override fun onCreate() {
        super.onCreate()
        TaskNotifications.ensureChannel(this)
        cloudSync.start()
        reminderScheduler.restoreOnAppStart()
        widgetRefresh.start()
    }
}
