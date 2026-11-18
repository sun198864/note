package com.davidsun.note.feature.vocab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.vocab.VocabProgress
import com.davidsun.note.domain.vocab.VocabSpeaker
import com.davidsun.note.domain.vocab.vocabPronunciation
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
    private val speaker: VocabSpeaker,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val entries = MutableStateFlow<List<VocabEntry>?>(null)
    private val failed = MutableStateFlow(false)
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val barsExpanded = savedStateHandle.getStateFlow(BARS_KEY, true)

    val uiState: StateFlow<VocabProgressUiState> = combine(
        entries,
        failed,
        query,
        barsExpanded,
        repository.masteredIds(),
    ) { loaded, loadFailed, currentQuery, expanded, mastered ->
        mapState(loaded, loadFailed, currentQuery, expanded, mastered)
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

    fun onToggleBars() {
        savedStateHandle[BARS_KEY] = !barsExpanded.value
    }

    fun onSpeak(word: String) {
        val spoken = vocabPronunciation(word)
        if (spoken.isBlank()) return
        speaker.speak(spoken)
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
        expanded: Boolean,
        mastered: Set<String>,
    ): VocabProgressUiState {
        if (loadFailed) {
            return VocabProgressUiState(isLoading = false, loadFailed = true, barsExpanded = expanded)
        }
        if (loaded == null) return VocabProgressUiState(barsExpanded = expanded)
        val summary = VocabProgress.summary(loaded, mastered)
        return VocabProgressUiState(
            isLoading = false,
            query = currentQuery,
            mastered = summary.mastered,
            total = summary.total,
            toeicMastered = summary.toeicMastered,
            toeicTotal = summary.toeicTotal,
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
                    level = entry.level,
                    toeic = entry.toeic,
                )
            },
            barsExpanded = expanded,
        )
    }

    override fun onCleared() {
        speaker.stop()
        super.onCleared()
    }

    private companion object {
        const val QUERY_KEY = "vocabProgressQuery"
        const val BARS_KEY = "vocabProgressBars"
    }
}
