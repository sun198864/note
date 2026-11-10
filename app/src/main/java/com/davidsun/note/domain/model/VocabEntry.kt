package com.davidsun.note.domain.model

data class VocabEntry(
    val id: String,
    val word: String,
    val partOfSpeech: String,
    val level: Int,
    val meaning: String,
    val phonetic: String,
    val definition: String,
    val example: String,
)
