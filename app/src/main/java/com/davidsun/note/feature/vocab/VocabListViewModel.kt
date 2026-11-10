package com.davidsun.note.feature.vocab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.vocab.VocabPicker
import com.davidsun.note.domain.vocab.VocabSamples
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
class VocabListViewModel @Inject constructor(
    private val repository: VocabRepository,
    private val samples: VocabSamples,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val entries = MutableStateFlow<List<VocabEntry>?>(null)
    private val failed = MutableStateFlow(false)
    private val sample = MutableStateFlow<List<String>?>(null)
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")

    val uiState: StateFlow<VocabListUiState> = combine(
        entries,
        failed,
        query,
        sample,
        repository.masteredIds(),
    ) { loaded, loadFailed, currentQuery, currentSample, mastered ->
        mapState(loaded, loadFailed, currentQuery, currentSample, mastered)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VocabListUiState(),
    )

    init {
        viewModelScope.launch {
            try {
                val loaded = repository.entries()
                sample.value = samples.ensure()
                entries.value = loaded
                repository.sampleIds().collect { ids -> sample.value = ids }
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

    fun onRefresh() {
        if (entries.value == null) return
        savedStateHandle[QUERY_KEY] = ""
        viewModelScope.launch {
            try {
                sample.value = samples.refresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Unit
            }
        }
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
        sample: List<String>?,
        mastered: Set<String>,
    ): VocabListUiState {
        if (loadFailed) return VocabListUiState(isLoading = false, loadFailed = true)
        if (loaded == null || sample == null) return VocabListUiState()
        val byId = loaded.associateBy { entry -> entry.id }
        val visible = if (currentQuery.isBlank()) {
            sample.take(VocabPicker.SAMPLE_SIZE).mapNotNull { id -> byId[id] }
        } else {
            VocabPicker.search(loaded, currentQuery)
        }
        return VocabListUiState(
            isLoading = false,
            query = currentQuery,
            items = visible.map { entry ->
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
        const val QUERY_KEY = "vocabQuery"
    }
}
