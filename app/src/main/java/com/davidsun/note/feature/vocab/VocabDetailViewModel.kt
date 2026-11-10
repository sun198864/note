package com.davidsun.note.feature.vocab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
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
class VocabDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VocabRepository,
    private val speaker: VocabSpeaker,
) : ViewModel() {
    private val vocabId: String = savedStateHandle.get<String>(VOCAB_ID).orEmpty()
    private val entry = MutableStateFlow<VocabEntry?>(null)
    private val missing = MutableStateFlow(false)

    val uiState: StateFlow<VocabDetailUiState> = combine(
        entry,
        missing,
        repository.masteredIds(),
    ) { item, isMissing, mastered ->
        when {
            isMissing -> VocabDetailUiState(isLoading = false, missing = true)
            item == null -> VocabDetailUiState()
            else -> VocabDetailUiState(
                isLoading = false,
                word = item.word,
                phonetic = item.phonetic,
                partOfSpeech = item.partOfSpeech,
                level = item.level,
                meaning = item.meaning,
                definition = item.definition,
                example = item.example,
                mastered = item.id in mastered,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VocabDetailUiState(),
    )

    init {
        viewModelScope.launch {
            try {
                val found = repository.entries().find { item -> item.id == vocabId }
                if (found == null) missing.value = true else entry.value = found
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                missing.value = true
            }
        }
    }

    fun onSpeak() {
        val spoken = vocabPronunciation(entry.value?.word.orEmpty())
        if (spoken.isBlank()) return
        speaker.speak(spoken)
    }

    fun onMasteredChange(mastered: Boolean) {
        if (vocabId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.setMastered(vocabId, mastered)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Unit
            }
        }
    }

    override fun onCleared() {
        speaker.stop()
        super.onCleared()
    }

    private companion object {
        const val VOCAB_ID = "vocabId"
    }
}
