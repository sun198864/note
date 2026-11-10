package com.davidsun.note.domain

import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.vocab.VocabPicker
import com.davidsun.note.domain.vocab.VocabSamples
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class VocabSamplesTest {
    private val repository = FakeVocabRepository(words(30))
    private val samples = VocabSamples(repository)

    @Test
    fun ensureKeepsTheSameFifteenWords() = runBlocking {
        val first = samples.ensure(Random(1))
        val second = samples.ensure(Random(2))

        assertEquals(VocabPicker.SAMPLE_SIZE, first.size)
        assertEquals(first, second)
        assertEquals(first, repository.sampleIds.value)
    }

    @Test
    fun ensureDropsUnknownIdsAndKeepsOrder() = runBlocking {
        repository.sampleIds.value = listOf("missing", "w2", "w0", "w2")

        assertEquals(listOf("w2", "w0"), samples.ensure(Random(1)))
    }

    @Test
    fun refreshReplacesTheSharedSample() = runBlocking {
        val first = samples.ensure(Random(1))

        val next = samples.refresh(Random(2))

        assertEquals(VocabPicker.SAMPLE_SIZE, next.size)
        assertEquals(next, repository.sampleIds.value)
        assertEquals(emptySet<String>(), first.toSet().intersect(next.toSet()))
    }

    private fun words(count: Int) = List(count) { index ->
        VocabEntry(
            id = "w$index",
            word = "word$index",
            partOfSpeech = "n.",
            level = 1,
            meaning = "",
            phonetic = "",
            definition = "",
            example = "",
        )
    }

    private class FakeVocabRepository(
        private val entries: List<VocabEntry>,
    ) : VocabRepository {
        val sampleIds = MutableStateFlow<List<String>>(emptyList())
        private val masteredIds = MutableStateFlow<Set<String>>(emptySet())

        override suspend fun entries(): List<VocabEntry> = entries

        override fun masteredIds(): Flow<Set<String>> = masteredIds

        override suspend fun setMastered(id: String, mastered: Boolean) {
            masteredIds.value = if (mastered) masteredIds.value + id else masteredIds.value - id
        }

        override fun sampleIds(): Flow<List<String>> = sampleIds

        override suspend fun setSampleIds(ids: List<String>) {
            sampleIds.value = ids
        }
    }
}
