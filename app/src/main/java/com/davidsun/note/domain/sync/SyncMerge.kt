package com.davidsun.note.domain.sync

object SyncMerge {
    fun remoteWins(
        localUpdatedAt: Long,
        localRevision: Long,
        remoteUpdatedAt: Long,
        remoteRevision: Long,
    ): Boolean = when {
        remoteUpdatedAt != localUpdatedAt -> remoteUpdatedAt > localUpdatedAt
        else -> remoteRevision > localRevision
    }
}
