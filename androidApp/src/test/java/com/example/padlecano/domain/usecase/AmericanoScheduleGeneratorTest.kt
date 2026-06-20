package com.example.padlecano.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmericanoScheduleGeneratorTest {

    @Test
    fun generate_everyPlayerPartnersWithEveryOtherPlayerExactlyOnce_forAllSupportedSizes() {
        for (playerCount: Int in listOf(4, 8, 12, 16)) {
            assertPartnerCoverage(playerCount)
        }
    }

    @Test
    fun generate_everyPlayerFacesEveryOtherPlayerAtLeastOnce_forAllSupportedSizes() {
        for (playerCount: Int in listOf(4, 8, 12, 16)) {
            assertMinimumOpponentCoverage(playerCount)
        }
    }

    @Test
    fun generate_prefersSpacedSecondOpponentMeetings_forEightPlayers() {
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerCount = 8)
        val opponentRounds: MutableMap<Pair<Int, Int>, MutableList<Int>> = mutableMapOf()
        schedule.forEachIndexed { roundIndex: Int, round: List<AmericanoScheduleGenerator.MatchSetup> ->
            round.forEach { match: AmericanoScheduleGenerator.MatchSetup ->
                recordOpponentRounds(opponentRounds, match, roundIndex)
            }
        }
        var spacedRematchCount: Int = 0
        var consecutiveRematchCount: Int = 0
        for (playerA: Int in 0 until 8) {
            for (playerB: Int in playerA + 1 until 8) {
                val rounds: List<Int> = opponentRounds.getValue(playerA to playerB).sorted()
                if (rounds.size >= 2) {
                    if (rounds[1] - rounds[0] >= 2) {
                        spacedRematchCount++
                    } else {
                        consecutiveRematchCount++
                    }
                }
            }
        }
        assertTrue(
            "Expected at least one spaced opponent rematch for 8 players",
            spacedRematchCount > 0,
        )
        assertTrue(
            "Expected spaced rematches to outnumber consecutive rematches",
            spacedRematchCount >= consecutiveRematchCount,
        )
    }

    @Test
    fun generate_producesExpectedRoundAndCourtCounts() {
        for (playerCount: Int in listOf(4, 8, 12)) {
            val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
                AmericanoScheduleGenerator.generate(playerCount)
            assertEquals(playerCount - 1, schedule.size)
            schedule.forEach { round: List<AmericanoScheduleGenerator.MatchSetup> ->
                assertEquals(playerCount / 4, round.size)
            }
        }
    }

    @Test
    fun generate_verifyScheduleAgainstIdeal_passesForGeneratedSchedule() {
        for (playerCount: Int in listOf(4, 8, 12, 16)) {
            val names: List<String> = (0 until playerCount).map { index: Int -> "P$index" }
            val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
                AmericanoScheduleGenerator.generate(playerCount)
            val records = schedule.flatMap { round: List<AmericanoScheduleGenerator.MatchSetup> ->
                round.map { match: AmericanoScheduleGenerator.MatchSetup ->
                    com.example.padlecano.domain.model.TournamentMatchRecord(
                        playerA1Index = match.playerA1,
                        playerA2Index = match.playerA2,
                        playerB1Index = match.playerB1,
                        playerB2Index = match.playerB2,
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
            assertTrue(
                "Schedule verification failed for $playerCount players",
                report.allOk,
            )
        }
    }

    private fun assertPartnerCoverage(playerCount: Int) {
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerCount)
        val partnerCounts: Array<IntArray> = Array(playerCount) { IntArray(playerCount) }
        schedule.forEach { round: List<AmericanoScheduleGenerator.MatchSetup> ->
            round.forEach { match: AmericanoScheduleGenerator.MatchSetup ->
                recordPartner(partnerCounts, match.playerA1, match.playerA2)
                recordPartner(partnerCounts, match.playerB1, match.playerB2)
            }
        }
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                assertEquals(
                    "Players $playerA and $playerB should partner exactly once in $playerCount-player event",
                    1,
                    partnerCounts[playerA][playerB],
                )
            }
        }
    }

    private fun assertMinimumOpponentCoverage(playerCount: Int) {
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerCount)
        val opponentCounts: Array<IntArray> = Array(playerCount) { IntArray(playerCount) }
        schedule.forEach { round: List<AmericanoScheduleGenerator.MatchSetup> ->
            round.forEach { match: AmericanoScheduleGenerator.MatchSetup ->
                recordOpponents(opponentCounts, match)
            }
        }
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                assertTrue(
                    "Players $playerA and $playerB should face each other in $playerCount-player event",
                    opponentCounts[playerA][playerB] >= 1,
                )
            }
        }
    }

    private fun recordOpponentRounds(
        opponentRounds: MutableMap<Pair<Int, Int>, MutableList<Int>>,
        match: AmericanoScheduleGenerator.MatchSetup,
        roundIndex: Int,
    ) {
        val teamA: List<Int> = listOf(match.playerA1, match.playerA2)
        val teamB: List<Int> = listOf(match.playerB1, match.playerB2)
        for (playerA: Int in teamA) {
            for (playerB: Int in teamB) {
                if (playerA != playerB) {
                    val key: Pair<Int, Int> = if (playerA < playerB) playerA to playerB else playerB to playerA
                    val rounds: MutableList<Int> = opponentRounds.getOrPut(key) { mutableListOf() }
                    rounds.add(roundIndex)
                }
            }
        }
    }

    private fun recordPartner(counts: Array<IntArray>, playerA: Int, playerB: Int) {
        val first: Int = minOf(playerA, playerB)
        val second: Int = maxOf(playerA, playerB)
        counts[first][second]++
    }

    private fun recordOpponents(
        counts: Array<IntArray>,
        match: AmericanoScheduleGenerator.MatchSetup,
    ) {
        val teamA: List<Int> = listOf(match.playerA1, match.playerA2)
        val teamB: List<Int> = listOf(match.playerB1, match.playerB2)
        for (playerA: Int in teamA) {
            for (playerB: Int in teamB) {
                if (playerA != playerB) {
                    val first: Int = minOf(playerA, playerB)
                    val second: Int = maxOf(playerA, playerB)
                    counts[first][second]++
                }
            }
        }
    }
}
