package com.davidsun.note.feature.vocab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.vocab.VocabProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VocabProgressViewModel @Inject constructor(
    private val repository: VocabRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val entries = MutableStateFlow<List<VocabEntry>?>(null)
    private val failed = MutableStateFlow(false)
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")

    val uiState: StateFlow<VocabProgressUiState> = combine(
        entries,
        failed,
        query,
        repository.masteredIds(),
    ) { loaded, loadFailed, currentQuery, mastered ->
        mapState(loaded, loadFailed, currentQuery, mastered)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VocabProgressUiState(),
    )

    init {
        viewModelScope.launch {
            try {
                entries.value = repository.entries()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failed.value = true
            }
        }
    }

    fun onQueryChange(value: String) {
        savedStateHandle[QUERY_KEY] = value
    }

    fun onMasteredChange(id: String, mastered: Boolean) {
        viewModelScope.launch {
            try {
                repository.setMastered(id, mastered)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Unit
            }
        }
    }

    private fun mapState(
        loaded: List<VocabEntry>?,
        loadFailed: Boolean,
        currentQuery: String,
        mastered: Set<String>,
    ): VocabProgressUiState {
        if (loadFailed) return VocabProgressUiState(isLoading = false, loadFailed = true)
        if (loaded == null) return VocabProgressUiState()
        val summary = VocabProgress.summary(loaded, mastered)
        return VocabProgressUiState(
            isLoading = false,
            query = currentQuery,
            mastered = summary.mastered,
            total = summary.total,
            levels = summary.levels.map { level ->
                VocabLevelProgressUi(
                    level = level.level,
                    mastered = level.mastered,
                    total = level.total,
                )
            },
            items = VocabProgress.checked(loaded, mastered, currentQuery).map { entry ->
                VocabItemUi(
                    id = entry.id,
                    word = entry.word,
                    partOfSpeech = entry.partOfSpeech,
                    meaning = entry.meaning,
                    mastered = entry.id in mastered,
                )
            },
        )
    }

    private companion object {
        const val QUERY_KEY = "vocabProgressQuery"
    }
}
