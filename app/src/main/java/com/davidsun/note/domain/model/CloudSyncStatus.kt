package com.davidsun.note.domain.model

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val issue: CloudSyncIssue = CloudSyncIssue.None,
    val switchedAccount: Boolean = false,
)

enum class CloudSyncIssue {
    None,
    Retrying,
    PermissionDenied,
}
