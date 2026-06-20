package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.TournamentMatchRecord
import com.example.padlecano.domain.model.TournamentRoundResults
import com.example.padlecano.domain.model.TournamentShareTextLabels
import com.example.padlecano.domain.model.TournamentType
import java.util.Locale

object TournamentShareTextFormatter {

    fun format(
        tournamentTitle: String,
        tournamentType: TournamentType,
        playerNames: List<String>,
        standingsRows: List<PlayerStandingRow>,
        rounds: List<TournamentRoundResults>,
        labels: TournamentShareTextLabels,
    ): String {
        val lines: MutableList<String> = mutableListOf()
        val resolvedTitle: String = tournamentTitle.trim().ifBlank { labels.untitledTournament }
        lines.add(resolvedTitle)
        lines.add(buildSubtitle(tournamentType = tournamentType, labels = labels))
        lines.add("")
        lines.add(labels.standingsSectionTitle)
        lines.addAll(formatStandings(rows = standingsRows, labels = labels))
        lines.add("")
        lines.add(labels.matchesSectionTitle)
        lines.addAll(formatMatches(playerNames = playerNames, rounds = rounds, labels = labels))
        return lines.joinToString(separator = "\n").trimEnd()
    }

    private fun buildSubtitle(tournamentType: TournamentType, labels: TournamentShareTextLabels): String {
        val typeLabel: String = when (tournamentType) {
            TournamentType.AMERICANO -> labels.tournamentTypeAmericano
        }
        return "$typeLabel · ${labels.appFooter}"
    }

    private fun formatStandings(rows: List<PlayerStandingRow>, labels: TournamentShareTextLabels): List<String> {
        if (rows.isEmpty()) {
            return emptyList()
        }
        val rankWidth: Int = rows.size.toString().length.coerceAtLeast(labels.rankColumn.length)
        val playerWidth: Int = rows.maxOf { it.displayName.length }.coerceAtLeast(labels.playerColumn.length)
        val pointsWidth: Int = rows.maxOf { it.totalPoints.toString().length }.coerceAtLeast(labels.pointsColumn.length)
        val gamesWidth: Int = rows.maxOf { it.matchesPlayed.toString().length }.coerceAtLeast(labels.gamesColumn.length)
        val winsWidth: Int = rows.maxOf { it.wins.toString().length }.coerceAtLeast(labels.winsColumn.length)
        val header: String = listOf(
            labels.rankColumn.padEnd(rankWidth),
            labels.playerColumn.padEnd(playerWidth),
            labels.pointsColumn.padStart(pointsWidth),
            labels.gamesColumn.padStart(gamesWidth),
            labels.winsColumn.padStart(winsWidth),
        ).joinToString(separator = "  ")
        val dataLines: List<String> = rows.mapIndexed { index: Int, row: PlayerStandingRow ->
            listOf(
                (index + 1).toString().padEnd(rankWidth),
                row.displayName.padEnd(playerWidth),
                row.totalPoints.toString().padStart(pointsWidth),
                row.matchesPlayed.toString().padStart(gamesWidth),
                row.wins.toString().padStart(winsWidth),
            ).joinToString(separator = "  ")
        }
        return listOf(header) + dataLines
    }

    private fun formatMatches(
        playerNames: List<String>,
        rounds: List<TournamentRoundResults>,
        labels: TournamentShareTextLabels,
    ): List<String> {
        if (rounds.isEmpty()) {
            return emptyList()
        }
        val totalRounds: Int = rounds.size
        val lines: MutableList<String> = mutableListOf()
        for (round: TournamentRoundResults in rounds) {
            lines.add(formatString(labels.roundHeaderFormat, round.roundNumber, totalRounds))
            round.matches.forEachIndexed { courtIndex: Int, match: TournamentMatchRecord ->
                lines.add(formatMatchLine(
                    courtNumber = courtIndex + 1,
                    match = match,
                    playerNames = playerNames,
                    labels = labels,
                ))
            }
        }
        return lines
    }

    private fun formatMatchLine(
        courtNumber: Int,
        match: TournamentMatchRecord,
        playerNames: List<String>,
        labels: TournamentShareTextLabels,
    ): String {
        val courtLabel: String = formatString(labels.courtLabelFormat, courtNumber)
        val teamA: String = formatTeamPair(
            playerNames = playerNames,
            playerIndexA = match.playerA1Index,
            playerIndexB = match.playerA2Index,
            separator = labels.teamPairSeparator,
        )
        val teamB: String = formatTeamPair(
            playerNames = playerNames,
            playerIndexA = match.playerB1Index,
            playerIndexB = match.playerB2Index,
            separator = labels.teamPairSeparator,
        )
        val teamsLine: String = formatString(labels.teamVsFormat, teamA, teamB)
        val scoreLine: String = if (match.isScoreSet) {
            formatString(labels.scoreLineFormat, match.scoreA, match.scoreB)
        } else {
            labels.scoreNotEntered
        }
        return "  $courtLabel: $teamsLine — $scoreLine"
    }

    private fun formatTeamPair(
        playerNames: List<String>,
        playerIndexA: Int,
        playerIndexB: Int,
        separator: String,
    ): String {
        val nameA: String = playerNames.getOrNull(playerIndexA) ?: "?"
        val nameB: String = playerNames.getOrNull(playerIndexB) ?: "?"
        return "$nameA$separator$nameB"
    }

    private fun formatString(format: String, vararg args: Any): String {
        return String.format(Locale.getDefault(), format, *args)
    }
}
