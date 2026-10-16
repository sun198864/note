package com.davidsun.note.feature.settings

import com.davidsun.note.domain.model.FontScale

data class SettingsUiState(
    val fontScale: FontScale = FontScale.MEDIUM,
)

data class SettingsActions(
    val onFontScaleSelected: (FontScale) -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onOpenExactAlarmSettings: () -> Unit,
)
