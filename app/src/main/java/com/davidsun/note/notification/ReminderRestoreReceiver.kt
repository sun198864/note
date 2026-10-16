package com.davidsun.note.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.davidsun.note.domain.repository.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderRestoreReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderScheduler.restoreAll()
            } finally {
                pending.finish()
            }
        }
    }
}
