package com.davidsun.note.domain

import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.vocab.VocabProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class VocabProgressTest {
    @Test
    fun summaryCountsKnownWordsByLevel() {
        val entries = listOf(
            word("a", "abandon", level = 1),
            word("b", "ability", level = 1),
            word("c", "about", level = 2),
        )

        val summary = VocabProgress.summary(entries, masteredIds = setOf("a", "missing"))

        assertEquals(1, summary.mastered)
        assertEquals(3, summary.total)
        assertEquals(listOf(1 to "1/2", 2 to "0/1"), summary.levels.map { level ->
            level.level to "${level.mastered}/${level.total}"
        })
    }

    @Test
    fun checkedListSortsByLevelThenWord() {
        val entries = listOf(
            word("b", "zebra", level = 1),
            word("a", "apple", level = 2),
            word("c", "ant", level = 1),
            word("d", "other", level = 1),
        )

        val words = VocabProgress.checked(entries, setOf("a", "b", "c"), query = "")
            .map { entry -> entry.word }

        assertEquals(listOf("ant", "zebra", "apple"), words)
    }

    @Test
    fun searchMatchesHeadwordAndMeaning() {
        val entries = listOf(
            word("a", "abandon", meaning = "放棄"),
            word("b", "ability", meaning = "能力"),
            word("c", "other", meaning = "放棄其他"),
        )

        assertEquals(
            listOf("abandon"),
            VocabProgress.checked(entries, setOf("a", "b", "c"), query = "aban").map { it.word },
        )
        assertEquals(
            listOf("abandon", "other"),
            VocabProgress.checked(entries, setOf("a", "b", "c"), query = "放棄").map { it.word },
        )
    }

    private fun word(
        id: String,
        headword: String,
        level: Int = 1,
        meaning: String = "",
    ) = VocabEntry(
        id = id,
        word = headword,
        partOfSpeech = "n.",
        level = level,
        meaning = meaning,
        phonetic = "",
        definition = "",
        example = "",
    )
}
