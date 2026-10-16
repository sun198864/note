package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.FontScale
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val fontScale: Flow<FontScale>

    suspend fun setFontScale(scale: FontScale)
}
