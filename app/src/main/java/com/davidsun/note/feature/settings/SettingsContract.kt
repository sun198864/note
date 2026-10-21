package com.davidsun.note.feature.settings

import com.davidsun.note.domain.model.CloudSyncIssue
import com.davidsun.note.domain.model.FontScale

data class SettingsUiState(
    val fontScale: FontScale = FontScale.MEDIUM,
    val isSignedIn: Boolean = false,
    val accountLabel: String? = null,
    val isSigningIn: Boolean = false,
    val signInFailed: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncedLabel: String? = null,
    val issue: CloudSyncIssue = CloudSyncIssue.None,
    val switchedAccount: Boolean = false,
)

data class SettingsActions(
    val onFontScaleSelected: (FontScale) -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onOpenExactAlarmSettings: () -> Unit,
    val onSignIn: () -> Unit,
    val onSignOut: () -> Unit,
)
