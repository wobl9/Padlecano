package com.example.padlecano.domain.usecase

/**
 * Generates an Americano padel schedule using the circle (round-robin) method.
 *
 * For [playerCount] players (must be a positive multiple of 4):
 * - Produces [playerCount] - 1 rounds.
 * - Every round: [playerCount] / 4 courts, all players participate.
 * - Each player partners with every other player exactly once.
 * - Court pairing within each round prefers opponents not yet faced.
 *
 * Algorithm: fix player index 0, rotate indices 1..[playerCount]-1 by one
 * position per round to build partner pairs. Then search court pairings across
 * rounds so every player faces every other player at least once.
 */
object AmericanoScheduleGenerator {
    data class MatchSetup(
        val playerA1: Int,
        val playerA2: Int,
        val playerB1: Int,
        val playerB2: Int,
    )

    fun generate(playerCount: Int): List<List<MatchSetup>> {
        require(playerCount >= 4 && playerCount % 4 == 0) {
            "Player count must be a positive multiple of 4, got $playerCount"
        }
        val partnerRounds: List<List<Pair<Int, Int>>> = (0 until playerCount - 1).map { roundIndex: Int ->
            buildPartnerPairs(playerCount, roundIndex)
        }
        val courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>> =
            partnerRounds.map { partnerPairs: List<Pair<Int, Int>> ->
                buildAllCourtPairings(partnerPairs)
            }
        val opponentCounts: Array<IntArray> = Array(playerCount) { IntArray(playerCount) }
        val selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = mutableListOf()
        val schedule: List<List<MatchSetup>>? = searchSchedule(
            playerCount = playerCount,
            roundIndex = 0,
            courtOptionsPerRound = courtOptionsPerRound,
            opponentCounts = opponentCounts,
            selectedCourts = selectedCourts,
        )
        requireNotNull(schedule) {
            "Could not build a schedule with full opponent coverage for $playerCount players"
        }
        return schedule
    }

    private fun searchSchedule(
        playerCount: Int,
        roundIndex: Int,
        courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
        opponentCounts: Array<IntArray>,
        selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>,
    ): List<List<MatchSetup>>? {
        if (roundIndex == playerCount - 1) {
            if (!hasFullOpponentCoverage(opponentCounts, playerCount)) {
                return null
            }
            return selectedCourts.map { roundCourts: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                roundCourts.map { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
                    val pairA: Pair<Int, Int> = court.first
                    val pairB: Pair<Int, Int> = court.second
                    MatchSetup(
                        playerA1 = pairA.first,
                        playerA2 = pairA.second,
                        playerB1 = pairB.first,
                        playerB2 = pairB.second,
                    )
                }
            }
        }
        val sortedOptions: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> =
            courtOptionsPerRound[roundIndex].sortedWith(
                compareBy<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                    -countNewOpponents(option, opponentCounts)
                }.thenBy { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                    totalCourtCost(option, opponentCounts)
                },
            )
        for (courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in sortedOptions) {
            val nextCounts: Array<IntArray> = cloneOpponentCounts(opponentCounts, playerCount)
            applyCourtPairings(courtPairings, nextCounts)
            selectedCourts.add(courtPairings)
            val result: List<List<MatchSetup>>? = searchSchedule(
                playerCount = playerCount,
                roundIndex = roundIndex + 1,
                courtOptionsPerRound = courtOptionsPerRound,
                opponentCounts = nextCounts,
                selectedCourts = selectedCourts,
            )
            if (result != null) {
                return result
            }
            selectedCourts.removeAt(selectedCourts.lastIndex)
        }
        return null
    }

    private fun buildPartnerPairs(playerCount: Int, roundIndex: Int): List<Pair<Int, Int>> {
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

    private fun buildAllCourtPairings(
        partnerPairs: List<Pair<Int, Int>>,
    ): List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
        if (partnerPairs.isEmpty()) {
            return listOf(emptyList())
        }
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
            val subPairings: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = buildAllCourtPairings(rest)
            for (sub: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in subPairings) {
                results.add(listOf(firstPair to opponentPair) + sub)
            }
        }
        return results
    }

    private fun countNewOpponents(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        opponentCounts: Array<IntArray>,
    ): Int {
        return courtPairings.sumOf { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
            val pairA: Pair<Int, Int> = court.first
            val pairB: Pair<Int, Int> = court.second
            val playersA: List<Int> = listOf(pairA.first, pairA.second)
            val playersB: List<Int> = listOf(pairB.first, pairB.second)
            playersA.sumOf { playerA: Int ->
                playersB.count { playerB: Int ->
                    playerA != playerB && opponentCounts[playerA][playerB] == 0
                }
            }
        }
    }

    private fun totalCourtCost(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        opponentCounts: Array<IntArray>,
    ): Int {
        return courtPairings.sumOf { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
            courtOpponentCost(court.first, court.second, opponentCounts)
        }
    }

    private fun courtOpponentCost(
        pairA: Pair<Int, Int>,
        pairB: Pair<Int, Int>,
        opponentCounts: Array<IntArray>,
    ): Int {
        val playersA: List<Int> = listOf(pairA.first, pairA.second)
        val playersB: List<Int> = listOf(pairB.first, pairB.second)
        return playersA.sumOf { playerA: Int ->
            playersB.sumOf { playerB: Int ->
                if (playerA == playerB) {
                    0
                } else {
                    opponentCounts[playerA][playerB]
                }
            }
        }
    }

    private fun applyCourtPairings(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        opponentCounts: Array<IntArray>,
    ) {
        for (court: Pair<Pair<Int, Int>, Pair<Int, Int>> in courtPairings) {
            val teamA: List<Int> = listOf(court.first.first, court.first.second)
            val teamB: List<Int> = listOf(court.second.first, court.second.second)
            for (playerA: Int in teamA) {
                for (playerB: Int in teamB) {
                    if (playerA != playerB) {
                        opponentCounts[playerA][playerB]++
                        opponentCounts[playerB][playerA]++
                    }
                }
            }
        }
    }

    private fun cloneOpponentCounts(
        opponentCounts: Array<IntArray>,
        playerCount: Int,
    ): Array<IntArray> {
        return Array(playerCount) { rowIndex: Int -> opponentCounts[rowIndex].clone() }
    }

    private fun hasFullOpponentCoverage(
        opponentCounts: Array<IntArray>,
        playerCount: Int,
    ): Boolean {
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                if (opponentCounts[playerA][playerB] == 0) {
                    return false
                }
            }
        }
        return true
    }
}
