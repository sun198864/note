package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.VocabEntry
import kotlinx.coroutines.flow.Flow

interface VocabRepository {
    suspend fun entries(): List<VocabEntry>

    fun masteredIds(): Flow<Set<String>>

    suspend fun setMastered(id: String, mastered: Boolean)

    fun sampleIds(): Flow<List<String>>

    suspend fun setSampleIds(ids: List<String>)
}
