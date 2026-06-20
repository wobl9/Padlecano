package com.example.padlecano.domain

import com.example.padlecano.domain.model.AuditedMatchRow
import com.example.padlecano.domain.model.MatchValidityAudit
import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.ScheduleVerificationReport
import com.example.padlecano.domain.usecase.AmericanoScheduleGenerator

object GoldenSnapshotFormatter {

    fun formatSchedule(
        playerCount: Int,
        schedule: List<List<AmericanoScheduleGenerator.MatchSetup>>,
    ): String {
        val lines: MutableList<String> = mutableListOf()
        lines.add("players=$playerCount")
        schedule.forEachIndexed { roundIndex: Int, round: List<AmericanoScheduleGenerator.MatchSetup> ->
            val courts: String = round.joinToString(separator = " ") { match: AmericanoScheduleGenerator.MatchSetup ->
                "${match.playerA1},${match.playerA2}|${match.playerB1},${match.playerB2}"
            }
            lines.add("round=${roundIndex + 1}: $courts")
        }
        return lines.joinToString(separator = "\n")
    }

    fun formatStandings(rows: List<PlayerStandingRow>): String {
        return rows.joinToString(separator = "\n") { row: PlayerStandingRow ->
            "${row.displayName}: points=${row.totalPoints}, played=${row.matchesPlayed}, wins=${row.wins}"
        }
    }

    fun formatScheduleVerification(report: ScheduleVerificationReport): String {
        val lines: MutableList<String> = mutableListOf()
        lines.add("allOk=${report.allOk}")
        report.byPlayer.forEach { playerReport ->
            lines.add("player=${playerReport.playerIndex}")
            playerReport.withEachOtherPlayer.forEach { check ->
                lines.add(
                    "  vs${check.otherPlayerIndex}: partner=${check.timesAsPartner}/${check.expectedTimesAsPartner} " +
                        "opponent=${check.timesAsOpponent}/${check.expectedTimesAsOpponent}",
                )
            }
        }
        return lines.joinToString(separator = "\n")
    }

    fun formatMatchValidityAudit(audit: MatchValidityAudit): String {
        val lines: MutableList<String> = mutableListOf()
        lines.add("title=${audit.tournamentTitle}")
        lines.add("maxCombined=${audit.maxCombinedMatchScore}")
        lines.add("allValid=${audit.allMatchesValid}")
        audit.rounds.forEach { section ->
            lines.add("round=${section.roundNumber}")
            section.matches.forEach { row: AuditedMatchRow ->
                lines.add(formatAuditedMatchRow(row))
            }
        }
        return lines.joinToString(separator = "\n")
    }

    private fun formatAuditedMatchRow(row: AuditedMatchRow): String {
        val issues: String = row.issues.joinToString(separator = ",") { issue -> issue.name }
        return "  court=${row.courtNumber} A=${row.teamAPairLabel} B=${row.teamBPairLabel} " +
            "score=${row.scoreA}:${row.scoreB} entered=${row.isScoreEntered} valid=${row.isValid} issues=[$issues]"
    }
}
