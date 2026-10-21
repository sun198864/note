package com.davidsun.note.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.davidsun.note.domain.repository.NoteRepository
import com.davidsun.note.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class WidgetRefreshCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                tasks.observeActiveTasks(),
                notes.observeActiveNotes(),
            ) { _, _ -> Unit }
                .debounce(REFRESH_DELAY_MILLIS)
                .collect {
                    try {
                        NoteListWidget().updateAll(context)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                    }
                }
        }
    }

    private companion object {
        const val REFRESH_DELAY_MILLIS = 300L
    }
}
