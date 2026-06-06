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
    fun generate_everyPlayerFacesEveryOtherPlayerAsOpponentAtLeastOnce_forAllSupportedSizes() {
        for (playerCount: Int in listOf(4, 8, 12, 16)) {
            assertOpponentCoverage(playerCount)
        }
    }

    @Test
    fun bruteForceSearch_eightPlayersHasFullOpponentCoverageSolution() {
        val playerCount: Int = 8
        val rounds: List<List<Pair<Int, Int>>> = (0 until playerCount - 1).map { roundIndex: Int ->
            buildPartnerPairsForTest(playerCount, roundIndex)
        }
        val optionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>> =
            rounds.map { pairs: List<Pair<Int, Int>> -> buildAllCourtPairingsForTest(pairs) }
        var solutionCount: Int = 0
        searchCourtAssignments(
            playerCount = playerCount,
            roundIndex = 0,
            optionsPerRound = optionsPerRound,
            opponentCounts = Array(playerCount) { IntArray(playerCount) },
            onComplete = { counts: Array<IntArray> ->
                if (hasFullOpponentCoverage(counts, playerCount)) {
                    solutionCount++
                }
            },
        )
        assertTrue("Expected at least one full opponent coverage schedule for 8 players", solutionCount > 0)
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

    private fun assertOpponentCoverage(playerCount: Int) {
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
                    "Players $playerA and $playerB should face each other as opponents in $playerCount-player event",
                    opponentCounts[playerA][playerB] >= 1,
                )
            }
        }
    }

    private fun buildPartnerPairsForTest(playerCount: Int, roundIndex: Int): List<Pair<Int, Int>> {
        val rotated: List<Int> = (0 until playerCount).map { position: Int ->
            if (position == 0) {
                0
            } else {
                ((position - 1 + roundIndex) % (playerCount - 1)) + 1
            }
        }
        return (0 until playerCount / 2).map { index: Int ->
            rotated[index] to rotated[playerCount - 1 - index]
        }
    }

    private fun buildAllCourtPairingsForTest(
        partnerPairs: List<Pair<Int, Int>>,
    ): List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
        if (partnerPairs.size == 2) {
            return listOf(listOf(partnerPairs[0] to partnerPairs[1]))
        }
        val firstPair: Pair<Int, Int> = partnerPairs.first()
        val remainingPairs: List<Pair<Int, Int>> = partnerPairs.drop(1)
        val results: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = mutableListOf()
        for (opponentIndex: Int in remainingPairs.indices) {
            val opponentPair: Pair<Int, Int> = remainingPairs[opponentIndex]
            val rest: List<Pair<Int, Int>> = remainingPairs.filterIndexed { index: Int, _: Pair<Int, Int> ->
                index != opponentIndex
            }
            val subPairings: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> =
                buildAllCourtPairingsForTest(rest)
            for (sub: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in subPairings) {
                results.add(listOf(firstPair to opponentPair) + sub)
            }
        }
        return results
    }

    private fun searchCourtAssignments(
        playerCount: Int,
        roundIndex: Int,
        optionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
        opponentCounts: Array<IntArray>,
        onComplete: (Array<IntArray>) -> Unit,
    ) {
        if (roundIndex == playerCount - 1) {
            onComplete(opponentCounts)
            return
        }
        for (choice: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in optionsPerRound[roundIndex]) {
            val nextCounts: Array<IntArray> = Array(playerCount) { row: Int -> opponentCounts[row].clone() }
            choice.forEach { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
                recordCourtOpponents(nextCounts, court)
            }
            searchCourtAssignments(
                playerCount = playerCount,
                roundIndex = roundIndex + 1,
                optionsPerRound = optionsPerRound,
                opponentCounts = nextCounts,
                onComplete = onComplete,
            )
        }
    }

    private fun hasFullOpponentCoverage(counts: Array<IntArray>, playerCount: Int): Boolean {
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                if (counts[playerA][playerB] == 0) {
                    return false
                }
            }
        }
        return true
    }

    private fun recordCourtOpponents(
        counts: Array<IntArray>,
        court: Pair<Pair<Int, Int>, Pair<Int, Int>>,
    ) {
        val teamA: List<Int> = listOf(court.first.first, court.first.second)
        val teamB: List<Int> = listOf(court.second.first, court.second.second)
        for (playerA: Int in teamA) {
            for (playerB: Int in teamB) {
                if (playerA != playerB) {
                    counts[playerA][playerB]++
                    counts[playerB][playerA]++
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
