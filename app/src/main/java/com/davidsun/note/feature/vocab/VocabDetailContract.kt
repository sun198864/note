package com.davidsun.note.feature.vocab

data class VocabDetailUiState(
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val word: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val level: Int = 0,
    val meaning: String = "",
    val definition: String = "",
    val example: String = "",
    val mastered: Boolean = false,
)

data class VocabDetailActions(
    val onBack: () -> Unit,
    val onMasteredChange: (Boolean) -> Unit,
    val onSpeak: () -> Unit,
)
