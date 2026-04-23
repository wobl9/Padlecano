package com.example.padlecano.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.TournamentSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GamesListUiState(
    val tournaments: List<TournamentSummary> = emptyList(),
)

class GamesListViewModel(
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {
    val uiState: StateFlow<GamesListUiState> = tournamentRepository.observeTournamentSummaries()
        .map { tournaments: List<TournamentSummary> ->
            GamesListUiState(tournaments = tournaments)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = GamesListUiState(),
        )
    companion object {
        fun createFactory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val application = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as PadlecanoApplication
                    if (modelClass.isAssignableFrom(GamesListViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return GamesListViewModel(application.tournamentRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel type $modelClass")
                }
            }
        }
    }
}
