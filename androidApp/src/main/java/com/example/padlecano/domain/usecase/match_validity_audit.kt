package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.model.AuditedMatchRow
import com.example.padlecano.domain.model.MatchValidityAudit
import com.example.padlecano.domain.model.MatchValidityIssue
import com.example.padlecano.domain.model.RawMatchForAudit
import com.example.padlecano.domain.model.RawRoundForAudit
import com.example.padlecano.domain.model.RoundValiditySection

/**
 * Builds a per-match audit: partner pairs, scores, and simple validity rules.
 */
object MatchValidityAuditBuilder {
    fun build(
        tournamentTitle: String,
        maxCombinedMatchScore: Int,
        playerDisplayNames: List<String>,
        rounds: List<RawRoundForAudit>,
    ): MatchValidityAudit {
        val sections: List<RoundValiditySection> = rounds.map { round: RawRoundForAudit ->
            val auditedMatches: List<AuditedMatchRow> = round.matches.mapIndexed { courtIndex: Int, raw: RawMatchForAudit ->
                auditSingleMatch(
                    maxCombinedMatchScore = maxCombinedMatchScore,
                    playerDisplayNames = playerDisplayNames,
                    roundNumber = round.roundNumber,
                    courtNumber = courtIndex + 1,
                    raw = raw,
                )
            }
            RoundValiditySection(roundNumber = round.roundNumber, matches = auditedMatches)
        }
        val allValid: Boolean = sections.flatMap { it.matches }.all { row: AuditedMatchRow -> row.isValid }
        return MatchValidityAudit(
            tournamentTitle = tournamentTitle,
            maxCombinedMatchScore = maxCombinedMatchScore,
            rounds = sections,
            allMatchesValid = allValid,
        )
    }

    private fun auditSingleMatch(
        maxCombinedMatchScore: Int,
        playerDisplayNames: List<String>,
        roundNumber: Int,
        courtNumber: Int,
        raw: RawMatchForAudit,
    ): AuditedMatchRow {
        val issues: MutableList<MatchValidityIssue> = mutableListOf()
        val indices: List<Int> = listOf(
            raw.playerA1Index,
            raw.playerA2Index,
            raw.playerB1Index,
            raw.playerB2Index,
        )
        if (indices.any { index: Int -> index !in playerDisplayNames.indices }) {
            issues.add(MatchValidityIssue.INVALID_PLAYER_INDEX)
        }
        if (indices.toSet().size != 4) {
            issues.add(MatchValidityIssue.DUPLICATE_PLAYERS_ON_COURT)
        }
        if (raw.playerA1Index == raw.playerA2Index) {
            issues.add(MatchValidityIssue.SAME_PLAYER_TWICE_ON_TEAM_A)
        }
        if (raw.playerB1Index == raw.playerB2Index) {
            issues.add(MatchValidityIssue.SAME_PLAYER_TWICE_ON_TEAM_B)
        }
        if (raw.isScoreSet) {
            if (raw.scoreA < 0 || raw.scoreB < 0) {
                issues.add(MatchValidityIssue.NEGATIVE_SCORE)
            }
            if (raw.scoreA + raw.scoreB > maxCombinedMatchScore) {
                issues.add(MatchValidityIssue.COMBINED_SCORE_EXCEEDS_LIMIT)
            }
        }
        fun nameAt(index: Int): String = playerDisplayNames.getOrNull(index) ?: "?"
        val teamA: String = "${nameAt(raw.playerA1Index)} & ${nameAt(raw.playerA2Index)}"
        val teamB: String = "${nameAt(raw.playerB1Index)} & ${nameAt(raw.playerB2Index)}"
        val scoreA: Int = raw.scoreA
        val scoreB: Int = raw.scoreB
        return AuditedMatchRow(
            matchId = raw.matchId,
            roundNumber = roundNumber,
            courtNumber = courtNumber,
            teamAPairLabel = teamA,
            teamBPairLabel = teamB,
            scoreA = scoreA,
            scoreB = scoreB,
            isScoreEntered = raw.isScoreSet,
            issues = issues.distinct(),
        )
    }
}
