package com.example.padlecano.domain.model

data class ActiveTournamentState(
    val tournamentId: EntityId,
    val title: String,
    val players: List<String>,
    val maxCombinedMatchScore: Int,
    val rounds: List<RoundState>,
)

data class RoundState(
    val roundId: EntityId,
    val roundNumber: Int,
    val matches: List<MatchState>,
)

data class MatchState(
    val matchId: EntityId,
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int,
    val scoreB: Int,
    val isScoreSet: Boolean,
)

data class MatchScoreUpdate(
    val matchId: EntityId,
    val scoreA: Int,
    val scoreB: Int,
)
