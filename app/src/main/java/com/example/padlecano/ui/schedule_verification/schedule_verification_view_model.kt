package com.example.padlecano.ui.schedule_verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.ScheduleVerificationReport
import com.example.padlecano.domain.usecase.AmericanoTournamentResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleVerificationUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val report: ScheduleVerificationReport? = null,
)

class ScheduleVerificationViewModel(
    private val tournamentId: Long,
    private val repository: TournamentRepository,
) : ViewModel() {
    private val state: MutableStateFlow<ScheduleVerificationUiState> =
        MutableStateFlow(ScheduleVerificationUiState())
    val uiState: StateFlow<ScheduleVerificationUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val payload = repository.loadTournamentResultsPayload(tournamentId)
            if (payload == null) {
                state.update { current: ScheduleVerificationUiState ->
                    current.copy(isLoading = false, loadFailed = true)
                }
                return@launch
            }
            val report: ScheduleVerificationReport =
                AmericanoTournamentResults.verifyScheduleAgainstIdeal(
                    playerNames = payload.playerDisplayNames,
                    allScheduledMatches = payload.matches,
                )
            state.update { current: ScheduleVerificationUiState ->
                current.copy(isLoading = false, loadFailed = false, report = report)
            }
        }
    }

    companion object {
        fun createFactory(tournamentId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val application: PadlecanoApplication = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as PadlecanoApplication
                    if (modelClass.isAssignableFrom(ScheduleVerificationViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ScheduleVerificationViewModel(
                            tournamentId = tournamentId,
                            repository = application.tournamentRepository,
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel type $modelClass")
                }
            }
        }
    }
}
