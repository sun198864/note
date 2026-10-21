package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.CloudSyncStatus
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncCoordinator {
    val status: StateFlow<CloudSyncStatus>

    fun start()

    fun notifyLocalDataChanged()

    suspend fun flush()
}
