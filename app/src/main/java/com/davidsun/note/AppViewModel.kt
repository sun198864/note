package com.davidsun.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val fontScale: StateFlow<FontScale> = settingsRepository.fontScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontScale.MEDIUM)
}
