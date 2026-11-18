package com.davidsun.note.feature.vocab

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.VocabEntry
import com.davidsun.note.domain.repository.VocabRepository
import com.davidsun.note.domain.vocab.VocabPicker
import com.davidsun.note.domain.vocab.VocabSamples
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
class VocabListViewModel @Inject constructor(
    private val repository: VocabRepository,
    private val samples: VocabSamples,
    private val speaker: VocabSpeaker,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val entries = MutableStateFlow<List<VocabEntry>?>(null)
    private val failed = MutableStateFlow(false)
    private val sample = MutableStateFlow<List<String>?>(null)
    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val searchOpen = savedStateHandle.getStateFlow(SEARCH_KEY, false)
    private val showChinese = savedStateHandle.getStateFlow(CHINESE_KEY, true)
    private val showEnglish = savedStateHandle.getStateFlow(ENGLISH_KEY, true)

    val uiState: StateFlow<VocabListUiState> = combine(
        entries,
        failed,
        sample,
        repository.masteredIds(),
        combine(query, searchOpen, showChinese, showEnglish) { currentQuery, open, chinese, english ->
            ListChrome(currentQuery, open, chinese, english)
        },
    ) { loaded, loadFailed, currentSample, mastered, chrome ->
        mapState(loaded, loadFailed, currentSample, mastered, chrome)
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

    fun onSearchOpenChange(open: Boolean) {
        savedStateHandle[SEARCH_KEY] = open
        if (!open) savedStateHandle[QUERY_KEY] = ""
    }

    fun onToggleChinese() {
        val next = !showChinese.value
        if (!next && !showEnglish.value) savedStateHandle[ENGLISH_KEY] = true
        savedStateHandle[CHINESE_KEY] = next
    }

    fun onToggleEnglish() {
        val next = !showEnglish.value
        if (!next && !showChinese.value) savedStateHandle[CHINESE_KEY] = true
        savedStateHandle[ENGLISH_KEY] = next
    }

    fun onRefresh() {
        if (entries.value == null) return
        savedStateHandle[QUERY_KEY] = ""
        savedStateHandle[SEARCH_KEY] = false
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
        sample: List<String>?,
        mastered: Set<String>,
        chrome: ListChrome,
    ): VocabListUiState {
        if (loadFailed) return VocabListUiState(isLoading = false, loadFailed = true)
        if (loaded == null || sample == null) return VocabListUiState()
        val byId = loaded.associateBy { entry -> entry.id }
        val visible = if (chrome.query.isBlank()) {
            sample.take(VocabPicker.SAMPLE_SIZE).mapNotNull { id -> byId[id] }
        } else {
            VocabPicker.search(loaded, chrome.query)
        }
        val showChinese = chrome.showChinese || !chrome.showEnglish
        val showEnglish = chrome.showEnglish || !showChinese
        return VocabListUiState(
            isLoading = false,
            query = chrome.query,
            searchOpen = chrome.searchOpen,
            showChinese = showChinese,
            showEnglish = showEnglish,
            items = visible.map { entry ->
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
        )
    }

    override fun onCleared() {
        speaker.stop()
        super.onCleared()
    }

    private data class ListChrome(
        val query: String,
        val searchOpen: Boolean,
        val showChinese: Boolean,
        val showEnglish: Boolean,
    )

    private companion object {
        const val QUERY_KEY = "vocabQuery"
        const val SEARCH_KEY = "vocabSearchOpen"
        const val CHINESE_KEY = "vocabShowChinese"
        const val ENGLISH_KEY = "vocabShowEnglish"
    }
}
