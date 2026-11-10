package com.davidsun.note.domain.vocab

import com.davidsun.note.domain.model.VocabEntry
import kotlin.random.Random

object VocabPicker {
    const val SAMPLE_SIZE = 15

    fun pick(
        entries: List<VocabEntry>,
        masteredIds: Set<String>,
        avoidIds: Set<String>,
        size: Int = SAMPLE_SIZE,
        random: Random,
    ): List<VocabEntry> {
        val pool = entries.filter { entry -> entry.id !in masteredIds }
        if (pool.size <= size) return pool.shuffled(random)
        val preferred = pool.filter { entry -> entry.id !in avoidIds }
        if (preferred.size >= size) return sample(preferred, size, random)
        val fill = sample(
            items = pool.filter { entry -> entry.id in avoidIds },
            count = size - preferred.size,
            random = random,
        )
        return (preferred + fill).shuffled(random)
    }

    fun search(entries: List<VocabEntry>, query: String): List<VocabEntry> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return entries
            .mapNotNull { entry -> rank(entry, needle)?.let { score -> score to entry } }
            .sortedWith(compareBy({ pair -> pair.first }, { pair -> pair.second.word.lowercase() }))
            .map { pair -> pair.second }
    }

    private fun rank(entry: VocabEntry, needle: String): Int? {
        val forms = vocabHeadwordForms(entry.word)
        return when {
            forms.any { form -> form.equals(needle, ignoreCase = true) } -> 0
            forms.any { form -> form.startsWith(needle, ignoreCase = true) } -> 1
            forms.any { form -> form.contains(needle, ignoreCase = true) } -> 2
            else -> null
        }
    }

    private fun <T> sample(items: List<T>, count: Int, random: Random): List<T> {
        if (count >= items.size) return items.shuffled(random)
        val pool = items.toMutableList()
        return List(count) { pool.removeAt(random.nextInt(pool.size)) }
    }
}
