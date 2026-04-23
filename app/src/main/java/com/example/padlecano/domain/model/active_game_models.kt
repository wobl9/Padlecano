package com.example.padlecano.domain.model

data class ActiveTournamentState(
    val tournamentId: Long,
    val title: String,
    val players: List<String>,
    val rounds: List<RoundState>,
)

data class RoundState(
    val roundId: Long,
    val roundNumber: Int,
    val matches: List<MatchState>,
)

data class MatchState(
    val matchId: Long,
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int,
    val scoreB: Int,
    val isScoreSet: Boolean,
)

data class MatchScoreUpdate(
    val matchId: Long,
    val scoreA: Int,
    val scoreB: Int,
)
