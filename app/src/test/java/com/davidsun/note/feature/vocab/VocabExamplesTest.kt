package com.davidsun.note.feature.vocab

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabExamplesTest {
    @Test
    fun pairsEachEnglishLineWithItsTranslation() {
        val pairs = pairedExamples(
            example = "1. Why did he abandon his family?\n2. They abandoned the plan.",
            exampleZh = "1. 他為什麼離棄家人？\n2. 他們中止了這個計畫。",
        )

        assertEquals(
            listOf(
                "Why did he abandon his family?" to "他為什麼離棄家人？",
                "They abandoned the plan." to "他們中止了這個計畫。",
            ),
            pairs,
        )
    }

    @Test
    fun eachLineDropsItsOwnNumber() {
        assertEquals(
            "They abandoned the plan.",
            exampleLineSpeech("2. They abandoned the plan."),
        )
    }

    @Test
    fun speechDropsSentenceNumbers() {
        assertEquals(
            "Why did he abandon his family? They abandoned the plan.",
            exampleSpeech("1. Why did he abandon his family?\n2. They abandoned the plan."),
        )
    }
}
