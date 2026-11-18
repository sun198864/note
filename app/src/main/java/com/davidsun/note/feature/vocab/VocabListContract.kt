package com.davidsun.note.feature.vocab

data class VocabItemUi(
    val id: String,
    val word: String,
    val partOfSpeech: String,
    val meaning: String,
    val mastered: Boolean,
    val level: Int = 0,
    val toeic: Boolean = false,
)

data class VocabListUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val query: String = "",
    val searchOpen: Boolean = false,
    val showChinese: Boolean = true,
    val showEnglish: Boolean = true,
    val items: List<VocabItemUi> = emptyList(),
)

data class VocabListActions(
    val onQueryChange: (String) -> Unit,
    val onSearchOpenChange: (Boolean) -> Unit,
    val onRefresh: () -> Unit,
    val onMasteredChange: (String, Boolean) -> Unit,
    val onItemClick: (String) -> Unit,
    val onOpenProgress: () -> Unit,
    val onSpeak: (String) -> Unit,
    val onToggleChinese: () -> Unit,
    val onToggleEnglish: () -> Unit,
)
