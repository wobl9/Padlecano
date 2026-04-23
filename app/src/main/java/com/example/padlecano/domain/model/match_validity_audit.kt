package com.example.padlecano.domain.model

enum class MatchValidityIssue {
    INVALID_PLAYER_INDEX,
    DUPLICATE_PLAYERS_ON_COURT,
    SAME_PLAYER_TWICE_ON_TEAM_A,
    SAME_PLAYER_TWICE_ON_TEAM_B,
    NEGATIVE_SCORE,
    COMBINED_SCORE_EXCEEDS_LIMIT,
}

data class RawMatchForAudit(
    val matchId: Long,
    val playerA1Index: Int,
    val playerA2Index: Int,
    val playerB1Index: Int,
    val playerB2Index: Int,
    val scoreA: Int,
    val scoreB: Int,
    val isScoreSet: Boolean,
)

data class RawRoundForAudit(
    val roundNumber: Int,
    val matches: List<RawMatchForAudit>,
)

data class AuditedMatchRow(
    val matchId: Long,
    val roundNumber: Int,
    val courtNumber: Int,
    val teamAPairLabel: String,
    val teamBPairLabel: String,
    val scoreA: Int,
    val scoreB: Int,
    val isScoreEntered: Boolean,
    val issues: List<MatchValidityIssue>,
) {
    val isValid: Boolean = issues.isEmpty()
}

data class RoundValiditySection(
    val roundNumber: Int,
    val matches: List<AuditedMatchRow>,
)

data class MatchValidityAudit(
    val tournamentTitle: String,
    val maxCombinedMatchScore: Int,
    val rounds: List<RoundValiditySection>,
    val allMatchesValid: Boolean,
)
