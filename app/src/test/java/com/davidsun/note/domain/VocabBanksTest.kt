package com.davidsun.note.domain

import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.vocab.mergeVocabBanks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabBanksTest {
    @Test
    fun overlapKeepsItsLevelAndToeicOnlyWordsAreAdded() {
        val merged = mergeVocabBanks(
            levels = listOf(entry(id = "lv", word = "agenda", level = 5)),
            toeic = listOf(
                entry(id = "", word = "agenda"),
                entry(id = "new", word = "invoice", meaning = "發票"),
            ),
        )

        assertEquals(listOf("agenda", "invoice"), merged.map { item -> item.word })
        assertEquals(5, merged[0].level)
        assertTrue(merged[0].toeic)
        assertEquals(0, merged[1].level)
        assertTrue(merged[1].toeic)
        assertFalse(merged[0].id.isBlank())
    }

    @Test
    fun incompleteToeicStubIsNotAdded() {
        val merged = mergeVocabBanks(
            levels = emptyList(),
            toeic = listOf(entry(id = "", word = "invoice")),
        )

        assertTrue(merged.isEmpty())
    }

    private fun entry(
        id: String,
        word: String,
        level: Int = 0,
        meaning: String = "",
    ) = VocabEntry(
        id = id,
        word = word,
        partOfSpeech = "n.",
        level = level,
        meaning = meaning,
        phonetic = "",
        definition = "",
        example = "",
    )
}
