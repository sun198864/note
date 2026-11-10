package com.davidsun.note.feature.vocab

data class VocabItemUi(
    val id: String,
    val word: String,
    val partOfSpeech: String,
    val meaning: String,
    val mastered: Boolean,
)

data class VocabListUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val items: List<VocabItemUi> = emptyList(),
)

data class VocabListActions(
    val onQueryChange: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onMasteredChange: (String, Boolean) -> Unit,
    val onItemClick: (String) -> Unit,
)
