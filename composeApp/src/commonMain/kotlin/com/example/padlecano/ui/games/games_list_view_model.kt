package com.example.padlecano.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.model.TournamentSummary
import com.example.padlecano.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

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
    fun deleteTournament(tournamentId: EntityId) {
        viewModelScope.launch {
            tournamentRepository.deleteTournament(tournamentId = tournamentId)
        }
    }
    fun deleteAllTournaments() {
        viewModelScope.launch {
            tournamentRepository.deleteAllTournaments()
        }
    }
}

fun gamesListViewModelFactory(tournamentRepository: TournamentRepository): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == GamesListViewModel::class) {
                @Suppress("UNCHECKED_CAST")
                return GamesListViewModel(tournamentRepository = tournamentRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel type $modelClass")
        }
    }
}
