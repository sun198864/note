package com.davidsun.note.domain

import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.vocab.VocabPicker
import com.davidsun.note.domain.vocab.vocabHeadwordForms
import com.davidsun.note.domain.vocab.vocabPronunciation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VocabPickerTest {
    @Test
    fun headwordFormsExpandSlashesAndSuffixes() {
        assertEquals(listOf("a", "an"), vocabHeadwordForms("a/an"))
        assertEquals(
            listOf("advertise", "advertisement", "ad"),
            vocabHeadwordForms("advertise(ment)/ad"),
        )
        assertEquals(listOf("chopstick", "chopsticks"), vocabHeadwordForms("chopstick(s)"))
        assertEquals(
            listOf("you", "your", "yours", "yourself", "yourselves"),
            vocabHeadwordForms("you (your, yours, yourself, yourselves)"),
        )
        assertEquals(listOf("am", "a.m."), vocabHeadwordForms("am/a.m."))
    }

    @Test
    fun pronunciationUsesTheFirstHeadword() {
        assertEquals("abandon", vocabPronunciation("abandon"))
        assertEquals("a", vocabPronunciation("a/an"))
        assertEquals("advertise", vocabPronunciation("advertise(ment)/ad"))
        assertEquals("you", vocabPronunciation("you (your, yours, yourself, yourselves)"))
        assertEquals("o'clock", vocabPronunciation("o’clock"))
    }

    @Test
    fun pickSkipsMasteredWordsAndFillsFifteen() {
        val entries = words(30)
        val mastered = setOf("w0", "w1", "w2")

        val picked = VocabPicker.pick(
            entries = entries,
            masteredIds = mastered,
            avoidIds = emptySet(),
            random = Random(1),
        )

        assertEquals(15, picked.size)
        assertTrue(picked.none { word -> word.id in mastered })
        assertEquals(picked.size, picked.map { word -> word.id }.distinct().size)
    }

    @Test
    fun pickReturnsEveryRemainingWordWhenFewerThanFifteen() {
        val entries = words(8)
        val picked = VocabPicker.pick(
            entries = entries,
            masteredIds = setOf("w0"),
            avoidIds = emptySet(),
            random = Random(2),
        )

        assertEquals(entries.drop(1).map { word -> word.id }.toSet(), picked.map { word -> word.id }.toSet())
    }

    @Test
    fun pickAvoidsTheCurrentSampleWhenEnoughWordsRemain() {
        val entries = words(40)
        val avoid = entries.take(15).map { word -> word.id }.toSet()

        val picked = VocabPicker.pick(
            entries = entries,
            masteredIds = emptySet(),
            avoidIds = avoid,
            random = Random(3),
        )

        assertEquals(15, picked.size)
        assertTrue(picked.none { word -> word.id in avoid })
    }

    @Test
    fun searchRanksExactFormsBeforePrefixes() {
        val entries = listOf(
            word("1", "planet"),
            word("2", "advertise(ment)/ad"),
            word("3", "address"),
            word("4", "apple"),
        )

        val found = VocabPicker.search(entries, "ad").map { entry -> entry.word }

        assertEquals(listOf("advertise(ment)/ad", "address"), found)
    }

    @Test
    fun searchMatchesAlternateSpellings() {
        val entries = listOf(word("1", "airplane/plane"), word("2", "plant"))

        val found = VocabPicker.search(entries, "plane").map { entry -> entry.id }

        assertEquals(listOf("1"), found)
    }

    private fun words(count: Int) = List(count) { index -> word("w$index", "word$index") }

    private fun word(id: String, headword: String) = VocabEntry(
        id = id,
        word = headword,
        partOfSpeech = "n.",
        level = 1,
        meaning = "",
        phonetic = "",
        definition = "",
        example = "",
    )
}
