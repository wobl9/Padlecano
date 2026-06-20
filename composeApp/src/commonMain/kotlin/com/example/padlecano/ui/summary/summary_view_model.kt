package com.example.padlecano.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.domain.model.EntityId
import com.example.padlecano.domain.repository.TournamentRepository
import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.SummarySortMode
import com.example.padlecano.domain.model.TournamentResultsPayload
import com.example.padlecano.domain.model.TournamentShareTextLabels
import com.example.padlecano.domain.usecase.AmericanoTournamentResults
import com.example.padlecano.domain.usecase.TournamentShareTextFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

data class SummaryUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val tournamentTitle: String = "",
    val sortMode: SummarySortMode = SummarySortMode.POINTS_DESC,
    val rows: List<PlayerStandingRow> = emptyList(),
)

class SummaryViewModel(
    private val tournamentId: EntityId,
    private val repository: TournamentRepository,
) : ViewModel() {
    private val sortMode: MutableStateFlow<SummarySortMode> =
        MutableStateFlow(SummarySortMode.POINTS_DESC)
    private val payload: MutableStateFlow<TournamentResultsPayload?> = MutableStateFlow(null)
    private val loadFailed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val loadFinished: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val loaded: TournamentResultsPayload? = repository.loadTournamentResultsPayload(tournamentId)
            if (loaded == null) {
                loadFailed.update { true }
            } else {
                payload.update { loaded }
            }
            loadFinished.update { true }
        }
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        sortMode,
        payload,
        loadFailed,
        loadFinished,
    ) { mode: SummarySortMode, data: TournamentResultsPayload?, failed: Boolean, finished: Boolean ->
        if (!finished) {
            return@combine SummaryUiState(isLoading = true)
        }
        if (failed || data == null) {
            return@combine SummaryUiState(isLoading = false, loadFailed = true)
        }
        val baseRows: List<PlayerStandingRow> = AmericanoTournamentResults.computeStandings(
            playerNames = data.playerDisplayNames,
            matches = data.matches,
        )
        val sorted: List<PlayerStandingRow> = sortStandings(rows = baseRows, mode = mode)
        SummaryUiState(
            isLoading = false,
            loadFailed = false,
            tournamentTitle = data.tournamentTitle,
            sortMode = mode,
            rows = sorted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = SummaryUiState(),
    )

    fun setSortMode(mode: SummarySortMode) {
        sortMode.update { mode }
    }

    fun buildShareText(labels: TournamentShareTextLabels): String? {
        val data: TournamentResultsPayload = payload.value ?: return null
        val currentRows: List<PlayerStandingRow> = uiState.value.rows
        if (currentRows.isEmpty() && data.rounds.isEmpty()) {
            return null
        }
        return TournamentShareTextFormatter.format(
            tournamentTitle = data.tournamentTitle,
            tournamentType = data.tournamentType,
            playerNames = data.playerDisplayNames,
            standingsRows = currentRows,
            rounds = data.rounds,
            labels = labels,
        )
    }
}

fun summaryViewModelFactory(
    tournamentId: EntityId,
    tournamentRepository: TournamentRepository,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            if (modelClass == SummaryViewModel::class) {
                @Suppress("UNCHECKED_CAST")
                return SummaryViewModel(
                    tournamentId = tournamentId,
                    repository = tournamentRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel type $modelClass")
        }
    }
}

private fun sortStandings(rows: List<PlayerStandingRow>, mode: SummarySortMode): List<PlayerStandingRow> {
    return when (mode) {
        SummarySortMode.POINTS_DESC -> rows.sortedWith(
            compareByDescending<PlayerStandingRow> { it.totalPoints }
                .thenByDescending { it.matchesPlayed }
                .thenByDescending { it.wins }
                .thenBy { it.displayName },
        )
        SummarySortMode.MATCHES_DESC -> rows.sortedWith(
            compareByDescending<PlayerStandingRow> { it.matchesPlayed }
                .thenByDescending { it.wins }
                .thenByDescending { it.totalPoints }
                .thenBy { it.displayName },
        )
    }
}
