package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.TournamentMatchRecord
import com.example.padlecano.domain.model.TournamentRoundResults
import com.example.padlecano.domain.model.TournamentShareTextLabels
import com.example.padlecano.domain.model.TournamentType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class TournamentShareTextFormatterTest {

    private val labels: TournamentShareTextLabels = TournamentShareTextLabels(
        untitledTournament = "Untitled tournament",
        tournamentTypeAmericano = "Americano",
        appFooter = "Padlecano",
        standingsSectionTitle = "STANDINGS",
        matchesSectionTitle = "MATCHES",
        rankColumn = "#",
        playerColumn = "Player",
        pointsColumn = "Pts",
        gamesColumn = "Games",
        winsColumn = "Wins",
        roundHeaderFormat = "Round %d of %d",
        courtLabelFormat = "Court %d",
        teamVsFormat = "%s vs %s",
        scoreLineFormat = "%d : %d",
        scoreNotEntered = "Not entered yet",
        teamPairSeparator = " & ",
    )

    @Test
    fun format_usesUntitledFallbackWhenTitleBlank() {
        val actualText: String = TournamentShareTextFormatter.format(
            tournamentTitle = "   ",
            tournamentType = TournamentType.AMERICANO,
            playerNames = listOf("Alice"),
            standingsRows = listOf(
                PlayerStandingRow(
                    playerIndex = 0,
                    displayName = "Alice",
                    totalPoints = 10,
                    matchesPlayed = 2,
                    wins = 1,
                ),
            ),
            rounds = emptyList(),
            labels = labels,
        )
        assertTrue(actualText.startsWith("Untitled tournament"))
        assertTrue(actualText.contains("Americano · Padlecano"))
    }

    @Test
    fun format_alignsStandingsColumns() {
        val actualText: String = TournamentShareTextFormatter.format(
            tournamentTitle = "Test Cup",
            tournamentType = TournamentType.AMERICANO,
            playerNames = listOf("Alice", "Bob"),
            standingsRows = listOf(
                PlayerStandingRow(0, "Alice", 42, 6, 4),
                PlayerStandingRow(1, "Bob", 8, 2, 1),
            ),
            rounds = emptyList(),
            labels = labels,
        )
        val lines: List<String> = actualText.lines()
        val headerIndex: Int = lines.indexOfFirst { it.startsWith("#") }
        assertTrue(headerIndex >= 0)
        assertEquals("#  Player  Pts  Games  Wins", lines[headerIndex])
        assertEquals("1  Alice    42      6     4", lines[headerIndex + 1])
        assertEquals("2  Bob       8      2     1", lines[headerIndex + 2])
    }

    @Test
    fun format_includesRoundsAndScores() {
        val actualText: String = TournamentShareTextFormatter.format(
            tournamentTitle = "Saturday",
            tournamentType = TournamentType.AMERICANO,
            playerNames = listOf("Alice", "Bob", "Carol", "Dave"),
            standingsRows = emptyList(),
            rounds = listOf(
                TournamentRoundResults(
                    roundNumber = 1,
                    matches = listOf(
                        TournamentMatchRecord(0, 1, 2, 3, 6, 2, true),
                    ),
                ),
                TournamentRoundResults(
                    roundNumber = 2,
                    matches = listOf(
                        TournamentMatchRecord(0, 2, 1, 3, 0, 0, false),
                    ),
                ),
            ),
            labels = labels,
        )
        assertTrue(actualText.contains("Round 1 of 2"))
        assertTrue(actualText.contains("  Court 1: Alice & Bob vs Carol & Dave — 6 : 2"))
        assertTrue(actualText.contains("Round 2 of 2"))
        assertTrue(actualText.contains("  Court 1: Alice & Carol vs Bob & Dave — Not entered yet"))
    }
}
