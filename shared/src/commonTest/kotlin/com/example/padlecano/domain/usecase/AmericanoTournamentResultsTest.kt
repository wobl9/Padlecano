package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.model.TournamentMatchRecord
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class AmericanoTournamentResultsTest {

    @Test
    fun verifyScheduleAgainstIdeal_matchesIdealAmericanoScheduleForFourPlayers() {
        val names: List<String> = listOf("A", "B", "C", "D")
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(names.size)
        val records: List<TournamentMatchRecord> = schedule.flatMap { round: List<AmericanoScheduleGenerator.MatchSetup> ->
            round.map { m: AmericanoScheduleGenerator.MatchSetup ->
                TournamentMatchRecord(
                    playerA1Index = m.playerA1,
                    playerA2Index = m.playerA2,
                    playerB1Index = m.playerB1,
                    playerB2Index = m.playerB2,
                    scoreA = 0,
                    scoreB = 0,
                    isScoreSet = false,
                )
            }
        }
        val report = AmericanoTournamentResults.verifyScheduleAgainstIdeal(
            playerNames = names,
            allScheduledMatches = records,
        )
        assertTrue(report.allOk)
        assertEquals(4, report.byPlayer.size)
    }

    @Test
    fun computeStandings_sumsTeamPointsAndWins() {
        val names: List<String> = listOf("A", "B", "C", "D")
        val matches: List<TournamentMatchRecord> = listOf(
            TournamentMatchRecord(0, 1, 2, 3, 6, 2, true),
        )
        val rows = AmericanoTournamentResults.computeStandings(names, matches)
        assertEquals(6, rows[0].totalPoints)
        assertEquals(6, rows[1].totalPoints)
        assertEquals(2, rows[2].totalPoints)
        assertEquals(2, rows[3].totalPoints)
        assertEquals(1, rows[0].wins)
        assertEquals(1, rows[1].wins)
        assertEquals(0, rows[2].wins)
        assertEquals(0, rows[3].wins)
        assertEquals(1, rows[0].matchesPlayed)
    }
}
