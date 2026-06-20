package com.example.padlecano.ui.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.padlecano.PadlecanoApplication
import com.example.padlecano.data.repository.TournamentRepository
import com.example.padlecano.domain.model.ActiveTournamentState
import com.example.padlecano.domain.model.EntityId
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
    val matchId: EntityId,
    val courtNumber: Int,
    val teamAName1: String,
    val teamAName2: String,
    val teamBName1: String,
    val teamBName2: String,
)

data class ActiveGameUiState(
    val isLoading: Boolean = true,
    val tournamentTitle: String = "",
    val maxCombinedMatchScore: Int = 0,
    val currentRoundNumber: Int = 0,
    val totalRounds: Int = 0,
    val courts: List<CourtUiModel> = emptyList(),
    val draftScores: Map<EntityId, ScoreInput> = emptyMap(),
    val isSaving: Boolean = false,
    val allScoreFieldsFilled: Boolean = false,
    val combinedScoreExceedsLimit: Boolean = false,
    val showMissingScoresMessage: Boolean = false,
)

sealed interface ActiveGameEvent {
    data class NavigateToSummary(val tournamentId: EntityId) : ActiveGameEvent
}

class ActiveGameViewModel(
    private val tournamentId: EntityId,
    private val repository: TournamentRepository,
) : ViewModel() {

    private val draftScores: MutableStateFlow<Map<EntityId, ScoreInput>> = MutableStateFlow(emptyMap())
    private val isSaving: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val showMissingScoresMessage: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val uiState: StateFlow<ActiveGameUiState> = combine(
        repository.observeActiveTournament(tournamentId),
        draftScores,
        isSaving,
        showMissingScoresMessage,
    ) { tournamentState: ActiveTournamentState?, drafts: Map<EntityId, ScoreInput>, saving: Boolean, missingMessage: Boolean ->
        buildUiState(
            tournamentState = tournamentState,
            drafts = drafts,
            saving = saving,
            showMissingScoresMessage = missingMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = ActiveGameUiState(),
    )

    private val eventChannel: Channel<ActiveGameEvent> = Channel(capacity = Channel.BUFFERED)
    val events: Flow<ActiveGameEvent> = eventChannel.receiveAsFlow()

    fun updateScoreA(matchId: EntityId, value: String) {
        val filtered: String = value.filter { it.isDigit() }.take(3)
        val maxCombined: Int = uiState.value.maxCombinedMatchScore
        draftScores.update { current: Map<EntityId, ScoreInput> ->
            val previous: ScoreInput = current[matchId] ?: ScoreInput()
            val next: ScoreInput = afterEditingScoreA(
                maxCombinedMatchScore = maxCombined,
                previous = previous,
                newScoreA = filtered,
            )
            current + (matchId to next)
        }
        showMissingScoresMessage.value = false
    }

    fun updateScoreB(matchId: EntityId, value: String) {
        val filtered: String = value.filter { it.isDigit() }.take(3)
        val maxCombined: Int = uiState.value.maxCombinedMatchScore
        draftScores.update { current: Map<EntityId, ScoreInput> ->
            val previous: ScoreInput = current[matchId] ?: ScoreInput()
            val next: ScoreInput = afterEditingScoreB(
                maxCombinedMatchScore = maxCombined,
                previous = previous,
                newScoreB = filtered,
            )
            current + (matchId to next)
        }
        showMissingScoresMessage.value = false
    }

    fun confirmRound() {
        val state: ActiveGameUiState = uiState.value
        if (state.isLoading || state.isSaving || state.courts.isEmpty()) return
        if (!state.allScoreFieldsFilled) {
            showMissingScoresMessage.value = true
            return
        }
        if (state.combinedScoreExceedsLimit) {
            return
        }
        showMissingScoresMessage.value = false
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

    private fun afterEditingScoreA(
        maxCombinedMatchScore: Int,
        previous: ScoreInput,
        newScoreA: String,
    ): ScoreInput {
        if (maxCombinedMatchScore <= 0) {
            return ScoreInput(scoreA = newScoreA, scoreB = previous.scoreB)
        }
        if (newScoreA.isEmpty()) {
            return ScoreInput(scoreA = "", scoreB = "")
        }
        val scoreBStr: String = previous.scoreB
        val aParsed: Int = newScoreA.toIntOrNull()
            ?: return ScoreInput(scoreA = newScoreA, scoreB = scoreBStr)
        if (scoreBStr.isEmpty()) {
            if (newScoreA.length == 1) {
                val complement: Int = maxCombinedMatchScore - aParsed
                return if (complement in MIN_AUTO_SCORE..MAX_AUTO_SCORE) {
                    ScoreInput(scoreA = newScoreA, scoreB = complement.toString())
                } else {
                    ScoreInput(scoreA = newScoreA, scoreB = "")
                }
            }
            if (aParsed > maxCombinedMatchScore) {
                return ScoreInput(
                    scoreA = maxCombinedMatchScore.coerceAtMost(MAX_AUTO_SCORE).toString(),
                    scoreB = "",
                )
            }
            return ScoreInput(scoreA = newScoreA, scoreB = "")
        }
        val bParsed: Int = scoreBStr.toIntOrNull()
            ?: return ScoreInput(scoreA = newScoreA, scoreB = scoreBStr)
        if (aParsed + bParsed <= maxCombinedMatchScore) {
            return ScoreInput(scoreA = newScoreA, scoreB = scoreBStr)
        }
        var nextA: Int = aParsed
        var nextB: Int = maxCombinedMatchScore - nextA
        if (nextB < MIN_AUTO_SCORE) {
            nextA = maxCombinedMatchScore.coerceAtMost(MAX_AUTO_SCORE)
            nextB = MIN_AUTO_SCORE
        }
        nextB = nextB.coerceIn(MIN_AUTO_SCORE, MAX_AUTO_SCORE)
        return ScoreInput(scoreA = nextA.toString(), scoreB = nextB.toString())
    }

    private fun afterEditingScoreB(
        maxCombinedMatchScore: Int,
        previous: ScoreInput,
        newScoreB: String,
    ): ScoreInput {
        if (maxCombinedMatchScore <= 0) {
            return ScoreInput(scoreA = previous.scoreA, scoreB = newScoreB)
        }
        if (newScoreB.isEmpty()) {
            return ScoreInput(scoreA = "", scoreB = "")
        }
        val scoreAStr: String = previous.scoreA
        val bParsed: Int = newScoreB.toIntOrNull()
            ?: return ScoreInput(scoreA = scoreAStr, scoreB = newScoreB)
        if (scoreAStr.isEmpty()) {
            if (newScoreB.length == 1) {
                val complement: Int = maxCombinedMatchScore - bParsed
                return if (complement in MIN_AUTO_SCORE..MAX_AUTO_SCORE) {
                    ScoreInput(scoreA = complement.toString(), scoreB = newScoreB)
                } else {
                    ScoreInput(scoreA = "", scoreB = newScoreB)
                }
            }
            if (bParsed > maxCombinedMatchScore) {
                return ScoreInput(
                    scoreA = "",
                    scoreB = maxCombinedMatchScore.coerceAtMost(MAX_AUTO_SCORE).toString(),
                )
            }
            return ScoreInput(scoreA = "", scoreB = newScoreB)
        }
        val aParsed: Int = scoreAStr.toIntOrNull()
            ?: return ScoreInput(scoreA = scoreAStr, scoreB = newScoreB)
        if (aParsed + bParsed <= maxCombinedMatchScore) {
            return ScoreInput(scoreA = scoreAStr, scoreB = newScoreB)
        }
        var nextB: Int = bParsed
        var nextA: Int = maxCombinedMatchScore - nextB
        if (nextA < MIN_AUTO_SCORE) {
            nextB = maxCombinedMatchScore.coerceAtMost(MAX_AUTO_SCORE)
            nextA = MIN_AUTO_SCORE
        }
        nextA = nextA.coerceIn(MIN_AUTO_SCORE, MAX_AUTO_SCORE)
        return ScoreInput(scoreA = nextA.toString(), scoreB = nextB.toString())
    }

    private fun buildUiState(
        tournamentState: ActiveTournamentState?,
        drafts: Map<EntityId, ScoreInput>,
        saving: Boolean,
        showMissingScoresMessage: Boolean,
    ): ActiveGameUiState {
        if (tournamentState == null) {
            return ActiveGameUiState(isLoading = false)
        }
        val maxCombined: Int = tournamentState.maxCombinedMatchScore
        val currentRound: RoundState? = tournamentState.rounds.firstOrNull { round: RoundState ->
            round.matches.any { !it.isScoreSet }
        }
        if (currentRound == null) {
            return ActiveGameUiState(
                isLoading = false,
                tournamentTitle = tournamentState.title,
                maxCombinedMatchScore = maxCombined,
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
        val allFilled: Boolean = courts.all { court: CourtUiModel ->
            val draft: ScoreInput? = drafts[court.matchId]
            draft != null && draft.scoreA.isNotBlank() && draft.scoreB.isNotBlank()
        }
        val exceedsLimit: Boolean = courts.any { court: CourtUiModel ->
            val draft: ScoreInput? = drafts[court.matchId]
            if (draft == null || draft.scoreA.isBlank() || draft.scoreB.isBlank()) {
                false
            } else {
                val a: Int = draft.scoreA.toIntOrNull() ?: 0
                val b: Int = draft.scoreB.toIntOrNull() ?: 0
                a + b > maxCombined
            }
        }
        val showMissing: Boolean = showMissingScoresMessage && !exceedsLimit
        return ActiveGameUiState(
            isLoading = false,
            tournamentTitle = tournamentState.title,
            maxCombinedMatchScore = maxCombined,
            currentRoundNumber = currentRound.roundNumber,
            totalRounds = tournamentState.rounds.size,
            courts = courts,
            draftScores = drafts,
            isSaving = saving,
            allScoreFieldsFilled = allFilled,
            combinedScoreExceedsLimit = exceedsLimit,
            showMissingScoresMessage = showMissing,
        )
    }

    companion object {
        private const val MIN_AUTO_SCORE: Int = 0
        private const val MAX_AUTO_SCORE: Int = 999
        fun createFactory(tournamentId: EntityId): ViewModelProvider.Factory {
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
