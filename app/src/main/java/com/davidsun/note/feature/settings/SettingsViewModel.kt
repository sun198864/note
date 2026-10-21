package com.davidsun.note.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsun.note.core.common.AppDateFormatter
import com.davidsun.note.domain.model.FontScale
import com.davidsun.note.domain.repository.AuthRepository
import com.davidsun.note.domain.repository.CloudSyncCoordinator
import com.davidsun.note.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val cloudSync: CloudSyncCoordinator,
    private val dateFormatter: AppDateFormatter,
) : ViewModel() {
    private val signingIn = MutableStateFlow(false)
    private val signInFailed = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.fontScale,
        authRepository.account,
        signingIn,
        signInFailed,
        cloudSync.status,
    ) { scale, account, isSigningIn, failed, sync ->
        SettingsUiState(
            fontScale = scale,
            isSignedIn = account != null,
            accountLabel = account?.email?.takeIf { it.isNotBlank() } ?: account?.displayName,
            isSigningIn = isSigningIn,
            signInFailed = failed,
            isSyncing = sync.isSyncing,
            lastSyncedLabel = sync.lastSyncedAt?.let(dateFormatter::formatReminderDateTime),
            issue = sync.issue,
            switchedAccount = sync.switchedAccount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onFontScaleSelected(scale: FontScale) {
        viewModelScope.launch {
            settingsRepository.setFontScale(scale)
        }
    }

    fun onSignInAttempt(attempt: GoogleSignInAttempt) {
        if (signingIn.value) return
        viewModelScope.launch {
            when (attempt) {
                GoogleSignInAttempt.Cancelled -> signInFailed.update { false }
                GoogleSignInAttempt.Failed -> signInFailed.update { true }
                is GoogleSignInAttempt.Token -> {
                    signingIn.update { true }
                    signInFailed.update { false }
                    val signedIn = authRepository.signInWithGoogle(attempt.idToken)
                    signingIn.update { false }
                    signInFailed.update { !signedIn }
                }
            }
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            signInFailed.update { false }
            authRepository.signOut()
        }
    }
}
