package com.davidsun.note.domain.vocab

import com.davidsun.note.domain.model.VocabEntry

data class VocabLevelCount(
    val level: Int,
    val mastered: Int,
    val total: Int,
)

data class VocabProgressSummary(
    val mastered: Int,
    val total: Int,
    val levels: List<VocabLevelCount>,
    val toeicMastered: Int,
    val toeicTotal: Int,
)

object VocabProgress {
    fun summary(entries: List<VocabEntry>, masteredIds: Set<String>): VocabProgressSummary {
        val knownIds = entries.map { entry -> entry.id }.toSet()
        val mastered = masteredIds.intersect(knownIds)
        val graded = entries.filter { entry -> entry.level in 1..6 }
        val totals = graded.groupingBy { entry -> entry.level }.eachCount()
        val done = graded
            .filter { entry -> entry.id in mastered }
            .groupingBy { entry -> entry.level }
            .eachCount()
        val levels = totals.keys.sorted().map { level ->
            VocabLevelCount(
                level = level,
                mastered = done[level] ?: 0,
                total = totals.getValue(level),
            )
        }
        val toeic = entries.filter { entry -> entry.toeic }
        return VocabProgressSummary(
            mastered = graded.count { entry -> entry.id in mastered },
            total = graded.size,
            levels = levels,
            toeicMastered = toeic.count { entry -> entry.id in mastered },
            toeicTotal = toeic.size,
        )
    }

    fun checked(
        entries: List<VocabEntry>,
        masteredIds: Set<String>,
        query: String,
    ): List<VocabEntry> {
        val mastered = entries.filter { entry -> entry.id in masteredIds }
        val needle = query.trim()
        if (needle.isEmpty()) {
            return mastered.sortedWith(
                compareBy({ entry -> levelOrder(entry) }, { entry -> entry.word.lowercase() }),
            )
        }
        val ranked = VocabPicker.search(mastered, needle)
        val rankedIds = ranked.map { entry -> entry.id }.toSet()
        val byMeaning = mastered
            .filter { entry -> entry.id !in rankedIds && entry.meaning.contains(needle, ignoreCase = true) }
            .sortedBy { entry -> entry.word.lowercase() }
        return ranked + byMeaning
    }

    private fun levelOrder(entry: VocabEntry): Int =
        if (entry.level in 1..6) entry.level else Int.MAX_VALUE
}
