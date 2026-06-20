package com.example.padlecano.ui.match_validity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.repository.TournamentRepository
import com.example.padlecano.domain.model.MatchValidityAudit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

data class MatchValidityUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val audit: MatchValidityAudit? = null,
)

class MatchValidityViewModel(
    private val tournamentId: EntityId,
    private val repository: TournamentRepository,
) : ViewModel() {
    private val state: MutableStateFlow<MatchValidityUiState> = MutableStateFlow(MatchValidityUiState())
    val uiState: StateFlow<MatchValidityUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            val audit: MatchValidityAudit? = repository.loadMatchValidityAudit(tournamentId)
            if (audit == null) {
                state.update { current: MatchValidityUiState ->
                    current.copy(isLoading = false, loadFailed = true)
                }
                return@launch
            }
            state.update { current: MatchValidityUiState ->
                current.copy(isLoading = false, loadFailed = false, audit = audit)
            }
        }
    }
}

fun matchValidityViewModelFactory(
    tournamentId: EntityId,
    tournamentRepository: TournamentRepository,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == MatchValidityViewModel::class) {
                @Suppress("UNCHECKED_CAST")
                return MatchValidityViewModel(
                    tournamentId = tournamentId,
                    repository = tournamentRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel type $modelClass")
        }
    }
}
