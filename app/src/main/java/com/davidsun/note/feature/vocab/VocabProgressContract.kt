package com.davidsun.note.feature.vocab

data class VocabLevelProgressUi(
    val level: Int,
    val mastered: Int,
    val total: Int,
)

data class VocabProgressUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val mastered: Int = 0,
    val total: Int = 0,
    val levels: List<VocabLevelProgressUi> = emptyList(),
    val items: List<VocabItemUi> = emptyList(),
)

data class VocabProgressActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onMasteredChange: (String, Boolean) -> Unit,
    val onItemClick: (String) -> Unit,
)
