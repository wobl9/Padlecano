package com.example.padlecano.domain.model

/**
 * One row in the final standings table for an Americano tournament.
 */
data class PlayerStandingRow(
    val playerIndex: Int,
    val displayName: String,
    val totalPoints: Int,
    val matchesPlayed: Int,
    val wins: Int,
)

enum class SummarySortMode {
    POINTS_DESC,
    MATCHES_DESC,
}

/**
 * How often two players were teammates or opponents in stored matches vs the ideal schedule.
 */
data class OtherPlayerScheduleCheck(
    val otherPlayerIndex: Int,
    val timesAsPartner: Int,
    val expectedTimesAsPartner: Int,
    val timesAsOpponent: Int,
    val expectedTimesAsOpponent: Int,
) {
    val partnerOk: Boolean = timesAsPartner == expectedTimesAsPartner
    val opponentOk: Boolean = timesAsOpponent == expectedTimesAsOpponent
}

data class PlayerScheduleVerification(
    val playerIndex: Int,
    val withEachOtherPlayer: List<OtherPlayerScheduleCheck>,
)

data class ScheduleVerificationReport(
    val playerNames: List<String>,
    val byPlayer: List<PlayerScheduleVerification>,
    val allOk: Boolean,
)

/**
 * Match lineup and scores as stored for a tournament (no Room dependency).
 */
data class TournamentMatchRecord(
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int,
    val scoreB: Int,
    val isScoreSet: Boolean,
)

data class TournamentRoundResults(
    val roundNumber: Int,
    val matches: List<TournamentMatchRecord>,
)

data class TournamentResultsPayload(
    val tournamentTitle: String,
    val tournamentType: TournamentType,
    val playerDisplayNames: List<String>,
    val matches: List<TournamentMatchRecord>,
    val rounds: List<TournamentRoundResults>,
)
