package com.example.padlecano.ui.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.MatchScoreUpdate
import com.example.padlecano.domain.model.RoundState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScoreInput(val scoreA: String = "", val scoreB: String = "")

data class CourtUiModel(
    val matchId: Long,
    val courtNumber: Int,
    val teamAName1: String,
    val teamAName2: String,
    val teamBName1: String,
    val teamBName2: String,
)

data class ActiveGameUiState(
    val isLoading: Boolean = true,
    val tournamentTitle: String = "",
    val currentRoundNumber: Int = 0,
    val totalRounds: Int = 0,
    val courts: List<CourtUiModel> = emptyList(),
    val draftScores: Map<Long, ScoreInput> = emptyMap(),
    val isSaving: Boolean = false,
    val showScoreError: Boolean = false,
)

sealed interface ActiveGameEvent {
    data class NavigateToSummary(val tournamentId: Long) : ActiveGameEvent
}

class ActiveGameViewModel(
    private val tournamentId: Long,
    private val repository: TournamentRepository,
) : ViewModel() {

    private val draftScores: MutableStateFlow<Map<Long, ScoreInput>> = MutableStateFlow(emptyMap())
    private val isSaving: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val showScoreError: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val uiState: StateFlow<ActiveGameUiState> = combine(
        repository.observeActiveTournament(tournamentId),
        draftScores,
        isSaving,
        showScoreError,
    ) { tournamentState: ActiveTournamentState?, drafts: Map<Long, ScoreInput>, saving: Boolean, error: Boolean ->
        buildUiState(tournamentState = tournamentState, drafts = drafts, saving = saving, showScoreError = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = ActiveGameUiState(),
    )

    private val eventChannel: Channel<ActiveGameEvent> = Channel(capacity = Channel.BUFFERED)
    val events: Flow<ActiveGameEvent> = eventChannel.receiveAsFlow()

    fun updateScoreA(matchId: Long, value: String) {
        val filtered: String = value.filter { it.isDigit() }.take(3)
        draftScores.update { current ->
            current + (matchId to (current[matchId] ?: ScoreInput()).copy(scoreA = filtered))
        }
        showScoreError.value = false
    }

    fun updateScoreB(matchId: Long, value: String) {
        val filtered: String = value.filter { it.isDigit() }.take(3)
        draftScores.update { current ->
            current + (matchId to (current[matchId] ?: ScoreInput()).copy(scoreB = filtered))
        }
        showScoreError.value = false
    }

    fun confirmRound() {
        val state: ActiveGameUiState = uiState.value
        if (state.isLoading || state.isSaving || state.courts.isEmpty()) return
        val allFilled: Boolean = state.courts.all { court: CourtUiModel ->
            val draft: ScoreInput? = state.draftScores[court.matchId]
            draft != null && draft.scoreA.isNotBlank() && draft.scoreB.isNotBlank()
        }
        if (!allFilled) {
            showScoreError.value = true
            return
        }
        showScoreError.value = false
        val isLastRound: Boolean = state.currentRoundNumber == state.totalRounds
        viewModelScope.launch {
            isSaving.value = true
            try {
                val updates: List<MatchScoreUpdate> = state.courts.map { court: CourtUiModel ->
                    val draft: ScoreInput = state.draftScores.getValue(court.matchId)
                    MatchScoreUpdate(
                        matchId = court.matchId,
                        scoreA = draft.scoreA.toIntOrNull() ?: 0,
                        scoreB = draft.scoreB.toIntOrNull() ?: 0,
                    )
                }
                repository.saveMatchScores(scores = updates)
                if (isLastRound) {
                    repository.finishTournament(tournamentId = tournamentId)
                    eventChannel.send(element = ActiveGameEvent.NavigateToSummary(tournamentId = tournamentId))
                } else {
                    draftScores.value = emptyMap()
                }
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun buildUiState(
        tournamentState: ActiveTournamentState?,
        drafts: Map<Long, ScoreInput>,
        saving: Boolean,
        showScoreError: Boolean,
    ): ActiveGameUiState {
        if (tournamentState == null) {
            return ActiveGameUiState(isLoading = false)
        }
        val currentRound: RoundState? = tournamentState.rounds.firstOrNull { round: RoundState ->
            round.matches.any { !it.isScoreSet }
        }
        if (currentRound == null) {
            return ActiveGameUiState(
                isLoading = false,
                tournamentTitle = tournamentState.title,
                totalRounds = tournamentState.rounds.size,
            )
        }
        val courts: List<CourtUiModel> = currentRound.matches.mapIndexed { index: Int, match ->
            CourtUiModel(
                matchId = match.matchId,
                courtNumber = index + 1,
                teamAName1 = tournamentState.players.getOrElse(match.playerA1Index) { "P${match.playerA1Index + 1}" },
                teamAName2 = tournamentState.players.getOrElse(match.playerA2Index) { "P${match.playerA2Index + 1}" },
                teamBName1 = tournamentState.players.getOrElse(match.playerB1Index) { "P${match.playerB1Index + 1}" },
                teamBName2 = tournamentState.players.getOrElse(match.playerB2Index) { "P${match.playerB2Index + 1}" },
            )
        }
        return ActiveGameUiState(
            isLoading = false,
            tournamentTitle = tournamentState.title,
            currentRoundNumber = currentRound.roundNumber,
            totalRounds = tournamentState.rounds.size,
            courts = courts,
            draftScores = drafts,
            isSaving = saving,
            showScoreError = showScoreError,
        )
    }

    companion object {
        fun createFactory(tournamentId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val application = checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as PadlecanoApplication
                    if (modelClass.isAssignableFrom(ActiveGameViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ActiveGameViewModel(
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
