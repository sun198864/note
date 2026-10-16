package com.davidsun.note.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val fontScale: Flow<FontScale> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> FontScale.fromStorageKey(preferences[FontScaleKey]) }

    override suspend fun setFontScale(scale: FontScale) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[FontScaleKey] = scale.storageKey
        }
    }

    private companion object {
        val FontScaleKey = stringPreferencesKey("font_scale")
    }
}
