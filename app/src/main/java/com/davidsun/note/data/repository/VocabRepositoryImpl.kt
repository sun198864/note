package com.davidsun.note.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.davidsun.note.data.vocab.VocabCatalog
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.vocabDataStore by preferencesDataStore(name = "vocab_preferences")

@Singleton
class VocabRepositoryImpl @Inject constructor(
    private val catalog: VocabCatalog,
    @ApplicationContext private val context: Context,
) : VocabRepository {
    override suspend fun entries(): List<VocabEntry> = catalog.entries()

    override fun masteredIds(): Flow<Set<String>> =
        preferences().map { stored -> stored[MasteredKey].orEmpty() }

    override suspend fun setMastered(id: String, mastered: Boolean) {
        context.vocabDataStore.edit { preferences ->
            val current = preferences[MasteredKey].orEmpty()
            preferences[MasteredKey] = if (mastered) current + id else current - id
        }
    }

    override fun sampleIds(): Flow<List<String>> =
        preferences().map { stored -> decodeSample(stored[SampleKey]) }

    override suspend fun setSampleIds(ids: List<String>) {
        val encoded = ids.map { id -> id.trim() }.filter { id -> id.isNotEmpty() }.distinct()
        context.vocabDataStore.edit { preferences ->
            if (encoded.isEmpty()) {
                preferences.remove(SampleKey)
            } else {
                preferences[SampleKey] = encoded.joinToString("\n")
            }
        }
    }

    private fun preferences(): Flow<Preferences> = context.vocabDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    private companion object {
        val MasteredKey = stringSetPreferencesKey("mastered_ids")
        val SampleKey = stringPreferencesKey("sample_ids")

        fun decodeSample(raw: String?): List<String> =
            raw.orEmpty().lineSequence().map { id -> id.trim() }.filter { id -> id.isNotEmpty() }.toList()
    }
}
