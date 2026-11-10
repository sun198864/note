package com.davidsun.note.domain.vocab

import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VocabSamples @Inject constructor(
    private val repository: VocabRepository,
) {
    private val lock = Mutex()

    suspend fun ensure(random: Random = Random.Default): List<String> = lock.withLock {
        val loaded = repository.entries()
        val known = loaded.map { entry -> entry.id }.toSet()
        val saved = repository.sampleIds().first()
        val valid = saved.filter { id -> id in known }.distinct().take(VocabPicker.SAMPLE_SIZE)
        if (valid.isNotEmpty()) {
            if (valid != saved) repository.setSampleIds(valid)
            return@withLock valid
        }
        val picked = pick(loaded, avoid = emptySet(), random = random)
        repository.setSampleIds(picked)
        picked
    }

    suspend fun refresh(random: Random = Random.Default): List<String> = lock.withLock {
        val loaded = repository.entries()
        val avoid = repository.sampleIds().first().toSet()
        val picked = pick(loaded, avoid, random)
        repository.setSampleIds(picked)
        picked
    }

    private suspend fun pick(
        loaded: List<VocabEntry>,
        avoid: Set<String>,
        random: Random,
    ): List<String> = VocabPicker.pick(
        entries = loaded,
        masteredIds = repository.masteredIds().first(),
        avoidIds = avoid,
        random = random,
    ).map { entry -> entry.id }
}
