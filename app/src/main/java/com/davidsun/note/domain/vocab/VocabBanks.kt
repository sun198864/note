package com.davidsun.note.domain.vocab

import com.davidsun.note.domain.model.VocabEntry

internal fun mergeVocabBanks(
    levels: List<VocabEntry>,
    toeic: List<VocabEntry>,
): List<VocabEntry> {
    val toeicForms = toeic.flatMap { entry -> vocabHeadwordForms(entry.word) }
        .map { form -> form.lowercase() }
        .toSet()
    val marked = levels.map { entry ->
        val forms = vocabHeadwordForms(entry.word).map { form -> form.lowercase() }
        if (forms.any { form -> form in toeicForms }) entry.copy(toeic = true) else entry
    }
    val known = marked.flatMap { entry -> vocabHeadwordForms(entry.word) }
        .map { form -> form.lowercase() }
        .toSet()
    val added = toeic.mapNotNull { entry ->
        val forms = vocabHeadwordForms(entry.word).map { form -> form.lowercase() }
        if (forms.any { form -> form in known }) return@mapNotNull null
        if (entry.id.isBlank() || entry.meaning.isBlank()) return@mapNotNull null
        entry.copy(level = 0, toeic = true)
    }
    return marked + added
}
